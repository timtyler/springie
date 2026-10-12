//This program has been placed into the public domain by its author.
package com.springie.messages;
import com.springie.world.World;

import com.springie.FrEnd;
import com.springie.context.ModelManager;

public class SystemMessages {
  public NewMessage getRestartMessage() {
    final NewMessage message = new NewMessage(null) {
      public Object execute() {
        // Route through the ModelManager: the shared World.data_input keeps
        // the NodeManager it was first given, so after a model switch it
        // would load the preset into a stale, invisible manager and the
        // restart would appear to do nothing.
        ModelManager.replaceCurrentModel("" + FrEnd.next_file_path);
        FrEnd.setFilePath(ModelManager.activeFilePath());
        FrEnd.reflectAllValuesInGUIAfterSeriousEditing();
        return null;
      }
    };
    return message;
  }
  
  public NewMessage getDelayedRestartMessage() {
    final NewMessage message = new NewMessage(null) {
      public Object execute() {
        World.new_message_manager.add(getRestartMessage());
        return null;
      }
    };
    return message;
  }
  
  public NewMessage getPauseMessage() {
    final NewMessage message = new NewMessage(null) {
      public Object execute() {
        World.paused = !World.paused;
        //FrEnd.panel_fundamental.button_bar_paused.getState();
        FrEnd.greyStepIfNeeded();
        return null;
      }
    };
    return message;
  }
}
