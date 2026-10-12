//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.selection.SelectionSpreader;
import com.springie.messages.NewMessage;

public class SpreadSelectionViaLinksMessage extends NewMessage {
  public SpreadSelectionViaLinksMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    new SelectionSpreader(node_manager).spreadSelectionViaLinks();

    return null;
  }
}
