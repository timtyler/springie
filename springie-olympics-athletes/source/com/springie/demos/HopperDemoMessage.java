//This program has been placed into the public domain by its author.
package com.springie.demos;
import com.springie.world.World;

import com.springie.world.NewMessage;
import java.awt.EventQueue;

public class HopperDemoMessage extends NewMessage {
  public HopperDemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=400 (pixels), centred in the arena for vertical hops.
    HopperDemo.buildAt(400);
    // Demo models run boundless with a follow camera: pin the creature
    // on X so it can never touch the side walls. The user can untick
    // 'Continuously center X' in Universe > Centering to get the walls back.
    World.continuously_centre_x = true;
    World.demo_model = true;
    com.springie.world.UniverseDefaults.snapshot();
    // Sync the Universe panel (gravity etc.) with the demo's settings.
    // Must run on the AWT thread: this message executes on the animation
    // thread, and AWT components are not thread-safe.
    EventQueue.invokeLater(() -> com.springie.modification.CommandContextHolder.get().reflectValuesInGUIAfterPropertyEditing());
    return null;
  }
}
