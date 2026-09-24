//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.FrEnd;
import com.springie.demos.CrawlerDemo;
import com.springie.messages.NewMessage;
import java.awt.EventQueue;

public class CrawlerDemoMessage extends NewMessage {
  public CrawlerDemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=100 (pixels) to leave room to walk to the right.
    CrawlerDemo.buildAt(100);
    // Demo models run boundless with a follow camera: pin the creature
    // on X so it can never touch the side walls. The user can untick
    // 'Continuously center X' in Universe > Centering to get the walls back.
    FrEnd.continuously_centre_x = true;
    FrEnd.demo_model = true;
    com.springie.world.UniverseDefaults.snapshot();
    // Sync the Universe panel (gravity etc.) with the demo's settings.
    // Must run on the AWT thread: this message executes on the animation
    // thread, and AWT components are not thread-safe.
    EventQueue.invokeLater(FrEnd::reflectValuesInGUIAfterPropertyEditing);
    return null;
  }
}
