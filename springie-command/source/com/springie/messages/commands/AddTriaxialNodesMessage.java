//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.world.NewMessage;
import com.springie.modification.hexagons.HexagonMaker;

public class AddTriaxialNodesMessage extends NewMessage {
  public AddTriaxialNodesMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final HexagonMaker hexagon_maker = new HexagonMaker(node_manager);
    final float sf2 = Float
      .parseFloat(CommandContextHolder.get().getAddTriaxialNodes());
    hexagon_maker.createNodesInTriaxialOuterLayer(sf2);
    CommandContextHolder.get().postCleanup();

    return null;
  }
}
