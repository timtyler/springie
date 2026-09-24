// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.springie.render.RendererDelegator;

/**
 * The tile size is prime, so no pixellation factor divides it evenly and
 * the old render scale (aa / px) was never the exact inverse of the
 * blit's upscale: the tile's [0, block_size) mapped to a fractional
 * coarse range, the last coarse row/column held only a sliver of content
 * which the rasterizer skipped, and the upscale sampled those unpainted
 * pixels as a dark seam along the tile's bottom and right edges. The
 * render scale is now coarse_w * aa / block_size, the exact inverse of
 * the upscale, so every sampled coarse pixel is fully rasterized. This
 * test renders a solid rect across a tile seam and checks the rows on
 * either side of it came out the rect's colour.
 */
public class RendererTileManagerPx3SeamTest {

  private static RendererTile tileAt(RendererTileManager manager, int x,
      int y) {
    try {
      final Field field = RendererTileManager.class.getDeclaredField("array");
      field.setAccessible(true);
      return ((RendererTile[][]) field.get(manager))[x][y];
    } catch (final ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static void setLastField(RendererTileManager manager, String name,
      int value) {
    try {
      final Field field = RendererTileManager.class.getDeclaredField(name);
      field.setAccessible(true);
      field.setInt(manager, value);
    } catch (final ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void px3LeavesNoSeamAlongTileEdges() throws Exception {
    final int saved_pixellation = RendererDelegator.pixellation;
    final int saved_frame = RendererTileManager.render_frame;
    final int block_size = RendererTileManager.divisor;
    final int coarse = (block_size + 3 - 1) / 3;
    RendererDelegator.pixellation = 3;
    // renderTiled is normally reached via render(), which bumps the frame
    // counter that the polygon colour cache keys off.
    RendererTileManager.render_frame = saved_frame + 1;
    try {
      final RendererTileManager tiles = new RendererTileManager();
      final RendererTileManager tiles_last = new RendererTileManager();
      tiles.resize(64, block_size * 2);
      tiles_last.resize(64, block_size * 2);

      // A solid red rect crossing the horizontal tile seam at
      // y = block_size, kept inside x < block_size so only that seam is
      // involved.
      final PolygonComposite polygon = new PolygonComposite(
          new PolygonObject2D[] {
              new PolygonObject2D(new int[] { 100, 200, 200, 100 },
                  new int[] { block_size - 10, block_size - 10,
                      block_size + 10, block_size + 10 },
                  0xFFFF0000), },
          0);
      for (int j = 0; j <= 1; j++) {
        final RendererTile tile = tileAt(tiles, 0, j);
        tile.image =
            new BufferedImage(coarse, coarse, BufferedImage.TYPE_INT_RGB);
        tile.vector.add(polygon);
      }
      setLastField(tiles, "last_block_size", block_size);
      setLastField(tiles, "last_antialiasing", 1);
      setLastField(tiles, "last_pixellation", 3);

      final BufferedImage screen =
          new BufferedImage(block_size, block_size * 2,
              BufferedImage.TYPE_INT_RGB);
      final Graphics graphics = screen.getGraphics();

      final Method render_tiled = RendererTileManager.class.getDeclaredMethod(
          "renderTiled", RendererTileManager.class, Graphics.class,
          int.class);
      render_tiled.setAccessible(true);
      render_tiled.invoke(tiles, tiles_last, graphics, block_size);

      // Control: the polygon painted at all.
      assertEquals(0xFFFF0000, screen.getRGB(150, block_size - 5),
          "the test polygon did not render");
      // The rows around the seam: the last coarse row of the upper tile
      // upscales onto the rows just above it. Before the fix these
      // sampled the unpainted sliver row and came out background-black.
      for (int y = block_size - 4; y <= block_size + 4; y++) {
        assertEquals(0xFFFF0000, screen.getRGB(150, y),
            "3x3 pixellation left a dark seam at screen row " + y);
      }
    } finally {
      RendererDelegator.pixellation = saved_pixellation;
      RendererTileManager.render_frame = saved_frame;
    }
  }
}
