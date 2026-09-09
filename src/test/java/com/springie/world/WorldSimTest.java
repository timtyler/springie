package com.springie.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Headless physics smoke test: builds a small world and steps the simulation,
 * exercising the node/link collection code paths without starting the GUI.
 */
class WorldSimTest {

  @Test
  void worldUpdateMovesNodesAndPreservesThem() {
    // FrEnd's static initialiser builds the GUI, so this test needs a display
    // (it runs under xvfb in headless CI environments).
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final boolean old_check_collisions = FrEnd.check_collisions;
    FrEnd.check_collisions = false;
    try {
      final NodeManager nodes_world = new NodeManager();
      final World world = nodes_world;

      // confine() bounces nodes off the containing "egg" node, so provide one.
      final Node egg = world.addNewAgent();
      egg.type.setSize(1 << 24);
      world.associated_node = egg;

      final int n = 12;
      final Node[] nodes = new Node[n];
      final int[] start_x = new int[n];
      for (int i = 0; i < n; i++) {
        final Node node = world.addNewAgent();
        node.pos = new Point3D(i * 500, 0, 0);
        node.velocity.x = 32;
        nodes[i] = node;
        start_x[i] = node.pos.x;
      }

      // Link the nodes into a chain, exercising the link manager's collections.
      final LinkTypeFactory link_types = new LinkTypeFactory();
      for (int i = 0; i < n - 1; i++) {
        world.getLinkManager().element.add(
          new Link(nodes[i], nodes[i + 1], link_types.getNew(100 << Coords.shift, 50), new Clazz(0)));
      }

      for (int step = 0; step < 30; step++) {
        world.privateWorldUnbufferedUpdate();
      }

      assertEquals(n + 1, world.element.size(), "nodes must be preserved");
      assertEquals(n - 1, world.getLinkManager().element.size(), "links must be preserved");
      for (int i = 0; i < n; i++) {
        assertNotEquals(start_x[i], nodes[i].pos.x, "node " + i + " should have moved");
      }
    } finally {
      FrEnd.check_collisions = old_check_collisions;
    }
  }
}
