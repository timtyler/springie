//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.hexagons.HexagonMaker;

public class AddTriaxialNodesMessage extends NewMessage {
  public AddTriaxialNodesMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final HexagonMaker hexagon_maker = new HexagonMaker(node_manager);
    final float sf2 = Float
      .parseFloat(FrEnd.panel_edit_misc.textfield_add_triaxial_nodes
        .getText());
    hexagon_maker.createNodesInTriaxialOuterLayer(sf2);
    FrEnd.postCleanup();

    return null;
  }
}
