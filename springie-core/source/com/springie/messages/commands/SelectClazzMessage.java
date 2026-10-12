//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.context.ContextManager;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class SelectClazzMessage extends NewMessage {
  public SelectClazzMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final FaceManager face_manager = node_manager.getFaceManager();
    final LinkManager link_manager = node_manager.getLinkManager();

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

    return null;
  }
}
