//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.context.ContextManager;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class SelectTypeMessage extends NewMessage {
  public SelectTypeMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final FaceManager face_manager = node_manager.getFaceManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    if (node_manager.isSelection()) {
      node_manager.selectAll();
    }

    if (link_manager.isSelection()) {
      link_manager.selectAll();
    }

    if (face_manager.isSelection()) {
      face_manager.selectAll();
    }

    return null;
  }
}
