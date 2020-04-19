// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.BorderLayout;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;

public class PanelControlsProperties {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public PanelControlsProperties(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    this.panel.setLayout(new BorderLayout());

    final TabbedPanel tab_properties = new TabbedPanel();
    tab_properties.add("Scalars", FrEnd.panel_edit_properties_scalars.panel);
    tab_properties.add("Delete", FrEnd.panel_edit_delete.panel);
    tab_properties.add("Edit", FrEnd.panel_edit_edit.panel);
    tab_properties.add("Color", FrEnd.panel_edit_color.panel);
    tab_properties.add("Misc", FrEnd.panel_edit_properties_misc.panel);

    this.panel.add(tab_properties, "Center");

    final TabbedPanel tab_flags = new TabbedPanel();
    tab_flags.add("Flags", FrEnd.panel_edit_properties_flags.panel);
    tab_flags.add("Names", FrEnd.panel_edit_properties_names.panel);
    tab_flags.add("Statistics", FrEnd.panel_controls_statistics.panel);

    this.panel.add(tab_flags, "South");
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
