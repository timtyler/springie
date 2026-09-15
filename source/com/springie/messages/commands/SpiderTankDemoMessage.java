//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.demos.SpiderTankDemo;
import com.springie.messages.NewMessage;

public class SpiderTankDemoMessage extends NewMessage {
  public SpiderTankDemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=100 (pixels) to leave room to walk to the right.
    SpiderTankDemo.buildAt(100);
    return null;
  }
}
