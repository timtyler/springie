//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.io.in.DataInput;
import com.springie.messages.NewMessage;
import com.springie.render.SetUpCode;

public class PresetChosenMessage extends NewMessage {
  public PresetChosenMessage() {
    super(null);
  }

  public Object execute() {
    // Bind the reset to the live manager: the shared FrEnd.data_input
    // keeps the NodeManager it was first given, so after a model switch
    // it would wipe a background model's workspace instead.
    new DataInput(ContextManager.getNodeManager()).resetWorkspaces();
    SetUpCode.clearAndThenAddInitialObjects();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
