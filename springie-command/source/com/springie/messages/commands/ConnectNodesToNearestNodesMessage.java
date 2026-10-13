//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.modification.polyhedra.MakeLinksToNearestNode;

public class ConnectNodesToNearestNodesMessage extends NewMessage {
  public ConnectNodesToNearestNodesMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final MakeLinksToNearestNode mltnn = new MakeLinksToNearestNode(
      node_manager);
    final int n_nearest = Integer
      .parseInt(CommandContextHolder.get().getLinkNearestNNodes());
    mltnn.connectNodesToNearestNodes(n_nearest);
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
