// This program has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.world.NewMessage;
import com.springie.world.World;

import java.awt.EventQueue;

public class RockingHorseDemoMessage extends NewMessage {
  public RockingHorseDemoMessage() {
    super(null);
  }

  public Object execute() {
    RockingHorseDemo.buildAt(400);
    // Demo models run boundless with a follow camera.
    World.continuously_centre_x = true;
    World.demo_model = true;
    com.springie.world.UniverseDefaults.snapshot();
    // Sync the Universe panel with the demo's settings.
    EventQueue.invokeLater(() -> com.springie.modification.CommandContextHolder.get().reflectValuesInGUIAfterPropertyEditing());
    return null;
  }
}
