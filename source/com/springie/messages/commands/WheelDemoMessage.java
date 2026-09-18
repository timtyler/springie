//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.demos.WheelDemo;
import com.springie.messages.NewMessage;
import java.awt.EventQueue;

public class WheelDemoMessage extends NewMessage {
  public WheelDemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=120 (pixels) to leave room to roll to the right.
    WheelDemo.buildAt(120);
    com.springie.world.UniverseDefaults.snapshot();
    // Sync the Universe panel (gravity etc.) with the demo's settings.
    // Must run on the AWT thread: this message executes on the animation
    // thread, and AWT components are not thread-safe.
    EventQueue.invokeLater(FrEnd::reflectValuesInGUIAfterPropertyEditing);
    return null;
  }
}
