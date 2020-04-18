// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import com.springie.FrEnd;
import com.springie.messages.MessageManager;

public class PanelControlsPropertiesScaleFactor {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

//  public Scrollbar scroll_bar_elasticity;
//
//  public Scrollbar scroll_bar_length;
//
//  public Scrollbar scroll_bar_radius;
//
//  public Scrollbar scroll_bar_charge;
//
//  public Scrollbar scroll_bar_damping;

  public Scrollbar scroll_bar_scale_by;

//  public Label label_elasticity;
//
//  public Label label_length;
//
//  public Label label_damping;
//
//  public Label label_radius;
//
//  public Label label_charge;

  private Label label_scale_by;

//  public Button button_setsize;
//
//  public Button button_radius_set;
//
//  public Button button_radius_fix;
//
//  public Button button_charge_fix;
//
//  public Button button_damping_set;
//
//  public Button button_damping_fix;
//
//  public Button button_elasticity_set;
//
//  public Button button_elasticity_fix;
//
//  public Button button_scale_elasticity_up;
//
//  public Button button_scale_elasticity_down;
//
//  public Button button_scale_lengths_up;
//
//  public Button button_scale_lengths_down;
//
//  public Button button_scale_radius_up;
//
//  public Button button_scale_radius_down;
//
//  public Button button_scale_charge_up;
//
//  public Button button_scale_charge_down;
//
//  public Button button_scale_damping_up;
//
//  public Button button_scale_damping_down;
//
//  public TextFieldWrapper textfield_colour_w;
//
//  public TextFieldWrapper textfield_colour_r;
//
//  public TextFieldWrapper textfield_size_r;
//
//  public TextFieldWrapper textfield_size_w;
//
//  public TextFieldWrapper textfield_e_r;
//
//  public TextFieldWrapper textfield_e_w;
//
//  public TextFieldWrapper textfield_radius_r;
//
//  public TextFieldWrapper textfield_radius_w;
//
//  public TextFieldWrapper textfield_charge_r;
//
//  public TextFieldWrapper textfield_charge_w;
//
//  public TextFieldWrapper textfield_change_elasticity_by;
//
//  public TextFieldWrapper textfield_change_damping_by;

  public PanelControlsPropertiesScaleFactor(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {

//    final Panel panel_length = setUpLengthSlider();
//
//    final Panel panel_radius = setUpRadiusSlider();
//
//    final Panel panel_elasticity = setUpElasticitySlider();
//
//    final Panel panel_damping = setUpSliderDamping();
//
//    final Panel panel_charge = setUpChargeSlider();

    final Panel panel_scale_by = getPanelScaleByQuantity();

    this.panel.add(panel_scale_by);
//    this.panel.add(panel_radius);
//    this.panel.add(panel_length);
//    this.panel.add(panel_elasticity);
//    this.panel.add(panel_damping);
//    this.panel.add(panel_charge);

    //this.panel.add(FrEnd.panel_edit_properties_flags.panel);
  }

  private Panel getPanelScaleByQuantity() {
    final Panel panel = new Panel();
    final Panel panel_east = new Panel();
    final Panel panel_west = new Panel();
    final Panel panel_center = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
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

    panel.add("East", panel_east);
    panel.add("West", panel_west);
    panel.add("Center", panel_center);

    return panel;
  }

//  private Panel setUpLengthSlider() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Length:", Label.RIGHT));
//
//    this.scroll_bar_length = new Scrollbar(Scrollbar.HORIZONTAL, 0, 100, 0,
//        99899);
//    this.scroll_bar_length.addAdjustmentListener(new AdjustmentListener() {
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        final int length = e.getValue();
//        getMessageManager().sendMessage(Message.MSG_ALTER_LENGTH, length, 0);
//        reflectLength();
//      }
//    });
//
//    panel.add("Center", this.scroll_bar_length);
//
//    this.label_length = new Label("X", Label.LEFT);
//    panel.add("East", this.label_length);
//
//    this.button_scale_lengths_up = new Button(GUIStrings.EDIT_CHANGE_UP);
//    this.button_scale_lengths_up.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_SCALE_LINKS_UP, 0, 0);
//      }
//    });
//
//    this.button_scale_lengths_down = new Button(GUIStrings.EDIT_CHANGE_DOWN);
//    this.button_scale_lengths_down.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_SCALE_LINKS_DOWN, 0, 0);
//      }
//    });
//
//    final Panel panel_east = new Panel();
//    panel_east.add(this.label_length);
//    panel_east.add(this.button_scale_lengths_down);
//    panel_east.add(this.button_scale_lengths_up);
//
//    panel.add("East", panel_east);
//
//    return panel;
//  }
//
//  private Panel setUpRadiusSlider() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Radius:", Label.RIGHT));
//
//    this.scroll_bar_radius = new Scrollbar(Scrollbar.HORIZONTAL, 0, 100, 0,
//        39899);
//    this.scroll_bar_radius.addAdjustmentListener(new AdjustmentListener() {
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        final int radius = e.getValue();
//        getMessageManager().sendMessage(Message.MSG_ALTER_RADIUS, radius, 0);
//      }
//    });
//
//    panel.add("Center", this.scroll_bar_radius);
//
//    this.label_radius = new Label("X", Label.LEFT);
//
//    this.button_scale_radius_up = new Button(GUIStrings.EDIT_CHANGE_UP);
//    this.button_scale_radius_up.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_DOME_NODES_EXPAND, 0, 0);
//      }
//    });
//
//    this.button_scale_radius_down = new Button(GUIStrings.EDIT_CHANGE_DOWN);
//    this.button_scale_radius_down.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_DOME_NODES_CONTRACT, 0, 0);
//      }
//    });
//
//    final Panel panel_east = new Panel();
//    panel_east.add(this.label_radius);
//    panel_east.add(this.button_scale_radius_down);
//    panel_east.add(this.button_scale_radius_up);
//
//    panel.add("East", panel_east);
//
//    return panel;
//  }
//
//  private Panel setUpChargeSlider() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Charge:", Label.RIGHT));
//
//    this.scroll_bar_charge = new Scrollbar(Scrollbar.HORIZONTAL, -100, 10,
//        -100, 110);
//    this.scroll_bar_charge.addAdjustmentListener(new AdjustmentListener() {
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        final int charge = e.getValue();
//        getMessageManager().sendMessage(Message.MSG_ALTER_CHARGE, charge, 0);
//      }
//    });
//
//    panel.add("Center", this.scroll_bar_charge);
//
//    this.label_charge = new Label("X", Label.LEFT);
//
//    this.button_scale_charge_up = new Button(GUIStrings.EDIT_CHANGE_DOWN);
//    this.button_scale_charge_up.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_EDIT_CHARGE_UP, 0, 0);
//      }
//    });
//
//    this.button_scale_charge_down = new Button(GUIStrings.EDIT_CHANGE_UP);
//    this.button_scale_charge_down.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_EDIT_CHARGE_DOWN, 0, 0);
//      }
//    });
//
//    final Panel panel_east = new Panel();
//    panel_east.add(this.label_charge);
//    panel_east.add(this.button_scale_charge_down);
//    panel_east.add(this.button_scale_charge_up);
//
//    panel.add("East", panel_east);
//
//    return panel;
//  }
//
//  private Panel setUpElasticitySlider() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Elasticity:", Label.RIGHT));
//
//    this.scroll_bar_elasticity = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0,
//        110);
//    this.scroll_bar_elasticity.addAdjustmentListener(new AdjustmentListener() {
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        final int elasticity = e.getValue();
//        getMessageManager().sendMessage(Message.MSG_ALTER_ELASTICITY,
//            elasticity, 0);
//        reflectElasticity();
//      }
//    });
//    panel.add("Center", this.scroll_bar_elasticity);
//    this.label_elasticity = new Label("X", Label.LEFT);
//
//    this.button_scale_elasticity_up = new Button(GUIStrings.EDIT_CHANGE_UP);
//    this.button_scale_elasticity_up.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_CHANGE_ELASTICITY_UP, 0, 0);
//      }
//    });
//
//    this.button_scale_elasticity_down = new Button(GUIStrings.EDIT_CHANGE_DOWN);
//    this.button_scale_elasticity_down.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager()
//            .sendMessage(Message.MSG_CHANGE_ELASTICITY_DOWN, 0, 0);
//      }
//    });
//
//    final Panel panel_east = new Panel();
//    panel_east.add(this.label_elasticity);
//    panel_east.add(this.button_scale_elasticity_down);
//    panel_east.add(this.button_scale_elasticity_up);
//
//    panel.add("East", panel_east);
//
//    return panel;
//  }
//
//  private Panel setUpSliderDamping() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Damping:", Label.RIGHT));
//
//    this.scroll_bar_damping = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0, 110);
//    this.scroll_bar_damping.addAdjustmentListener(new AdjustmentListener() {
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        final int damping = e.getValue();
//        getMessageManager().sendMessage(Message.MSG_ALTER_STIFFNESS, damping, 0);
//        reflectStiffness();
//      }
//    });
//    panel.add("Center", this.scroll_bar_damping);
//    this.label_damping = new Label("X", Label.LEFT);
//
//    this.button_scale_damping_up = new Button(GUIStrings.EDIT_CHANGE_UP);
//    this.button_scale_damping_up.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_CHANGE_STIFFNESS_UP, 0, 0);
//      }
//    });
//
//    this.button_scale_damping_down = new Button(GUIStrings.EDIT_CHANGE_DOWN);
//    this.button_scale_damping_down.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_CHANGE_STIFFNESS_DOWN, 0, 0);
//      }
//    });
//
//    final Panel panel_east = new Panel();
//    panel_east.add(this.label_damping);
//    panel_east.add(this.button_scale_damping_down);
//    panel_east.add(this.button_scale_damping_up);
//
//    panel.add("East", panel_east);
//
//    return panel;
//  }

//  public void updateGUIToReflectSelectionChange() {
//    reflectRadius();
//    reflectElasticity();
//    reflectStiffness();
//    reflectLength();
//    reflectCharge();
//
//    final PanelStatistics panel_statistics = FrEnd.panel_statistics;
//    final NodeManager node_manager = FrEnd.node_manager;
//
//    panel_statistics.label_number_of_nodes_selected.setText(""
//        + node_manager.getNumberOfSelectedNodes());
//    panel_statistics.label_number_of_links_selected.setText(""
//        + node_manager.link_manager.getNumberOfSelected());
//    panel_statistics.label_number_of_faces_selected.setText(""
//        + node_manager.face_manager.getNumberOfSelected());
//  }

//  public void reflectElasticity() {
//    final int v = new AverageElasticityGetter(FrEnd.node_manager).getAverage();
//    this.scroll_bar_elasticity.setValue(v);
//    this.label_elasticity.setText("" + v);
//  }
//
//  public void reflectStiffness() {
//    final int v = new AverageStiffnessGetter(FrEnd.node_manager).getAverage();
//    this.scroll_bar_damping.setValue(v);
//    this.label_damping.setText("" + v);
//  }
//
//  public void reflectLength() {
//    final int v = new AverageLengthGetter(FrEnd.node_manager).getAverage();
//    this.scroll_bar_length.setValue(v);
//    this.label_length.setText("" + v);
//  }
//
//  public void reflectRadius() {
//    final int v = new AverageRadiusGetter(FrEnd.node_manager).getAverage();
//    this.scroll_bar_radius.setValue(v);
//    this.label_radius.setText("" + v);
//  }
//
//  public void reflectCharge() {
//    final int v = new AverageChargeGetter(FrEnd.node_manager).getAverage();
//    this.scroll_bar_charge.setValue(v);
//    this.label_charge.setText("" + v);
//  }
//
//  public void setElasticityLabel(int e) {
//    this.label_elasticity.setText("" + e);
//  }
//
//  public void setLengthLabel(int l) {
//    this.label_length.setText("" + l);
//  }
//
//  public void setRadiusLabel(int r) {
//    this.label_radius.setText("" + r);
//  }
//
  public Label getLabelScaleBy() {
    return this.label_scale_by;
  }
//
//  public TextFieldWrapper getTextfieldEW() {
//    return this.textfield_e_w;
//  }
//
//  public TextFieldWrapper getTextfieldER() {
//    return this.textfield_e_r;
//  }
//
//  public TextFieldWrapper getTextfieldRadiusW() {
//    return this.textfield_radius_w;
//  }
//
//  public TextFieldWrapper getTextfieldRadiusR() {
//    return this.textfield_radius_r;
//  }
//
//  public TextFieldWrapper getTextfieldSizeW() {
//    return this.textfield_size_w;
//  }
//
//  public TextFieldWrapper getTextfieldSizeR() {
//    return this.textfield_size_r;
//  }
//
//  public Label getLabelElasticity() {
//    return this.label_elasticity;
//  }
//
//  public Label getLabelLength() {
//    return this.label_length;
//  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
