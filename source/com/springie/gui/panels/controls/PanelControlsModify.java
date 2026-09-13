// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.BorderLayout;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;

public class PanelControlsModify {
  public Panel panel = new Panel();


  public PanelControlsModify() {
    makePanel();
  }

  void makePanel() {
    this.panel.setLayout(new BorderLayout());
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Properties", FrEnd.panel_edit_properties.panel);
    tab.add("Controls", FrEnd.panel_controls.panel);
//    tab.add("Delete", FrEnd.panel_edit_delete.panel);
//    tab.add("Edit", FrEnd.panel_edit_edit.panel);

    this.panel.add(FrEnd.panel_edit_select.panel, "North");
    this.panel.add(tab, "Center");
  }
}
