// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Container;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;

/**
 * The bottom button bar must span the entire width of the main window in
 * every controls-window docking mode. The docked controls narrow only the
 * canvas (they live in the inner panel's East); the bar stays in the
 * outer South, so its width must equal the window's content width whether
 * the controls are docked, always-on-top, or free floating.
 */
class BottomBarFullWidthTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void restoreAndDispose() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      // Leave the app in its default state for the next test class.
      FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
      FrEnd.applyControlsWindowOptions();
    });
    GuiTestSupport.disposeFrames();
  }

  @Test
  void bottomBarSpansFullWidthWhileDocked() throws Exception {
    applyModeAndResize(FrEnd.CONTROLS_DOCKED);
    final int[] widths = readWidths();
    assertEquals(widths[1], widths[0],
        "docked: bottom bar (" + widths[0] + ") must span the full"
            + " content width (" + widths[1] + ")");
    assertTrue(widths[0] > widths[2],
        "docked: the bar (" + widths[0] + ") must be wider than the"
            + " canvas (" + widths[2] + ") -- the docked controls narrow"
            + " the canvas, not the bar");
    final Container[] parent = new Container[1];
    SwingUtilities.invokeAndWait(
        () -> parent[0] = FrEnd.panel_controls_all.panel.getParent());
    assertEquals(FrEnd.panel_main_area, parent[0],
        "docked controls must live in the inner area's East");
  }

  @Test
  void bottomBarSpansFullWidthWhileAlwaysOnTop() throws Exception {
    applyModeAndResize(FrEnd.CONTROLS_ALWAYS_ON_TOP);
    final int[] widths = readWidths();
    assertEquals(widths[1], widths[0],
        "always-on-top: bottom bar (" + widths[0] + ") must span the"
            + " full content width (" + widths[1] + ")");
  }

  @Test
  void bottomBarSpansFullWidthWhileFreeFloating() throws Exception {
    applyModeAndResize(FrEnd.CONTROLS_FREE_FLOATING);
    final int[] widths = readWidths();
    assertEquals(widths[1], widths[0],
        "free-floating: bottom bar (" + widths[0] + ") must span the"
            + " full content width (" + widths[1] + ")");
  }

  /**
   * Applies a docking mode and sizes the main window at a typical
   * width. Everything happens on the EDT with a validate so the
   * BorderLayout widths are settled before the assertions read them.
   */
  private static void applyModeAndResize(int mode) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = mode;
      FrEnd.applyControlsWindowOptions();
      FrEnd.frame_main.setSize(900, 700);
      FrEnd.frame_main.validate();
    });
  }

  /**
   * Returns {bottom-bar width, main content width, canvas width}.
   */
  private static int[] readWidths() throws Exception {
    final int[] widths = new int[3];
    SwingUtilities.invokeAndWait(() -> {
      widths[0] = FrEnd.panel_with_controls_at_bottom.getWidth();
      widths[1] = FrEnd.frame_main.getWidth()
          - FrEnd.frame_main.getInsets().left
          - FrEnd.frame_main.getInsets().right;
      widths[2] = FrEnd.main_canvas.panel.getWidth();
    });
    return widths;
  }
}
