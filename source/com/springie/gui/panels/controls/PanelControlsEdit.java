// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;

public class PanelControlsEdit {
  public Panel panel = FrEnd.setUpPanelForFrame2();



  public Button button_edit_make_motionless;

  public Button button_edit_freeze;

  public Button button_edit_reverse;

  public PanelControlsEdit() {
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Velocity", FrEnd.panel_edit_velocities.panel);

    this.panel.add(tab);
  }


}
