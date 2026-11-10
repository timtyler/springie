//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.modification.MuscleTools;
import com.springie.world.NewMessage;

public class MusclesAddAllMessage extends NewMessage {
  public MusclesAddAllMessage() {
    super(null);
  }

  public Object execute() {
    MuscleTools.addToAll();
    return null;
  }
}
