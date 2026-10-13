//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.modification.CommandContextHolder;

import com.springie.world.ArgumentList;
import com.springie.world.NewMessage;
import com.springie.presets.ProceduralObject;

public class GenerateMatrixMessage extends NewMessage {
  public GenerateMatrixMessage() {
    super(null);
  }

  public Object execute() {
    ProceduralObjects.allNew(new ArgumentList() {
      public Object getArguments(final int i) {
        switch (i) {
          case 0:
            return ProceduralObject.matrix;
          case 1:
            return Integer.valueOf(
              CommandContextHolder.get().getGenerateMatrixX());
          case 2:
            return Integer.valueOf(
              CommandContextHolder.get().getGenerateMatrixY());
          case 3:
            return Integer.valueOf(
              CommandContextHolder.get().getGenerateMatrixZ());
          default:
            return null;
        }
      }
    });

    return null;
  }
}
