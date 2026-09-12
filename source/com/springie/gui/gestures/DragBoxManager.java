// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import java.awt.Point;
import java.util.ArrayList;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
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

public class DragBoxManager {
  public Point drag_box_start;

  public Point drag_box_end;

  ArrayList<Node> list_of_nodes;

  public void drag(int x, int y) {
    if (FrEnd.button_virginity) {
      this.drag_box_start = new Point(x, y);
    }

    this.drag_box_end = new Point(x, y);
    RendererDelegator.repaint_some_objects = true;
  }

  public void terminate(int x, int y) {
    this.list_of_nodes = new ArrayList<>();

    if (this.drag_box_start != null) {
      // The selection box comes from the gesture itself (press point and
      // release point), not from the renderer-cached box: the cache is only
      // updated on paint, so a release processed before the next paint
      // would select with a stale rectangle.
      final Point min = new Point(
          Math.min(this.drag_box_start.x, x), Math.min(this.drag_box_start.y, y));
      final Point max = new Point(
          Math.max(this.drag_box_start.x, x), Math.max(this.drag_box_start.y, y));

      // prepare
      if (FrEnd.panel_edit_select_main.checkbox_select_nodes.getState()) {
        new PrepareToModifyNodeTypes(ContextManager.getNodeManager()).prepare();

        selectNodesInBox(min, max);
      }

      if (FrEnd.panel_edit_select_main.checkbox_select_links.getState()) {
        new PrepareToModifyLinkTypes(ContextManager.getLinkManager()).prepare();
        selectLinksWithNodesInList(this.list_of_nodes);
      }

      if (FrEnd.panel_edit_select_main.checkbox_select_faces.getState()) {
        new PrepareToModifyFaceTypes(ContextManager.getFaceManager()).prepare();
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
    final NodeManager node_manager = ContextManager.getNodeManager();

    final int number_of_nodes = node_manager.element.size();
    for (int temp = 0; temp < number_of_nodes; temp++) {
      final Node node = (Node) node_manager.element.get(temp);
      final int z = node.pos.z;
      final int radius = Coords.getRadiusInternal(node.type.radius, z);
      final int x = Coords.getXCoordsInternal(node.pos.x, z);
      if (x - radius > min.x) {
        if (x + radius < max.x) {
          final int y = Coords.getYCoordsInternal(node.pos.y, z);
          if (y - radius > min.y) {
            if (y + radius < max.y) {
              this.list_of_nodes.add(node);
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

  private void selectLinksWithNodesInList(ArrayList<Node> list_of_nodes) {
    final LinkManager link_manager = ContextManager.getLinkManager();

    final int number = link_manager.element.size();
    for (int temp = 0; temp < number; temp++) {
      final Link link = link_manager.element.get(temp);
      if (!link.type.hidden || FrEnd.render_hidden_links) {
        if (allNodesInArrayAreInVector(link.nodes, list_of_nodes)) {
          link.setSelectedFiltered(true);
        }
      }
    }

    FrEnd.updateGUIToReflectSelectionChange();
  }

  private boolean allNodesInArrayAreInVector(Node[] nodes, ArrayList<Node> list_of_nodes) {
    final int total = nodes.length;
    for (int i = 0; i < total; i++) {
      if (!nodeIsInList(nodes[i], list_of_nodes)) {
        return false;
      }
    }

    return true;
  }

  private void selectFacesWithNodesInList(ArrayList<Node> list_of_nodes) {
    final FaceManager face_manager = ContextManager.getFaceManager();

    final int number = face_manager.element.size();
    for (int temp = 0; temp < number; temp++) {
      final Face face = face_manager.element.get(temp);
      if (!face.type.hidden || FrEnd.render_hidden_faces) {
        if (faceHasAllNodesInList(face, list_of_nodes)) {
          face.type.selected = true;
        }
      }
    }

    FrEnd.updateGUIToReflectSelectionChange();
  }

  private boolean faceHasAllNodesInList(Face face, ArrayList<Node> vector) {
    final int n_of_nodes = face.nodes.size();
    for (int nn = n_of_nodes; --nn >= 0;) {
      if (!nodeIsInList(face.nodes.get(nn), vector)) {
        return false;
      }
    }

    return true;
  }

  private boolean nodeIsInList(Node node, ArrayList<Node> vector) {
    final int number = vector.size();
    for (int temp = 0; temp < number; temp++) {
      final Node candidate = vector.get(temp);
      if (node == candidate) {
        return true;
      }
    }

    return false;
  }
}
