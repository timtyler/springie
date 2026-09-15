//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.demos.WheelDemo;
import com.springie.messages.NewMessage;

public class WheelDemoMessage extends NewMessage {
  public WheelDemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=120 (pixels) to leave room to roll to the right.
    WheelDemo.buildAt(120);
    return null;
  }
}
