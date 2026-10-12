//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterChargeMessage extends NewMessage {
  private final int charge;

  public AlterChargeMessage(int charge) {
    super(null);
    this.charge = charge;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();

    FrEnd.prepareToModifyNodeTypes();
    node_manager.setChargeOfSelected(this.charge);
    FrEnd.panel_edit_properties_scalars.reflectCharge();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
