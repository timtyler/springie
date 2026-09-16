//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;
import com.springie.preferences.Preferences;

public class DoubleBufferOldMessage extends NewMessage {
  public DoubleBufferOldMessage() {
    super(null);
  }

  public Object execute() {
    final boolean bool_db = FrEnd.panel_preferences_shared_show.checkbox_db_old.getState();
    final String pref = Preferences.renderer_old_double_buffer;
    FrEnd.preferences.map.put(pref, Boolean.valueOf(bool_db));

    FrEnd.postCleanup();

    FrEnd.main_canvas.setUpGraphicsHandle();
    FrEnd.main_canvas.forceResize();

    return null;
  }
}
