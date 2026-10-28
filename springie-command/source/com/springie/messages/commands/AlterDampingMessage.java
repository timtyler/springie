//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;

public class AlterDampingMessage extends NewMessage {
  private final int damping;

  public AlterDampingMessage(int damping) {
    super(null);
    this.damping = damping;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    CommandContextHolder.get().prepareToModifyLinkTypes();
    link_manager.setDampingOfSelected(this.damping);
    CommandContextHolder.get().reflectDamping();
    CommandContextHolder.get().reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
