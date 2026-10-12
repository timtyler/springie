//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterSizeMessage extends NewMessage {
  private final int size;

  public AlterSizeMessage(int size) {
    super(null);
    this.size = size;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    FrEnd.prepareToModifyAllTypes();
    node_manager.setSizeOfSelected((byte) this.size);
    link_manager.setSizeOfSelected(this.size);
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
