// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import com.springie.world.World;

/**
 * The wheel demo must build a short, fat tetrahedral rolling wheel:
 * 8 nodes per rim, one hub, 32 rim links (rim edges, cross links,
 * diagonal bracing), 16 muscle spokes (17 nodes, 48 links). The spokes
 * carry ground-contact push-off reflex controllers (self-synchronizing).
 */
class WheelDemoTest {

  private boolean old_muscles_enabled;
  private boolean old_gravity_active;
  private int old_friction;
  private int old_temperature;
  private boolean old_reflex;
  private int old_push;
  private int old_pull;
  private int old_direction;
  private boolean old_paused;
  private int old_frame_frequency;
  private int old_active_oscillator;
  private int old_coords_x;
  private int old_coords_y;
  private int old_coords_z;

  @BeforeEach
  void setUp() {
    old_muscles_enabled = Muscles.enabled;
    old_gravity_active = World.gravity_active;
    old_friction = World.ground_friction;
    old_temperature = World.global_temperature;
    old_reflex = WheelDemo.use_reflex_drive;
    old_push = WheelDemo.reflex_push_pct;
    old_pull = WheelDemo.reflex_pull_pct;
    old_direction = WheelDemo.roll_direction;
    old_paused = FrEnd.paused;
    old_frame_frequency = FrEnd.frame_frequency;
    old_active_oscillator = Muscles.active_oscillator;
    old_coords_x = Coords.x_pixels;
    old_coords_y = Coords.y_pixels;
    old_coords_z = Coords.z_pixels;
    // Pin the reflex to the tuned values so the tests are deterministic
    // even if the statics were changed by an earlier test.
    WheelDemo.use_reflex_drive = true;
    WheelDemo.reflex_push_pct = 20;
    WheelDemo.reflex_pull_pct = 5;
    WheelDemo.roll_direction = 1;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = old_muscles_enabled;
    World.gravity_active = old_gravity_active;
    World.ground_friction = old_friction;
    World.global_temperature = old_temperature;
    WheelDemo.use_reflex_drive = old_reflex;
    WheelDemo.reflex_push_pct = old_push;
    WheelDemo.reflex_pull_pct = old_pull;
    WheelDemo.roll_direction = old_direction;
    FrEnd.paused = old_paused;
    FrEnd.frame_frequency = old_frame_frequency;
    Muscles.active_oscillator = old_active_oscillator;
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
  void buildsSeventeenNodesFortyEightLinks() {
    final Node hub = WheelDemo.buildAt(120);
    assertNotNull(hub);

    final NodeManager nm = ContextManager.getNodeManager();
    // 8 nodes per rim x 2 rims + 1 hub = 17 nodes.
    assertEquals(17, nm.element.size());

    final LinkManager lm = nm.getLinkManager();
    // 8 segments x 4 (rim0, rim1, cross, diagonal) = 32 rim links
    // + 16 hub spokes = 48 links.
    assertEquals(48, lm.element.size());
  }

  @Test
  void spokesAreMusclesWithReflexControllers() {
    WheelDemo.buildAt(120);
    final LinkManager lm =
        ContextManager.getNodeManager().getLinkManager();

    int muscle_count = 0;
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof WheelPushController) {
        muscle_count++;
      }
    }
    // 16 hub-to-rim spokes (the only muscles).
    assertEquals(16, muscle_count);
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
    WheelDemo.buildAt(120);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
    assertTrue(Muscles.enabled);
  }
}
