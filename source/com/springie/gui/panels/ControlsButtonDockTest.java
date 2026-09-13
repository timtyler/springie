// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.gui.components.ImageButton;

/**
 * The green check-mark "Controls" button at the bottom of the main
 * window, and the hover tooltips on the bottom-bar buttons.
 *
 * The Controls button's job is to show the controls. Undocked it shows
 * the controls frame; docked it toggles the docked panel inside the
 * main window, so the button keeps operating instead of greying out.
 * It never pops up the empty controls frame while docked.
 */
class ControlsButtonDockTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterEach
  void restoreOptions() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = FrEnd.CONTROLS_ALWAYS_ON_TOP;
      FrEnd.applyControlsWindowOptions();
      FrEnd.frame_controls.setVisible(true);
    });
  }

  @AfterAll
  static void disposeFrames() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  private static void collectImageButtons(Container container,
      List<ImageButton> out) {
    for (final Component child : container.getComponents()) {
      if (child instanceof ImageButton) {
        out.add((ImageButton) child);
      }
      if (child instanceof Container) {
        collectImageButtons((Container) child, out);
      }
    }
  }

  @Test
  void everyBottomBarButtonHasATooltip() throws Exception {
    final List<ImageButton> buttons = new ArrayList<>();
    SwingUtilities.invokeAndWait(() -> collectImageButtons(
        FrEnd.panel_fundamental.panel, buttons));
    assertFalse(buttons.isEmpty(),
        "expected image buttons on the bottom bar");
    for (final ImageButton button : buttons) {
      final String tooltip = button.getTooltipText();
      assertNotNull(tooltip, "bottom-bar button must have a tooltip");
      assertFalse(tooltip.trim().isEmpty(),
          "bottom-bar button tooltip must not be empty");
    }
  }

  @Test
  void controlsButtonTooltipNamesItsJob() throws Exception {
    final String[] tooltip = new String[1];
    SwingUtilities.invokeAndWait(() -> tooltip[0] = FrEnd.panel_fundamental.button_controls
        .getTooltipText());
    assertEquals("Show/hide the controls", tooltip[0]);
  }

  @Test
  void controlsButtonStaysEnabledWhenDocked() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
      FrEnd.applyControlsWindowOptions();
    });
    final boolean[] enabled = new boolean[1];
    SwingUtilities.invokeAndWait(() -> enabled[0] = FrEnd.panel_fundamental.button_controls
        .isEnabled());
    assertTrue(enabled[0],
        "Controls button must stay enabled while the controls are docked");
  }

  @Test
  void controlsButtonTogglesTheDockedPanel() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
      FrEnd.applyControlsWindowOptions();
    });
    // ImageButton has no doClick; fire the action path directly.
    SwingUtilities.invokeAndWait(() -> FrEnd.panel_fundamental.button_controls
        .actionPerformed(new ActionEvent(
            FrEnd.panel_fundamental.button_controls,
            ActionEvent.ACTION_PERFORMED, "Controls")));
    final boolean[] hidden = new boolean[1];
    SwingUtilities.invokeAndWait(
        () -> hidden[0] = !FrEnd.panel_controls_all.panel.isVisible());
    assertTrue(hidden[0],
        "docked Controls button must hide the docked panel");
    SwingUtilities.invokeAndWait(() -> FrEnd.panel_fundamental.button_controls
        .actionPerformed(new ActionEvent(
            FrEnd.panel_fundamental.button_controls,
            ActionEvent.ACTION_PERFORMED, "Controls")));
    final boolean[] shown = new boolean[1];
    SwingUtilities.invokeAndWait(
        () -> shown[0] = FrEnd.panel_controls_all.panel.isVisible());
    assertTrue(shown[0],
        "docked Controls button must show the docked panel again");
    final boolean[] frame_visible = new boolean[1];
    SwingUtilities.invokeAndWait(
        () -> frame_visible[0] = FrEnd.frame_controls.isVisible());
    assertFalse(frame_visible[0],
        "docked controls must not pop up the (empty) controls frame");
  }

  @Test
  void controlsButtonShowsTheFrameWhenUndocked() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_controls.setVisible(false);
      FrEnd.panel_fundamental.button_controls
          .actionPerformed(new ActionEvent(
              FrEnd.panel_fundamental.button_controls,
              ActionEvent.ACTION_PERFORMED, "Controls"));
    });
    final boolean[] visible = new boolean[1];
    SwingUtilities.invokeAndWait(
        () -> visible[0] = FrEnd.frame_controls.isVisible());
    assertTrue(visible[0],
        "undocked Controls button must show the controls frame");
  }
}
