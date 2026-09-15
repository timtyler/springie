//This program has been placed into the public domain by its author.
package com.springie.messages.commands;

import com.springie.demos.CrawlerDemo;
import com.springie.messages.NewMessage;

public class CrawlerDemoMessage extends NewMessage {
  public CrawlerDemoMessage() {
    super(null);
  }

  public Object execute() {
    // Build at x=100 (pixels) to leave room to walk to the right.
    CrawlerDemo.buildAt(100);
    return null;
  }
}
