package com.springie.modification.faces;

import java.util.Vector;

import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.utilities.random.JUR;

public class FaceReverser {
  JUR rnd = new JUR();

  NodeManager node_manager;

  LinkManager link_manager;

  FaceManager face_manager;

  // DomeMakingUtilities utils = new DomeMakingUtilities();

  public FaceReverser(NodeManager node_manager) {
    this.node_manager = node_manager;
    this.link_manager = node_manager.getLinkManager();
    this.face_manager = node_manager.getFaceManager();
  }

  public void reverse() {
    final int size = this.face_manager.element.size();

    for (int i = 0; i < size; i++) {
      final Face face = (Face) this.face_manager.element.elementAt(i);
      if (face.type.selected) {
        final Vector nodes = face.nodes;
        final int non = nodes.size();
        final int half = non >> 1;
        for (int j = 0; j < half; j++) {
          final Node temp = (Node) nodes.elementAt(j);
          nodes.setElementAt(nodes.elementAt(non - 1 - j), j);
          nodes.setElementAt(temp, non - 1 - j);
        }
      }
    }
  }
}
