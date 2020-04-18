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
    final TabbedPanel tab_show = new TabbedPanel();
    tab_show.add("Show", FrEnd.panel_preferences_shared_show.panel);

    this.panel.add(tab_show);

    final TabbedPanel tab_misc = new TabbedPanel();
    tab_misc.add("Misc", FrEnd.panel_preferences_shared_misc.panel);

    this.panel.add(tab_misc);
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
