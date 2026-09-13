//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;

public class ContinuouslyCentreMessage extends NewMessage {
  public ContinuouslyCentreMessage() {
    super(null);
  }

  public Object execute() {
    FrEnd.continuously_centre = !FrEnd.continuously_centre;
    return null;
  }
}
