//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.ArgumentList;
import com.springie.messages.NewMessage;
import com.springie.presets.ProceduralObject;

public class GenerateTubeMessage extends NewMessage {
  public GenerateTubeMessage() {
    super(null);
  }

  public Object execute() {
    ProceduralObjects.allNew(new ArgumentList() {
      public Object getArguments(int i) {
        switch (i) {
          case 0:
            return ProceduralObject.tube;
          case 1:
            return Integer.valueOf(
              FrEnd.panel_edit_generate.textfield_generate_tube_circumference
                .getText());
          case 2:
            return Integer.valueOf(
              FrEnd.panel_edit_generate.textfield_generate_tube_length
                .getText());
          default:
            return null;
        }
      }
    });

    return null;
  }
}
