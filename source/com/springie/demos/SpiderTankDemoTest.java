// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

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
 * Tests for the spider tank demo: a 4-legged walker (front and back pairs)
 * with a trot gait, built from tetrahedral blocks. Each leg is a rigid
 * paddle driven by a single cable muscle (crest-to-foot lift cable).
 * The model must hold its shape under gravity (max passive strain &lt; 0.3).
 */
public class SpiderTankDemoTest {
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
    old_amplitude = SpiderTankDemo.muscle_amplitude_pct;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = old_muscles_enabled;
    World.gravity_active = old_gravity_active;
    World.ground_friction = old_friction;
    FrEnd.check_collisions = old_collisions;
    SpiderTankDemo.muscle_amplitude_pct = old_amplitude;
  }

  @Test
  void buildsFourLegsWithMuscles() {
    SpiderTankDemo.buildAt(0);
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    // 9 body nodes (triangular tube) + 2 turret nodes
    // + 4 legs x 2 new nodes (knee, foot) = 19 nodes.
    assertEquals(19, node_manager.element.size());

    // Count muscle links (those with a controller).
    int muscles = 0;
    int cable_muscles = 0;
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        muscles++;
        // Muscles belong on cables (tension members), not struts.
        if (!link.type.compression) {
          cable_muscles++;
        }
      }
    }
    // 4 legs x 1 lift-cable muscle each.
    assertEquals(4, muscles);
    assertEquals(4, cable_muscles);
  }

  @Test
  void trotGaitHasDiagonalPairsInPhase() {
    SpiderTankDemo.buildAt(0);
    final LinkManager link_manager =
        ContextManager.getNodeManager().getLinkManager();

    final Set<Integer> phases = new HashSet<>();
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        phases.add(link.phase);
      }
    }
    // 4 muscles, trot: 2 at phase 0 (LF, RB), 2 at half-period (RF, LB).
    assertEquals(2, phases.size());
    assertTrue(phases.contains(0));
    assertTrue(phases.contains(60));
  }

  @Test
  void enablesGravityAndFriction() {
    final Node body = SpiderTankDemo.buildAt(0);
    assertTrue(Muscles.enabled);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
    // Body node exists.
    assertTrue(body != null);
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
      FrEnd.links_disabled = false;
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

      SpiderTankDemo.muscle_amplitude_pct = 0;
      ContextManager.setNodeManager(new NodeManager());
      final NodeManager nm = ContextManager.getNodeManager();
      SpiderTankDemo.buildAt(0);
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
