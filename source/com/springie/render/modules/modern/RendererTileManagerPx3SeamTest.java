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
 * At 3x3 pixellation the render scale (1/3) and the blit's upscale
 * (114 coarse pixels back onto the 340px tile) were not exact inverses:
 * the tile's [0, 340) mapped to coarse [0, 113.33), so the last coarse
 * row/column held only a fractional sliver of content which the
 * rasterizer skipped, and the upscale sampled those unpainted pixels as
 * a dark 1-coarse-pixel seam along the tile's bottom and right edges.
 * The render scale is now coarse_w * aa / block_size, the exact inverse
 * of the upscale, so every sampled coarse pixel is fully rasterized.
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
    RendererDelegator.pixellation = 3;
    // renderTiled is normally reached via render(), which bumps the frame
    // counter that the polygon colour cache keys off.
    RendererTileManager.render_frame = saved_frame + 1;
    try {
      final RendererTileManager tiles = new RendererTileManager();
      final RendererTileManager tiles_last = new RendererTileManager();
      tiles.resize(64, 700);
      tiles_last.resize(64, 700);

      // A solid red rect crossing the horizontal tile seam at y = 340,
      // kept inside x < 340 so only that seam is involved.
      final PolygonComposite polygon = new PolygonComposite(
          new PolygonObject2D[] {
              new PolygonObject2D(new int[] { 100, 200, 200, 100 },
                  new int[] { 330, 330, 350, 350 }, 0xFFFF0000), },
          0);
      for (int j = 0; j <= 1; j++) {
        final RendererTile tile = tileAt(tiles, 0, j);
        tile.image =
            new BufferedImage(114, 114, BufferedImage.TYPE_INT_RGB);
        tile.vector.add(polygon);
      }
      setLastField(tiles, "last_block_size", 340);
      setLastField(tiles, "last_antialiasing", 1);
      setLastField(tiles, "last_pixellation", 3);

      final BufferedImage screen =
          new BufferedImage(340, 680, BufferedImage.TYPE_INT_RGB);
      final Graphics graphics = screen.getGraphics();

      final Method render_tiled = RendererTileManager.class.getDeclaredMethod(
          "renderTiled", RendererTileManager.class, Graphics.class,
          int.class);
      render_tiled.setAccessible(true);
      render_tiled.invoke(tiles, tiles_last, graphics, 340);

      // Control: the polygon painted at all.
      assertEquals(0xFFFF0000, screen.getRGB(150, 335),
          "the test polygon did not render");
      // The seam band: the last coarse row of the upper tile upscales
      // onto screen rows 337-339. Before the fix these sampled the
      // unpainted sliver row and came out background-black.
      for (int y = 337; y <= 339; y++) {
        assertEquals(0xFFFF0000, screen.getRGB(150, y),
            "3x3 pixellation left a dark seam at screen row " + y);
      }
      assertEquals(0xFFFF0000, screen.getRGB(150, 341),
          "the lower tile's content did not render");
    } finally {
      RendererDelegator.pixellation = saved_pixellation;
      RendererTileManager.render_frame = saved_frame;
    }
  }
}
