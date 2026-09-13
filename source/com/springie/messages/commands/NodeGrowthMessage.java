//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;

public class NodeGrowthMessage extends NewMessage {
  public NodeGrowthMessage() {
    super(null);
  }

  public Object execute() {
    FrEnd.node_growth = !FrEnd.node_growth;
    return null;
  }
}
