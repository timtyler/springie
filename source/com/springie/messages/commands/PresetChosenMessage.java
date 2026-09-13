//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;
import com.springie.render.SetUpCode;

public class PresetChosenMessage extends NewMessage {
  public PresetChosenMessage() {
    super(null);
  }

  public Object execute() {
    FrEnd.data_input.resetWorkspaces();
    SetUpCode.clearAndThenAddInitialObjects();
    FrEnd.reflectValuesInGUIAfterPropertyEditing();

    return null;
  }
}
