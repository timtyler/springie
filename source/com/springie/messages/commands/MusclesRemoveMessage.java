//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.messages.NewMessage;
import com.springie.modification.MuscleTools;

public class MusclesRemoveMessage extends NewMessage {
  public MusclesRemoveMessage() {
    super(null);
  }

  public Object execute() {
    MuscleTools.remove();
    return null;
  }
}
