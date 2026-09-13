//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
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
      .parseInt(FrEnd.panel_edit_misc.textfield_link_nearest_n_nodes
        .getText());
    mltnn.connectNodesToNearestNodes(n_nearest);
    FrEnd.postCleanup();

    return null;
  }
}
