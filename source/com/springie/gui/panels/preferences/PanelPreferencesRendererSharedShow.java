// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.commands.DoubleBufferOldMessage;
import com.springie.preferences.Preferences;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

public class PanelPreferencesRendererSharedShow {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_shared = FrEnd.setUpPanelForFrame();

  /**
   * The "Main" sub-tab of the Renderer tab: the primary renderer
   * options live here.
   */
  public Panel panel_main = FrEnd.setUpPanelForFrame2();

  /**
   * The "Bins" sub-tab of the Renderer tab: bin size and bin display.
   */
  public Panel panel_bins = FrEnd.setUpPanelForFrame2();

  /**
   * The "Fog" sub-tab of the Renderer tab: the fog controls.
   */
  public Panel panel_fog = FrEnd.setUpPanelForFrame2();


  public Checkbox checkbox_redraw_deepest_first;

  public Checkbox checkbox_render_hidden_nodes;

  public Checkbox checkbox_render_hidden_links;

  public Checkbox checkbox_render_hidden_polygons;

  public Checkbox checkbox_render_nodes;

  public Checkbox checkbox_render_links;

  public Checkbox checkbox_render_polygons;

  private Checkbox checkbox_render_charges;

  public Checkbox checkbox_scenic_background;

  public Checkbox checkbox_db_old;

  public PanelPreferencesRendererSharedShow() {
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

    this.panel_main.add(panel_render_normal);
    this.panel_main.add(panel_render_charges);
    this.panel_main.add(panel_render_hidden);
    this.panel_main.add(getDoubleBufferingPanel());
    this.panel_main.add(getBackgroundPanel());

    // The Renderer tab is split into sub-tabs: the bin controls and the
    // fog controls each get their own, everything else stays on Main.
    final TabbedPanel sub_tabs = new TabbedPanel();
    sub_tabs.add("Main", this.panel_main);
    sub_tabs.add("Bins", this.panel_bins);
    sub_tabs.add("Fog", this.panel_fog);
    this.panel.setLayout(new BorderLayout());
    this.panel.add(sub_tabs, BorderLayout.CENTER);
  }
//    this.panel.add(panel_redraw_deepest_first);
//    this.panel.add(panel_fog);
//    this.panel.add(panel_visible_explosions);
//    this.panel.add(panel_face_render_number);

  private Panel getBackgroundPanel() {
    final Panel panel_background = new Panel();
    panel_background.add(new Label("Background:"));
    this.checkbox_scenic_background = new Checkbox("Grass/sky",
        RendererDelegator.scenic_background);
    this.checkbox_scenic_background.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        RendererDelegator.scenic_background =
            ((Checkbox) e.getSource()).getState();
        // The background is baked into the cached bin tiles; a full
        // repaint is needed, not just the damage-repair pass.
        RendererDelegator.repaint_all_objects = true;
      }
    });
    panel_background.add(this.checkbox_scenic_background);
    return panel_background;
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

  /**
   * Restores the default visibility checkboxes: everything rendered, nothing
   * hidden.
   */
  public void resetToDefaults() {
    // The listeners copy the checkbox state into the FrEnd statics.
    this.checkbox_render_nodes.setState(true);
    this.checkbox_render_links.setState(true);
    this.checkbox_render_polygons.setState(true);
    getCheckboxRenderCharges().setState(true);

    getCheckboxRenderHiddenNodes().setState(false);
    getCheckboxRenderHiddenLinks().setState(false);
    getCheckboxRenderHiddenPolygons().setState(false);
    this.checkbox_scenic_background.setState(false);
    if (this.checkbox_db_old != null) {
      this.checkbox_db_old.setState(false);
    }

    // In case a checkbox was already in its default state (no item event).
    FrEnd.render_nodes = true;
    FrEnd.render_links = true;
    FrEnd.render_faces = true;
    FrEnd.render_charges = true;
    FrEnd.render_hidden_nodes = false;
    FrEnd.render_hidden_links = false;
    FrEnd.render_hidden_faces = false;
    RendererDelegator.scenic_background = false;
    FrEnd.preferences.map.put(Preferences.renderer_old_double_buffer,
        Boolean.FALSE);

    RendererDelegator.repaintAll();
  }

  /**
   * The old double-buffering toggle. Moved here from the Original
   * renderer panel so it lives on the Renderer tab's Main sub-tab.
   */
  private Panel getDoubleBufferingPanel() {
    final Panel panel_double_buffering = new Panel();
    this.checkbox_db_old = new Checkbox(GUIStrings.DB,
        RendererDelegator.isUnderlyingOldDoubleBuffer());
    this.checkbox_db_old.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.new_message_manager.add(new DoubleBufferOldMessage());
      }
    });
    panel_double_buffering.add(this.checkbox_db_old);
    return panel_double_buffering;
  }

}
