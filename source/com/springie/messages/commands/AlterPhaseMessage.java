//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;

public class AlterPhaseMessage extends NewMessage {
  private final int phase;

  public AlterPhaseMessage(int phase) {
    super(null);
    this.phase = phase;
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    link_manager.setPhaseOfSelected(this.phase);
    FrEnd.panel_edit_properties_scalars.reflectPhase();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
