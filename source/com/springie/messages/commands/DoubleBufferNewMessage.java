//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.messages.NewMessage;
import com.springie.preferences.Preferences;

public class DoubleBufferNewMessage extends NewMessage {
  public DoubleBufferNewMessage() {
    super(null);
  }

  public Object execute() {
    final boolean bool_db1 = FrEnd.panel_preferences_renderer_modern.checkbox_db_new.getState();
    final String pref2 = Preferences.renderer_new_double_buffer;
    FrEnd.preferences.map.put(pref2, Boolean.valueOf(bool_db1));

    FrEnd.postCleanup();

    FrEnd.main_canvas.setUpGraphicsHandle();
    FrEnd.main_canvas.forceResize();

    return null;
  }
}
