// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;

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

    this.panel.validate();
  }

  protected Panel getPanel() {
    return this.panel;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
