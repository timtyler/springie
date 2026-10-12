//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.hexagons.HexagonMaker;

public class AddInnerEdenNodesMessage extends NewMessage {
  public AddInnerEdenNodesMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final HexagonMaker hexagon_maker = new HexagonMaker(node_manager);
    final float sf4 = Float
      .parseFloat(FrEnd.panel_edit_misc.textfield_add_nodes_inner_eden
        .getText());
    hexagon_maker.createNodesInTriaxialOuterLayer(sf4);
    //createNodesInInnerEdenLayer(sf4);
    //hexagon_maker.createNodesInTriaxialOuterLayer(sf4);
    FrEnd.postCleanup();

    return null;
  }
}
