package com.springie.modification.post;

import com.springie.elements.electrostatics.ElectrostaticRepulsion;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.lists.ListOfIntegers;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.RendererDelegator;

public class PostModification {
  NodeManager node_manager;

  LinkManager link_manager;

  FaceManager face_manager;

  public PostModification(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    this.face_manager = node_manager.getFaceManager();
  }

  public void thoroughCleanup() {
    this.node_manager.nodes_have_been_deleted = true;
    cleanup();
    RendererDelegator.repaintAll();
  }
  
  public void cleanup() {
    generateChargedNodeList();
    if (this.node_manager.nodes_have_been_deleted) {
      generateListOfLinks();
      generateListOfFaces();
      setTensegrityFlag();
    }
    RendererDelegator.repaint_some_objects = true;
  }

  private void setTensegrityFlag() {
    this.node_manager.is_tensegrity = false;
    final int n_o_l = this.link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.link_manager.element.get(temp);
      if (!l.type.compression) {
        this.node_manager.is_tensegrity = true;
      }
    }
  }

  private void generateChargedNodeList() {
    final ElectrostaticRepulsion electrostatic = this.node_manager.electrostatic;
    electrostatic.charged.removeAllElements();

    final int number_of_nodes = this.node_manager.element.size();
    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) this.node_manager.element.get(counter);
      if (candidate.type.charge != 0) {
        if (!candidate.type.disabled) {
          electrostatic.charged.addElement(candidate);
        }
      }
    }
  }

  public void generateListOfLinks() {
    final int number_of_nodes = this.node_manager.element.size();
    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) this.node_manager.element
        .get(counter);
      candidate.list_of_links = getListOfLinks(candidate);
    }
  }

  public void generateListOfFaces() {
    final int number_of_nodes = this.node_manager.element.size();
    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) this.node_manager.element
        .get(counter);
      candidate.list_of_polygons = getListOfFaces(candidate);
    }
  }

  public ListOfIntegers getListOfLinks(Node candidate) {
    final ListOfIntegers list = new ListOfIntegers();

    final int n_o_l = this.link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link l = (Link) this.link_manager.element.get(temp);
      if (l.hasNode(candidate)) {
      //if ((l.node1 == candidate) || (l.node2 == candidate)) {
        list.add(temp);
      }
    }

    return list;
  }

  private ListOfIntegers getListOfFaces(Node candidate) {
    final ListOfIntegers list = new ListOfIntegers();

    final int n_o_l = this.face_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Face f = (Face) this.face_manager.element.get(temp);
      if (f.containsNodes(candidate)) {
        list.add(temp);
      }
    }

    return list;
  }
}
