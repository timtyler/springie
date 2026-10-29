// This program has been placed into the public domain by its author.

package com.springie.metrics;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Coords;
import com.springie.geometry.Point3D;
import com.springie.world.World;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gradient descent on displaced two-node springs. Starting very
 * underdamped, the first step must raise the damping, and a full run
 * must end with the damping raised and the wobble reduced. Types that
 * differ only by length are one material and step together.
 */
class DampingOptimizerTest {

  private static void step(final World world, final Node a, final Node b) {
    a.travel();
    b.travel();
    world.wrappedLinkExerciser(world.getLinkManager());
    WobbleMeter.sampleIfActive(world.getLinkManager());
    DampingOptimizer.tickIfRunning(world.getLinkManager());
  }

  private static void stepAll(final World world, final Node[] nodes) {
    for (int i = 0; i < nodes.length; i++) {
      nodes[i].travel();
    }
    world.wrappedLinkExerciser(world.getLinkManager());
    WobbleMeter.sampleIfActive(world.getLinkManager());
    DampingOptimizer.tickIfRunning(world.getLinkManager());
  }

  @Test
  void optimiseReducesWobble() {
    final NodeManager manager = new NodeManager();
    final World world = manager;
    final Node a = world.addNewAgent();
    final Node b = world.addNewAgent();
    a.pos = new Point3D(0, 0, 0);
    b.pos = new Point3D(150 << Coords.shift, 0, 0);
    final LinkTypeFactory link_types = new LinkTypeFactory();
    final LinkType type = link_types.getNew(100 << Coords.shift, 50);
    type.damping = 10;
    world.getLinkManager().element.add(new Link(a, b, type, new Clazz(0)));

    final boolean was_paused = World.paused;
    final boolean was_three_d = World.three_d;
    World.paused = false;
    World.three_d = true;
    WobbleMeter.setActive(true);
    try {
      int before_max = 0;
      for (int tick = 0; tick < 150; tick++) {
        step(world, a, b);
        if (tick >= 100) {
          before_max = Math.max(before_max,
              WobbleMeter.getWobbleTenthsPercent());
        }
      }
      assertTrue(before_max > 0, "expected initial wobble");
      assertTrue(DampingOptimizer.start(world.getLinkManager(), 60000));
      int ticks = 0;
      while (type.damping != 30 && ticks < 1000) {
        step(world, a, b);
        ticks++;
      }
      assertEquals(30, type.damping);
      DampingOptimizer.abort();
      assertFalse(DampingOptimizer.isRunning());
      assertEquals(10, type.damping);
      assertTrue(DampingOptimizer.start(world.getLinkManager(), 2000));
      final long deadline = System.currentTimeMillis() + 10000;
      while (DampingOptimizer.isRunning()
          && System.currentTimeMillis() < deadline) {
        step(world, a, b);
      }
      assertFalse(DampingOptimizer.isRunning());
      assertTrue(type.damping > 10, "damping was " + type.damping);
      for (int tick = 0; tick < 110; tick++) {
        step(world, a, b);
      }
      final int after = WobbleMeter.getWobbleTenthsPercent();
      assertTrue(after < before_max,
          "wobble before " + before_max + " after " + after);
    } finally {
      DampingOptimizer.abort();
      WobbleMeter.setActive(false);
      World.paused = was_paused;
      World.three_d = was_three_d;
    }
  }

  @Test
  void sameMaterialTypesStepTogether() {
    final NodeManager manager = new NodeManager();
    final World world = manager;
    final Node a = world.addNewAgent();
    final Node b = world.addNewAgent();
    final Node c = world.addNewAgent();
    final Node d = world.addNewAgent();
    a.pos = new Point3D(0, 0, 0);
    b.pos = new Point3D(150 << Coords.shift, 0, 0);
    c.pos = new Point3D(400 << Coords.shift, 0, 0);
    d.pos = new Point3D(580 << Coords.shift, 0, 0);
    final Node[] nodes = {a, b, c, d};
    final LinkTypeFactory link_types = new LinkTypeFactory();
    final LinkType type_a = link_types.getNew(100 << Coords.shift, 50);
    final LinkType type_b = link_types.getNew(120 << Coords.shift, 50);
    type_b.radius = type_a.radius;
    type_a.damping = 10;
    type_b.damping = 10;
    world.getLinkManager().element.add(new Link(a, b, type_a, new Clazz(0)));
    world.getLinkManager().element.add(new Link(c, d, type_b, new Clazz(0)));

    final boolean was_paused = World.paused;
    final boolean was_three_d = World.three_d;
    World.paused = false;
    World.three_d = true;
    WobbleMeter.setActive(true);
    try {
      for (int tick = 0; tick < 150; tick++) {
        stepAll(world, nodes);
      }
      assertTrue(DampingOptimizer.start(world.getLinkManager(), 60000));
      int ticks = 0;
      while (type_a.damping != 30 && ticks < 1000) {
        stepAll(world, nodes);
        ticks++;
      }
      assertEquals(30, type_a.damping);
      assertEquals(30, type_b.damping);
      assertTrue(ticks <= 230, "step took " + ticks + " ticks");
    } finally {
      DampingOptimizer.abort();
      WobbleMeter.setActive(false);
      World.paused = was_paused;
      World.three_d = was_three_d;
    }
  }
}
