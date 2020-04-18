// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessageManager;

public class PanelControlsEdit {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  NewMessageManager new_message_manager;

  public Button button_edit_make_motionless;

  public Button button_edit_freeze;

  public Button button_edit_reverse;

  public PanelControlsEdit(MessageManager message_manager,
      NewMessageManager new_message_manager) {
    this.message_manager = message_manager;
    this.new_message_manager = new_message_manager;
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Velocity", FrEnd.panel_edit_velocities.panel);

    this.panel.add(tab);
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}
