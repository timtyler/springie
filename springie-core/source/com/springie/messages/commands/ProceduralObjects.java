//This program has been placed into the public domain by its author.

package com.springie.messages.commands;
import com.springie.world.World;

import com.springie.FrEnd;
import com.springie.world.ArgumentList;
import com.springie.render.SetUpCode;

public final class ProceduralObjects {
  private ProceduralObjects() {
  }

  public static void allNew(final ArgumentList al) {
    new com.springie.io.in.DataInput(null).resetWorkspaces();
    SetUpCode.clearAndThenAddProceduralObjects(al);
    FrEnd.reflectValuesInGUIAfterPropertyEditing();
  }
}
