// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.preferences.Preferences;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.modern.RendererBinManager;

/**
 * "Show active bins" must draw red outlines around the bins that hold
 * content, in both the tiled (double-buffered) and direct render paths,
 * and disappear entirely when switched off. The selected-element colour
 * is also red, so the test compares against a baseline capture rather
 * than asserting on absolute zero.
 *
 * The captures are driven deterministically: instead of sleeping and
 * hoping a repaint happened, each check paints the canvas synchronously
 * and polls until the expected pixels appear (or a deadline passes).
 */
class ShowActiveBinsTest {

  /** How much redder than baseline the outlines must make the capture. */
  private static final int OUTLINE_RED_MARGIN = 100;

  /** How long to keep painting and re-capturing before giving up. */
  private static final long POLL_DEADLINE_MS = 15000;

  /** The most recent Robot capture, kept for failure diagnosis. */
  private BufferedImage last_capture;

  /** The screen area the most recent capture covered. */
  private Rectangle last_capture_area;

  /** The renderer the app booted with, restored after the tests. */
  private static ModularRendererBase saved_renderer;

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
    SwingUtilities.invokeAndWait(() -> {
      saved_renderer = RendererDelegator.renderer;
    });
  }

  /**
   * This test exercises the modern renderer's tiled/direct paths, so pin
   * the renderer before each test instead of depending on the app-wide
   * default (currently the asynchronous ray-traced renderer, whose
   * background frame publishing this test has no business waiting for).
   * Per-test pinning matters because resetPreferences() (called by the
   * first test) re-applies the Display panel default, which would swap
   * the ray tracer back in from under the second test.
   */
  @BeforeEach
  void pinModernRenderer() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = new ModularRendererNew();
      FrEnd.main_canvas.forceResize();
    });
  }

  @AfterAll
  static void dispose() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = saved_renderer;
      FrEnd.main_canvas.forceResize();
    });
    GuiTestSupport.disposeFrames();
  }

  @Test
  void activeBinsDrawRedOutlines() throws Exception {
    final int baseline_red = countRedPixelsAfterPaint();

    setShowActiveBins(true);

    // Tiled (double-buffered) path is the default.
    assertTrue(RendererDelegator.isNewDoubleBuffer());
    waitForRedPixels(baseline_red,
        "red outlines on non-empty bins in the tiled path");

    // Direct path: double-buffering off.
    setDoubleBuffer(false);
    assertFalse(RendererDelegator.isNewDoubleBuffer());
    waitForRedPixels(baseline_red,
        "red outlines on non-empty bins in the direct path");

    // Back to the default path, outlines switched off.
    setDoubleBuffer(true);
    setShowActiveBins(false);
    waitForPixelCount(baseline_red, "no bin outlines once disabled");

    // Reset preferences restores the default (off).
    SwingUtilities.invokeAndWait(() -> {
      RendererBinManager.show_active_bins = true;
      FrEnd.panel_preferences.resetPreferences();
    });
    assertFalse(RendererBinManager.show_active_bins);
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
          Boolean.TRUE);
      RendererBinManager.show_active_bins = false;
    });
  }

  private void setShowActiveBins(boolean state) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererBinManager.show_active_bins = state;
    });
  }

  private void setDoubleBuffer(boolean state) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
          state ? Boolean.TRUE : Boolean.FALSE);
    });
  }

  /**
   * Paints the canvas synchronously on the EDT, exactly as an AWT paint
   * would (forceResize raises the repaint flags that update() consumes).
   */
  private void paintNow() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.main_canvas.forceResize();
      final Graphics g = FrEnd.main_canvas.panel.getGraphics();
      try {
        FrEnd.main_canvas.update(g);
      } finally {
        g.dispose();
      }
    });
  }

  private int countRedPixelsAfterPaint() throws Exception {
    paintNow();
    return countRedPixels();
  }

  /** Polls until the outlines are visible, then returns. */
  private void waitForRedPixels(int baseline_red, String what)
      throws Exception {
    final long deadline = System.currentTimeMillis() + POLL_DEADLINE_MS;
    int red = 0;
    while (System.currentTimeMillis() < deadline) {
      red = countRedPixelsAfterPaint();
      if (red > baseline_red + OUTLINE_RED_MARGIN) {
        return;
      }
      Thread.sleep(250);
    }
    fail(what + ": saw " + red + " red pixels, expected more than "
        + (baseline_red + OUTLINE_RED_MARGIN) + ". Failing capture saved to "
        + saveFailureShot() + " (captured area " + this.last_capture_area
        + ")");
  }

  /**
   * Writes the most recent Robot capture to a PNG in the temp dir, so a
   * failure can be diagnosed by looking at what the screen actually
   * showed (covered window? misaligned capture? empty canvas?).
   */
  private String saveFailureShot() {
    try {
      final File file = new File(System.getProperty("java.io.tmpdir"),
          "show-active-bins-failure.png");
      ImageIO.write(this.last_capture, "png", file);
      return file.getAbsolutePath();
    } catch (Exception e) {
      return "<could not save capture: " + e + ">";
    }
  }

  /** Polls until the capture matches the baseline exactly, then returns. */
  private void waitForPixelCount(int expected_red, String what)
      throws Exception {
    final long deadline = System.currentTimeMillis() + POLL_DEADLINE_MS;
    int red = -1;
    while (System.currentTimeMillis() < deadline) {
      red = countRedPixelsAfterPaint();
      if (red == expected_red) {
        return;
      }
      Thread.sleep(250);
    }
    assertEquals(expected_red, red,
        () -> what + ". Failing capture saved to " + saveFailureShot()
            + " (captured area " + this.last_capture_area + ")");
  }

  private int countRedPixels() throws Exception {
    final Rectangle[] area = new Rectangle[1];
    SwingUtilities.invokeAndWait(() -> {
      // The Robot captures the real screen, so make sure the main window
      // is on top first: a covering window would hide the app (and its
      // outlines) from the capture.
      FrEnd.frame_main.toFront();
      final Point p = FrEnd.main_canvas.panel.getLocationOnScreen();
      area[0] = new Rectangle(p, FrEnd.main_canvas.panel.getSize());
    });
    final BufferedImage image = new Robot().createScreenCapture(area[0]);
    this.last_capture = image;
    this.last_capture_area = area[0];
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

  /**
   * Regression test for "active bins show as active if they have ever been
   * active": the red outline sits at the bin's block border, outside the
   * content union that scrubs and blits cover, so nothing ever erased it.
   * Disabling the option used to leave every outline behind (the old test
   * only passed because it forced a resize, which rebuilds the offscreen
   * image and repaints everything). Here the option is toggled with no
   * resize, in both render paths: the toggle-off frame must widen each
   * bin's painted region to its full block so the outlines are scrubbed
   * away and the red count returns to baseline.
   */
  @Test
  void disablingOutlinesWithoutResizeErasesThem() throws Exception {
    paintNow();
    final int baseline = countRedPixels();

    // Tiled (double-buffered) path.
    setShowActiveBinsNoResize(true);
    waitForRedPixels(baseline, "tiled outlines did not appear");
    setShowActiveBinsNoResize(false);
    waitForPixelCountNoResize(baseline, "tiled outlines did not disappear");

    // Direct path.
    setDoubleBufferedNoResize(false);
    setShowActiveBinsNoResize(true);
    waitForRedPixels(baseline, "direct outlines did not appear");
    setShowActiveBinsNoResize(false);
    waitForPixelCountNoResize(baseline, "direct outlines did not disappear");
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
          Boolean.TRUE);
      RendererBinManager.show_active_bins = false;
    });
  }

  /** Toggles the option without forcing a resize (no full-repaint rescue). */
  private void setShowActiveBinsNoResize(boolean on) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererBinManager.show_active_bins = on;
      RendererDelegator.repaint_some_objects = true;
    });
  }

  private void setDoubleBufferedNoResize(boolean on) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.preferences.map.put(Preferences.renderer_new_double_buffer,
          Boolean.valueOf(on));
      RendererDelegator.repaint_some_objects = true;
    });
  }

  /** Paints one frame without forcing a resize. */
  private void pumpFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.repaint_some_objects = true;
      final Graphics g = FrEnd.main_canvas.panel.getGraphics();
      try {
        FrEnd.main_canvas.update(g);
      } finally {
        g.dispose();
      }
    });
  }

  /** Polls plain (resize-free) frames until the capture matches baseline. */
  private void waitForPixelCountNoResize(int expected_red, String what)
      throws Exception {
    final long deadline = System.currentTimeMillis() + POLL_DEADLINE_MS;
    int red = -1;
    while (System.currentTimeMillis() < deadline) {
      pumpFrame();
      red = countRedPixels();
      if (red == expected_red) {
        return;
      }
      Thread.sleep(250);
    }
    assertEquals(expected_red, red,
        () -> what + ". Failing capture saved to " + saveFailureShot()
            + " (captured area " + this.last_capture_area + ")");
  }
}
