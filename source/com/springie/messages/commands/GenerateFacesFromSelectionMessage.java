//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.faces.FaceMaker;

public class GenerateFacesFromSelectionMessage extends NewMessage {
  public GenerateFacesFromSelectionMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    new FaceMaker(node_manager).makeFacesFromSelection();

    return null;
  }
}
