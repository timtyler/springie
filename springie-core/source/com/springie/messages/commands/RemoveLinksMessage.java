//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.delete.DeleteLinks;

public class RemoveLinksMessage extends NewMessage {
  public RemoveLinksMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final DeleteLinks deleter_l = new DeleteLinks(node_manager);
    deleter_l.delete();

    FrEnd.postCleanup();

    return null;
  }
}
