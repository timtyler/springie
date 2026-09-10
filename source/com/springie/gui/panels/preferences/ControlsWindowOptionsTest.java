// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Container;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.gui.GuiTestSupport;

/**
 * The controls-window options on the Preferences tab: stay-on-top and
 * docking with the main window.
 */
class ControlsWindowOptionsTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterEach
  void restoreOptions() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_stay_on_top = true;
      FrEnd.controls_dock_with_main = false;
      FrEnd.applyControlsWindowOptions();
    });
  }

  @AfterAll
  static void disposeFrames() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @Test
  void stayOnTopDefaultsToTrueAndAppliesToFrame() throws Exception {
    final boolean[] states = new boolean[2];
    SwingUtilities.invokeAndWait(() -> {
      states[0] = FrEnd.controls_stay_on_top;
      states[1] = FrEnd.frame_controls.isAlwaysOnTop();
    });
    assertTrue(states[0], "stay on top should default to true");
    assertTrue(states[1], "controls frame should be always-on-top by default");

    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_stay_on_top = false;
      FrEnd.applyControlsWindowOptions();
    });
    final boolean[] off = new boolean[1];
    SwingUtilities.invokeAndWait(
        () -> off[0] = FrEnd.frame_controls.isAlwaysOnTop());
    assertFalse(off[0], "unchecking stay on top should clear the window flag");
  }

  @Test
  void dockingSnapsControlsAgainstMainWindow() throws Exception {
    final int[] geometry = new int[3];
    SwingUtilities.invokeAndWait(() -> {
      // Park the main window where docked controls fit on its right.
      FrEnd.frame_main.setLocation(100, 100);
      FrEnd.controls_dock_with_main = true;
      FrEnd.applyControlsWindowOptions();

      geometry[0] = FrEnd.frame_main.getX();
      geometry[1] = FrEnd.frame_main.getWidth();
      geometry[2] = FrEnd.frame_controls.getX();
    });

    assertEquals(geometry[0] + geometry[1], geometry[2],
        "docking should snap the controls against the main window");
  }

  @Test
  void dockedControlsFollowMainWindowMoves() throws Exception {
    final int[] snapped = new int[2];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_main.setLocation(100, 100);
      FrEnd.controls_dock_with_main = true;
      FrEnd.applyControlsWindowOptions();

      snapped[0] = FrEnd.frame_controls.getX();
      snapped[1] = FrEnd.frame_controls.getY();

      // The move event reaches the dock listener asynchronously.
      FrEnd.frame_main.setLocation(160, 140);
    });

    final int[] followed = waitForControlsAt(snapped[0] + 60, snapped[1] + 40);
    assertEquals(snapped[0] + 60, followed[0],
        "controls should follow the main window by the same x delta");
    assertEquals(snapped[1] + 40, followed[1],
        "controls should follow the main window by the same y delta");
  }

  @Test
  void dockingTwiceDoesNotDoubleTheFollow() throws Exception {
    final int[] snapped = new int[2];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_main.setLocation(100, 100);
      FrEnd.controls_dock_with_main = true;
      FrEnd.applyControlsWindowOptions();
      // A second apply must not attach a second listener.
      FrEnd.applyControlsWindowOptions();

      snapped[0] = FrEnd.frame_controls.getX();
      snapped[1] = FrEnd.frame_controls.getY();

      FrEnd.frame_main.setLocation(160, 140);
    });

    // Two listeners would move the controls by twice the delta.
    final int[] followed = waitForControlsAt(snapped[0] + 60, snapped[1] + 40);
    assertEquals(snapped[0] + 60, followed[0]);
    assertEquals(snapped[1] + 40, followed[1]);
  }

  @Test
  void undockingReleasesTheControlsWindow() throws Exception {
    final int[] snapped = new int[2];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_main.setLocation(100, 100);
      FrEnd.controls_dock_with_main = true;
      FrEnd.applyControlsWindowOptions();

      snapped[0] = FrEnd.frame_controls.getX();
      snapped[1] = FrEnd.frame_controls.getY();

      FrEnd.controls_dock_with_main = false;
      FrEnd.applyControlsWindowOptions();

      FrEnd.frame_main.setLocation(300, 300);
    });

    // Give any stray listener a chance to fire, then check nothing moved.
    Thread.sleep(1000);
    final int[] pos = new int[2];
    SwingUtilities.invokeAndWait(() -> {
      pos[0] = FrEnd.frame_controls.getX();
      pos[1] = FrEnd.frame_controls.getY();
    });
    assertEquals(snapped[0], pos[0],
        "undocked controls should not follow the main window");
    assertEquals(snapped[1], pos[1],
        "undocked controls should not follow the main window");
  }

  @Test
  void preferencesTabHasTheWindowCheckboxes() throws Exception {
    final Checkbox[] boxes = new Checkbox[2];
    SwingUtilities.invokeAndWait(() -> {
      boxes[0] = findCheckbox(FrEnd.panel_preferences.panel,
          GUIStrings.CONTROL_WINDOW_STAY_ON_TOP);
      boxes[1] = findCheckbox(FrEnd.panel_preferences.panel,
          GUIStrings.CONTROL_WINDOW_DOCK);
    });

    assertNotNull(boxes[0], "Preferences tab should have a Stay on top option");
    assertNotNull(boxes[1],
        "Preferences tab should have a Dock with main window option");
    assertTrue(boxes[0].getState(), "Stay on top should start checked");
    assertFalse(boxes[1].getState(), "Dock should start unchecked");
  }

  /**
   * Polls until the controls window reaches the expected position, so the
   * test doesn't depend on how quickly the dock listener's move event is
   * delivered.
   */
  private static int[] waitForControlsAt(int x, int y) throws Exception {
    final long deadline = System.currentTimeMillis() + 10000;
    final int[] pos = new int[2];
    while (true) {
      SwingUtilities.invokeAndWait(() -> {
        pos[0] = FrEnd.frame_controls.getX();
        pos[1] = FrEnd.frame_controls.getY();
      });
      if (pos[0] == x && pos[1] == y) {
        return pos;
      }
      if (System.currentTimeMillis() > deadline) {
        return pos;
      }
      Thread.sleep(100);
    }
  }

  private static Checkbox findCheckbox(Container root, String label) {
    for (final Component c : root.getComponents()) {
      if (c instanceof Checkbox) {
        final Checkbox checkbox = (Checkbox) c;
        if (label.equals(checkbox.getLabel())) {
          return checkbox;
        }
      }
      if (c instanceof Container) {
        final Checkbox found = findCheckbox((Container) c, label);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

}
