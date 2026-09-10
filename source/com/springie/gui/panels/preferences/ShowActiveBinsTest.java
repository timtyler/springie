// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.preferences.Preferences;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.RendererBinManager;

/**
 * "Show active bins" must draw red outlines around the bins that hold
 * content, in both the tiled (double-buffered) and direct render paths,
 * and disappear entirely when switched off. The selected-element colour
 * is also red, so the test compares against a baseline capture rather
 * than asserting on absolute zero.
 */
class ShowActiveBinsTest {

  @Test
  void activeBinsDrawRedOutlines() throws Exception {
    GuiTestSupport.bootApp();
    try {
      final int baseline_red = countRedPixels();

      setShowActiveBins(true);

      // Tiled (double-buffered) path is the default.
      assertTrue(RendererDelegator.isNewDoubleBuffer());
      assertTrue(countRedPixels() > baseline_red + 100,
          "red outlines on non-empty bins in the tiled path");

      // Direct path: double-buffering off.
      SwingUtilities.invokeAndWait(() -> {
        FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
            Boolean.FALSE);
        FrEnd.main_canvas.forceResize();
      });
      Thread.sleep(3000);
      assertFalse(RendererDelegator.isNewDoubleBuffer());
      assertTrue(countRedPixels() > baseline_red + 100,
          "red outlines on non-empty bins in the direct path");

      // Back to the default path, outlines switched off.
      SwingUtilities.invokeAndWait(() -> {
        FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
            Boolean.TRUE);
        FrEnd.main_canvas.forceResize();
      });
      setShowActiveBins(false);
      assertEquals(baseline_red, countRedPixels(),
          "no bin outlines once disabled");

      // Reset preferences restores the default (off).
      SwingUtilities.invokeAndWait(() -> {
        RendererBinManager.show_active_bins = true;
        FrEnd.panel_preferences.resetPreferences();
      });
      assertFalse(RendererBinManager.show_active_bins);
    } finally {
      SwingUtilities.invokeAndWait(() -> {
        FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
            Boolean.TRUE);
        RendererBinManager.show_active_bins = false;
      });
      GuiTestSupport.disposeFrames();
    }
  }

  private void setShowActiveBins(boolean state) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererBinManager.show_active_bins = state;
      FrEnd.main_canvas.forceResize();
    });
    Thread.sleep(3000);
  }

  private int countRedPixels() throws Exception {
    final Rectangle[] area = new Rectangle[1];
    SwingUtilities.invokeAndWait(() -> {
      final Point p = FrEnd.main_canvas.panel.getLocationOnScreen();
      area[0] = new Rectangle(p, FrEnd.main_canvas.panel.getSize());
    });
    final BufferedImage image = new Robot().createScreenCapture(area[0]);
    int count = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        final int rgb = image.getRGB(x, y);
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        if (r > 200 && g < 80 && b < 80) {
          count++;
        }
      }
    }
    return count;
  }
}
