// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import com.springie.world.World;

/**
 * The wheel demo must build a big, clean rolling wheel: 7 nodes per rim,
 * two rims each with its own central hub node (two hubs joined by an
 * axle), 35 rim links (rim edges, cross links, diagonal bracing), 1 axle
 * link, 14 muscle cable spokes (16 nodes, 50 links). The spokes are the
 * only muscles, all sharing one GlobalOscillatorController with identical
 * phase -- no subset special treatment (Tim, 2026-09-28 redesign: hamster
 * deleted, its mass moved into the axle, its muscles moved onto the
 * spokes).
 */
class HamsterWheelDemoTest {

  private boolean old_muscles_enabled;
  private boolean old_gravity_active;
  private int old_friction;
  private int old_temperature;
  private int old_direction;
  private int old_stabilizer_bias;
  private int old_z_offset;
  private int old_radius;
  private int old_half_width;
  private int old_inset;
  private int old_rim_log_mass;
  private int old_hub_log_mass;
  private int old_bracing;
  private int old_spoke_scale;
  private boolean old_paused;
  private int old_frame_frequency;
  private int old_coords_x;
  private int old_coords_y;
  private int old_coords_z;

  @BeforeEach
  void setUp() {
    old_muscles_enabled = Muscles.enabled;
    old_gravity_active = World.gravity_active;
    old_friction = World.ground_friction;
    old_temperature = World.global_temperature;
    old_direction = HamsterWheelDemo.roll_direction;
    old_stabilizer_bias = HamsterWheelDemo.axle_stabilizer_bias;
    old_z_offset = HamsterWheelDemo.z_offset_px;
    old_radius = HamsterWheelDemo.rim_radius_px;
    old_half_width = HamsterWheelDemo.rim_half_width_px;
    old_inset = HamsterWheelDemo.axle_inset_px;
    old_rim_log_mass = HamsterWheelDemo.rim_log_mass;
    old_hub_log_mass = HamsterWheelDemo.hub_log_mass;
    old_bracing = HamsterWheelDemo.bracing_elasticity;
    old_spoke_scale = HamsterWheelDemo.spoke_rest_scale_pct;
    old_paused = FrEnd.paused;
    old_frame_frequency = FrEnd.frame_frequency;
    old_coords_x = Coords.x_pixels;
    old_coords_y = Coords.y_pixels;
    old_coords_z = Coords.z_pixels;
    // Pin the drive to the tuned values so the tests are deterministic
    // even if the statics were changed by an earlier test.
    HamsterWheelDemo.roll_direction = 1;
    HamsterWheelDemo.axle_stabilizer_bias = 13;
    HamsterWheelDemo.z_offset_px = 100;
    HamsterWheelDemo.rim_radius_px = 160;
    HamsterWheelDemo.rim_half_width_px = 162;
    HamsterWheelDemo.axle_inset_px = 60;
    HamsterWheelDemo.rim_log_mass = 17;
    HamsterWheelDemo.hub_log_mass = 20;
    HamsterWheelDemo.bracing_elasticity = 30;
    HamsterWheelDemo.spoke_rest_scale_pct = 95;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = old_muscles_enabled;
    World.gravity_active = old_gravity_active;
    World.ground_friction = old_friction;
    World.global_temperature = old_temperature;
    HamsterWheelDemo.roll_direction = old_direction;
    HamsterWheelDemo.axle_stabilizer_bias = old_stabilizer_bias;
    HamsterWheelDemo.z_offset_px = old_z_offset;
    HamsterWheelDemo.rim_radius_px = old_radius;
    HamsterWheelDemo.rim_half_width_px = old_half_width;
    HamsterWheelDemo.axle_inset_px = old_inset;
    HamsterWheelDemo.rim_log_mass = old_rim_log_mass;
    HamsterWheelDemo.hub_log_mass = old_hub_log_mass;
    HamsterWheelDemo.bracing_elasticity = old_bracing;
    HamsterWheelDemo.spoke_rest_scale_pct = old_spoke_scale;
    FrEnd.paused = old_paused;
    FrEnd.frame_frequency = old_frame_frequency;
    Coords.x_pixels = old_coords_x;
    Coords.y_pixels = old_coords_y;
    Coords.z_pixels = old_coords_z;
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
  void wheelNodesMatchLinkRadius() {
    HamsterWheelDemo.buildAt(120);

    final int expected = HamsterWheelDemo.nodeRadius();
    final NodeManager nm = ContextManager.getNodeManager();
    for (int i = 0; i < nm.element.size(); i++) {
      final Node node = (Node) nm.element.get(i);
      assertEquals(expected, node.type.radius, "wheel node size");
    }
  }

  @Test
  void buildsSixteenNodesFiftyLinks() {
    final Node hub0 = HamsterWheelDemo.buildAt(120);
    assertNotNull(hub0);

    final NodeManager nm = ContextManager.getNodeManager();
    // 7 nodes per rim x 2 rims + 2 hubs (one per rim) = 16 (no hamster).
    assertEquals(16, nm.element.size());

    final LinkManager lm = nm.getLinkManager();
    // 7 segments x 5 (rim0, rim1, cross, 2 mirror diagonals) = 35 rim
    // links + 1 axle (hub0-hub1) + 14 muscle spokes = 50 links.
    assertEquals(50, lm.element.size());
  }

  @Test
  void spokeMusclesAreTheOnlyMuscles() {
    HamsterWheelDemo.buildAt(120);
    final LinkManager lm =
        ContextManager.getNodeManager().getLinkManager();

    int muscle_count = 0;
    GlobalOscillatorController first = null;
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        muscle_count++;
        // The spokes are cables (tension-only): they haul, never push.
        assertFalse(link.type.compression,
            "spoke muscle must be a cable (tension-only)");
        // All spokes share the identical controller instance -- no
        // subset special treatment.
        if (first == null) {
          first = (GlobalOscillatorController) link.controller;
        } else {
          assertTrue(link.controller == first,
              "all spoke muscles must share one controller");
        }
      }
    }
    // 14 hub-to-rim spoke muscles (the only muscles).
    assertEquals(14, muscle_count);
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
    final Node hub = HamsterWheelDemo.buildAt(120);
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

  @Disabled("re-enable when the wheel retune lands: spoke-muscle drive needs judging")
  @Test
  void rollingMatchExceedsThreshold() {
    final RollingJudge.Result result = RollingJudge.score(600, false);
    // The wheel must actually roll, not slide or hop.
    assertTrue(result.rolling_match > 0.8,
        "rollingMatch=" + result.rolling_match + " (expected > 0.8)");
    assertTrue(!result.disqualified,
        "judged run must not be disqualified");
    assertTrue(result.distance_px > 150,
        "distance=" + result.distance_px + " (expected > 150px)");
  }

  @Test
  void staysUprightWhileRolling() {
    final RollingJudge.Result result = RollingJudge.score(600, false);
    // Tim's requirement: keep it standing on its end (axis horizontal).
    // The fat wheel must stay upright for the vast majority of the run.
    assertTrue(result.upright_fraction > 0.8,
        "uprightFraction=" + result.upright_fraction + " (expected > 0.8)");
    // And it must roll straight, not veer sideways.
    assertTrue(Math.abs(result.z_drift_px) < 50,
        "zDrift=" + result.z_drift_px + " (expected < 50px)");
  }

  @Test
  void enablesGravityAndFriction() {
    HamsterWheelDemo.buildAt(120);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
    assertTrue(Muscles.enabled);
  }
}
