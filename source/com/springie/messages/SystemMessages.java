//This program has been placed into the public domain by its author.
package com.springie.messages;

import com.springie.FrEnd;

public class SystemMessages {
  public NewMessage getRestartMessage() {
    final NewMessage message = new NewMessage(null) {
      public Object execute() {
        FrEnd.data_input.loadFile("" + FrEnd.next_file_path);
        FrEnd.reflectValuesInGUIAfterPropertyEditing();
        return null;
      }
    };
    return message;
  }
  
  public NewMessage getDelayedRestartMessage() {
    final NewMessage message = new NewMessage(null) {
      public Object execute() {
        FrEnd.new_message_manager.add(getRestartMessage());
        return null;
      }
    };
    return message;
  }
  
  public NewMessage getPauseMessage() {
    final NewMessage message = new NewMessage(null) {
      public Object execute() {
        FrEnd.paused = !FrEnd.paused;
        //FrEnd.panel_fundamental.button_bar_paused.getState();
        FrEnd.greyStepIfNeeded();
        return null;
      }
    };
    return message;
  }
}
