//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;

public class AlterChargeMessage extends NewMessage {
  private final int charge;

  public AlterChargeMessage(int charge) {
    super(null);
    this.charge = charge;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();

    CommandContextHolder.get().prepareToModifyNodeTypes();
    node_manager.setChargeOfSelected(this.charge);
    FrEnd.panel_edit_properties_scalars.reflectCharge();
    CommandContextHolder.get().reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
