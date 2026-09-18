//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.demos.Caterpillar3Demo;
import com.springie.messages.NewMessage;
import java.awt.EventQueue;

public class Caterpillar3DemoMessage extends NewMessage {
  public Caterpillar3DemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=100 (pixels), near the top-left, facing +x.
    Caterpillar3Demo.buildAt(100);
    // Sync the Universe panel (gravity etc.) with the demo's settings.
    // Must run on the AWT thread: this message executes on the animation
    // thread, and AWT components are not thread-safe.
    EventQueue.invokeLater(FrEnd::reflectValuesInGUIAfterPropertyEditing);
    return null;
  }
}
