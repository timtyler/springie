//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;
import com.springie.modification.DomeRelatedChangeDelegator;
import com.springie.world.NewMessage;

public class ChargeUpMessage extends NewMessage {
  public ChargeUpMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.chargeUp();
    CommandContextHolder.get().reflectCharge();

    return null;
  }
}
