//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;
import com.springie.modification.DomeRelatedChangeDelegator;
import com.springie.world.NewMessage;

public class ExpandNodesMessage extends NewMessage {
  public ExpandNodesMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.expand();
    CommandContextHolder.get().reflectRadius();

    return null;
  }
}
