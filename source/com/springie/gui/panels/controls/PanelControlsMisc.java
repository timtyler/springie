// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.constants.ToolTypes;
import com.springie.context.ContextMananger;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TTChoice;
import com.springie.gui.components.TextFieldWrapper;
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.springie.modification.faces.FaceReverser;
import com.springie.modification.links.ExtendLinks;
import com.springie.modification.projection.PrismaticProjection;
import com.springie.modification.stellation.DimpleMaker;
import com.tifsoft.Forget;

public class PanelControlsMisc {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  NewMessageManager new_message_manager;

  public TextFieldWrapper textfield_add_polygons;

  public TextFieldWrapper textfield_add_stellations;

  public TextFieldWrapper textfield_add_stellation_vertex_count;

  public TextFieldWrapper textfield_add_inner_hex_nodes;

  public TextFieldWrapper textfield_add_nodes_inner_eden;

  public TextFieldWrapper textfield_add_triaxial_nodes;

  public TextFieldWrapper textfield_split_links;

  public TextFieldWrapper textfield_automatic_node_radius;

  public TextFieldWrapper textfield_automatic_link_radius;

  public TextFieldWrapper textfield_link_nearest_n_nodes;

  //public Button button_edit_remove_link_and_fuse_ends;

  //public Button button_edit_fuse_nodes;

  public Button button_add_faces;

  public Button button_add_stellations;

  public Button button_split_links;

  public Button button_edit_equalise_link_lengths;

  public Button button_edit_reset_link_lengths;

  public Button button_automatic_node_radius;

  public Button button_automatic_link_radius;

  public Button button_hex_triaxial;

  public Checkbox checkbox_split_links_remove_old;

  public Button button_add_central_hub;

  public TextFieldWrapper textfield_prismatic_projection;

  public Button button_connect_nodes_to_nearest_nodes;

  public Button button_face_reverse;

  public Button button_cartesian_colourer;

  public Button button_dimple;

  public Button button_extend_links;

  TextFieldWrapper textfield_extend_links;

  public PanelControlsMisc(MessageManager message_manager,
      NewMessageManager new_message_manager) {
    this.message_manager = message_manager;
    this.new_message_manager = new_message_manager;
    makeEditMiscPanel();
  }

  void makeEditMiscPanel() {
    final Button button_add_nodes_inner_eden = new Button(
        GUIStrings.EDIT_ADD_NODES_INNER_EDEN);
    button_add_nodes_inner_eden.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_ADD_NODES_INNER_EDEN, 0, 0);
      }
    });
    final Panel panel_add_nodes_inner_eden = new Panel();
    panel_add_nodes_inner_eden.add(button_add_nodes_inner_eden);
    this.textfield_add_nodes_inner_eden = new TextFieldWrapper("-0.18");
    panel_add_nodes_inner_eden.add(this.textfield_add_nodes_inner_eden);

    this.button_split_links = new Button(GUIStrings.EDIT_SPLIT_LINKS);
    this.button_split_links.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_SPLIT_LINKS, 0, 0);
      }
    });
    final Panel panel_split_links = new Panel();
    panel_split_links.add(this.button_split_links);
    this.textfield_split_links = new TextFieldWrapper("2");
    panel_split_links.add(this.textfield_split_links);
    this.checkbox_split_links_remove_old = new Checkbox("kill");
    panel_split_links.add(this.checkbox_split_links_remove_old);

    this.button_add_stellations = new Button(GUIStrings.EDIT_ADD_STELLATIONS);
    this.button_add_stellations.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_ADD_STELLATIONS, 0, 0);
      }
    });

    final Panel panel_add_stellations = new Panel();
    panel_add_stellations.add(this.button_add_stellations);
    this.textfield_add_stellations = new TextFieldWrapper("0.2");
    panel_add_stellations.add(this.textfield_add_stellations);

    final Button button_prismatic_projection = new Button(
        GUIStrings.EDIT_PRISMATIC_PROJECTION);
    button_prismatic_projection.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            final PrismaticProjection prismatic_projection = new PrismaticProjection(
                ContextMananger.getNodeManager());
            final float sf3 = Float
                .parseFloat(FrEnd.panel_edit_misc.textfield_prismatic_projection
                    .getText());
            prismatic_projection.project(sf3);
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });

    this.button_extend_links = new Button(GUIStrings.EDIT_EXTEND_LINKS);
    this.button_extend_links.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            final ExtendLinks extend_links = new ExtendLinks(ContextMananger.getNodeManager());
            final float sf3 = Float
                .parseFloat(FrEnd.panel_edit_misc.textfield_extend_links
                    .getText());
            extend_links.extend(sf3);
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });

    this.button_dimple = new Button(GUIStrings.EDIT_DIMPLE);
    this.button_dimple.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            final DimpleMaker dimple_maker = new DimpleMaker(ContextMananger.getNodeManager());
            dimple_maker.dimple();
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });
    final Panel panel_dimple = new Panel();
    panel_dimple.add(this.button_dimple);

    this.button_face_reverse = new Button(GUIStrings.FACE_REVERSE);
    this.button_face_reverse.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            final FaceReverser face_reverser = new FaceReverser(
                ContextMananger.getNodeManager());
            face_reverser.reverse();
            FrEnd.postCleanup();
            return null;
          }
        });
      }
    });
    final Panel panel_face_reverse = new Panel();
    panel_face_reverse.add(this.button_face_reverse);

    final Panel panel_prismatic_projection = new Panel();
    panel_prismatic_projection.add(button_prismatic_projection);
    this.textfield_prismatic_projection = new TextFieldWrapper("-0.2");
    panel_prismatic_projection.add(this.textfield_prismatic_projection);

    final Panel panel_extend_links = new Panel();
    panel_extend_links.add(this.button_extend_links);
    this.textfield_extend_links = new TextFieldWrapper("0.5");
    panel_extend_links.add(this.textfield_extend_links);

    this.button_hex_triaxial = new Button(GUIStrings.EDIT_ADD_NODES_TRIAXIAL);
    this.button_hex_triaxial.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_ADD_NODES_TRIAXIAL, 0, 0);
      }
    });
    final Panel panel_add_nodes_triaxial = new Panel();
    panel_add_nodes_triaxial.add(this.button_hex_triaxial);
    this.textfield_add_triaxial_nodes = new TextFieldWrapper("-0.17");
    panel_add_nodes_triaxial.add(this.textfield_add_triaxial_nodes);

    this.button_connect_nodes_to_nearest_nodes = new Button(
        GUIStrings.EDIT_ADD_LINKS_TO_NEAREST);
    this.button_connect_nodes_to_nearest_nodes
        .addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Forget.about(e);
            getMessageManager().sendMessage(
                Message.MSG_CONNECT_NODES_TO_NEAREST_NODES, 0, 0);
          }
        });
    final Panel panel_add_links_outer_hex = new Panel();
    panel_add_links_outer_hex.add(this.button_connect_nodes_to_nearest_nodes);
    this.textfield_link_nearest_n_nodes = new TextFieldWrapper("3");
    panel_add_links_outer_hex.add(this.textfield_link_nearest_n_nodes);

    this.textfield_add_polygons = new TextFieldWrapper("3");

    this.button_add_faces = new Button(GUIStrings.EDIT_ADD_POLYGONS);
    this.button_add_faces.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_ADD_POLYGONS, 0, 0);
      }
    });
    final Panel panel_add_polygons = new Panel();
    panel_add_polygons.add(this.button_add_faces);
    panel_add_polygons.add(this.textfield_add_polygons);

    // ...

//    final Panel panel_edit_remove_link_and_fuse_ends = getPanelRemoveLinkAndFuseEnds();
//    final Panel panel_edit_fuse_selected_nodes = getPanelFuseSelectedNodes();

    // button_edit_fuse_nodes

    final Panel panel_tool = new Panel();
    panel_tool.add(new Label("Use", Label.RIGHT));

    FrEnd.choose_tool = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }

        final String stateChangedString = (String) (e.getItem());

        if (FrEnd.choose_tool.str_to_num(stateChangedString) != -99) {
          FrEnd.weapon_type = FrEnd.choose_tool.str_to_num(stateChangedString);
        }
      }
    });

    FrEnd.choose_tool.add("a pencil", ToolTypes._PENCIL);
    FrEnd.choose_tool.add("a brush", ToolTypes._BRUSH);
    FrEnd.choose_tool.add("a toothbrush", ToolTypes._MACHINE);
    FrEnd.choose_tool.add("an aerosol", ToolTypes._SPRAY);
    FrEnd.choose_tool.add("potato printing", ToolTypes._POTATO);
    FrEnd.choose_tool.choice.select(FrEnd.choose_tool
        .num_to_str(FrEnd.weapon_type));
    panel_tool.add(FrEnd.choose_tool.choice);

//    // MAKE_MOTIONLESS
//    this.button_edit_make_motionless = new Button(
//        GUIStrings.DOME_MAKE_MOTIONLESS);
//    this.button_edit_make_motionless.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        Forget.about(e);
//        getMessageManager().sendMessage(Message.MSG_DOME_MAKE_MOTIONLESS, 0, 0);
//      }
//    });
//    final Panel panel_edit_make_motionless = new Panel();
//    panel_edit_make_motionless.add(this.button_edit_make_motionless);
//
    this.panel.add(panel_dimple);

    this.panel.add(getEqualiseLinkLengthsPanel());
    this.panel.add(getResetLinkLengthsPanel());

    this.panel.add(panel_add_polygons);

    this.panel.add(panel_add_stellations);

    this.panel.add(panel_extend_links);

    this.panel.add(panel_split_links);

    this.panel.add(getAddCentralHubPanel());

    this.panel.add(getAutomaticLinkRadiusPanel());
    this.panel.add(getAutomaticNodeRadiusPanel());
    this.panel.add(getCartesianColourerPanel());
    
    this.panel.add(panel_prismatic_projection);

    this.panel.add(panel_add_nodes_triaxial);

    this.panel.add(panel_add_links_outer_hex);

    //this.panel.add(panel_edit_make_motionless);

    this.panel.add(panel_face_reverse);

    // this.panel.add(getSpreadSelectionViaLinksPanel());
    //
    // this.panel.add(getSelectAllNodesWithNLinkPanel());
    // this.panel.add(getSelectAllFacesWithNSidesPanel());
    // this.panel.add(getClearPanel());
    //
    if (FrEnd.development_version) {
      this.panel.add(panel_tool);
    }
  }

//  private Panel getPanelRemoveLinkAndFuseEnds() {
//    this.button_edit_remove_link_and_fuse_ends = new Button(
//        GUIStrings.EDIT_REMOVE_LINK_AND_FUSE_ENDS);
//    this.button_edit_remove_link_and_fuse_ends
//        .addActionListener(new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            Forget.about(e);
//            getMessageManager().sendMessage(
//                Message.MSG_EDIT_REMOVE_LINK_AND_FUSE_ENDS, 0, 0);
//          }
//        });
//    final Panel panel_edit_remove_link_and_fuse_ends = new Panel();
//    panel_edit_remove_link_and_fuse_ends
//        .add(this.button_edit_remove_link_and_fuse_ends);
//    return panel_edit_remove_link_and_fuse_ends;
//  }
//
//  private Panel getPanelFuseSelectedNodes() {
//    this.button_edit_fuse_nodes = new Button(
//        GUIStrings.EDIT_FUSE_SELECTED_NODES);
//    this.button_edit_fuse_nodes
//        .addActionListener(new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            Forget.about(e);
//            getNewMessageManager().add(new NewMessage(null) {
//              public Object execute() {
//                Log.log("EXEXC!");
//                final FuseSelectedNodes fsn = new FuseSelectedNodes(
//                    ContextMananger.getNodeManager());
//                fsn.action();
//                return null;
//              }
//            });
//          }
//        });
//    final Panel panel_edit_fuse_selected_nodes = new Panel();
//    panel_edit_fuse_selected_nodes.add(this.button_edit_fuse_nodes);
//    return panel_edit_fuse_selected_nodes;
//  }

  private Panel getResetLinkLengthsPanel() {
    this.button_edit_reset_link_lengths = new Button(
        GUIStrings.DOME_LINKS_RESET_LENGTHS);
    this.button_edit_reset_link_lengths.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_DOME_LINKS_RESET_LENGTHS,
            0, 0);
      }
    });
    final Panel panel_edit_reset_link_lengths = new Panel();
    panel_edit_reset_link_lengths.add(this.button_edit_reset_link_lengths);
    return panel_edit_reset_link_lengths;
  }

  private Panel getEqualiseLinkLengthsPanel() {
    this.button_edit_equalise_link_lengths = new Button(
        GUIStrings.DOME_LINKS_EQUALISE_LINK_LENGTHS);
    this.button_edit_equalise_link_lengths
        .addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Forget.about(e);
            getMessageManager().sendMessage(
                Message.MSG_DOME_LINKS_EQUALISE_LINK_LENGTHS, 0, 0);
          }
        });

    final Panel panel_edit_equalise_link_lengths = new Panel();
    panel_edit_equalise_link_lengths
        .add(this.button_edit_equalise_link_lengths);
    return panel_edit_equalise_link_lengths;
  }

  private Panel getAutomaticLinkRadiusPanel() {
    this.button_automatic_link_radius = new Button(
        GUIStrings.EDIT_AUTOMATIC_LINK_RADIUS);
    this.button_automatic_link_radius.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_AUTOMATIC_LINK_RADIUS,
            0, 0);
      }
    });

    final Panel panel_automatic_link_radius = new Panel();
    panel_automatic_link_radius.add(this.button_automatic_link_radius);
    this.textfield_automatic_link_radius = new TextFieldWrapper("0.05");
    panel_automatic_link_radius.add(this.textfield_automatic_link_radius);

    return panel_automatic_link_radius;
  }

  private Panel getAutomaticNodeRadiusPanel() {
    this.button_automatic_node_radius = new Button(
        GUIStrings.EDIT_AUTOMATIC_NODE_RADIUS);
    this.button_automatic_node_radius.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_AUTOMATIC_NODE_RADIUS,
            0, 0);
      }
    });

    final Panel panel_automatic_node_radius = new Panel();
    panel_automatic_node_radius.add(this.button_automatic_node_radius);
    this.textfield_automatic_node_radius = new TextFieldWrapper("1.5");
    panel_automatic_node_radius.add(this.textfield_automatic_node_radius);

    return panel_automatic_node_radius;
  }

  private Panel getAddCentralHubPanel() {
    this.button_add_central_hub = new Button(GUIStrings.EDIT_ADD_CENTRAL_HUB);
    this.button_add_central_hub.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager().sendMessage(Message.MSG_EDIT_ADD_CENTRAL_HUB, 0, 0);
      }
    });
    final Panel panel = new Panel();
    panel.add(this.button_add_central_hub);
    return panel;
  }

  // private Panel getRemoveFacesPanel() {
  // this.button_edit_remove_polygons = new Button(
  // GUIStrings.DOME_REMOVE_POLYGONS);
  // this.button_edit_remove_polygons.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // getMessageManager().newmessage(Message.MSG_EDIT_REMOVE_POLYGONS, 0, 0);
  // }
  // });
  // final Panel panel = new Panel();
  // panel.add(this.button_edit_remove_polygons);
  // return panel;
  // }
  //
  // private Panel getRemoveLinksPanel() {
  // this.button_edit_remove_links = new Button(GUIStrings.DOME_REMOVE_LINKS);
  // this.button_edit_remove_links.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // getMessageManager().newmessage(Message.MSG_EDIT_REMOVE_LINKS, 0, 0);
  // }
  // });
  // final Panel panel = new Panel();
  // panel.add(this.button_edit_remove_links);
  // return panel;
  // }
  //
  // private Panel getSpreadSelectionViaLinksPanel() {
  // this.button_edit_spread_selection_via_links = new
  // Button(GUIStrings.SPREAD_SELECTION_VIA_LINKS);
  // this.button_edit_spread_selection_via_links.addActionListener(new
  // ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // getMessageManager().newmessage(Message.MSG_EDIT_SPREAD_SELECTION_VIA_LINKS,
  // 0, 0);
  // }
  // });
  // final Panel panel = new Panel();
  // panel.add(this.button_edit_spread_selection_via_links);
  // return panel;
  // }
  //
  // private Panel getClearPanel() {
  // final Button button_clear = new Button(GUIStrings.BUTTON_CLEAR);
  // button_clear.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // getNewMessageManager().add(new NewMessage(null) {
  // public Object execute() {
  // ContextMananger.getNodeManager().initialSetUp();
  //
  // FrEnd.last_file_path = null;
  //
  // FrEnd.reflectStatusInGUI();
  // return null;
  // }
  // });
  //        
  // //ewmessage(Message.MSG_CLEAR, 0, 0);
  //        
  // // case Message.MSG_CLEAR:
  // // ContextMananger.getNodeManager().initialSetUp();
  // //
  // // FrEnd.last_file_path = null;
  // //
  // // FrEnd.reflectStatusInGUI();
  // //
  // // break;
  //
  //
  // }
  // });
  // final Panel panel = new Panel();
  // panel.add(button_clear);
  // return panel;
  // }

  // private Panel getSelectAllFacesWithNSidesPanel() {
  // final Panel panel = new Panel();
  // Button button_select_faces;
  //
  // button_select_faces = new Button("Select faces with");
  // button_select_faces.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // getMessageManager().newmessage(
  // Message.MSG_SELECT_ALL_FACES_WITH_N_SIDES, 0, 0);
  // }
  // });
  // panel.add(button_select_faces);
  //
  // this.textfield_select_faces_with_n_sides = new TextFieldWrapper("5");
  // panel.add(this.textfield_select_faces_with_n_sides);
  //
  // final Label label_select = new Label("sides", Label.LEFT);
  // panel.add(label_select);
  //
  // return panel;
  // }
  //  
  // private Panel getSelectAllNodesWithNLinkPanel() {
  // final Panel panel = new Panel();
  //
  // this.button_select_nodes = new Button("Select nodes with");
  // this.button_select_nodes.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // getNewMessageManager().add(new NewMessage(null) {
  // public Object execute() {
  // FrEnd.prepareToModifyNodeTypes();
  // final int n_links = Integer
  // .parseInt(FrEnd.panel_edit_misc.textfield_select_nodes_with_n_links
  // .getText());
  // ContextMananger.getNodeManager().selectAllWithNLinks(n_links);
  // FrEnd.postCleanup();
  // return null;
  // }
  // });
  // }
  // });
  // panel.add(this.button_select_nodes);
  //
  // this.textfield_select_nodes_with_n_links = new TextFieldWrapper("0");
  // panel.add(this.textfield_select_nodes_with_n_links);
  //
  // final Label label_select = new Label("links", Label.LEFT);
  // panel.add(label_select);
  //
  // return panel;
  // }

  private Panel getCartesianColourerPanel() {
    final Panel panel = new Panel();

    this.button_cartesian_colourer = new Button("Cartesian colourer");
    this.button_cartesian_colourer.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getMessageManager()
            .sendMessage(Message.MSG_EDIT_COLOUR_CARTESIAN, 0, 0);
      }
    });
    panel.add(this.button_cartesian_colourer);

    return panel;
  }

  // Two methods prevent checkstyle complaints
  // private void testTemp() {
  // this.button_select_nodes = null;
  // this.textfield_select_nodes_with_n_links = null;
  // testTemp2();
  // }
  //  
  // private void testTemp2() {
  // testTemp();
  // }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}