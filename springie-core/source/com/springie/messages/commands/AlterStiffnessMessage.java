//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterStiffnessMessage extends NewMessage {
  private final int stiffness;

  public AlterStiffnessMessage(int stiffness) {
    super(null);
    this.stiffness = stiffness;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    FrEnd.prepareToModifyLinkTypes();
    link_manager.setStiffnessOfSelected(this.stiffness);
    FrEnd.panel_edit_properties_scalars.reflectStiffness();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
