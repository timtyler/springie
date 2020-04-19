// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.tifsoft.Forget;

public class PanelControlsPropertiesScaleFactor {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public Scrollbar scroll_bar_scale_by;

  private Label label_scale_by;

  public PanelControlsPropertiesScaleFactor(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {

    final Panel panel_scale_by = getPanelMisc();

    this.panel.add(panel_scale_by);
  }

  private Panel getPanelMisc() {
    final Panel panel_east = new Panel();
    final Panel panel_west = new Panel();
    final Panel panel_center = new Panel();

    final Panel panel_scale_factor = new Panel();
    panel_scale_factor.setLayout(new BorderLayout(0, 8));
        
    panel_scale_factor.setLayout(new BorderLayout(0, 8));
    panel_center.setLayout(new GridLayout(1, 1, 0, 0));
    final Label label = new Label("Scale by:", Label.RIGHT);
    panel_west.add(label);

    this.scroll_bar_scale_by = new Scrollbar(Scrollbar.HORIZONTAL, 10, 10, 0,
        110);
    this.scroll_bar_scale_by.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        final int value = e.getValue();
        getLabelScaleBy().setText("" + value + "%");
      }
    });
    panel_center.add(this.scroll_bar_scale_by);
    this.label_scale_by = new Label("10%", Label.LEFT);
    panel_east.add(this.label_scale_by);

    panel_scale_factor.add("East", panel_east);
    panel_scale_factor.add("West", panel_west);
    panel_scale_factor.add("Center", panel_center);
    
    return panel_scale_factor;
  }

  public Label getLabelScaleBy() {
    return this.label_scale_by;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
