package com.springie.modification.stellation;

import java.util.Vector;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.lists.ListOfIntegers;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;

public class DimpleMaker {
  NodeManager node_manager;

  LinkManager link_manager;

  public DimpleMaker(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
  }

  public void dimple() {
    final Vector vector = new Vector();
    for (int count = 0; count < this.node_manager.element.size(); count++) {
      final Node node = (Node) this.node_manager.element.elementAt(count);
      if (node.type.selected) {
        final Point3D p = tryToMakeDimple(node);
        vector.addElement(p);
      }
    }

    int i = 0;
    for (int count = 0; count < this.node_manager.element.size(); count++) {
      final Node node = (Node) this.node_manager.element.elementAt(count);
      if (node.type.selected) {
        final Point3D delta = (Point3D) vector.elementAt(i++);
        node.pos.addTuple3D(delta);
        node.pos.addTuple3D(delta);
      }
    }
  }

  
  public Point3D tryToMakeDimple(Node node) {
    final Point3D initial = node.pos;
    final Point3D average = new Point3D(0, 0, 0);
    final ListOfIntegers list_of_integers = node.list_of_links;
    final int total = list_of_integers.size();
    if (total > 0) {
      for (int count = 0; count < total; count++) {
        final int i = list_of_integers.retreive(count);
        final Link link = (Link) this.link_manager.element.elementAt(i);
        final Node other = link.theOtherEnd(node);
        average.addTuple3D(other.pos);
        average.subtractTuple3D(initial);
      }

      average.divideBy(total);
    }
    
    return average;
  }
}