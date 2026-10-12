//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.messages.NewMessage;
import com.springie.modification.DomeRelatedChangeDelegator;

public class FixFlagMessage extends NewMessage {
  public FixFlagMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.fix();
    return null;
  }
}
