//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.world.ArgumentList;
import com.springie.world.NewMessage;
import com.springie.presets.ProceduralObject;

public class GenerateTubeMessage extends NewMessage {
  public GenerateTubeMessage() {
    super(null);
  }

  public Object execute() {
    ProceduralObjects.allNew(new ArgumentList() {
      public Object getArguments(final int i) {
        switch (i) {
          case 0:
            return ProceduralObject.tube;
          case 1:
            return Integer.valueOf(
              CommandContextHolder.get().getGenerateTubeCircumference());
          case 2:
            return Integer.valueOf(
              CommandContextHolder.get().getGenerateTubeLength());
          default:
            return null;
        }
      }
    });

    return null;
  }
}
