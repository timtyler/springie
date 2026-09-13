//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.geodesic.GeodesicMaker;

public class ConnectNodesInFirstLayerMessage extends NewMessage {
  public ConnectNodesInFirstLayerMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final GeodesicMaker geodesic_maker = new GeodesicMaker(node_manager);
    geodesic_maker.connectNodesInFirstLayer();
    FrEnd.postCleanup();

    return null;
  }
}
