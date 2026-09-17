// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.messages.commands.HopperDemoMessage;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;

/**
 * The hopper demo must build a tetrahedral pogo creature (body of two
 * face-sharing tetrahedra, four edge-joined tetrahedral legs) whose
 * hip-foot extensor muscles drive a sustained hopping rhythm, and the
 * judge must score the rhythm deterministically.
 */
class HopperDemoTest {

  private boolean old_enabled;
  private int old_active;
  private int old_amplitude;
  private int old_period;
  private int old_phase;
  private boolean old_paused;
  private int old_frame_frequency;
  private int old_coords_x;
  private int old_coords_y;
  private int old_coords_z;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    ContextManager.setNodeManager(new NodeManager());

    this.old_enabled = Muscles.enabled;
    this.old_active = Muscles.active_oscillator;
    final var active = Muscles.activeOscillator();
    this.old_amplitude = active.getAmplitude();
    this.old_period = active.getPeriodTicks();
    this.old_phase = active.getPhase();
    this.old_paused = FrEnd.paused;
    this.old_frame_frequency = FrEnd.frame_frequency;
    this.old_coords_x = Coords.x_pixels;
    this.old_coords_y = Coords.y_pixels;
    this.old_coords_z = Coords.z_pixels;
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_enabled;
    Muscles.active_oscillator = this.old_active;
    final var active = Muscles.activeOscillator();
    active.setAmplitude(this.old_amplitude);
    active.setPeriodTicks(this.old_period);
    active.setPhase(this.old_phase);
    FrEnd.paused = this.old_paused;
    FrEnd.frame_frequency = this.old_frame_frequency;
    Coords.x_pixels = this.old_coords_x;
    Coords.y_pixels = this.old_coords_y;
    Coords.z_pixels = this.old_coords_z;
  }

  @Test
  void buildsATetrahedralHopperWithEightExtensors() {
    HopperDemo.buildAt(400);

    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    // 6 body nodes + 2 nodes per leg x 4 legs.
    assertEquals(14, node_manager.element.size(),
        "hopper must have 14 nodes");
    // 12 body edges + 5 edges per leg x 4 legs.
    assertEquals(32, link_manager.element.size(),
        "hopper must have 32 links");

    int muscles = 0;
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller == null) {
        continue;
      }
      muscles++;
      assertTrue(link.controller instanceof GlobalOscillatorController,
          "link " + i + " must follow the global oscillator");
      assertEquals(0, link.phase,
          "all extensors must fire in unison for level hops");
    }
    // Two hip-foot extensors per leg, four legs.
    assertEquals(8, muscles, "hopper must have 8 extensor muscles");

    assertTrue(Muscles.enabled, "the demo must enable muscles");
  }

  @Test
  void buildPlacementIsClean() {
    HopperDemo.buildAt(400);

    final NodeManager node_manager = ContextManager.getNodeManager();
    final int n = node_manager.element.size();
    final int ground_wall = Coords.y_pixels << Coords.shift;

    // No node at or below the z = 0 wall (the z-wall crush lesson).
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      assertTrue(node.pos.z > 0,
          "node " + i + " starts at/below the z=0 wall");
    }

    // All four feet start planted just above the ground wall.
    assertEquals(4, HopperDemo.feet.length);
    for (int f = 0; f < 4; f++) {
      final int clearance = (ground_wall - HopperDemo.feet[f].pos.y) >> Coords.shift;
      assertTrue(clearance >= 0 && clearance <= 4,
          "foot " + f + " must start on the ground, clearance was " + clearance + "px");
    }

    // The marker is the crown: the topmost node of the build.
    int min_y = Integer.MAX_VALUE;
    for (int i = 0; i < n; i++) {
      min_y = Math.min(min_y, ((Node) node_manager.element.get(i)).pos.y);
    }
    assertEquals(min_y, HopperDemo.marker.pos.y,
        "the marker must be the topmost node");
  }

  @Test
  void demoIsRegisteredInTheCatalog() {
    final DemoCatalog.Demo demo = DemoCatalog.forLabel("Demo: Hopper");
    assertTrue(demo != null, "hopper must be in the DemoCatalog");
    assertTrue(demo.newMessage() instanceof HopperDemoMessage,
        "the catalog entry must launch via HopperDemoMessage");
  }

  /**
   * The judge must be deterministic: two runs with the same parameters
   * must produce identical scores, even in a full-suite JVM.
   */
  @Test
  void judgeIsDeterministic() {
    final HopperJudge.Result r1 = HopperJudge.score(600);
    final HopperJudge.Result r2 = HopperJudge.score(600);
    assertEquals(r1.air_ticks, r2.air_ticks);
    assertEquals(r1.hops, r2.hops);
    assertEquals(r1.max_streak, r2.max_streak);
    assertEquals(r1.max_height_px, r2.max_height_px);
    assertEquals(r1.max_speed_px, r2.max_speed_px);
    assertEquals(r1.disqualified, r2.disqualified);
    assertEquals(r1.score, r2.score, 1e-9);
  }

  /**
   * The hopper must actually hop: mostly airborne, several consecutive
   * feet-first hops. Measured on 2026-09-16: air 0.935, 7 hops, streak
   * 7, score 2.5713 -- the bars sit well below that as regression
   * guards.
   */
  @Test
  void hopQualityBar() {
    final HopperJudge.Result r = HopperJudge.score(600);
    assertTrue(!r.disqualified,
        "the hopping must not disqualify (speed/strain blow-up)");
    assertTrue(r.air_fraction >= 0.85,
        "hopper must be airborne most of the time, got " + r.air_fraction);
    assertTrue(r.hops >= 5,
        "hopper must hop repeatedly, got " + r.hops + " hops");
    // Cable-driven (muscles on tension-only cables per design rules):
    // peaks at streak 4 / score 1.76. The 7-hop streak needed strut
    // muscles, which the rules forbid.
    assertTrue(r.max_streak >= 4,
        "hopper must chain feet-first hops, got streak " + r.max_streak);
    assertTrue(r.score >= 1.7,
        "hopper score must clear 1.7, got " + r.score);
    assertTrue(r.max_passive_strain < 0.3,
        "the truss must hold its shape: max passive strain " + r.max_passive_strain);
  }
}
