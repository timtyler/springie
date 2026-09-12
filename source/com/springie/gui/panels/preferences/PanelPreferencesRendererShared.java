// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;

public class PanelPreferencesRendererShared {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public PanelPreferencesRendererShared(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Show", FrEnd.panel_preferences_shared_show.panel);
    tab.add("Misc", FrEnd.panel_preferences_shared_misc.panel);

    this.panel.add(tab);
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
