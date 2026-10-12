//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.delete.DeletePolygons;

public class RemovePolygonsMessage extends NewMessage {
  public RemovePolygonsMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final DeletePolygons deleter_p = new DeletePolygons(node_manager);
    deleter_p.delete();

    FrEnd.postCleanup();

    return null;
  }
}
