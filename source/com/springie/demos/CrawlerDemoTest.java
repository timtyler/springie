// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.muscles.Sensors;
import com.springie.render.Coords;
import com.springie.utilities.random.Hortensius32Fast;
import com.springie.world.World;
import com.springie.FrEnd;

/**
 * The crawler demo must build a 4-legged walker: a rigid tetrahedral body
 * with four rigid paddle legs, each leg driven by a single cable muscle
 * (ridge-to-foot lift cable), with a trot gait (diagonal legs in phase).
 * The model must hold its shape under gravity (max passive strain &lt; 0.3).
 */
class CrawlerDemoTest {

  private boolean old_muscles_enabled;
  private boolean old_gravity_active;
  private int old_friction;
  private boolean old_collisions;
  private int old_amplitude;

  @BeforeEach
  void setUp() {
    old_muscles_enabled = Muscles.enabled;
    old_gravity_active = World.gravity_active;
    old_friction = World.ground_friction;
    old_collisions = FrEnd.check_collisions;
    old_amplitude = CrawlerDemo.muscle_amplitude_pct;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = old_muscles_enabled;
    World.gravity_active = old_gravity_active;
    World.ground_friction = old_friction;
    FrEnd.check_collisions = old_collisions;
    CrawlerDemo.muscle_amplitude_pct = old_amplitude;
  }

  @Test
  void buildsFourLegsWithMuscles() {
    final Node body = CrawlerDemo.buildAt(0);
    assertNotNull(body);

    final NodeManager nm = ContextManager.getNodeManager();
    // 6 body nodes + 4 legs x 2 nodes (knee + foot) = 14 nodes.
    assertEquals(14, nm.element.size());

    final LinkManager lm = nm.getLinkManager();
    int muscle_count = 0;
    int cable_muscle_count = 0;
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        muscle_count++;
        // Muscles belong on cables (tension members), not struts.
        if (!link.type.compression) {
          cable_muscle_count++;
        }
      }
    }
    // 4 legs x 1 lift-cable muscle each.
    assertEquals(4, muscle_count);
    assertEquals(4, cable_muscle_count);
  }

  @Test
  void trotGaitHasDiagonalPairsInPhase() {
    CrawlerDemo.buildAt(0);
    final NodeManager nm = ContextManager.getNodeManager();
    final LinkManager lm = nm.getLinkManager();

    // Collect muscle phases.
    final java.util.List<Integer> phases = new java.util.ArrayList<>();
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        phases.add(link.phase);
      }
    }

    assertEquals(4, phases.size());
    // Diagonal pairs (FL+BR, FR+BL) share phases; the pairs differ.
    // FL=0, FR=60, BL=60, BR=0 for the default 120-tick period.
    assertTrue(phases.contains(0));
    assertTrue(phases.contains(60));
  }

  @Test
  void enablesGravityAndFriction() {
    CrawlerDemo.buildAt(0);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
    assertTrue(Muscles.enabled);
  }

  @Test
  void holdsShapeUnderGravity() throws Exception {
    // Deterministic headless run: the passive structure must keep
    // max strain < 0.3 over 600 ticks (Tim's "holds shape" criterion).
    synchronized (ContextManager.class) {
      final java.lang.reflect.Field field =
          World.class.getDeclaredField("rnd");
      field.setAccessible(true);
      ((Hortensius32Fast) field.get(null)).setSeed(4357);

      World.gravity_active = true;
      World.gravity_strength = 5;
      World.global_temperature = 6;
      World.ground_friction = 100;
      World.minimum_magnitude = 0;
      World.maximum_magnitude = Integer.MAX_VALUE;
      Node.max_speed = Integer.MAX_VALUE;
      Node.viscocity = 0;
      FrEnd.three_d = true;
      FrEnd.check_collisions = false;
      FrEnd.continuously_centre = false;
      FrEnd.node_growth = false;
      FrEnd.boundaries = true;
      FrEnd.explosions = false;
      FrEnd.oscd = true;
      FrEnd.dragged_element = null;
      FrEnd.forces_disabled_during_gesture = false;
      FrEnd.paused = false;
      FrEnd.frame_frequency = 0;
      Muscles.enabled = false;
      Muscles.active_oscillator = 0;
      Coords.x_pixels = 800;
      Coords.y_pixels = 600;
      Coords.z_pixels = 1024;

      CrawlerDemo.muscle_amplitude_pct = 0;
      ContextManager.setNodeManager(new NodeManager());
      final NodeManager nm = ContextManager.getNodeManager();
      CrawlerDemo.buildAt(0);
      FrEnd.check_collisions = false;

      final LinkManager lm = nm.getLinkManager();
      double max_strain = 0;
      for (int t = 0; t < 600; t++) {
        nm.nodeAndLinkUpdate();
      }
      for (int i = 0; i < lm.element.size(); i++) {
        final Link link = (Link) lm.element.get(i);
        if (link.controller == null) { // passive links only
          final double s = Math.abs(Sensors.strain(link)) / 4096.0;
          if (s > max_strain) {
            max_strain = s;
          }
        }
      }
      assertTrue(max_strain < 0.3,
          "max passive strain " + max_strain + " >= 0.3");
    }
  }
}
