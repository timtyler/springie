//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.demos.GrasshopperDemo;
import com.springie.messages.NewMessage;
import java.awt.EventQueue;

public class GrasshopperDemoMessage extends NewMessage {
  public GrasshopperDemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=400 (pixels), centred in the arena for a vertical jump.
    GrasshopperDemo.buildAt(400);
    com.springie.world.UniverseDefaults.snapshot();
    // Sync the Universe panel (gravity etc.) with the demo's settings.
    // Must run on the AWT thread: this message executes on the animation
    // thread, and AWT components are not thread-safe.
    EventQueue.invokeLater(FrEnd::reflectValuesInGUIAfterPropertyEditing);
    return null;
  }
}
