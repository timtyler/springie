//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.modification.hexagons.HexagonMaker;

public class AddInnerEdenNodesMessage extends NewMessage {
  public AddInnerEdenNodesMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final HexagonMaker hexagon_maker = new HexagonMaker(node_manager);
    final float sf4 = Float
      .parseFloat(CommandContextHolder.get().getAddNodesInnerEden());
    hexagon_maker.createNodesInTriaxialOuterLayer(sf4);
    //createNodesInInnerEdenLayer(sf4);
    //hexagon_maker.createNodesInTriaxialOuterLayer(sf4);
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
