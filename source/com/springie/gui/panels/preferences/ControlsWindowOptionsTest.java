// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
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
 * The controls-window mode on the Preferences tab: a three-way choice
 * between always-on-top, free floating, and docked. Docked reparents the
 * controls panel into the main window's layout (BorderLayout.EAST) --
 * true docking, not a snapped separate window.
 */
class ControlsWindowOptionsTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterEach
  void restoreOptions() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = FrEnd.CONTROLS_ALWAYS_ON_TOP;
      FrEnd.applyControlsWindowOptions();
      // Known-good state for the next test: undocked and visible. (The
      // frame starts hidden at boot until the user opens it via the menu,
      // so apply() alone does not guarantee visibility.)
      FrEnd.frame_controls.setVisible(true);
    });
  }

  @AfterAll
  static void disposeFrames() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @Test
  void modeDefaultsToAlwaysOnTop() throws Exception {
    final int[] mode = new int[1];
    final boolean[] stay_on_top = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      mode[0] = FrEnd.controls_window_mode;
      stay_on_top[0] = FrEnd.isControlsStayOnTopActive();
    });
    assertEquals(FrEnd.CONTROLS_ALWAYS_ON_TOP, mode[0],
        "controls window mode must default to always-on-top");
    assertTrue(stay_on_top[0],
        "stay-on-top listener must be active in always-on-top mode");
  }

  @Test
  void alwaysOnTopKeepsControlsInSeparateVisibleFrame() throws Exception {
    final boolean[] visible = new boolean[1];
    final Container[] parent = new Container[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_controls.setVisible(true);
      FrEnd.controls_window_mode = FrEnd.CONTROLS_ALWAYS_ON_TOP;
      FrEnd.applyControlsWindowOptions();
      visible[0] = FrEnd.frame_controls.isVisible();
      parent[0] = FrEnd.panel_controls_all.panel.getParent();
    });
    assertTrue(visible[0],
        "controls frame must be visible in always-on-top mode");
    assertEquals(FrEnd.frame_controls, parent[0],
        "controls panel must stay in its own frame when not docked");
  }

  @Test
  void freeFloatingDisablesStayOnTopButKeepsSeparateFrame() throws Exception {
    final boolean[] stay_on_top = new boolean[1];
    final boolean[] visible = new boolean[1];
    final Container[] parent = new Container[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_controls.setVisible(true);
      FrEnd.controls_window_mode = FrEnd.CONTROLS_FREE_FLOATING;
      FrEnd.applyControlsWindowOptions();
      stay_on_top[0] = FrEnd.isControlsStayOnTopActive();
      visible[0] = FrEnd.frame_controls.isVisible();
      parent[0] = FrEnd.panel_controls_all.panel.getParent();
    });
    assertFalse(stay_on_top[0],
        "stay-on-top listener must be inactive in free-floating mode");
    assertTrue(visible[0],
        "controls frame must be visible in free-floating mode");
    assertEquals(FrEnd.frame_controls, parent[0],
        "controls panel must stay in its own frame when not docked");
  }

  @Test
  void dockedReparentsControlsIntoMainWindowLayout() throws Exception {
    final Container[] parent = new Container[1];
    final boolean[] frame_visible = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
      FrEnd.applyControlsWindowOptions();
      parent[0] = FrEnd.panel_controls_all.panel.getParent();
      frame_visible[0] = FrEnd.frame_controls.isVisible();
    });
    assertEquals(FrEnd.frame_main, parent[0],
        "docked controls panel must be reparented into the main window");
    assertFalse(frame_visible[0],
        "separate controls frame must be hidden while docked");
  }

  @Test
  void undockingRestoresControlsToSeparateFrame() throws Exception {
    final Container[] parent = new Container[1];
    final boolean[] visible = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      // Dock first.
      FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
      FrEnd.applyControlsWindowOptions();
      // Then undock to free-floating.
      FrEnd.controls_window_mode = FrEnd.CONTROLS_FREE_FLOATING;
      FrEnd.applyControlsWindowOptions();
      parent[0] = FrEnd.panel_controls_all.panel.getParent();
      visible[0] = FrEnd.frame_controls.isVisible();
    });
    assertEquals(FrEnd.frame_controls, parent[0],
        "undocked controls panel must return to its own frame");
    assertTrue(visible[0],
        "controls frame must be visible again after undocking");
  }

  @Test
  void dockingTwiceDoesNotReparentTwice() throws Exception {
    final int[] count = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
      FrEnd.applyControlsWindowOptions();
      FrEnd.applyControlsWindowOptions(); // Second time.
      // Count how many times the controls panel appears in frame_main.
      count[0] = 0;
      for (Component c : FrEnd.frame_main.getComponents()) {
        if (c == FrEnd.panel_controls_all.panel) {
          count[0]++;
        }
      }
    });
    assertEquals(1, count[0],
        "docking twice must not add the panel twice");
  }

  @Test
  void preferencesTabHasThreeWayChoice() throws Exception {
    final Choice[] choice = new Choice[1];
    final int[] item_count = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      choice[0] = findChoice(FrEnd.panel_preferences.panel,
          GUIStrings.CONTROL_WINDOW_ALWAYS_ON_TOP);
      if (choice[0] != null) {
        item_count[0] = choice[0].getItemCount();
      }
    });
    assertNotNull(choice[0],
        "Preferences must have a Choice with the three window modes");
    assertEquals(3, item_count[0],
        "Choice must offer always-on-top, free floating, and docked");
    SwingUtilities.invokeAndWait(() -> {
      assertEquals(GUIStrings.CONTROL_WINDOW_ALWAYS_ON_TOP,
          choice[0].getItem(0));
      assertEquals(GUIStrings.CONTROL_WINDOW_FREE_FLOATING,
          choice[0].getItem(1));
      assertEquals(GUIStrings.CONTROL_WINDOW_DOCKED,
          choice[0].getItem(2));
    });
  }

  @Test
  void resetRestoresAlwaysOnTop() throws Exception {
    final int[] mode = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
      FrEnd.applyControlsWindowOptions();
      FrEnd.panel_preferences.resetPreferences();
      mode[0] = FrEnd.controls_window_mode;
    });
    assertEquals(FrEnd.CONTROLS_ALWAYS_ON_TOP, mode[0],
        "reset must restore always-on-top mode");
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
  void controlsModeChoiceIsFullyVisibleAtRealWindowWidth() throws Exception {
    // The old "Dock with main window" checkbox vanished at the real 320px
    // controls width (FlowLayout wrap-and-clip). The three-way Choice must
    // not suffer the same fate.
    final Choice[] choice = new Choice[1];
    final boolean[] clipped = new boolean[1];
    final String[] clip_details = new String[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame controls = FrEnd.frame_controls;
      controls.setSize(320, 600);
      controls.setVisible(true);
      findTabbedPanel(controls).show("Preferences");
      controls.validate();
      choice[0] = findChoice(FrEnd.panel_preferences.panel,
          GUIStrings.CONTROL_WINDOW_DOCKED);
      // Walk up the ancestors: the choice rectangle must sit fully
      // inside each of them, otherwise some parent is clipping it.
      java.awt.Rectangle rect = choice[0].getBounds();
      for (Container parent = choice[0].getParent(); parent != null;
          parent = parent.getParent()) {
        if (rect.x < 0 || rect.y < 0
            || rect.x + rect.width > parent.getWidth()
            || rect.y + rect.height > parent.getHeight()) {
          clipped[0] = true;
          clip_details[0] = "clipped by " + parent.getClass().getSimpleName()
              + " " + parent.getWidth() + "x" + parent.getHeight()
              + ", choice at " + rect;
          break;
        }
        rect = new java.awt.Rectangle(parent.getX() + rect.x,
            parent.getY() + rect.y, rect.width, rect.height);
      }
    });

    assertNotNull(choice[0], "expected the controls-mode Choice");
    assertFalse(clipped[0],
        "the controls-mode Choice must be fully visible at 320px width"
            + (clip_details[0] == null ? "" : ": " + clip_details[0]));
  }

  /**
   * Finds a Choice containing the given item text, searching the
   * container hierarchy.
   */
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

  /**
   * Finds a Choice containing the given item text, searching the
   * container hierarchy.
   */
  private static Choice findChoice(Container container, String item_text) {
    for (Component c : container.getComponents()) {
      if (c instanceof Choice) {
        final Choice ch = (Choice) c;
        for (int i = 0; i < ch.getItemCount(); i++) {
          if (ch.getItem(i).equals(item_text)) {
            return ch;
          }
        }
      } else if (c instanceof Container) {
        final Choice found = findChoice((Container) c, item_text);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
