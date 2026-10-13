//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.io.IoConfig;
import com.springie.world.World;

import com.springie.world.ArgumentList;

public final class ProceduralObjects {
  private ProceduralObjects() {
  }

  public static void allNew(final ArgumentList al) {
    new com.springie.io.in.DataInput(null).resetWorkspaces();
    // Was SetUpCode.clearAndThenAddProceduralObjects(al) (decoupled from gui).
    com.springie.context.ContextManager.getNodeManager().initialWithPreset(al);
    com.springie.world.WorldManager.initial();
    com.springie.io.IoConfig.on_reset.run();
    com.springie.io.IoConfig.after_property_editing.run();
  }
}
