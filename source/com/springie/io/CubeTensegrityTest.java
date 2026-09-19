// Regression test for the bundled cube.spr tensegrity model:
// six equal, non-touching struts in cubic geometry, 24 cables.

package com.springie.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;
import com.springie.utilities.random.Hortensius32Fast;
import com.springie.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class CubeTensegrityTest {

  private boolean old_paused;
  private boolean old_merge;

  /**
   * The 4000-tick stability run must see fresh-JVM universe state.
   * DataInput.loadFile normalises the universe via resetUniverseState(),
   * but only when FrEnd.merge is false; a leaked merge=true would skip
   * the reset and leave stale gravity/collision/temperature values from
   * earlier tests. Temperature jitter draws from World.rnd, so the RNG
   * is also reset for determinism.
   */
  @BeforeEach
  void setUp() {
    old_paused = FrEnd.paused;
    old_merge = FrEnd.merge;
    FrEnd.merge = false;
    resetWorldRandom();
  }

  @AfterEach
  void tearDown() {
    FrEnd.paused = old_paused;
    FrEnd.merge = old_merge;
    // Leave the RNG pristine so later tests see a fresh-JVM sequence.
    resetWorldRandom();
  }

  private static void resetWorldRandom() {
    try {
      final java.lang.reflect.Field field =
          World.class.getDeclaredField("rnd");
      field.setAccessible(true);
      final Hortensius32Fast rnd = (Hortensius32Fast) field.get(null);
      rnd.setSeed(4357);
    } catch (Exception e) {
      throw new RuntimeException("Failed to reset World.rnd", e);
    }
  }

  private static double linkLength(Link link) {
    final Node a = link.nodes[0];
    final Node b = link.nodes[1];
    final double dx = a.pos.x - b.pos.x;
    final double dy = a.pos.y - b.pos.y;
    final double dz = a.pos.z - b.pos.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  @Test
  void cubeIsAStableTensegrity() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    final NodeManager manager = new NodeManager();
    ContextManager.setNodeManager(manager);
    // Pin the canvas size: ReaderSPRExecutor scales the model to fit
    // Coords.x/y/z_pixels, and earlier GUI tests (e.g. BottomBarWrapTest)
    // leave those at whatever narrow width they last resized to, which
    // changes the integer scale factor and hence the link elasticities.
    com.springie.render.Coords.x_pixels = 800;
    com.springie.render.Coords.y_pixels = 600;
    com.springie.render.Coords.z_pixels = 1024;
    new DataInput(manager).loadFile("resource://models/cube.spr");

    // Belt-and-braces: explicitly establish the universe state the
    // 4000-tick run needs. DataInput.resetUniverseState() normally does
    // this, but only when FrEnd.merge is false; earlier GUI tests leak
    // global state, so pin everything here.
    World.gravity_strength = 0; // as specified by cube.spr
    World.gravity_active = false; // as specified by cube.spr
    World.global_temperature = 6;
    World.ground_friction = 0;
    World.minimum_magnitude = 0;
    Node.max_speed = Integer.MAX_VALUE;
    Node.viscocity = 0;
    FrEnd.three_d = true;
    FrEnd.check_collisions = true;
    FrEnd.continuously_centre = false;
    FrEnd.boundaries = true;
    FrEnd.explosions = true;
    FrEnd.oscd = true;
    FrEnd.dragged_element = null;
    FrEnd.forces_disabled_during_gesture = false;
    com.springie.muscles.Muscles.enabled = false;
    manager.electrostatic.charge_active = false; // as specified by cube.spr

    assertEquals(12, manager.element.size(), "cube: 12 nodes");
    final LinkManager links = manager.getLinkManager();
    assertEquals(30, links.element.size(), "cube: 6 struts + 24 cables");

    int struts = 0;
    int cables = 0;
    for (int i = 0; i < links.element.size(); i++) {
      final Link link = (Link) links.element.get(i);
      if (!link.type.tension) {
        struts++;
      }
      if (!link.type.compression) {
        cables++;
      }
    }
    assertEquals(6, struts, "cube: 6 compression-only struts");
    assertEquals(24, cables, "cube: 24 tension-only cables");

    // no two struts share a node (they must not touch or join)
    final java.util.Set<Node> strutNodes = new java.util.HashSet<>();
    for (int i = 0; i < links.element.size(); i++) {
      final Link link = (Link) links.element.get(i);
      if (!link.type.tension) {
        assertTrue(strutNodes.add(link.nodes[0]), "strut endpoints are unique");
        assertTrue(strutNodes.add(link.nodes[1]), "strut endpoints are unique");
      }
    }

    // the loader scales models to fit the screen; measure stability
    // relative to the post-load lengths
    final double[] initial = new double[links.element.size()];
    for (int i = 0; i < initial.length; i++) {
      initial[i] = linkLength((Link) links.element.get(i));
    }

    FrEnd.paused = false;
    for (int step = 0; step < 4000; step++) {
      manager.nodeAndLinkUpdate();
    }

    double strutMin = Double.MAX_VALUE;
    double strutMax = 0;
    for (int i = 0; i < links.element.size(); i++) {
      final Link link = (Link) links.element.get(i);
      final double length = linkLength(link);
      final double ratio = length / initial[i];
      assertTrue(ratio > 0.95 && ratio < 1.05,
          "link " + i + " holds its length under dynamics");
      if (!link.type.tension) {
        strutMin = Math.min(strutMin, length);
        strutMax = Math.max(strutMax, length);
      }
    }
    assertTrue((strutMax - strutMin) / strutMin < 0.05,
        "the six struts stay equal in length");
  }
}
