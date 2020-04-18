// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;

public class PanelAllControls {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public PanelAllControls() {
    //this.panel.setLayout(new BorderLayout());
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Controls", FrEnd.panel_controls.panel);
    tab.add("Preferences", FrEnd.panel_preferences.panel);
    tab.add("Statistics", FrEnd.panel_statistics.panel);

    this.panel.add(tab);

    //this.panel.add(FrEnd.panel_edit_select.panel, "North");
    //this.panel.add(tab);
  }
}
