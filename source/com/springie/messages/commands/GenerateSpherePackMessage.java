//This program has been placed into the public domain by its author.

package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.ArgumentList;
import com.springie.messages.NewMessage;
import com.springie.presets.ProceduralObject;

public class GenerateSpherePackMessage extends NewMessage {
  public GenerateSpherePackMessage() {
    super(null);
  }

  public Object execute() {
    ProceduralObjects.allNew(new ArgumentList() {
      public Object getArguments(int i) {
        switch (i) {
          case 0:
            return ProceduralObject.sphere_pack;
          case 1:
            return Integer.valueOf(
              FrEnd.panel_edit_generate.textfield_generate_sphere_pack
                .getText());
          default:
            return null;
        }
      }
    });

    return null;
  }
}
