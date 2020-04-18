// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import java.awt.Point;
import java.util.Vector;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.pre.PrepareToModifyFaceTypes;
import com.springie.modification.pre.PrepareToModifyLinkTypes;
import com.springie.modification.pre.PrepareToModifyNodeTypes;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.RendererDragBox;
import com.tifsoft.Forget;

public class DragBoxManager {
  public Point drag_box_start;

  public Point drag_box_end;

  Vector list_of_nodes;

  public void drag(int x, int y) {
    if (FrEnd.button_virginity) {
      this.drag_box_start = new Point(x, y);

      final RendererDragBox renderer = ContextMananger.getNodeManager().renderer.renderer_drag_box;
      renderer.min = new Point(0, 0);
      renderer.max = new Point(0, 0);
    }

    this.drag_box_end = new Point(x, y);
    RendererDelegator.repaint_some_objects = true;
  }

  public void terminate(int x, int y) {
    Forget.about(x);
    Forget.about(y);

    this.list_of_nodes = new Vector();

    if (this.drag_box_start != null) {
      final RendererDragBox renderer = ContextMananger.getNodeManager().renderer.renderer_drag_box;
      final Point min = renderer.min;
      final Point max = renderer.max;

      // prepare
      if (FrEnd.panel_edit_select_main.checkbox_select_nodes.getState()) {
        new PrepareToModifyNodeTypes(ContextMananger.getNodeManager()).prepare();

        selectNodesInBox(min, max);
      }

      if (FrEnd.panel_edit_select_main.checkbox_select_links.getState()) {
        new PrepareToModifyLinkTypes(ContextMananger.getLinkManager()).prepare();
        selectLinksWithNodesInList(this.list_of_nodes);
      }

      if (FrEnd.panel_edit_select_main.checkbox_select_faces.getState()) {
        new PrepareToModifyFaceTypes(ContextMananger.getFaceManager()).prepare();
        selectFacesWithNodesInList(this.list_of_nodes);
      }
      
      //renderer.min = new Point(0,0);
      //renderer.max = renderer.min;
    }

    RendererDelegator.repaint_some_objects = true;
    this.drag_box_start = null;
    //this.drag_box_end = null;
  }

  private void selectNodesInBox(Point min, Point max) {
    final NodeManager node_manager = ContextMananger.getNodeManager();

    final int number_of_nodes = node_manager.element.size();
    for (int temp = 0; temp < number_of_nodes; temp++) {
      final Node node = (Node) node_manager.element.elementAt(temp);
      final int z = node.pos.z;
      final int radius = Coords.getRadiusInternal(node.type.radius, z);
      final int x = Coords.getXCoordsInternal(node.pos.x, z);
      if (x - radius > min.x) {
        if (x + radius < max.x) {
          final int y = Coords.getYCoordsInternal(node.pos.y, z);
          if (y - radius > min.y) {
            if (y + radius < max.y) {
              this.list_of_nodes.addElement(node);
              if (!node.type.hidden || FrEnd.render_hidden_nodes) {
                node.type.selected = true;
              }
            }
          }
        }
      }
    }

    FrEnd.updateGUIToReflectSelectionChange();
  }

  private void selectLinksWithNodesInList(Vector list_of_nodes) {
    final LinkManager link_manager = ContextMananger.getLinkManager();

    final int number = link_manager.element.size();
    for (int temp = 0; temp < number; temp++) {
      final Link link = (Link) link_manager.element.elementAt(temp);
      if (!link.type.hidden || FrEnd.render_hidden_links) {
        if (allNodesInArrayAreInVector(link.nodes, list_of_nodes)) {
          link.setSelectedFiltered(true);
        }
      }
    }

    FrEnd.updateGUIToReflectSelectionChange();
  }

  private boolean allNodesInArrayAreInVector(Node[] nodes, Vector list_of_nodes) {
    final int total = nodes.length;
    for (int i = 0; i < total; i++) {
      if (!nodeIsInList(nodes[i], list_of_nodes)) {
        return false;
      }
    }

    return true;
  }

  private void selectFacesWithNodesInList(Vector list_of_nodes) {
    final FaceManager face_manager = ContextMananger.getFaceManager();

    final int number = face_manager.element.size();
    for (int temp = 0; temp < number; temp++) {
      final Face face = (Face) face_manager.element.elementAt(temp);
      if (!face.type.hidden || FrEnd.render_hidden_faces) {
        if (faceHasAllNodesInList(face, list_of_nodes)) {
          face.type.selected = true;
        }
      }
    }

    FrEnd.updateGUIToReflectSelectionChange();
  }

  private boolean faceHasAllNodesInList(Face face, Vector vector) {
    final int n_of_nodes = face.nodes.size();
    for (int nn = n_of_nodes; --nn >= 0;) {
      if (!nodeIsInList((Node) face.nodes.elementAt(nn), vector)) {
        return false;
      }
    }

    return true;
  }

  private boolean nodeIsInList(Node node, Vector vector) {
    final int number = vector.size();
    for (int temp = 0; temp < number; temp++) {
      final Node candidate = (Node) vector.elementAt(temp);
      if (node == candidate) {
        return true;
      }
    }

    return false;
  }
}
