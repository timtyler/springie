// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.Muscles;

/**
 * The caterpillar track's axle: two nodes on the z-axis through the
 * middle of the track, joined by a rigid shaft and spoked to every
 * inner ring node on both sides, carrying opposite compass headings so
 * the universe bias pulls the ends apart and tensions the spokes.
 * Plus the muscle wave: the central circle's links vary in length
 * about their rest length, phased as one travelling wavelength.
 */
class CaterpillarTrackDemoTest {

  private int saved_bias;
  private boolean saved_muscles;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.saved_bias = CompassPoint.bias_size;
    this.saved_muscles = Muscles.enabled;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    CompassPoint.bias_size = this.saved_bias;
    Muscles.enabled = this.saved_muscles;
  }

  @Test
  void axleEndsCarryOpposingCompassHeadings() {
    CaterpillarTrackDemo.buildAt(300);
    final NodeManager manager = ContextManager.getNodeManager();

    // 21 ring nodes + 2 axle nodes.
    assertEquals(23, manager.element.size(), "node count");

    Node north = null;
    Node south = null;
    for (int i = 0; i < manager.element.size(); i++) {
      final Node node = (Node) manager.element.get(i);
      if (node.compass == CompassPoint.N) {
        north = node;
      } else if (node.compass == CompassPoint.S) {
        south = node;
      } else {
        assertEquals(null, node.compass, "ring nodes carry no heading");
      }
    }
    assertNotNull(north, "one axle end heads N");
    assertNotNull(south, "one axle end heads S");
    // N = -z, S = +z: the ends sit on opposite sides of the track
    // middle, clear of the z=0 depth wall.
    final int shift = com.springie.render.Coords.shift;
    assertEquals(
        (CaterpillarTrackDemo.Z_OFFSET_PX - CaterpillarTrackDemo.AXLE_HALF_PX)
            << shift,
        north.pos.z, "N end z");
    assertEquals(
        (CaterpillarTrackDemo.Z_OFFSET_PX + CaterpillarTrackDemo.AXLE_HALF_PX)
            << shift,
        south.pos.z, "S end z");
    // Both ends on the z-axis through the track middle.
    assertEquals(north.pos.x, south.pos.x, "axle ends share x");
    assertEquals(north.pos.y, south.pos.y, "axle ends share y");

    // 63 ring links + 14 spokes + 1 shaft.
    assertEquals(78, manager.getLinkManager().element.size(), "link count");

    // Each axle end: 7 spokes + the shaft.
    assertEquals(8, north.list_of_links.size(), "N end link count");
    assertEquals(8, south.list_of_links.size(), "S end link count");

    // The default bias is on, so the pull actually happens.
    assertEquals(CaterpillarTrackDemo.compass_bias, CompassPoint.bias_size,
        "default compass bias applied");
  }

  @Test
  void centralCircleLinksAreMuscles() {
    CaterpillarTrackDemo.buildAt(300);
    final NodeManager manager = ContextManager.getNodeManager();
    final LinkManager link_manager = manager.getLinkManager();

    // Muscles are switched on for this model.
    assertTrue(Muscles.enabled, "muscles enabled");

    final List<Link> muscles = new ArrayList<>();
    final int n_links = link_manager.element.size();
    for (int i = 0; i < n_links; i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller != null) {
        muscles.add(link);
      }
    }
    // Exactly the 7 links of the central circle; everything else --
    // ring bracing, spokes, shaft -- stays passive.
    assertEquals(7, muscles.size(), "muscle count");

    // One full wavelength around the ring: phases i * period / 7,
    // each used exactly once.
    final int period = CaterpillarTrackDemo.muscle_period_ticks;
    final boolean[] seen = new boolean[7];
    for (final Link link : muscles) {
      boolean matched = false;
      for (int i = 0; i < 7; i++) {
        if (link.phase == i * period / 7) {
          assertFalse(seen[i], "phase used once");
          seen[i] = true;
          matched = true;
        }
      }
      assertTrue(matched, "muscle phase sits on the wave");
    }
  }
}
