//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.DomeRelatedChangeDelegator;
import com.springie.world.NewMessage;

public class HideFlagMessage extends NewMessage {
  public HideFlagMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.hide();
    return null;
  }
}
