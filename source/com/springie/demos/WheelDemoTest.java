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
import com.springie.world.World;

/**
 * The wheel demo must build a twin-rim rolling wheel: 12 nodes per rim,
 * one hub, 24 rim-ring links, 12 inter-rim struts, 24 muscle spokes
 * (25 nodes, 60 links). The spokes carry angle-derived oscillator phases
 * forming a travelling contraction wave.
 */
class WheelDemoTest {

  private boolean old_muscles_enabled;
  private boolean old_gravity_active;
  private int old_friction;
  private int old_temperature;
  private int old_amplitude;
  private int old_period;
  private int old_direction;

  @BeforeEach
  void setUp() {
    old_muscles_enabled = Muscles.enabled;
    old_gravity_active = World.gravity_active;
    old_friction = World.ground_friction;
    old_temperature = World.global_temperature;
    old_amplitude = WheelDemo.muscle_amplitude_pct;
    old_period = WheelDemo.muscle_period_ticks;
    old_direction = WheelDemo.phase_direction;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = old_muscles_enabled;
    World.gravity_active = old_gravity_active;
    World.ground_friction = old_friction;
    World.global_temperature = old_temperature;
    WheelDemo.muscle_amplitude_pct = old_amplitude;
    WheelDemo.muscle_period_ticks = old_period;
    WheelDemo.phase_direction = old_direction;
    // Leave the world's RNG in its initial state so later tests
    // (e.g. CubeTensegrityTest) see the same sequence as a fresh JVM.
    resetWorldRandom();
  }

  private static void resetWorldRandom() {
    try {
      final java.lang.reflect.Field field =
          World.class.getDeclaredField("rnd");
      field.setAccessible(true);
      final com.springie.utilities.random.Hortensius32Fast rnd =
          (com.springie.utilities.random.Hortensius32Fast) field.get(null);
      rnd.setSeed(4357);
    } catch (Exception e) {
      throw new RuntimeException("Failed to reset World.rnd", e);
    }
  }

  @Test
  void buildsTwentyFiveNodesSixtyLinks() {
    final Node hub = WheelDemo.buildAt(120);
    assertNotNull(hub);

    final NodeManager nm = ContextManager.getNodeManager();
    // 12 nodes per rim × 2 rims + 1 hub = 25 nodes.
    assertEquals(25, nm.element.size());

    final LinkManager lm = nm.getLinkManager();
    // 24 rim-ring + 12 struts + 24 spokes = 60 links.
    assertEquals(60, lm.element.size());
  }

  @Test
  void spokesAreMusclesWithControllers() {
    WheelDemo.buildAt(120);
    final LinkManager lm =
        ContextManager.getNodeManager().getLinkManager();

    int muscle_count = 0;
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        muscle_count++;
      }
    }
    // 24 hub-to-rim spokes.
    assertEquals(24, muscle_count);
  }

  @Test
  void spokePhasesCorrespondToRimAngles() {
    WheelDemo.buildAt(120);
    final LinkManager lm =
        ContextManager.getNodeManager().getLinkManager();

    // Collect spoke phases (muscle links only).
    final java.util.List<Integer> phases = new java.util.ArrayList<>();
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        phases.add(link.phase);
      }
    }
    assertEquals(24, phases.size());

    // Phases must span a full wave around the rim: with 12 rim angles
    // and period P, we expect phases at multiples of P/12 (each angle
    // appears twice, once per rim). Check the set covers the range.
    final int period = WheelDemo.muscle_period_ticks;
    final java.util.Set<Integer> unique =
        new java.util.HashSet<>(phases);
    // 12 distinct phases (one per rim angle).
    assertEquals(12, unique.size());

    // Verify the phase for rim angle 0 is 0, and phases increase
    // monotonically with angle (direction -1 gives decreasing, but the
    // absolute values must match angle * period / 2PI).
    for (int i = 0; i < WheelDemo.RIM_COUNT; i++) {
      final int expected = (int) (WheelDemo.phase_direction
          * (2.0 * Math.PI * i / WheelDemo.RIM_COUNT)
          * period / (2.0 * Math.PI));
      assertTrue(unique.contains(expected),
          "Missing phase " + expected + " for rim angle index " + i);
    }
  }

  @Test
  void judgeIsDeterministic() {
    final RollingJudge.Result r1 = RollingJudge.score(600, false);
    final RollingJudge.Result r2 = RollingJudge.score(600, false);
    assertEquals(r1.distance_px, r2.distance_px);
    assertEquals(r1.theta_total, r2.theta_total, 1e-9);
    assertEquals(r1.rolling_match, r2.rolling_match, 1e-9);
    assertEquals(r1.score, r2.score, 1e-9);
  }

  @Test
  void remainsNumericallyStableFor120Ticks() {
    final Node hub = WheelDemo.buildAt(120);
    final NodeManager nm = ContextManager.getNodeManager();

    for (int t = 0; t < 120; t++) {
      nm.nodeAndLinkUpdate();
    }

    // No NaN or infinite positions; hub stays in the universe.
    assertTrue(Double.isFinite(hub.pos.x));
    assertTrue(Double.isFinite(hub.pos.y));
    assertTrue(Double.isFinite(hub.pos.z));
    final int n = nm.element.size();
    for (int i = 0; i < n; i++) {
      final Node node = (Node) nm.element.get(i);
      assertTrue(Double.isFinite(node.pos.x), "Node " + i + " x is not finite");
      assertTrue(Double.isFinite(node.pos.y), "Node " + i + " y is not finite");
      assertTrue(Double.isFinite(node.pos.z), "Node " + i + " z is not finite");
    }
  }

  @Test
  void rollingMatchExceedsThreshold() {
    final RollingJudge.Result result = RollingJudge.score(600, false);
    // The wheel must actually roll, not slide or hop.
    assertTrue(result.rolling_match > 0.8,
        "rollingMatch=" + result.rolling_match + " (expected > 0.8)");
    // And it must travel a meaningful distance.
    assertTrue(result.distance_px > 100,
        "distance=" + result.distance_px + " (expected > 100px)");
  }

  @Test
  void enablesGravityAndFriction() {
    WheelDemo.buildAt(120);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
    assertTrue(Muscles.enabled);
  }
}
