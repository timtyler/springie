//This program has been placed into the public domain by its author.

//Deprecated - use NewMessageManager instead

package com.springie.messages;

import java.awt.Point;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.selection.SelectionSpreader;
import com.springie.gui.panels.controls.PanelControlsSelectMain;
import com.springie.modification.DomeRelatedChangeDelegator;
import com.springie.modification.automaticradius.AutomaticLinkRadius;
import com.springie.modification.automaticradius.AutomaticNodeRadius;
import com.springie.modification.colour.ColourClassificationCartesian;
import com.springie.modification.delete.DeleteLinks;
import com.springie.modification.delete.DeletePolygons;
import com.springie.modification.faces.FaceMaker;
import com.springie.modification.geodesic.GeodesicMaker;
import com.springie.modification.hexagons.HexagonMaker;
import com.springie.modification.links.LinkSubdivider;
import com.springie.modification.links.RemoveLinkAndFuseEnds;
import com.springie.modification.polyhedra.MakeLinksToNearestNode;
import com.springie.modification.stellation.CentralHubCreator;
import com.springie.modification.stellation.StellationMaker;
import com.springie.preferences.Preferences;
import com.springie.presets.ProceduralObject;
import com.springie.render.SetUpCode;
import com.springie.utilities.log.Log;

public class MessageManager {
  public int current_message; // make private...

  MessageObj[] msgq;

  static final int MSGQ_SIZE = 32;

  public MessageManager() {
    this.msgq = new MessageObj[MSGQ_SIZE];

    for (int c = 0; c < MSGQ_SIZE; c++) {
      this.msgq[c] = new MessageObj(0, 0, 0);
    }
  }

  public final void process() {
    int msgn = 0;
    FrEnd.generation++;

    final int number_of_messages = this.current_message;

    for (msgn = 0; msgn < number_of_messages; msgn++) {
      processMessageAndSwallowErrors(msgn);
    }

    shuffleUpFutureMessages(number_of_messages);

    if (FrEnd.mouse_pressed) {
      FrEnd.new_message_manager.add(new NewMessage(new Point(FrEnd.last_mousex, FrEnd.last_mousey)) {
        public Object execute() {
          final Point p = (Point) this.context;
          FrEnd.springieMouseClicked(p.x, p.y);
          return null;
        }
      });
    }
  }

  private void processMessageAndSwallowErrors(int msgn) {
    try {
      processMessage(msgn);
    } catch (RuntimeException e) {
      Log
        .log("Error processing message (number " + this.msgq[msgn].type + "):");
      e.printStackTrace();
    }
  }

  private void processMessage(int msgn) {
    HexagonMaker hexagon_maker;
    //final MessageObj message = this.msgq[msgn];
    final NodeManager node_manager = ContextMananger.getNodeManager();
    final FaceManager face_manager = node_manager.getFaceManager();
    final LinkManager link_manager = node_manager.getLinkManager();
    
    switch (this.msgq[msgn].type) {
      case Message.MSG_PRESET_CHOSEN:
        FrEnd.data_input.resetWorkspaces();
        SetUpCode.clearAndThenAddInitialObjects();
        FrEnd.reflectValuesInGUIAfterPropertyEditing();

        break;

      case Message.MSG_GENERATE_TUBE:
        allNewProceduralObject(new ArgumentList() {
          public Object getArguments(int i) {
            switch (i) {
              case 0:
                return ProceduralObject.tube;
              case 1:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_tube_circumference
                    .getText());
              case 2:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_tube_length
                    .getText());
              default:
                return null;
            }
          }
        });

        break;

      case Message.MSG_GENERATE_STRING:
        allNewProceduralObject(new ArgumentList() {
          public Object getArguments(int i) {
            switch (i) {
              case 0:
                return ProceduralObject.string;
              case 1:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_string_length
                    .getText());
              default:
                return null;
            }
          }
        });

        break;

      case Message.MSG_GENERATE_FREE_NODES:
        allNewProceduralObject(new ArgumentList() {
          public Object getArguments(int i) {
            switch (i) {
              case 0:
                return ProceduralObject.free_nodes;
              case 1:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_free_nodes
                    .getText());
              default:
                return null;
            }
          }
        });

        break;

      case Message.MSG_GENERATE_SPHERE_PACK:
        allNewProceduralObject(new ArgumentList() {
          public Object getArguments(int i) {
            switch (i) {
              case 0:
                return ProceduralObject.sphere_pack;
              case 1:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_sphere_pack
                    .getText());
              default:
                return null;
            }
          }
        });

        break;

      case Message.MSG_GENERATE_MATRIX:
        allNewProceduralObject(new ArgumentList() {
          public Object getArguments(int i) {
            switch (i) {
              case 0:
                return ProceduralObject.matrix;
              case 1:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_matrix_x
                    .getText());
              case 2:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_matrix_y
                    .getText());
              case 3:
                return new Integer(
                  FrEnd.panel_edit_generate.textfield_generate_matrix_z
                    .getText());
              default:
                return null;
            }
          }
        });

        break;

      case Message.MSG_DOME_CONNECT_NODES_IN_FIRST_LAYER:
        final GeodesicMaker geodesic_maker = new GeodesicMaker(node_manager);
        geodesic_maker.connectNodesInFirstLayer();
        FrEnd.postCleanup();

        break;

      case Message.MSG_ADD_STELLATIONS:
        final StellationMaker stellation_maker = new StellationMaker(
          node_manager);
        final float sf = Float
          .parseFloat(FrEnd.panel_edit_misc.textfield_add_stellations.getText());

        stellation_maker.addStellations(1F + sf, 0xFFFFFF60);
        FrEnd.postCleanup();

        break;

//      case Message.MSG_PRISMATIC_PROJECTION:
//        final PrismaticProjection prismatic_projection = new PrismaticProjection(
//          node_manager);
//        final float sf3 = Float
//          .parseFloat(FrEnd.panel_edit_misc.textfield_prismatic_projection
//            .getText());
//        prismatic_projection.project(sf3);
//        FrEnd.postCleanup();
//
//        break;

      case Message.MSG_EDIT_ADD_CENTRAL_HUB:
        final CentralHubCreator central_hub_creator = new CentralHubCreator(
          node_manager);
        central_hub_creator.create();
        FrEnd.postCleanup();

        break;

      case Message.MSG_ADD_NODES_TRIAXIAL:
        hexagon_maker = new HexagonMaker(node_manager);
        final float sf2 = Float
          .parseFloat(FrEnd.panel_edit_misc.textfield_add_triaxial_nodes
            .getText());
        hexagon_maker.createNodesInTriaxialOuterLayer(sf2);
        FrEnd.postCleanup();

        break;

      case Message.MSG_ADD_NODES_INNER_EDEN:
        hexagon_maker = new HexagonMaker(node_manager);
        final float sf4 = Float
          .parseFloat(FrEnd.panel_edit_misc.textfield_add_nodes_inner_eden
            .getText());
        hexagon_maker.createNodesInTriaxialOuterLayer(sf4);
        //createNodesInInnerEdenLayer(sf4);
        //hexagon_maker.createNodesInTriaxialOuterLayer(sf4);
        FrEnd.postCleanup();

        break;

      case Message.MSG_CONNECT_NODES_TO_NEAREST_NODES:
        final MakeLinksToNearestNode mltnn = new MakeLinksToNearestNode(
          node_manager);
        final int n_nearest = Integer
          .parseInt(FrEnd.panel_edit_misc.textfield_link_nearest_n_nodes
            .getText());
        mltnn.connectNodesToNearestNodes(n_nearest);
        FrEnd.postCleanup();

        break;

      case Message.MSG_EDIT_AUTOMATIC_NODE_RADIUS:
        final AutomaticNodeRadius anr = new AutomaticNodeRadius(node_manager);
        final String anr_s = FrEnd.panel_edit_misc.textfield_automatic_node_radius
          .getText();
        final double anr_d = Double.parseDouble(anr_s);
        anr.set(anr_d, false);
        FrEnd.postCleanup();

        break;

      case Message.MSG_EDIT_AUTOMATIC_LINK_RADIUS:
        final AutomaticLinkRadius alr = new AutomaticLinkRadius(node_manager);
        final String alr_s = FrEnd.panel_edit_misc.textfield_automatic_link_radius
          .getText();
        final double alr_d = Double.parseDouble(alr_s);
        alr.set(alr_d);
        FrEnd.postCleanup();

        break;

      case Message.MSG_EDIT_SPLIT_LINKS:
        final LinkSubdivider ls = new LinkSubdivider(node_manager);
        final String ls_s = FrEnd.panel_edit_misc.textfield_split_links
          .getText();
        final int ls_i = Integer.parseInt(ls_s);
        final boolean b = FrEnd.panel_edit_misc.checkbox_split_links_remove_old
          .getState();

        ls.divide(ls_i, b);

        FrEnd.postCleanup();

        break;

      case Message.MSG_EDIT_ADD_POLYGONS:
        final FaceMaker polygon_maker = new FaceMaker(node_manager);
        //final int c = FrEnd.colour_picker.getColour();
        final int i = Integer
          .parseInt(FrEnd.panel_edit_misc.textfield_add_polygons.getText());
        polygon_maker.addPolygons(i);

        FrEnd.postCleanup();

        break;

      case Message.MSG_EDIT_REMOVE_LINKS:
        final DeleteLinks deleter_l = new DeleteLinks(node_manager);
        deleter_l.delete();

        FrEnd.postCleanup();

        break;

      case Message.MSG_EDIT_SPREAD_SELECTION_VIA_LINKS:
        new SelectionSpreader(node_manager).spreadSelectionViaLinks();
        break;

      case Message.MSG_EDIT_REMOVE_POLYGONS:
        final DeletePolygons deleter_p = new DeletePolygons(node_manager);
        deleter_p.delete();

        FrEnd.postCleanup();

        break;

      case Message.MSG_DOME_NODES_CONTRACT:
        DomeRelatedChangeDelegator.contract();
        FrEnd.panel_edit_properties_scalars.reflectRadius();

        break;

      case Message.MSG_DOME_NODES_EXPAND:
        DomeRelatedChangeDelegator.expand();
        FrEnd.panel_edit_properties_scalars.reflectRadius();

        break;

      case Message.MSG_EDIT_CHARGE_UP:
        DomeRelatedChangeDelegator.chargeUp();
        FrEnd.panel_edit_properties_scalars.reflectCharge();

        break;

      case Message.MSG_EDIT_CHARGE_DOWN:
        DomeRelatedChangeDelegator.chargeDown();
        FrEnd.panel_edit_properties_scalars.reflectCharge();

        break;

      case Message.MSG_CHANGE_ELASTICITY_UP:
        DomeRelatedChangeDelegator.elasticityUp();
        FrEnd.panel_edit_properties_scalars.reflectElasticity();

        break;

      case Message.MSG_CHANGE_ELASTICITY_DOWN:
        DomeRelatedChangeDelegator.elasticityDown();
        FrEnd.panel_edit_properties_scalars.reflectElasticity();

        break;

      case Message.MSG_SCALE_LINKS_DOWN:
        DomeRelatedChangeDelegator.shortenLinks();
        FrEnd.panel_edit_properties_scalars.reflectLength();

        break;

      case Message.MSG_SCALE_LINKS_UP:
        DomeRelatedChangeDelegator.lengthenLinks();
        FrEnd.panel_edit_properties_scalars.reflectLength();

        break;

      case Message.MSG_CHANGE_STIFFNESS_DOWN:
        DomeRelatedChangeDelegator.stiffnessDown();
        FrEnd.panel_edit_properties_scalars.reflectStiffness();

        break;

      case Message.MSG_CHANGE_STIFFNESS_UP:
        DomeRelatedChangeDelegator.stiffnessUp();
        FrEnd.panel_edit_properties_scalars.reflectStiffness();

        break;

      case Message.MSG_EDIT_HIDE_FLAG:
        DomeRelatedChangeDelegator.hide();

        break;

      case Message.MSG_EDIT_FIXED_FLAG:
        DomeRelatedChangeDelegator.fix();

        break;

      case Message.MSG_EDIT_ROPE_FLAG:
        DomeRelatedChangeDelegator.rope();

        break;

      case Message.MSG_EDIT_DISABLED_FLAG:
        DomeRelatedChangeDelegator.disable();

        break;

      case Message.MSG_DOME_LINKS_EQUALISE_LINK_LENGTHS:
        DomeRelatedChangeDelegator.equaliseLinkLengths();
        break;

      case Message.MSG_DOME_LINKS_RESET_LENGTHS:
        DomeRelatedChangeDelegator.resetLinkLengths();
        break;

//      case Message.MSG_DOME_MAKE_MOTIONLESS:
//        new MotionlessMaker().freeze(node_manager);
//        break;

      case Message.MSG_DOUBLE_BUFFER_OLD:
        final boolean bool_db = FrEnd.panel_preferences_renderer_original.checkbox_db.getState();
        final String pref = Preferences.renderer_old_double_buffer; 
        FrEnd.preferences.map.put(pref, new Boolean(bool_db));

        FrEnd.postCleanup();

        FrEnd.main_canvas.setUpGraphicsHandle();
        FrEnd.main_canvas.forceResize();

        break;

      case Message.MSG_DOUBLE_BUFFER_NEW:
        final boolean bool_db1 = FrEnd.panel_preferences_renderer_modern.checkbox_db_new.getState();
        final String pref2 = Preferences.renderer_new_double_buffer;
        FrEnd.preferences.map.put(pref2, new Boolean(bool_db1));

        FrEnd.postCleanup();

        FrEnd.main_canvas.setUpGraphicsHandle();
        FrEnd.main_canvas.forceResize();

        break;

      case Message.MSG_NODE_GROWTH:
        FrEnd.node_growth = !FrEnd.node_growth;

        break;

      case Message.MSG_CONTINUOUSLY_CENTRE:
        FrEnd.continuously_centre = !FrEnd.continuously_centre;

        break;

      case Message.MSG_DELETE:
        node_manager.deleteSelected();
        link_manager.deleteSelected();
        face_manager.deleteSelected();
        FrEnd.postCleanup();

        break;

      case Message.MSG_LINKLENGTH:
        FrEnd.prepareToModifyLinkTypes();
        Link.link_display_length = this.msgq[msgn].data1;
        FrEnd.postCleanup();

        break;

//      case Message.MSG_SELECT_ALL_TYPE:
//        FrEnd.prepareToModifyAllTypes();
//        selectAllOfType();
//
//        break;
//
//      case Message.MSG_INVERT_ALL_TYPE:
//        FrEnd.prepareToModifyAllTypes();
//        invertAllOfType();
//
//        break;
//        
//      case Message.MSG_DESELECT_ALL_TYPE:
//        FrEnd.prepareToModifyAllTypes();
//        deselectAllOfType();
//
//        break;

      case Message.MSG_SELECT_CLAZZ:
        if (node_manager.isSelection()) {
          final Node n_clazz = node_manager.getSelectedNode();
          node_manager.selectAll(n_clazz.clazz.colour);
        }

        if (link_manager.isSelection()) {
          final Link l = link_manager.getFirstSelectedLink();
          link_manager.selectAll(l.clazz.colour);
        }

        if (face_manager.isSelection()) {
          final Face p = face_manager.getFirstSelectedPolygon();
          face_manager.selectAll(p.clazz.colour);
        }

        break;

      case Message.MSG_SELECT_TYPE:
        if (node_manager.isSelection()) {
          node_manager.selectAll();
        }

        if (link_manager.isSelection()) {
          link_manager.selectAll();
        }

        if (face_manager.isSelection()) {
          face_manager.selectAll();
        }

        break;

//      case Message.MSG_SELECT_ALL_NODES:
//        FrEnd.prepareToModifyNodeTypes();
//        node_manager.selectAll();
//        FrEnd.postCleanup();
//
//        break;
//
//      case Message.MSG_SELECT_ALL_LINKS:
//        FrEnd.prepareToModifyLinkTypes();
//        link_manager.selectAll();
//        FrEnd.postCleanup();
//
//        break;
//
//      case Message.MSG_SELECT_ALL_FACES:
//        FrEnd.prepareToModifyFaceTypes();
//        face_manager.selectAll();
//        FrEnd.postCleanup();
//
//        break;

      case Message.MSG_SELECT_ALL_FACES_WITH_N_SIDES:
        FrEnd.prepareToModifyFaceTypes();
        final int n_sides = Integer
          .parseInt(FrEnd.panel_edit_select_advanced.textfield_select_faces_with_n_sides
            .getText());
        face_manager.selectAllWithNSides(n_sides);
        FrEnd.postCleanup();

        break;

      case Message.MSG_SELECT_ALL:
        FrEnd.prepareToModifyAllTypes();
        node_manager.selectAll();
        link_manager.selectAll();
        face_manager.selectAll();
        FrEnd.postCleanup();

        break;

      case Message.MSG_DESELECT_ALL:
        FrEnd.prepareToModifyAllTypes();
        node_manager.deselectAll();
        link_manager.deselectAll();
        face_manager.deselectAll();
        FrEnd.postCleanup();

        break;

      case Message.MSG_ALTERSIZE:
        FrEnd.prepareToModifyAllTypes();
        node_manager.setSizeOfSelected((byte) this.msgq[msgn].data1);
        link_manager.setSizeOfSelected(this.msgq[msgn].data1);
        FrEnd.reflectValuesInGUIAfterPropertyEditing();

        break;

      case Message.MSG_ALTER_ELASTICITY:
        FrEnd.prepareToModifyLinkTypes();
        final int elasticity = this.msgq[msgn].data1;
        link_manager.setElasticityOfSelected(elasticity);
        FrEnd.panel_edit_properties_scalars.reflectElasticity();
        FrEnd.reflectValuesInGUIAfterPropertyEditing();

        break;

      case Message.MSG_ALTER_STIFFNESS:
        FrEnd.prepareToModifyLinkTypes();
        final int stiffness = this.msgq[msgn].data1;
        link_manager.setStiffnessOfSelected(stiffness);
        FrEnd.panel_edit_properties_scalars.reflectStiffness();
        FrEnd.reflectValuesInGUIAfterPropertyEditing();

        break;

      case Message.MSG_ALTER_LENGTH:
        FrEnd.prepareToModifyLinkTypes();
        final int length = this.msgq[msgn].data1;
        link_manager.setLengthOfSelected(length);
        FrEnd.panel_edit_properties_scalars.reflectLength();
        FrEnd.reflectValuesInGUIAfterPropertyEditing();

        break;

      case Message.MSG_ALTER_RADIUS:
        FrEnd.prepareToModifyAllTypes();
        final int radius = this.msgq[msgn].data1;
        link_manager.setRadiusOfSelected(radius);
        node_manager.setRadiusOfSelected(radius);
        FrEnd.panel_edit_properties_scalars.reflectRadius();
        FrEnd.reflectValuesInGUIAfterPropertyEditing();

        break;

      case Message.MSG_ALTER_CHARGE:
        FrEnd.prepareToModifyNodeTypes();
        final int charge = this.msgq[msgn].data1;
        node_manager.setChargeOfSelected(charge);
        FrEnd.panel_edit_properties_scalars.reflectCharge();
        FrEnd.reflectValuesInGUIAfterPropertyEditing();

        break;

      case Message.MSG_EDIT_REMOVE_LINK_AND_FUSE_ENDS:
        FrEnd.prepareToModifyLinkTypes();
        final RemoveLinkAndFuseEnds end_fuse = new RemoveLinkAndFuseEnds(
          node_manager);
        end_fuse.action();
        FrEnd.postCleanup();

        break;

      case Message.MSG_EDIT_COLOUR_CARTESIAN:
        new ColourClassificationCartesian(node_manager).setColour();
        FrEnd.postCleanup();

        break;

      default:
        throw new RuntimeException("Message Error");

    }
  }

  private void shuffleUpFutureMessages(final int number_of_messages) {
    int num_left = this.current_message - number_of_messages;

    if (num_left > 0) {
      for (int i = 0; i < num_left; i++) {
        this.msgq[i] = this.msgq[number_of_messages + i];
      }
    }

    this.current_message = num_left;
  }

  public static void deselectAllOfType() {
    final PanelControlsSelectMain panel = FrEnd.panel_edit_select_main;
    if (panel.checkbox_select_nodes.getState()) {
      ContextMananger.getNodeManager().deselectAll();
    }

    if (panel.checkbox_select_links.getState()) {
      ContextMananger.getLinkManager().deselectAll();
    }

    if (panel.checkbox_select_faces.getState()) {
      ContextMananger.getFaceManager().deselectAll();
    }
  }

  public static void selectAllOfType() {
    final PanelControlsSelectMain panel = FrEnd.panel_edit_select_main;
    if (panel.checkbox_select_nodes.getState()) {
      ContextMananger.getNodeManager().selectAll();
    }

    if (panel.checkbox_select_links.getState()) {
      ContextMananger.getLinkManager().selectAll();
    }

    if (panel.checkbox_select_faces.getState()) {
      ContextMananger.getFaceManager().selectAll();
    }
  }
  
  public static void invertAllOfType() {
    final PanelControlsSelectMain panel = FrEnd.panel_edit_select_main;
    if (panel.checkbox_select_nodes.getState()) {
      ContextMananger.getNodeManager().selectionInvert();
    }

    if (panel.checkbox_select_links.getState()) {
      ContextMananger.getLinkManager().selectionInvert();
    }

    if (panel.checkbox_select_faces.getState()) {
      ContextMananger.getFaceManager().selectionInvert();
    }
  }
  
  

  public void allNewProceduralObject(ArgumentList al) {
    FrEnd.data_input.resetWorkspaces();
    SetUpCode.clearAndThenAddProceduralObjects(al);
    FrEnd.reflectValuesInGUIAfterPropertyEditing();
  }

  public final void sendMessage(int t, int d1, int d2) {
    if (this.current_message < MSGQ_SIZE) {
      this.msgq[this.current_message].type = t;
      this.msgq[this.current_message].data1 = d1;
      this.msgq[this.current_message].data2 = d2;

      this.current_message++;
    }
  }
}