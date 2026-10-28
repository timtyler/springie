// This program has been placed into the public domain by its author.

package com.springie.metrics;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With no links selected the meter averages over all links; with some
 * selected it averages over the selected links only.
 */
class WobbleMeterTest {

  private static Node nodeAt(final int x) {
    final Node node = new Node();
    node.pos = new Point3D(x, 0, 0);
    return node;
  }

  @Test
  void selectedLinksOnly() {
    final LinkManager link_manager = new LinkManager();
    final LinkTypeFactory link_type_factory = new LinkTypeFactory();
    final LinkType type_selected = link_type_factory.getNew();
    final LinkType type_plain = link_type_factory.getNew();
    type_selected.selected = true;
    link_manager.element.add(new Link(nodeAt(0), nodeAt(100000),
        type_selected, new Clazz(0)));
    final Node moving = nodeAt(100000);
    link_manager.element.add(new Link(nodeAt(0), moving,
        type_plain, new Clazz(0)));
    WobbleMeter.setActive(true);
    try {
      for (int tick = 0; tick < WobbleMeter.WINDOW_TICKS; tick++) {
        moving.pos = new Point3D(tick % 2 == 0 ? 100000 : 110000, 0, 0);
        WobbleMeter.sampleIfActive(link_manager);
      }
      assertEquals(0, WobbleMeter.getWobbleTenthsPercent());
      type_selected.selected = false;
      for (int tick = 0; tick < WobbleMeter.WINDOW_TICKS; tick++) {
        moving.pos = new Point3D(tick % 2 == 0 ? 100000 : 110000, 0, 0);
        WobbleMeter.sampleIfActive(link_manager);
      }
      final int all = WobbleMeter.getWobbleTenthsPercent();
      assertTrue(all >= 40 && all <= 55, "wobble was " + all);
    } finally {
      WobbleMeter.setActive(false);
    }
  }
}
