//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterElasticityMessage extends NewMessage {
  private final int elasticity;

  public AlterElasticityMessage(int elasticity) {
    super(null);
    this.elasticity = elasticity;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    FrEnd.prepareToModifyLinkTypes();
    link_manager.setElasticityOfSelected(this.elasticity);
    FrEnd.panel_edit_properties_scalars.reflectElasticity();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
