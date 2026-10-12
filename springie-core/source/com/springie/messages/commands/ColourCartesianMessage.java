//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.NewMessage;
import com.springie.modification.colour.ColourClassificationCartesian;

public class ColourCartesianMessage extends NewMessage {
  public ColourCartesianMessage() {
    super(null);
  }

  public Object execute() {
    final NodeManager node_manager = ContextManager.getNodeManager();

    new ColourClassificationCartesian(node_manager).setColour();
    FrEnd.postCleanup();

    return null;
  }
}
