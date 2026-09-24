// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.springie.render.RendererDelegator;

/**
 * The tiled polygon renderer's blit loop sets the graphics clip to each
 * tile's union rectangle as it blits. It must restore the full canvas clip
 * afterwards: the screen-space painters that run after the renderer (the
 * boundary-box dots, the info button) draw on the same graphics, and a
 * stale tile clip clips them down to almost nothing. Toggling "Show active
 * tiles" used to mask this -- its outline pass resets the clip as a side
 * effect, which is why the dots only appeared with it on.
 */
public class RendererTileManagerClipTest {

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
  public void renderTiledRestoresTheFullCanvasClip() throws Exception {
    final RendererTileManager tiles = new RendererTileManager();
    final RendererTileManager tiles_last = new RendererTileManager();
    tiles.resize(64, 64);
    tiles_last.resize(64, 64);

    // One tile holding a small triangle, with its cached image already
    // built so the render path blits it (which is what stomps the clip
    // onto the tile's union). The last_* fields are pre-seeded so the
    // tile-size check does not drop the image (that path needs the app).
    final RendererTile tile = tileAt(tiles, 0, 0);
    tile.image = new BufferedImage(340, 340, BufferedImage.TYPE_INT_RGB);
    tile.vector.add(new PolygonComposite(
        new PolygonObject2D[] {
            new PolygonObject2D(new int[] { 10, 30, 30 },
                new int[] { 10, 10, 30 }, 0xFF0000), },
        0));
    setLastField(tiles, "last_block_size", 340);
    setLastField(tiles, "last_antialiasing", RendererDelegator.antialiasing);
    setLastField(tiles, "last_pixellation", RendererDelegator.pixellation);

    final BufferedImage screen =
        new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    final Graphics graphics = screen.getGraphics();

    final Method render_tiled = RendererTileManager.class.getDeclaredMethod(
        "renderTiled", RendererTileManager.class, Graphics.class, int.class);
    render_tiled.setAccessible(true);
    render_tiled.invoke(tiles, tiles_last, graphics, 340);

    assertEquals(new Rectangle(0, 0, 9999, 9999), graphics.getClipBounds(),
        "the blit loop must not leak its tile clip to later painters");
  }
}
