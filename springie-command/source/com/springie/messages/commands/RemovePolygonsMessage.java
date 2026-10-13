//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.modification.delete.DeletePolygons;

public class RemovePolygonsMessage extends NewMessage {
  public RemovePolygonsMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final DeletePolygons deleter_p = new DeletePolygons(node_manager);
    deleter_p.delete();

    CommandContextHolder.get().postCleanup();

    return null;
  }
}
