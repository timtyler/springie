//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.DomeRelatedChangeDelegator;
import com.springie.world.NewMessage;

public class EqualiseLinkLengthsMessage extends NewMessage {
  public EqualiseLinkLengthsMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.equaliseLinkLengths();
    return null;
  }
}
