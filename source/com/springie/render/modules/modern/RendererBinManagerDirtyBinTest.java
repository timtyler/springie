package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.preferences.Preferences;
import com.springie.render.MainCanvas;
import com.springie.render.RendererDelegator;

/**
 * Tests the exact per-bin content comparison used by dirty-bin skipping.
 * A false "equal" would corrupt the display (skipped redraw of changed
 * content); a false "different" only costs a wasted redraw.
 */
class RendererBinManagerDirtyBinTest {
  private static PolygonComposite composite(int z, int colour, int... coords) {
    final int n = coords.length / 2;
    final int[] x = new int[n];
    final int[] y = new int[n];
    for (int i = 0; i < n; i++) {
      x[i] = coords[2 * i];
      y[i] = coords[2 * i + 1];
    }
    return new PolygonComposite(new PolygonObject2D[] {
        new PolygonObject2D(x, y, colour) }, z);
  }

  private static ArrayList<PolygonComposite> bin(PolygonComposite... composites) {
    final ArrayList<PolygonComposite> v = new ArrayList<>();
    for (final PolygonComposite c : composites) {
      v.add(c);
    }
    return v;
  }

  @Test
  void testEmptyBinsAreEqual() {
    assertTrue(RendererBinManager.binsEqual(bin(), bin()));
  }

  @Test
  void testIdenticalContentIsEqual() {
    assertTrue(RendererBinManager.binsEqual(
        bin(composite(5, 0xFF0000, 1, 2, 3, 4), composite(9, 0x00FF00, 5, 6)),
        bin(composite(5, 0xFF0000, 1, 2, 3, 4), composite(9, 0x00FF00, 5, 6))));
  }

  @Test
  void testDifferentSizeIsNotEqual() {
    assertFalse(RendererBinManager.binsEqual(
        bin(composite(5, 0xFF0000, 1, 2)),
        bin(composite(5, 0xFF0000, 1, 2), composite(9, 0x00FF00, 5, 6))));
  }

  @Test
  void testMovedVertexIsNotEqual() {
    assertFalse(RendererBinManager.binsEqual(
        bin(composite(5, 0xFF0000, 1, 2, 3, 4)),
        bin(composite(5, 0xFF0000, 1, 2, 3, 5))));
  }

  @Test
  void testChangedDepthIsNotEqual() {
    assertFalse(RendererBinManager.binsEqual(
        bin(composite(5, 0xFF0000, 1, 2)),
        bin(composite(6, 0xFF0000, 1, 2))));
  }

  @Test
  void testChangedColourIsNotEqual() {
    assertFalse(RendererBinManager.binsEqual(
        bin(composite(5, 0xFF0000, 1, 2)),
        bin(composite(5, 0x00FF00, 1, 2))));
  }

  @Test
  void testReorderedCompositesCountAsDifferent() {
    // Conservative: equivalent content in a different order is dirty
    // (a wasted redraw, never a missed one).
    final PolygonComposite a = composite(5, 0xFF0000, 1, 2);
    final PolygonComposite b = composite(9, 0x00FF00, 5, 6);
    assertFalse(RendererBinManager.binsEqual(bin(a, b), bin(b, a)));
  }

  @Test
  void testDifferentVertexCountIsNotEqual() {
    assertFalse(RendererBinManager.binsEqual(
        bin(composite(5, 0xFF0000, 1, 2, 3, 4)),
        bin(composite(5, 0xFF0000, 1, 2))));
  }

  @Test
  void testEmptyVsOccupiedIsNotEqual() {
    assertFalse(RendererBinManager.binsEqual(bin(),
        bin(composite(5, 0xFF0000, 1, 2))));
  }

  private static void renderOnce(RendererBinManager bins_current,
      RendererBinManager bins_last, Graphics g, PolygonComposite... content) {
    bins_current.clear();
    for (final PolygonComposite pc : content) {
      bins_current.add(pc);
    }
    g.setClip(0, 0, 9999, 9999);
    bins_current.render(bins_last, g);
    bins_current.rotateFrameState(bins_last);
  }

  private static boolean anyTiles(RendererBinManager manager) throws Exception {
    final Field f = RendererBinManager.class.getDeclaredField("array");
    f.setAccessible(true);
    final RendererBin[][] array = (RendererBin[][]) f.get(manager);
    for (final RendererBin[] row : array) {
      for (final RendererBin bin : row) {
        if (bin.image != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Toggling the double-buffer preference mid-session must not corrupt the
   * display: the direct path paints without tiles, and re-enabling tiling
   * rebuilds every tile (no holes from dropped caches).
   */
  @Test
  void testDoubleBufferPreferenceToggle() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final Frame frame = new Frame("toggle-test");
    final MainCanvas canvas = new MainCanvas(null);
    FrEnd.main_canvas = canvas;
    frame.add(canvas.panel);
    frame.setSize(400, 400);
    frame.setVisible(true);
    ContextManager.setNodeManager(new NodeManager());

    RendererBinManager.divisor = 192;
    RendererBinManager.show_bins = false;
    RendererBinManager.colour_modifier_filled = ColourModifier.natural;
    RendererBinManager.colour_modifier_wireframe = ColourModifier.disabled;
    RendererDelegator.color_background = Color.BLACK;
    FrEnd.redraw_deepest_first = true;

    final RendererBinManager bins_current = new RendererBinManager();
    final RendererBinManager bins_last = new RendererBinManager();
    bins_current.resize(400, 400);
    bins_last.resize(400, 400);

    final BufferedImage screen =
        new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
    final Graphics g = screen.getGraphics();
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, 400, 400);

    final PolygonComposite red =
        composite(5, 0xFFFF0000, 10, 10, 50, 10, 50, 50, 10, 50);

    try {
      renderOnce(bins_current, bins_last, g, red);
      assertEquals(0xFFFF0000, screen.getRGB(30, 30),
          "tiled path paints the square");

      FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
          Boolean.FALSE);
      renderOnce(bins_current, bins_last, g, red);
      assertEquals(0xFFFF0000, screen.getRGB(30, 30),
          "direct path paints the square");
      assertFalse(anyTiles(bins_current),
          "direct path keeps no cached tiles");

      FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
          Boolean.TRUE);
      renderOnce(bins_current, bins_last, g, red);
      assertEquals(0xFFFF0000, screen.getRGB(30, 30),
          "re-enabled tiling rebuilds the tile without holes");
    } finally {
      FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
          Boolean.TRUE);
      frame.dispose();
    }
  }
}
