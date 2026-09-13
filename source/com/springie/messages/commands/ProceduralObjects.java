//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.ArgumentList;
import com.springie.render.SetUpCode;

public final class ProceduralObjects {
  private ProceduralObjects() {
  }

  public static void allNew(ArgumentList al) {
    FrEnd.data_input.resetWorkspaces();
    SetUpCode.clearAndThenAddProceduralObjects(al);
    FrEnd.reflectValuesInGUIAfterPropertyEditing();
  }
}
