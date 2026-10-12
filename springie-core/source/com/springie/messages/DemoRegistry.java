// This program has been placed into the public domain by its author.

package com.springie.messages;

import com.springie.world.NewMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Registry of available demos: display name plus the message that launches
 * the demo. The Models menu's Demos submenu and the bottom button bar's
 * file card both read from here, so the two entry points always offer the
 * same demos and launch them through the exact same message -- the two can
 * never drift apart.
 *
 * <p>Demos are provided by {@link DemoProvider} implementations, discovered
 * via {@link ServiceLoader}. The springie-demos module provides them; this
 * class lives in springie-core so the GUI can list demos without depending
 * on the demos module.
 */
public final class DemoRegistry {

  private DemoRegistry() {
  }

  /** A single demo: display name plus a supplier of its launch message. */
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

  private static final List<Demo> DEMOS = new ArrayList<>();

  private static boolean loaded;

  /** Registers a demo. Called by {@link DemoProvider} implementations. */
  public static synchronized void register(String name, Supplier<NewMessage> message) {
    DEMOS.add(new Demo(name, message));
  }

  /** Loads providers once, via ServiceLoader. */
  private static synchronized void ensureLoaded() {
    if (!loaded) {
      loaded = true;
      for (DemoProvider provider : ServiceLoader.load(DemoProvider.class)) {
        provider.registerDemos();
      }
    }
  }

  /** All registered demos, in registration order. */
  public static List<Demo> demos() {
    ensureLoaded();
    return Collections.unmodifiableList(DEMOS);
  }

  /**
   * Finds the demo with the given name, or null when there is none. The
   * placeholder in the demos dropdown maps to no demo.
   */
  public static Demo forName(final String name) {
    ensureLoaded();
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
