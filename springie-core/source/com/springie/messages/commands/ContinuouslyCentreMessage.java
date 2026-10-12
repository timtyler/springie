//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;

public class ContinuouslyCentreMessage extends NewMessage {
  /** Which axis to toggle: 0 = x, 1 = y, 2 = z. */
  private final int axis;

  public ContinuouslyCentreMessage(int axis) {
    super(null);
    this.axis = axis;
  }

  public Object execute() {
    if (this.axis == 0) {
      FrEnd.continuously_centre_x = !FrEnd.continuously_centre_x;
    } else if (this.axis == 1) {
      FrEnd.continuously_centre_y = !FrEnd.continuously_centre_y;
    } else {
      FrEnd.continuously_centre_z = !FrEnd.continuously_centre_z;
    }
    return null;
  }
}
