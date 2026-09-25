// This program has been placed into the public domain by its author.

package com.springie.demos;

import java.util.function.Supplier;

import com.springie.messages.NewMessage;
import com.springie.messages.commands.Caterpillar2DemoMessage;
import com.springie.messages.commands.CaterpillarDemoMessage;
import com.springie.messages.commands.CaterpillarTrackDemoMessage;
import com.springie.messages.commands.CrawlerDemoMessage;
import com.springie.messages.commands.HopperDemoMessage;
import com.springie.messages.commands.SlinkyDemoMessage;
import com.springie.messages.commands.SidewinderDemoMessage;
import com.springie.messages.commands.SpiderTankDemoMessage;
import com.springie.messages.commands.HamsterWheelDemoMessage;

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

    /** The launch message -- the same one the Demos menu enqueues. */
    public NewMessage newMessage() {
      return this.message.get();
    }
  }

  public static final Demo[] DEMOS = {
      new Demo("Sidewinder", SidewinderDemoMessage::new),
      new Demo("Crawler", CrawlerDemoMessage::new),
      new Demo("Spider Tank", SpiderTankDemoMessage::new),
      new Demo("Hamster wheel", HamsterWheelDemoMessage::new),
      new Demo("Hopper", HopperDemoMessage::new),
      new Demo("Caterpillar", CaterpillarDemoMessage::new),
      new Demo("Caterpillar 2", Caterpillar2DemoMessage::new),
      new Demo("Caterpillar track", CaterpillarTrackDemoMessage::new),
      new Demo("Slinky", SlinkyDemoMessage::new),
  };

  /**
   * Finds the demo with the given name, or null when there is none. The
   * placeholder in the demos dropdown maps to no demo.
   */
  public static Demo forName(String name) {
    if (name == null) {
      return null;
    }
    for (final Demo demo : DEMOS) {
      if (demo.name.equals(name)) {
        return demo;
      }
    }
    return null;
  }
}
