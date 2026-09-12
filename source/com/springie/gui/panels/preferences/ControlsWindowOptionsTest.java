// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

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
  void stayOnTopDefaultsToTrueAndStaysAboveMainWindowOnly() throws Exception {
    final boolean[] states = new boolean[3];
    final int[] listener_count = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      states[0] = FrEnd.controls_stay_on_top;
      // Never system-wide: the controls must not sit above every window
      // on the machine.
      states[1] = FrEnd.frame_controls.isAlwaysOnTop();
      states[2] = FrEnd.isControlsStayOnTopActive();
      listener_count[0] = Toolkit.getDefaultToolkit().getAWTEventListeners().length;
    });
    assertTrue(states[0], "stay on top should default to true");
    assertFalse(states[1], "controls frame must not be always-on-top");
    assertTrue(states[2], "the stay-on-top listener should be installed");

    // Re-applying must not install a second listener.
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.applyControlsWindowOptions();
      listener_count[0] = Toolkit.getDefaultToolkit().getAWTEventListeners().length
          - listener_count[0];
      states[2] = FrEnd.isControlsStayOnTopActive();
    });
    assertEquals(0, listener_count[0], "re-applying must not duplicate it");
    assertTrue(states[2]);

    // A mouse press inside the main window brings the controls forward,
    // without touching the system-wide flag.
    final boolean[] brought_forward = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame real_controls = FrEnd.frame_controls;
      try {
        FrEnd.frame_controls = new Frame() {
          public void toFront() {
            brought_forward[0] = true;
          }

          public boolean isVisible() {
            return true;
          }
        };
        dispatchToStayOnTopListeners(
            mousePress(FrEnd.frame_main));
      } finally {
        FrEnd.frame_controls = real_controls;
      }
    });
    assertTrue(brought_forward[0],
        "pressing inside the main window should bring the controls forward");
    final boolean[] still_not_on_top = new boolean[1];
    SwingUtilities.invokeAndWait(
        () -> still_not_on_top[0] = FrEnd.frame_controls.isAlwaysOnTop());
    assertFalse(still_not_on_top[0]);

    // A press inside the controls window itself must not yank it forward:
    // that is the path menu clicks used to take, via window activation.
    final boolean[] yanked = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame real_controls = FrEnd.frame_controls;
      try {
        FrEnd.frame_controls = new Frame() {
          public void toFront() {
            yanked[0] = true;
          }

          public boolean isVisible() {
            return true;
          }
        };
        dispatchToStayOnTopListeners(
            mousePress(FrEnd.frame_controls));
      } finally {
        FrEnd.frame_controls = real_controls;
      }
    });
    assertFalse(yanked[0],
        "presses outside the main window must not move the controls");

    // A press inside an owned dialog (e.g. a file dialog) must be left
    // alone too.
    final boolean[] disturbed = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame real_controls = FrEnd.frame_controls;
      final Dialog dialog = new Dialog(FrEnd.frame_main, "test", false);
      try {
        FrEnd.frame_controls = new Frame() {
          public void toFront() {
            disturbed[0] = true;
          }

          public boolean isVisible() {
            return true;
          }
        };
        final Component button = new Component() {
        };
        dialog.add(button);
        dialog.pack();
        dispatchToStayOnTopListeners(mousePress(button));
      } finally {
        dialog.dispose();
        FrEnd.frame_controls = real_controls;
      }
    });
    assertFalse(disturbed[0],
        "presses inside an owned dialog must not move the controls");

    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_stay_on_top = false;
      FrEnd.applyControlsWindowOptions();
      states[1] = FrEnd.frame_controls.isAlwaysOnTop();
      states[2] = FrEnd.isControlsStayOnTopActive();
    });
    assertFalse(states[2], "unchecking stay on top should detach it");
    assertFalse(states[1], "the window flag should stay clear");
  }

  @Test
  void stayOnTopFollowsRecreatedFrames() throws Exception {
    // Booting again recreates the frames; the toolkit listener reads the
    // current frame_main, so it follows the new main window with no
    // re-attaching.
    GuiTestSupport.bootApp();

    final boolean[] brought_forward = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      assertTrue(FrEnd.isControlsStayOnTopActive());
      final Frame real_controls = FrEnd.frame_controls;
      try {
        FrEnd.frame_controls = new Frame() {
          public void toFront() {
            brought_forward[0] = true;
          }

          public boolean isVisible() {
            return true;
          }
        };
        dispatchToStayOnTopListeners(
            mousePress(FrEnd.frame_main));
      } finally {
        FrEnd.frame_controls = real_controls;
      }
    });
    assertTrue(brought_forward[0],
        "after a fresh boot the new main window should drive stay-on-top");
  }

  @Test
  void stayOnTopBringsControlsForwardOnMainWindowActivation()
      throws Exception {
    // Activating the main window by any path (title bar, task bar,
    // Alt+Tab) must bring the controls back above it: the press branch
    // alone misses all of these. The raise must not activate the
    // controls -- stealing focus here is what used to cancel the main
    // window's menus mid-click -- so the window is briefly made
    // non-focusable around the toFront().
    final boolean[] raised = new boolean[1];
    final boolean[] non_focusable_during_raise = new boolean[1];
    final boolean[] focusable_after = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame real_controls = FrEnd.frame_controls;
      try {
        FrEnd.frame_controls = new Frame() {
          public void toFront() {
            raised[0] = true;
            non_focusable_during_raise[0] = !getFocusableWindowState();
          }

          public boolean isVisible() {
            return true;
          }
        };
        dispatchToStayOnTopListeners(
            new WindowEvent(FrEnd.frame_main, WindowEvent.WINDOW_ACTIVATED));
      } finally {
        FrEnd.frame_controls = real_controls;
      }
      focusable_after[0] = FrEnd.frame_controls.getFocusableWindowState();
    });
    assertTrue(raised[0],
        "activating the main window should bring the controls forward");
    assertTrue(non_focusable_during_raise[0],
        "the activation raise must not activate the controls (menus!)");
    assertTrue(focusable_after[0],
        "the focusable state must be restored after the raise");

    // Activating any other window must leave the controls alone.
    final boolean[] disturbed = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame real_controls = FrEnd.frame_controls;
      final Frame other = new Frame();
      try {
        FrEnd.frame_controls = new Frame() {
          public void toFront() {
            disturbed[0] = true;
          }

          public boolean isVisible() {
            return true;
          }
        };
        dispatchToStayOnTopListeners(
            new WindowEvent(other, WindowEvent.WINDOW_ACTIVATED));
      } finally {
        other.dispose();
        FrEnd.frame_controls = real_controls;
      }
    });
    assertFalse(disturbed[0],
        "activating another window must not move the controls");
  }

  @Test
  void mainWindowPressMustNotActivateControls() throws Exception {
    // Regression: the press branch used a plain toFront(), which could
    // steal window activation and dismiss the model-selection dropdown
    // in the main window (seen on Windows). A press must raise the
    // controls without activating them, exactly like the activation
    // branch.
    final boolean[] raised = new boolean[1];
    final boolean[] non_focusable_during_raise = new boolean[1];
    final boolean[] focusable_after = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame real_controls = FrEnd.frame_controls;
      try {
        FrEnd.frame_controls = new Frame() {
          public void toFront() {
            raised[0] = true;
            non_focusable_during_raise[0] = !getFocusableWindowState();
          }

          public boolean isVisible() {
            return true;
          }
        };
        dispatchToStayOnTopListeners(mousePress(FrEnd.frame_main));
      } finally {
        FrEnd.frame_controls = real_controls;
      }
      focusable_after[0] = FrEnd.frame_controls.getFocusableWindowState();
    });
    assertTrue(raised[0],
        "pressing main-window content should still bring the controls forward");
    assertTrue(non_focusable_during_raise[0],
        "the press raise must not activate the controls (dropdown!)");
    assertTrue(focusable_after[0],
        "the focusable state must be restored after the raise");
  }

  /**
   * Delivers a synthetic event to every toolkit-level event listener, the
   * way the event queue would.
   */
  private static void dispatchToStayOnTopListeners(AWTEvent e) {
    for (final AWTEventListener listener
        : Toolkit.getDefaultToolkit().getAWTEventListeners()) {
      listener.eventDispatched(e);
    }
  }

  private static MouseEvent mousePress(Component source) {
    return new MouseEvent(source, MouseEvent.MOUSE_PRESSED,
        System.currentTimeMillis(), 0, 10, 10, 1, false);
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
   * The window checkboxes must not be clipped by a wrapping FlowLayout:
   * at the real 320px controls width the "Dock with main window"
   * checkbox wrapped onto a second, invisible row and vanished.
   */
  @Test
  void dockCheckboxIsFullyVisibleAtRealWindowWidth() throws Exception {
    final Checkbox[] box = new Checkbox[1];
    final boolean[] clipped = new boolean[1];
    final String[] clip_details = new String[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame controls = FrEnd.frame_controls;
      controls.setSize(320, 600);
      controls.setVisible(true);
      findTabbedPanel(controls).show("Preferences");
      controls.validate();
      box[0] = findCheckbox(FrEnd.panel_preferences.panel,
          GUIStrings.CONTROL_WINDOW_DOCK);
      // Walk up the ancestors: the checkbox rectangle must sit fully
      // inside each of them, otherwise some parent is clipping it.
      java.awt.Rectangle rect = box[0].getBounds();
      for (Container parent = box[0].getParent(); parent != null;
          parent = parent.getParent()) {
        if (rect.x < 0 || rect.y < 0
            || rect.x + rect.width > parent.getWidth()
            || rect.y + rect.height > parent.getHeight()) {
          clipped[0] = true;
          clip_details[0] = "clipped by " + parent.getClass().getSimpleName()
              + " " + parent.getWidth() + "x" + parent.getHeight()
              + ", checkbox at " + rect;
          break;
        }
        rect = new java.awt.Rectangle(parent.getX() + rect.x,
            parent.getY() + rect.y, rect.width, rect.height);
      }
    });

    assertNotNull(box[0], "expected the Dock checkbox");
    assertFalse(clipped[0],
        "the Dock checkbox must be fully visible at 320px width"
            + (clip_details[0] == null ? "" : ": " + clip_details[0]));
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

  private static com.springie.gui.components.TabbedPanel findTabbedPanel(
      Container root) {
    for (final Component c : root.getComponents()) {
      if (c instanceof com.springie.gui.components.TabbedPanel) {
        return (com.springie.gui.components.TabbedPanel) c;
      }
      if (c instanceof Container) {
        final com.springie.gui.components.TabbedPanel found =
            findTabbedPanel((Container) c);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
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
