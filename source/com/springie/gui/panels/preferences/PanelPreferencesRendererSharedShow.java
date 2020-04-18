// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

public class PanelPreferencesRendererSharedShow {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_shared = FrEnd.setUpPanelForFrame();

  MessageManager message_manager;

  public Checkbox checkbox_redraw_deepest_first;

  public Checkbox checkbox_render_hidden_nodes;

  public Checkbox checkbox_render_hidden_links;

  public Checkbox checkbox_render_hidden_polygons;

  public Checkbox checkbox_render_nodes;

  public Checkbox checkbox_render_links;

  public Checkbox checkbox_render_polygons;

  private Checkbox checkbox_render_charges;

  public PanelPreferencesRendererSharedShow(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final Panel panel_render_normal = new Panel();
    panel_render_normal.add(new Label("Show:"));

    this.checkbox_render_nodes = new Checkbox("Nodes", FrEnd.render_nodes);
    this.checkbox_render_nodes.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.render_nodes = ((Checkbox) e.getSource()).getState();
        RendererDelegator.repaintAll();
      }
    });
    panel_render_normal.add(this.checkbox_render_nodes);

    this.checkbox_render_links = new Checkbox("Links", FrEnd.render_links);
    this.checkbox_render_links.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.render_links = ((Checkbox) e.getSource()).getState();
        RendererDelegator.repaintAll();
      }
    });
    panel_render_normal.add(this.checkbox_render_links);

    this.checkbox_render_polygons = new Checkbox("Faces", FrEnd.render_faces);
    this.checkbox_render_polygons.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.render_faces = getCheckboxRenderPolygons().getState();
        RendererDelegator.repaintAll();
      }
    });
    panel_render_normal.add(this.checkbox_render_polygons);

    final Panel panel_render_charges = new Panel();
    panel_render_charges.add(new Label("Show:"));
    
    this.checkbox_render_charges = new Checkbox("Charges", FrEnd.render_charges);
    this.checkbox_render_charges.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.render_charges = ((Checkbox) e.getSource()).getState();
        RendererDelegator.repaintAll();
      }
    });
    panel_render_charges.add(this.checkbox_render_charges);

    final Panel panel_render_hidden = new Panel();
    this.checkbox_render_hidden_nodes = new Checkbox("Nodes",
        FrEnd.render_hidden_nodes);
    this.checkbox_render_hidden_nodes.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.render_hidden_nodes = getCheckboxRenderHiddenNodes().getState();
        RendererDelegator.repaintAll();
      }
    });
    panel_render_hidden.add(new Label("Show hidden:"));
    panel_render_hidden.add(this.checkbox_render_hidden_nodes);

    this.checkbox_render_hidden_links = new Checkbox("Links",
        FrEnd.render_hidden_links);
    this.checkbox_render_hidden_links.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.render_hidden_links = getCheckboxRenderHiddenLinks().getState();
        RendererDelegator.repaintAll();
      }
    });
    panel_render_hidden.add(this.checkbox_render_hidden_links);

    this.checkbox_render_hidden_polygons = new Checkbox("Faces",
        FrEnd.render_hidden_faces);
    this.checkbox_render_hidden_polygons.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.render_hidden_faces = getCheckboxRenderHiddenPolygons()
            .getState();
        RendererDelegator.repaintAll();
      }
    });
    panel_render_hidden.add(this.checkbox_render_hidden_polygons);

//    final TTChoice choose_display_struts = new TTChoice(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        final String scs = (String) e.getItem();
//        Link.link_display_struts_type = FrEnd.choose_display_struts
//            .str_to_num(scs);
//        RendererDelegator.repaintAll();
//      }
//    });
//
//    FrEnd.choose_display_struts = choose_display_struts;

////    final Panel panel_bin_size = getBinSizePanel();
//    final Panel panel_fog = getFogPanel();
//
//    final Panel panel_visible_explosions = new Panel();
//    this.checkbox_explosions = new Checkbox(GUIStrings.EXPLOSIONS);
//    this.checkbox_explosions.addItemListener(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        Forget.about(e);
//        FrEnd.explosions = ((Checkbox) e.getSource()).getState();
//      }
//    });
//    this.checkbox_explosions.setState(FrEnd.explosions);
//    panel_visible_explosions.add(this.checkbox_explosions);
//
//    final Panel panel_redraw_deepest_first = new Panel();
//    this.checkbox_redraw_deepest_first = new Checkbox(
//        "Render deepest objects first");
//    this.checkbox_redraw_deepest_first.addItemListener(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        Forget.about(e);
//        FrEnd.redraw_deepest_first = ((Checkbox) e.getSource()).getState();
//      }
//    });
//    this.checkbox_redraw_deepest_first.setState(FrEnd.redraw_deepest_first);
//    panel_redraw_deepest_first.add(this.checkbox_redraw_deepest_first);
//    
//    final Panel panel_face_render_number = getFaceRenderNumber();

    // Common preferences...

    this.panel.add(panel_render_normal);
    this.panel.add(panel_render_charges);
    this.panel.add(panel_render_hidden);
//    this.panel.add(panel_redraw_deepest_first);
//    this.panel.add(panel_fog);
//    this.panel.add(panel_visible_explosions);
//    this.panel.add(panel_face_render_number);
  }


//  private Panel getFogPanel() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Fog:", Label.RIGHT));
//
//    final Scrollbar scroll_bar_fog = new Scrollbar(Scrollbar.HORIZONTAL,
//        DeepObjectColourCalculator.factor / 10, 10, 0, 110);
//    scroll_bar_fog.addAdjustmentListener(new AdjustmentListener() {
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        final int temp = e.getValue();
//        DeepObjectColourCalculator.factor = temp * 10;
//        reflectLabelFog();
//      }
//    });
//
//    panel.add("Center", scroll_bar_fog);
//
//    this.label_fog = new Label("", Label.LEFT);
//    panel.add("East", this.label_fog);
//    reflectLabelFog();
//
//    return panel;
//  }
//
//  private Panel getFaceRenderNumber() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Face lines:", Label.RIGHT));
//
//    final Scrollbar scroll_bar_face_render_number = new Scrollbar(
//        Scrollbar.HORIZONTAL, Face.number_of_render_divisions, 4, 0, 28);
//    scroll_bar_face_render_number
//        .addAdjustmentListener(new AdjustmentListener() {
//          public void adjustmentValueChanged(AdjustmentEvent e) {
//            final int temp = e.getValue();
//            Face.number_of_render_divisions = temp;
//            reflectLabelFaceRenderNumber();
//            RendererDelegator.repaintAll();
//          }
//        });
//
//    panel.add("Center", scroll_bar_face_render_number);
//
//    this.label_face_render_number = new Label("", Label.LEFT);
//    panel.add("East", this.label_face_render_number);
//    reflectLabelFaceRenderNumber();
//
//    return panel;
//  }
//
//  private void reflectLabelFog() {
//    getLabelFog().setText("" + (DeepObjectColourCalculator.factor / 10));
//  }
//
//  public Label getLabelFog() {
//    return this.label_fog;
//  }
//
//  private void reflectLabelFaceRenderNumber() {
//    getLabelFaceRenderNumber().setText("" + Face.number_of_render_divisions);
//  }
//  public Label getLabelFaceRenderNumber() {
//    return this.label_face_render_number;
//  }

  public Checkbox getCheckboxRenderLinks() {
    return this.checkbox_render_links;
  }

  public Checkbox getCheckboxRenderNodes() {
    return this.checkbox_render_nodes;
  }

  public Checkbox getCheckboxRenderPolygons() {
    return this.checkbox_render_polygons;
  }

  public Checkbox getCheckboxRenderCharges() {
    return this.checkbox_render_charges;
  }

  public Checkbox getCheckboxRenderHiddenLinks() {
    return this.checkbox_render_hidden_links;
  }

  public Checkbox getCheckboxRenderHiddenNodes() {
    return this.checkbox_render_hidden_nodes;
  }

  public Checkbox getCheckboxRenderHiddenPolygons() {
    return this.checkbox_render_hidden_polygons;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
