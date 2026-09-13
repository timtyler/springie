// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Label;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;
import com.springie.preferences.Preferences;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

public class PanelPreferences {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_centre = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  private Choice choice_controls_window_mode;

  public PanelPreferences(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Display", FrEnd.panel_preferences_display.panel);
    tab.add("Viewpoint", FrEnd.panel_preferences_viewpoint.panel);
    //tab.add("Stereo3D", FrEnd.panel_preferences_stereo3d.panel);
    tab.add("Editing", FrEnd.panel_preferences_edit.panel);
    tab.add("Update", FrEnd.panel_preferences_update.panel);
    tab.add("I/O", FrEnd.panel_preferences_io.panel);

    this.panel.setLayout(new BorderLayout());
    this.panel.add(tab, BorderLayout.CENTER);

    // Compact strip: one control per row. (A single FlowLayout row wraps
    // when the window is narrow, and FlowLayout's preferred height
    // ignores wrapping, so the wrapped controls ended up clipped --
    // first the reset button, then the "Dock with main window" checkbox.)
    final Panel panel_south = new Panel(new GridLayout(0, 1));

    final Panel panel_controls_mode = new Panel();
    panel_controls_mode.add(new Label(GUIStrings.CONTROL_WINDOW_MODE));
    this.choice_controls_window_mode = new Choice();
    this.choice_controls_window_mode.add(GUIStrings.CONTROL_WINDOW_ALWAYS_ON_TOP);
    this.choice_controls_window_mode.add(GUIStrings.CONTROL_WINDOW_FREE_FLOATING);
    this.choice_controls_window_mode.add(GUIStrings.CONTROL_WINDOW_DOCKED);
    this.choice_controls_window_mode.select(FrEnd.controls_window_mode);
    this.choice_controls_window_mode.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.controls_window_mode =
            ((Choice) e.getSource()).getSelectedIndex();
        FrEnd.applyControlsWindowOptions();
      }
    });
    panel_controls_mode.add(this.choice_controls_window_mode);
    panel_south.add(panel_controls_mode);

    final Button button_reset = new Button(GUIStrings.RESET_PREFERENCES);
    button_reset.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetPreferences();
      }
    });
    final Panel panel_reset = new Panel();
    panel_reset.add(button_reset);
    panel_south.add(panel_reset);

    this.panel.add(panel_south, BorderLayout.SOUTH);

    this.panel.validate();
  }

  /**
   * Restores every preference to its default value and updates the controls
   * to match. The Preferences map defaults come from its constructor; the
   * remaining defaults are the static field initialisers, restored by each
   * tab panel.
   */
  public void resetPreferences() {
    FrEnd.preferences = new Preferences();

    FrEnd.controls_window_mode = FrEnd.CONTROLS_DOCKED;
    this.choice_controls_window_mode.select(FrEnd.CONTROLS_DOCKED);
    // In case the choice was already in its default state (no item event
    // fires then).
    FrEnd.applyControlsWindowOptions();

    FrEnd.panel_preferences_display.resetToDefaults();
    FrEnd.panel_preferences_viewpoint.resetToDefaults();
    FrEnd.panel_preferences_edit.resetToDefaults();
    FrEnd.panel_preferences_update.resetToDefaults();
    FrEnd.panel_preferences_io.resetToDefaults();

    RendererDelegator.repaintAll();
  }

  protected Panel getPanel() {
    return this.panel;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
