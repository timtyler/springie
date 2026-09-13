//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;
import com.springie.modification.DomeRelatedChangeDelegator;

public class StiffnessDownMessage extends NewMessage {
  public StiffnessDownMessage() {
    super(null);
  }

  public Object execute() {
    DomeRelatedChangeDelegator.stiffnessDown();
    FrEnd.panel_edit_properties_scalars.reflectStiffness();
    return null;
  }
}
