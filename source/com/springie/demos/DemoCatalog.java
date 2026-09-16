// This program has been placed into the public domain by its author.

package com.springie.demos;

import java.util.function.Supplier;

import com.springie.messages.NewMessage;
import com.springie.messages.commands.CrawlerDemoMessage;
import com.springie.messages.commands.GrasshopperDemoMessage;
import com.springie.messages.commands.HopperDemoMessage;
import com.springie.messages.commands.SnakeDemoMessage;
import com.springie.messages.commands.SpiderTankDemoMessage;
import com.springie.messages.commands.WheelDemoMessage;

/**
 * The procedural demos in one place: display name plus the message that
 * launches the demo. The Models menu's Demos submenu and the bottom button
 * bar's file card both read from here, so the two entry points always offer
 * the same demos and launch them through the exact same message -- the two
 * can never drift apart.
 */
public final class DemoCatalog {

  private DemoCatalog() {
  }

  public static final class Demo {
    public final String name;

    private final Supplier<NewMessage> message;

    public Demo(String name, Supplier<NewMessage> message) {
      this.name = name;
      this.message = message;
    }

    /** The label this demo appears under in the bottom bar's file card. */
    public String label() {
      return "Demo: " + this.name;
    }

    /** The launch message -- the same one the Demos menu enqueues. */
    public NewMessage newMessage() {
      return this.message.get();
    }
  }

  public static final Demo[] DEMOS = {
      new Demo("Snake", SnakeDemoMessage::new),
      new Demo("Crawler", CrawlerDemoMessage::new),
      new Demo("Spider Tank", SpiderTankDemoMessage::new),
      new Demo("Wheel", WheelDemoMessage::new),
      new Demo("Grasshopper", GrasshopperDemoMessage::new),
      new Demo("Hopper", HopperDemoMessage::new),
  };

  /**
   * Finds the demo shown under the given file-card label, or null when the
   * label is a model file rather than a demo entry.
   */
  public static Demo forLabel(String label) {
    if (label == null) {
      return null;
    }
    for (final Demo demo : DEMOS) {
      if (demo.label().equals(label)) {
        return demo;
      }
    }
    return null;
  }
}
