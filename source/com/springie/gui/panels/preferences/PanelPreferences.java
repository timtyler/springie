// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Button;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;
import com.springie.preferences.Preferences;
import com.springie.render.RendererDelegator;

public class PanelPreferences {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_centre = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

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

    this.panel.add(tab);

    final Button button_reset = new Button(GUIStrings.RESET_PREFERENCES);
    button_reset.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetPreferences();
      }
    });
    final Panel panel_reset = new Panel();
    panel_reset.add(button_reset);
    this.panel.add(panel_reset);

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
