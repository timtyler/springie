//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class SelectAllMessage extends NewMessage {
  public SelectAllMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final FaceManager face_manager = node_manager.getFaceManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    FrEnd.prepareToModifyAllTypes();
    node_manager.selectAll();
    link_manager.selectAll();
    face_manager.selectAll();
    FrEnd.postCleanup();

    return null;
  }
}
