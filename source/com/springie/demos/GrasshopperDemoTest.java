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
import com.springie.messages.commands.GrasshopperDemoMessage;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;

/**
 * The grasshopper demo must build a tetrahedral jumper (body of two
 * face-sharing tetrahedra, four edge-joined tetrahedral legs) whose
 * hip-foot extensor muscles launch it straight up, and the judge must
 * score the jump deterministically.
 */
class GrasshopperDemoTest {

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
  void buildsATetrahedralJumperWithEightExtensors() {
    GrasshopperDemo.buildAt(400);

    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    // 6 body nodes + 2 nodes per leg x 4 legs.
    assertEquals(14, node_manager.element.size(),
        "grasshopper must have 14 nodes");
    // 11 body edges + 5 edges per leg x 4 legs.
    assertEquals(31, link_manager.element.size(),
        "grasshopper must have 31 links");

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
          "all extensors must fire in unison for a level launch");
    }
    // Two hip-foot extensors per leg, four legs.
    assertEquals(8, muscles, "grasshopper must have 8 extensor muscles");

    assertTrue(Muscles.enabled, "the demo must enable muscles");
  }

  @Test
  void buildPlacementIsClean() {
    GrasshopperDemo.buildAt(400);

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
    assertEquals(4, GrasshopperDemo.feet.length);
    for (int f = 0; f < 4; f++) {
      final int clearance = (ground_wall - GrasshopperDemo.feet[f].pos.y) >> Coords.shift;
      assertTrue(clearance >= 0 && clearance <= 4,
          "foot " + f + " must start on the ground, clearance was " + clearance + "px");
    }

    // The marker is the crown: the topmost node of the build.
    int min_y = Integer.MAX_VALUE;
    for (int i = 0; i < n; i++) {
      min_y = Math.min(min_y, ((Node) node_manager.element.get(i)).pos.y);
    }
    assertEquals(min_y, GrasshopperDemo.marker.pos.y,
        "the marker must be the topmost node");
  }

  @Test
  void demoIsRegisteredInTheCatalog() {
    final DemoCatalog.Demo demo = DemoCatalog.forLabel("Demo: Grasshopper");
    assertTrue(demo != null, "grasshopper must be in the DemoCatalog");
    assertTrue(demo.newMessage() instanceof GrasshopperDemoMessage,
        "the catalog entry must launch via GrasshopperDemoMessage");
  }

  /**
   * The judge must be deterministic: two runs with the same parameters
   * must produce identical scores, even in a full-suite JVM.
   */
  @Test
  void judgeIsDeterministic() {
    final GrasshopperJudge.Result r1 = GrasshopperJudge.score(600);
    final GrasshopperJudge.Result r2 = GrasshopperJudge.score(600);
    assertEquals(r1.height_px, r2.height_px);
    assertEquals(r1.airborne, r2.airborne);
    assertEquals(r1.disqualified, r2.disqualified);
    assertEquals(r1.max_speed_px, r2.max_speed_px);
    assertEquals(r1.score, r2.score);
  }

  /**
   * The grasshopper must actually jump: the crown marker must rise well
   * above its start (measured 346px on 2026-09-16; the bar sits well
   * below that as a regression guard).
   */
  @Test
  void jumpQualityBar() {
    final GrasshopperJudge.Result r = GrasshopperJudge.score(600);
    assertTrue(r.airborne, "every foot must leave the ground");
    assertTrue(!r.disqualified,
        "the jump must not disqualify (speed/strain blow-up)");
    assertTrue(r.score >= 250,
        "crown must rise at least 250px, got " + r.score + "px");
  }
}
