//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.stellation.StellationMaker;

public class AddStellationsMessage extends NewMessage {
  public AddStellationsMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    final StellationMaker stellation_maker = new StellationMaker(
      node_manager);
    final float sf = Float
      .parseFloat(FrEnd.panel_edit_misc.textfield_add_stellations.getText());

    stellation_maker.addStellations(1F + sf, 0xFFFFFF60);
    FrEnd.postCleanup();

    return null;
  }
}
