//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.messages.NewMessage;
import com.springie.modification.DomeRelatedChangeDelegator;

public class DisableFlagMessage extends NewMessage {
  public DisableFlagMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.disable();
    return null;
  }
}
