// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;

public class PanelControlsSelect {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public PanelControlsSelect(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Select", FrEnd.panel_edit_select_main.panel);
    tab.add("Links", FrEnd.panel_edit_select_links.panel);
    tab.add("Advanced", FrEnd.panel_edit_select_advanced.panel);

    this.panel.add(tab);
  }
  
  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
