// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.constants.Quality;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceRenderTypes;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkRenderType;
import com.springie.elements.nodes.Node;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TabbedPanel;
import com.springie.gui.components.TTChoice;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

public class PanelPreferencesRendererOriginal {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_main = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public Checkbox checkbox_db;

  public Checkbox checkbox_longlinks;

  public Checkbox checkbox_shortlinks;

  TTChoice choose_polygon_render_type;

  private Scrollbar scroll_bar_link_render_cables;

  private Label label_cable_render_number;

  private Label label_strut_render_number;

  private Label label_node_render_number;

  public PanelPreferencesRendererOriginal(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    tab.add("Options", this.panel_main);
    
    tab.add("Stereo 3D",  FrEnd.panel_preferences_stereo3d.panel);

    this.panel.add(tab);
    
    final Panel panel_node_render_type = panelNodeRenderType();
    
    final TTChoice choose_display_struts = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        Link.link_display_struts_type = FrEnd.choose_display_struts
            .str_to_num(scs);
        RendererDelegator.repaintAll();
      }
    });

    FrEnd.choose_display_struts = choose_display_struts;

    final Panel panel_link_display_struts = new Panel();
    panel_link_display_struts.add(new Label("Struts are", Label.RIGHT));
    addLinkDisplayTypes(choose_display_struts);
    choose_display_struts.choice.select(choose_display_struts
        .num_to_str(Link.link_display_struts_type));
    panel_link_display_struts.add(choose_display_struts.choice);

    final TTChoice choose_display_cables = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        Link.link_display_cables_type = FrEnd.choose_display_cables
            .str_to_num(scs);
        RendererDelegator.repaintAll();
      }
    });

    FrEnd.choose_display_cables = choose_display_cables;

    final Panel panel_link_display_cable = new Panel();
    panel_link_display_cable.add(new Label("Cables are", Label.RIGHT));
    addLinkDisplayTypes(choose_display_cables);
    choose_display_cables.choice.select(choose_display_cables
        .num_to_str(Link.link_display_cables_type));

    panel_link_display_cable.add(choose_display_cables.choice);

    final Panel panel_face_render_type = chooseFaceRenderType();
//    final Panel panel_face_render_number = getFaceRenderNumber();
    final Panel panel_strut_render_number = getStrutRenderNumber();
    final Panel panel_cable_render_number = getCableRenderNumber();
    final Panel panel_node_render_number = getNodeRenderNumber();
    //final Panel panel_bin_size = getBinSizePanel();
    //final Panel panel_fog = getFogPanel();

    final CheckboxGroup checkbox_linklength = new CheckboxGroup();

    final Panel panel_link_length = new Panel();
    panel_link_length.add(new Label("Struts and cables are:", Label.RIGHT));
    this.checkbox_shortlinks = new Checkbox("short", checkbox_linklength, true);
    this.checkbox_shortlinks.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_LINKLENGTH, Link.SHORT, 0);
      }
    });

    this.checkbox_longlinks = new Checkbox("long", checkbox_linklength, false);
    this.checkbox_longlinks.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_LINKLENGTH, Link.LONG, 0);
      }
    });

    panel_link_length.add(this.checkbox_shortlinks);
    panel_link_length.add(this.checkbox_longlinks);

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

//    final Panel panel_new_double_buffering = new Panel();
//    this.checkbox_db_new = new Checkbox(GUIStrings.DB_NEW, RendererDelegator
//        .isNewDoubleBuffer());
//
//    this.checkbox_db_new.addItemListener(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        Forget.about(e);
//        getMessageManager().newmessage(Message.MSG_DB_NEW, 0, 0);
//      }
//    });
//
//    panel_new_double_buffering.add(this.checkbox_db_new);

    final Panel panel_double_buffering = new Panel();
    this.checkbox_db = new Checkbox(GUIStrings.DB, RendererDelegator
        .isUnderlyingOldDoubleBuffer()); // TODO: default?

    this.checkbox_db.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_DOUBLE_BUFFER_OLD, 0, 0);
      }
    });

    panel_double_buffering.add(this.checkbox_db);

    final Checkbox checkbox_xor = new Checkbox(GUIStrings.XOR, FrEnd.xor);
    checkbox_xor.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.xor = ((Checkbox) e.getSource()).getState();
        RendererDelegator.repaintAll();
      }
    });
    if (FrEnd.development_version) {
    panel_double_buffering.add(checkbox_xor);
    }

    // Old preferences...

    this.panel_main.add(panel_node_render_type);
    this.panel_main.add(panel_node_render_number);

    this.panel_main.add(panel_link_display_struts);
    this.panel_main.add(panel_strut_render_number);

    this.panel_main.add(panel_link_display_cable);
    this.panel_main.add(panel_cable_render_number);

    this.panel_main.add(panel_link_length);

    this.panel_main.add(panel_face_render_type);
    //this.panel_main.add(panel_face_render_number);

    this.panel_main.add(panel_double_buffering);
  }

  private Panel panelNodeRenderType() {
    final Panel panel_node_render_type = new Panel();
    panel_node_render_type.add(new Label("Nodes are:", Label.RIGHT));

    FrEnd.choose_quality = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        FrEnd.quality = FrEnd.choose_quality.str_to_num(scs);
        RendererDelegator.repaintAll();
      }
    });
    final TTChoice choose_quality = FrEnd.choose_quality;

    choose_quality.add("thick outline", Quality.THICK_OUTLINE);
    // choose_quality.add("thin outline", Quality.THIN_OUTLINE);
    choose_quality.add("solid", Quality.SOLID);
    choose_quality.add("multiple", Quality.MULTIPLE);
    // choose_quality.add("< medium", Quality._QUALITY_2A);
    choose_quality.add("poor", Quality.SOLID);
    choose_quality.add("< poor", Quality._QUALITY_1A);
    choose_quality.add("terrible", Quality._QUALITY_3);
    choose_quality.add("< terrible", Quality._QUALITY_3A);
    choose_quality.add("abysmal", Quality._QUALITY_4);
    choose_quality.add("< abysmal", Quality._QUALITY_4A);
    choose_quality.add("dots only", Quality._QUALITY_5);
    choose_quality.add("gone", Quality._QUALITY_6);
    choose_quality.choice.select(choose_quality.num_to_str(FrEnd.quality));
    panel_node_render_type.add(choose_quality.choice);
    return panel_node_render_type;
  }

//  private Panel panelNodePolyhedron() {
//    final Panel panel = new Panel();
//    panel.add(new Label("Node polyhedron:", Label.RIGHT));
//
//    this.choose_polyhedron = new TTChoice(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        final String scs = (String) e.getItem();
//        final int val = PanelPreferencesRendererOriginal.this.choose_polyhedron.str_to_num(scs);
//        if (val == 1) {
//          ModularRendererNew.sphere_object = new SimpleDodecahedron();
//        } else if (val == 2) {
//          ModularRendererNew.sphere_object = new SimpleOctahedron();
//        } else if (val == 3) {
//          ModularRendererNew.sphere_object = new SimpleCube();
//        } else if (val == 4) {
//          ModularRendererNew.sphere_object = new SimpleIcosahedron();
//        }
//        RendererDelegator.repaint_all_objects = true;
//      }
//    });
//
//    this.choose_polyhedron.add("Dodecahedron", 1);
//    this.choose_polyhedron.add("Octahedron", 2);
//    this.choose_polyhedron.add("Cube", 3);
//    this.choose_polyhedron.add("Icosahedron", 4);
//    this.choose_polyhedron.choice.select(this.choose_polyhedron
//        .num_to_str(1));
//    panel.add(this.choose_polyhedron.choice);
//
//    return panel;
//  }
  
//  private Panel panelColourModifierWireframe() {
//    final Panel panel = new Panel();
//    panel.add(new Label("Wireframe:", Label.RIGHT));
//
//    this.choose_colour_modifier_wireframe = new TTChoice(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        final String scs = (String) e.getItem();
//        final int val = PanelPreferencesRendererOriginal.this.choose_colour_modifier_wireframe.str_to_num(scs);
//        RendererBinManager.colour_modifier_wireframe = val;
//        RendererDelegator.repaint_all_objects = true;
//      }
//    });
//
//    addColourOptions(this.choose_colour_modifier_wireframe);
//
//    this.choose_colour_modifier_wireframe.choice.select(this.choose_colour_modifier_filled
//        .num_to_str(RendererBinManager.colour_modifier_wireframe));
//    panel.add(this.choose_colour_modifier_wireframe.choice);
//
//    return panel;
//  }
//  
//  private Panel panelColourModifierFilled() {
//    final Panel panel = new Panel();
//    panel.add(new Label("Filled:", Label.RIGHT));
//
//    this.choose_colour_modifier_filled = new TTChoice(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        final String scs = (String) e.getItem();
//        final int val = PanelPreferencesRendererOriginal.this.choose_colour_modifier_filled.str_to_num(scs);
//        RendererBinManager.colour_modifier_filled = val;
//        RendererDelegator.repaint_all_objects = true;
//      }
//    });
//
//    addColourOptions(this.choose_colour_modifier_filled);
//
//    this.choose_colour_modifier_filled.choice.select(this.choose_colour_modifier_filled
//        .num_to_str(RendererBinManager.colour_modifier_filled));
//    panel.add(this.choose_colour_modifier_filled.choice);
//
//    return panel;
//  }
//
//  private void addColourOptions(TTChoice choice) {
//    choice.add("Disabled", ColourModifier.disabled);
//    choice.add("Natural", ColourModifier.natural);
//    choice.add("Lighter", ColourModifier.lighter);
//    choice.add("Darker", ColourModifier.darker);
//    choice.add("White", ColourModifier.white);
//    choice.add("Grey", ColourModifier.grey);
//    choice.add("Black", ColourModifier.black);
//  }

  private void addLinkDisplayTypes(final TTChoice choose_display_cables) {
    choose_display_cables.add("filled", LinkRenderType.SOLID);
    choose_display_cables.add("multiple", LinkRenderType.MULTIPLE);
    choose_display_cables.add("dotted", LinkRenderType.DOTTED);
    choose_display_cables.add("circle (thin)", LinkRenderType.CIRCLE_THIN);
    choose_display_cables.add("circle (filled)", LinkRenderType.CIRCLE_THICK);
    choose_display_cables.add("point", LinkRenderType.POINT);
    choose_display_cables.add("invisible", LinkRenderType.INVISIBLE);
  }

  private Panel chooseFaceRenderType() {
    final Panel panel_polygon_render_type = new Panel();
    // panel_quality.setBackground(colour_grey1);
    panel_polygon_render_type.add(new Label("Polygon fill:", Label.RIGHT));

    this.choose_polygon_render_type = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        Face.face_display_type = getChoosePolygonRenderType().str_to_num(scs);
        RendererDelegator.repaintAll();
      }
    });

    this.choose_polygon_render_type.add("concentric",
        FaceRenderTypes.CONCENTRIC);
    this.choose_polygon_render_type.add("segmented", FaceRenderTypes.SEGMENTED);
    this.choose_polygon_render_type.add("clockwise", FaceRenderTypes.CLOCKWISE);
    this.choose_polygon_render_type.add("anti-clockwise",
        FaceRenderTypes.ANTI_CLOCKWISE);
    this.choose_polygon_render_type.choice
        .select(this.choose_polygon_render_type
            .num_to_str(Face.face_display_type));
    panel_polygon_render_type.add(this.choose_polygon_render_type.choice);
    return panel_polygon_render_type;
  }

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
//  private Panel getBinSizePanel() {
//    final Panel panel = new Panel();
//    panel.setLayout(new BorderLayout(0, 8));
//    panel.add("West", new Label("Bin size:", Label.RIGHT));
//
//    final Scrollbar scroll_bar = new Scrollbar(Scrollbar.HORIZONTAL,
//        RendererBinManager.divisor, 100, 50, 500);
//    scroll_bar.addAdjustmentListener(new AdjustmentListener() {
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        final int temp = e.getValue();
//        RendererBinManager.divisor = temp;
//        reflectBinSizeNumber();
//        FrEnd.main_canvas.forceResize();
//      }
//    });
//
//    panel.add("Center", scroll_bar);
//
//    this.label_bin_size_number = new Label("", Label.LEFT);
//    panel.add("East", this.label_bin_size_number);
//    reflectBinSizeNumber();
//
//    return panel;
//  }
//
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

  private Panel getStrutRenderNumber() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Strut lines:", Label.RIGHT));

    this.scroll_bar_link_render_cables = new Scrollbar(Scrollbar.HORIZONTAL,
        Link.number_of_strut_render_divisions, 4, 0, 22);
    this.scroll_bar_link_render_cables
        .addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e) {
            final int temp = e.getValue();
            Link.number_of_strut_render_divisions = temp;
            reflectLabelStrutRenderNumber();
            RendererDelegator.repaintAll();
          }
        });

    panel.add("Center", this.scroll_bar_link_render_cables);

    this.label_strut_render_number = new Label("", Label.LEFT);
    panel.add("East", this.label_strut_render_number);
    reflectLabelStrutRenderNumber();

    return panel;
  }

  private Panel getCableRenderNumber() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Cable lines:", Label.RIGHT));

    this.scroll_bar_link_render_cables = new Scrollbar(Scrollbar.HORIZONTAL,
        Link.number_of_cable_render_divisions, 4, 0, 22);
    this.scroll_bar_link_render_cables
        .addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e) {
            final int temp = e.getValue();
            Link.number_of_cable_render_divisions = temp;
            reflectLabelCableRenderNumber();
            RendererDelegator.repaintAll();
          }
        });

    panel.add("Center", this.scroll_bar_link_render_cables);

    this.label_cable_render_number = new Label("", Label.LEFT);
    panel.add("East", this.label_cable_render_number);
    reflectLabelCableRenderNumber();

    return panel;
  }

  private Panel getNodeRenderNumber() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Node lines:", Label.RIGHT));

    final Scrollbar scroll_bar_render_nodes = new Scrollbar(
        Scrollbar.HORIZONTAL, Node.number_of_render_divisions, 4, 0, 12);
    scroll_bar_render_nodes.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        final int temp = e.getValue();
        Node.number_of_render_divisions = temp;
        reflectLabelNodeRenderNumber();
        RendererDelegator.repaintAll();
      }
    });

    panel.add("Center", scroll_bar_render_nodes);

    this.label_node_render_number = new Label("", Label.LEFT);
    panel.add("East", this.label_node_render_number);
    reflectLabelNodeRenderNumber();

    return panel;
  }

//  private void reflectLabelFaceRenderNumber() {
//    getLabelFaceRenderNumber().setText("" + Face.number_of_render_divisions);
//  }

  //private void reflectBinSizeNumber() {
    //getLabelBinSizeNumber().setText("" + RendererBinManager.divisor);
  //}

  //private void reflectLabelFog() {
    //getLabelFog().setText("" + (DeepObjectColourCalculator.factor / 10));
  //}

  private void reflectLabelCableRenderNumber() {
    getLabelCableRenderNumber().setText(
        "" + Link.number_of_cable_render_divisions);
  }

  private void reflectLabelStrutRenderNumber() {
    getLabelStrutRenderNumber().setText(
        "" + Link.number_of_strut_render_divisions);
  }

  private void reflectLabelNodeRenderNumber() {
    getLabelNodeRenderNumber().setText("" + Node.number_of_render_divisions);
  }

  //public Label getLabelFog() {
    //return this.label_fog;
  //}

//  public Label getLabelFaceRenderNumber() {
//    return this.label_face_render_number;
//  }
//
  //public Label getLabelBinSizeNumber() {
    //return this.label_bin_size_number;
  //}

  public Label getLabelCableRenderNumber() {
    return this.label_cable_render_number;
  }

  public Label getLabelNodeRenderNumber() {
    return this.label_node_render_number;
  }

  public Label getLabelStrutRenderNumber() {
    return this.label_strut_render_number;
  }

//  public Checkbox getCheckboxRenderLinks() {
//    return this.checkbox_render_links;
//  }
//
//  public Checkbox getCheckboxRenderNodes() {
//    return this.checkbox_render_nodes;
//  }
//
//  public Checkbox getCheckboxRenderPolygons() {
//    return this.checkbox_render_polygons;
//  }
//
//  public Checkbox getCheckboxRenderCharges() {
//    return this.checkbox_render_charges;
//  }
//
//  public Checkbox getCheckboxRenderHiddenLinks() {
//    return this.checkbox_render_hidden_links;
//  }
//
//  public Checkbox getCheckboxRenderHiddenNodes() {
//    return this.checkbox_render_hidden_nodes;
//  }
//
//  public Checkbox getCheckboxRenderHiddenPolygons() {
//    return this.checkbox_render_hidden_polygons;
//  }

  public TTChoice getChoosePolygonRenderType() {
    return this.choose_polygon_render_type;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}