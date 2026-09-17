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
import com.springie.muscles.Muscles;
import com.springie.render.Coords;

/**
 * The slinky demo must build three octagonal wheels side by side (8 rim
 * nodes + 1 hub per turn) with hub-to-rim spoke muscles driven by the
 * wheel's push-off reflex, and the judge must see it take discrete
 * forward steps in a straight line, deterministically.
 */
class SlinkyDemoTest {

  private boolean old_enabled;
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
    this.old_paused = FrEnd.paused;
    this.old_frame_frequency = FrEnd.frame_frequency;
    this.old_coords_x = Coords.x_pixels;
    this.old_coords_y = Coords.y_pixels;
    this.old_coords_z = Coords.z_pixels;
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_enabled;
    FrEnd.paused = this.old_paused;
    FrEnd.frame_frequency = this.old_frame_frequency;
    Coords.x_pixels = this.old_coords_x;
    Coords.y_pixels = this.old_coords_y;
    Coords.z_pixels = this.old_coords_z;
  }

  @Test
  void buildsThreeWheelsWithSpokeMuscles() {
    SlinkyDemo.build();

    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    // 3 turns x (8 rim + 1 hub).
    assertEquals(27, node_manager.element.size(),
        "slinky must have 27 nodes");

    // 24 rim edges + 16 axial links + 24 spokes.
    assertEquals(64, link_manager.element.size(),
        "slinky must have 64 links");

    int muscles = 0;
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller != null) {
        muscles++;
        assertTrue(link.controller instanceof WheelPushController,
            "link " + i + " must use the wheel push-off reflex");
      }
    }

    // 8 spokes per turn x 3 turns.
    assertEquals(24, muscles, "slinky must have 24 spoke muscles");

    // No node at or behind the z=0 wall.
    final int shift = Coords.shift;
    for (int i = 0; i < SlinkyDemo.coil_nodes.length; i++) {
      final Node nd = SlinkyDemo.coil_nodes[i];
      assertTrue(nd.pos.z > 0, "node " + i + " must be off the z=0 wall");
      assertTrue(nd.pos.y < (Coords.y_pixels << shift),
          "node " + i + " must start above the ground");
    }

    assertTrue(Muscles.enabled, "the demo must enable muscles");
  }

  /**
   * The judge must be deterministic: two runs with the same parameters
   * must produce identical scores.
   */
  @Test
  void judgeIsDeterministic() {
    final SlinkyJudge.Result r1 = SlinkyJudge.score(600);
    final SlinkyJudge.Result r2 = SlinkyJudge.score(600);
    assertEquals(r1.distance_px, r2.distance_px);
    assertEquals(r1.lateral_px, r2.lateral_px);
    assertEquals(r1.steps, r2.steps);
    assertEquals(r1.score, r2.score, 1e-9);
  }

  /**
   * The slinky must step forward in a straight line (not veer, not hop
   * in place) and hold together (no DQ).
   */
  @Test
  void stepsForwardInAStraightLine() {
    final SlinkyJudge.Result r = SlinkyJudge.score(600);
    assertTrue(!r.disqualified, "slinky must not DQ (explode or over-strain)");
    assertTrue(r.distance_px > 200,
        "slinky must step forward, got " + r.distance_px + "px");
    assertTrue(r.straightness > 0.9,
        "slinky must track straight, got " + r.straightness);
    assertTrue(r.steps >= 15,
        "slinky must take at least 15 steps, got " + r.steps);
  }
}
