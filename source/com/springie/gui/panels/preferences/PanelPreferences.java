// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
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

  private Checkbox checkbox_stay_on_top;

  private Checkbox checkbox_dock_with_main;

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

    final Panel panel_stay_on_top = new Panel();
    this.checkbox_stay_on_top = new Checkbox(
        GUIStrings.CONTROL_WINDOW_STAY_ON_TOP, FrEnd.controls_stay_on_top);
    this.checkbox_stay_on_top.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.controls_stay_on_top = ((Checkbox) e.getSource()).getState();
        FrEnd.applyControlsWindowOptions();
      }
    });
    panel_stay_on_top.add(this.checkbox_stay_on_top);
    panel_south.add(panel_stay_on_top);

    final Panel panel_dock_with_main = new Panel();
    this.checkbox_dock_with_main = new Checkbox(
        GUIStrings.CONTROL_WINDOW_DOCK, FrEnd.controls_dock_with_main);
    this.checkbox_dock_with_main.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.controls_dock_with_main = ((Checkbox) e.getSource()).getState();
        FrEnd.applyControlsWindowOptions();
      }
    });
    panel_dock_with_main.add(this.checkbox_dock_with_main);
    panel_south.add(panel_dock_with_main);

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

    FrEnd.controls_stay_on_top = true;
    FrEnd.controls_dock_with_main = false;
    this.checkbox_stay_on_top.setState(true);
    this.checkbox_dock_with_main.setState(false);
    // In case a checkbox was already in its default state (no item event
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
