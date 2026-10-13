//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.world.NewMessage;
import com.springie.modification.DomeRelatedChangeDelegator;

public class ChargeDownMessage extends NewMessage {
  public ChargeDownMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.chargeDown();
    CommandContextHolder.get().reflectCharge();

    return null;
  }
}
