// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;

public class PanelControls {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_centre = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public PanelControls(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Modify", FrEnd.panel_edit_modify.panel);
    tab.add("Universe", FrEnd.panel_edit_universe.panel);
    tab.add("Create",  FrEnd.panel_edit_generate.panel);
    tab.add("Misc", FrEnd.panel_edit_misc.panel);

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
