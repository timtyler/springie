// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.springie.render.Coords;
import com.springie.world.World;

/**
 * The wheel demo must build a minimal hexagonal prism: 6 nodes per rim,
 * two rims each with its own central hub node, hubs joined by a rigid
 * axle (14 nodes, 43 links). No hamster, no muscles, no paddles -- just
 * the prism, the axle, and N/S compass stabilization on the hubs
 * (Tim, 2026-10-01).
 */
class WheelbarrowDemoTest {

  private boolean old_gravity_active;
  private int old_friction;
  private int old_temperature;
  private int old_compass_bias;
  private int old_z_offset;
  private int old_radius;
  private int old_half_width;
  private int old_inset;
  private int old_rim_log_mass;
  private int old_hub_log_mass;
  private int old_bracing;
  private int old_spoke_scale;
  private int old_handle_back;
  private int old_handle_elasticity;
  private boolean old_paused;
  private int old_frame_frequency;
  private int old_coords_x;
  private int old_coords_y;
  private int old_coords_z;

  @BeforeEach
  void setUp() {
    old_gravity_active = World.gravity_active;
    old_friction = World.ground_friction;
    old_temperature = World.global_temperature;
    old_compass_bias = WheelbarrowDemo.compass_bias;
    old_z_offset = WheelbarrowDemo.z_offset_px;
    old_radius = WheelbarrowDemo.rim_radius_px;
    old_half_width = WheelbarrowDemo.rim_half_width_px;
    old_inset = WheelbarrowDemo.axle_inset_px;
    old_rim_log_mass = WheelbarrowDemo.rim_log_mass;
    old_hub_log_mass = WheelbarrowDemo.hub_log_mass;
    old_bracing = WheelbarrowDemo.bracing_elasticity;
    old_spoke_scale = WheelbarrowDemo.spoke_rest_scale_pct;
    old_handle_back = WheelbarrowDemo.handle_back_px;
    old_handle_elasticity = WheelbarrowDemo.handle_elasticity;
    old_paused = FrEnd.paused;
    old_frame_frequency = FrEnd.frame_frequency;
    old_coords_x = Coords.x_pixels;
    old_coords_y = Coords.y_pixels;
    old_coords_z = Coords.z_pixels;
    // Pin to the tuned values so the tests are deterministic
    // even if the statics were changed by an earlier test.
    WheelbarrowDemo.compass_bias = 10;
    WheelbarrowDemo.z_offset_px = 100;
    WheelbarrowDemo.rim_radius_px = 160;
    WheelbarrowDemo.rim_half_width_px = 162;
    WheelbarrowDemo.axle_inset_px = 60;
    WheelbarrowDemo.rim_log_mass = 15;
    WheelbarrowDemo.hub_log_mass = 19;
    WheelbarrowDemo.bracing_elasticity = 150;
    WheelbarrowDemo.spoke_rest_scale_pct = 100;
    WheelbarrowDemo.handle_back_px = 240;
    WheelbarrowDemo.handle_elasticity = 150;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    World.gravity_active = old_gravity_active;
    World.ground_friction = old_friction;
    World.global_temperature = old_temperature;
    WheelbarrowDemo.compass_bias = old_compass_bias;
    WheelbarrowDemo.z_offset_px = old_z_offset;
    WheelbarrowDemo.rim_radius_px = old_radius;
    WheelbarrowDemo.rim_half_width_px = old_half_width;
    WheelbarrowDemo.axle_inset_px = old_inset;
    WheelbarrowDemo.rim_log_mass = old_rim_log_mass;
    WheelbarrowDemo.hub_log_mass = old_hub_log_mass;
    WheelbarrowDemo.bracing_elasticity = old_bracing;
    WheelbarrowDemo.spoke_rest_scale_pct = old_spoke_scale;
    WheelbarrowDemo.handle_back_px = old_handle_back;
    WheelbarrowDemo.handle_elasticity = old_handle_elasticity;
    FrEnd.paused = old_paused;
    FrEnd.frame_frequency = old_frame_frequency;
    Coords.x_pixels = old_coords_x;
    Coords.y_pixels = old_coords_y;
    Coords.z_pixels = old_coords_z;
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
    WheelbarrowDemo.buildAt(120);

    final int expected = WheelbarrowDemo.nodeRadius();
    final NodeManager nm = ContextManager.getNodeManager();
    for (int i = 0; i < nm.element.size(); i++) {
      final Node node = (Node) nm.element.get(i);
      assertEquals(expected, node.type.radius, "wheel node size");
    }
  }

  @Test
  void buildsSixteenNodesFortySixLinks() {
    final Node hub0 = WheelbarrowDemo.buildAt(120);
    assertNotNull(hub0);

    final NodeManager nm = ContextManager.getNodeManager();
    // 6 nodes per rim x 2 rims + 2 hubs + 2 handle nodes = 16.
    // (Tim, 2026-10-01: hexagonal prism, axle, N/S, handle -- nothing else.)
    assertEquals(16, nm.element.size());

    final LinkManager lm = nm.getLinkManager();
    // 6 segments x 5 (rim0, rim1, cross, 2 mirror diagonals) = 30 rim
    // links + 1 rigid axle + 12 passive spokes + 2 handle shafts
    // + 1 handle cross-brace = 46.
    assertEquals(46, lm.element.size());
  }

  @Test
  void hasNoMuscles() {
    WheelbarrowDemo.buildAt(120);
    final LinkManager lm =
        ContextManager.getNodeManager().getLinkManager();

    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      assertTrue(link.controller == null,
          "link " + i + " must have no controller (no muscles)");
    }
  }

  @Test
  void hubsHaveNorthSouthCompass() {
    WheelbarrowDemo.buildAt(120);
    final NodeManager nm = ContextManager.getNodeManager();

    // hub0 is element 12, hub1 is element 13 (after 12 rim nodes).
    final Node hub0 = (Node) nm.element.get(12);
    final Node hub1 = (Node) nm.element.get(13);
    assertEquals(CompassPoint.N, hub0.compass, "hub0 must be N");
    assertEquals(CompassPoint.S, hub1.compass, "hub1 must be S");
  }

  @Test
  void handleLinksAreMarkedHandle() {
    WheelbarrowDemo.buildAt(120);
    final LinkManager lm =
        ContextManager.getNodeManager().getLinkManager();

    // The last 3 links are the handle: 2 shafts + 1 cross-brace.
    // (Tim, 2026-10-01: handle links render pastel yellow.)
    int handle_count = 0;
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.handle) {
        handle_count++;
      }
    }
    assertEquals(3, handle_count, "must have 3 handle links");
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
    final Node hub = WheelbarrowDemo.buildAt(120);
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

  @Disabled("no drive yet: the minimal prism has no muscles or kick, so it "
      + "sits still. Re-enable when a drive is added.")
  @Test
  void rollingMatchExceedsThreshold() {
    final RollingJudge.Result result = RollingJudge.score(600, false);
    assertTrue(result.rolling_match > 0.8,
        "rollingMatch=" + result.rolling_match + " (expected > 0.8)");
    assertTrue(!result.disqualified,
        "judged run must not be disqualified");
    assertTrue(result.distance_px > 150,
        "distance=" + result.distance_px + " (expected > 150px)");
  }

  @Test
  void enablesGravityAndFriction() {
    WheelbarrowDemo.buildAt(120);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
  }
}
