//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.stellation.CentralHubCreator;

public class AddCentralHubMessage extends NewMessage {
  public AddCentralHubMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final CentralHubCreator central_hub_creator = new CentralHubCreator(
      node_manager);
    central_hub_creator.create();
    FrEnd.postCleanup();

    return null;
  }
}
