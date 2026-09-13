//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterLengthMessage extends NewMessage {
  private final int length;

  public AlterLengthMessage(int length) {
    super(null);
    this.length = length;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    FrEnd.prepareToModifyLinkTypes();
    link_manager.setLengthOfSelected(this.length);
    FrEnd.panel_edit_properties_scalars.reflectLength();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
