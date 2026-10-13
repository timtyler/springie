//This program has been placed into the public domain by its author.
package com.springie.messages.commands;
import com.springie.world.World;

import com.springie.FrEnd;
import com.springie.world.NewMessage;

public class ContinuouslyCentreMessage extends NewMessage {
  /** Which axis to toggle: 0 = x, 1 = y, 2 = z. */
  private final int axis;

  public ContinuouslyCentreMessage(int axis) {
    super(null);
    this.axis = axis;
  }

  public Object execute() {
    if (this.axis == 0) {
      World.continuously_centre_x = !World.continuously_centre_x;
    } else if (this.axis == 1) {
      World.continuously_centre_y = !World.continuously_centre_y;
    } else {
      World.continuously_centre_z = !World.continuously_centre_z;
    }
    return null;
  }
}
