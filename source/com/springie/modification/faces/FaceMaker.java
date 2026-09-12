package com.springie.modification.faces;

import java.util.ArrayList;

import com.springie.FrEnd;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.faces.FaceType;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.modification.DomeMakingUtilities;
import com.springie.utilities.random.JUR;

public class FaceMaker {
  JUR rnd = new JUR();
  NodeManager node_manager;

  LinkManager link_manager;

  DomeMakingUtilities utils = new DomeMakingUtilities();

  public FaceMaker(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
  }
  
  public void addPolygons(int number) {
    for (int cnt = 3000; --cnt >= 0;) {
      tryToMakePolygon(number);
    }
  }

  /**
   * Creates a face for every minimal cycle in the selected nodes and
   * links (see FaceCycleFinder for the algorithm). Cycles that already
   * exist as polygons are skipped, and the new faces are selected.
   *
   * @return the number of faces created
   */
  public int makeFacesFromSelection() {
    final ArrayList<Node> selected_nodes = new ArrayList<>();
    final int number_of_nodes = this.node_manager.element.size();
    for (int i = number_of_nodes; --i >= 0;) {
      final Node node = (Node) this.node_manager.element.get(i);
      if (node.type.selected) {
        selected_nodes.add(node);
      }
    }

    final ArrayList<Link> selected_links = new ArrayList<>();
    final int number_of_links = this.link_manager.element.size();
    for (int i = number_of_links; --i >= 0;) {
      final Link link = (Link) this.link_manager.element.get(i);
      if (link.type.selected) {
        selected_links.add(link);
      }
    }

    final FaceManager polygon_manager = this.node_manager.getFaceManager();

    int created = 0;
    final ArrayList<ArrayList<Node>> cycles = FaceCycleFinder
      .findFaceCycles(selected_nodes, selected_links);
    for (ArrayList<Node> cycle : cycles) {
      if (polygon_manager.isThereAPolygonWithNodes(cycle)) {
        continue;
      }

      final FaceType type = polygon_manager.face_type_factory.getNew();
      final Clazz clazz = this.node_manager.clazz_factory.getNew(0x80FFFF80);
      final Face face = polygon_manager.setPolygon(cycle, type, clazz);
      face.setSelected(true);
      created++;
    }

    if (created > 0) {
      FrEnd.updateGUIToReflectSelectionChange();
    }

    return created;
  }

  public void tryToMakePolygon(int number) {
    final int number_of_nodes = this.node_manager.element.size();
    final ArrayList<Node> node = new ArrayList<>();

    final int a0 = this.rnd.nextInt(number_of_nodes);
    final Node n0 = (Node) this.node_manager.element.get(a0);

    if (!n0.type.selected) {
      return;
    }

    node.add(n0);

    Node last_node = n0;
    for (int cnt = 1; cnt < number; cnt++) {
      final Node can = this.utils.getRandomCellLinkedTo(this.node_manager,
        last_node);

      if (nodeIsOnList(can, node)) {
        return;
      }

      node.add(can);
      last_node = can;
    }

    if (!this.link_manager.isThereALinkBetween(n0, last_node)) {
      return;
    }

    final FaceManager polygon_manager = this.node_manager.getFaceManager();

    if (polygon_manager.isThereAPolygonWithNodes(node)) {
      return;
    }

    // make it...
    final FaceType type = polygon_manager.face_type_factory.getNew();
    
    final Clazz clazz = this.node_manager.clazz_factory.getNew(0x80FFFF80);

    polygon_manager.setPolygon(node, type, clazz);
  }
  
  private boolean nodeIsOnList(Node node_to_check, ArrayList<Node> node_list) {
    final int number_of_nodes = node_list.size();
    for (int cnt = number_of_nodes; --cnt >= 0;) {
      final Node candidate = node_list.get(cnt);
      if (candidate == node_to_check) {
        return true;
      }
    }

    return false;
  }
}
