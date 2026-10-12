// This program has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.messages.DemoProvider;
import com.springie.messages.DemoRegistry;

/**
 * The procedural demos in one place: display name plus the message that
 * launches the demo. Implements {@link DemoProvider} so the demos are
 * discovered via {@link java.util.ServiceLoader} -- springie-core's GUI
 * reads them from {@link DemoRegistry} without depending on this module.
 */
public final class DemoCatalog implements DemoProvider {

  @Override
  public void registerDemos() {
    DemoRegistry.register("Sidewinder", SidewinderDemoMessage::new);
    DemoRegistry.register("Crawler", CrawlerDemoMessage::new);
    DemoRegistry.register("Spider Tank", SpiderTankDemoMessage::new);
    DemoRegistry.register("Wheelbarrow", WheelbarrowDemoMessage::new);
    DemoRegistry.register("Hopper", HopperDemoMessage::new);
    DemoRegistry.register("Caterpillar", CaterpillarDemoMessage::new);
    DemoRegistry.register("Caterpillar 2", Caterpillar2DemoMessage::new);
    DemoRegistry.register("Caterpillar track", CaterpillarTrackDemoMessage::new);
    DemoRegistry.register("Slinky", SlinkyDemoMessage::new);
  }
}
