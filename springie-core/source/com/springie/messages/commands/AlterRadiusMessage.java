//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterRadiusMessage extends NewMessage {
  private final int radius;

  public AlterRadiusMessage(int radius) {
    super(null);
    this.radius = radius;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    FrEnd.prepareToModifyAllTypes();
    link_manager.setRadiusOfSelected(this.radius);
    node_manager.setRadiusOfSelected(this.radius);
    FrEnd.panel_edit_properties_scalars.reflectRadius();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
