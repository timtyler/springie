//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.demos.SnakeDemo;
import com.springie.messages.NewMessage;
import java.awt.EventQueue;

public class SnakeDemoMessage extends NewMessage {
  public SnakeDemoMessage() {
    super(null);
  }

  public Object execute() {
    SnakeDemo.build();
    // Sync the Universe panel (gravity etc.) with the demo's settings.
    // Must run on the AWT thread: this message executes on the animation
    // thread, and AWT components are not thread-safe.
    EventQueue.invokeLater(FrEnd::reflectValuesInGUIAfterPropertyEditing);
    return null;
  }
}
