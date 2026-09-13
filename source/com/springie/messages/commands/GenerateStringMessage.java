//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.ArgumentList;
import com.springie.messages.NewMessage;
import com.springie.presets.ProceduralObject;

public class GenerateStringMessage extends NewMessage {
  public GenerateStringMessage() {
    super(null);
  }

  public Object execute() {
    ProceduralObjects.allNew(new ArgumentList() {
      public Object getArguments(int i) {
        switch (i) {
          case 0:
            return ProceduralObject.string;
          case 1:
            return Integer.valueOf(
              FrEnd.panel_edit_generate.textfield_generate_string_length
                .getText());
          default:
            return null;
        }
      }
    });

    return null;
  }
}
