//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.messages.NewMessage;
import com.springie.modification.DomeRelatedChangeDelegator;

public class RopeFlagMessage extends NewMessage {
  public RopeFlagMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.rope();
    return null;
  }
}
