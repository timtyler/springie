//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class DeleteSelectedMessage extends NewMessage {
  public DeleteSelectedMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final FaceManager face_manager = node_manager.getFaceManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    node_manager.deleteSelected();
    link_manager.deleteSelected();
    face_manager.deleteSelected();
    FrEnd.postCleanup();

    return null;
  }
}
