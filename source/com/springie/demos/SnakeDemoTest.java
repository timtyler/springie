// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;

/**
 * The snake demo must build a chain of tetrahedra (4 nodes + 3 new nodes
 * per additional segment) where every edge is a muscle, with a traveling
 * phase wave along the body.
 */
class SnakeDemoTest {

  private boolean old_enabled;
  private int old_active;
  private int old_amplitude;
  private int old_period;
  private int old_phase;

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
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_enabled;
    Muscles.active_oscillator = this.old_active;
    final var active = Muscles.activeOscillator();
    active.setAmplitude(this.old_amplitude);
    active.setPeriodTicks(this.old_period);
    active.setPhase(this.old_phase);
  }

  @Test
  void buildsATetrahedralChainOfMuscles() {
    SnakeDemo.build();

    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    // 4 nodes for the first tetrahedron, 1 new node per additional segment.
    assertEquals(SnakeDemo.SEGMENTS + 3, node_manager.element.size(),
        "snake must have SEGMENTS + 3 nodes");

    // 6 edges for the first tetrahedron, 3 new edges per additional segment
    // (the shared face's 3 edges already exist).
    assertEquals(3 * SnakeDemo.SEGMENTS + 3, link_manager.element.size(),
        "snake must have 3*SEGMENTS + 3 links");

    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      assertNotNull(link.controller, "link " + i + " must have a controller");
      assertTrue(link.controller instanceof GlobalOscillatorController,
          "link " + i + " must follow the global oscillator");
    }

    assertTrue(Muscles.enabled, "the demo must enable muscles");
  }

  @Test
  void phaseTravelsAlongTheBody() {
    SnakeDemo.build();

    final LinkManager link_manager =
        ContextManager.getNodeManager().getLinkManager();
    final int period = Muscles.activeOscillator().getPeriodTicks();

    int min_phase = Integer.MAX_VALUE;
    int max_phase = Integer.MIN_VALUE;
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      assertTrue(link.phase >= 0 && link.phase < period,
          "link " + i + " phase must be within one period");
      min_phase = Math.min(min_phase, link.phase);
      max_phase = Math.max(max_phase, link.phase);
    }

    assertEquals(0, min_phase, "the head must start at phase 0");
    assertTrue(max_phase > period / 2,
        "phases must span most of one period for a visible traveling wave");
  }

  /**
   * Regression test: the snake's short (30px) links in a 6-links-per-node
   * tetrahedral packing used to exceed the spring integrator's stability
   * limit (elasticity 50) and explode to the universe walls within a few
   * ticks -- with or without muscles. The demo now uses softer springs;
   * the model must stay in one piece and keep moving.
   */
  @Test
  void dynamicsStayNumericallyStable() {
    SnakeDemo.build();

    final NodeManager node_manager = ContextManager.getNodeManager();
    final int n = node_manager.element.size();
    final Node mid = (Node) node_manager.element.get(11);
    final int start_x = mid.pos.x;
    final int start_y = mid.pos.y;

    // Force dynamics on: other tests may leave FrEnd.paused set.
    final boolean old_paused = FrEnd.paused;
    FrEnd.paused = false;
    try {
      // The old setup blew up by tick 5-9; run well past that point.
      for (int t = 0; t < 120; t++) {
        node_manager.nodeAndLinkUpdate();
      }
    } finally {
      FrEnd.paused = old_paused;
    }

    int min_x = Integer.MAX_VALUE;
    int max_x = Integer.MIN_VALUE;
    int min_y = Integer.MAX_VALUE;
    int max_y = Integer.MIN_VALUE;
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      min_x = Math.min(min_x, node.pos.x);
      max_x = Math.max(max_x, node.pos.x);
      min_y = Math.min(min_y, node.pos.y);
      max_y = Math.max(max_y, node.pos.y);
    }
    final int w = (max_x - min_x) >> Coords.shift;
    final int h = (max_y - min_y) >> Coords.shift;
    assertTrue(w < 400 && h < 400,
        "snake must not explode to the universe walls, bbox was " + w + "x" + h);

    // ...and the muscles must actually drive it, not leave it frozen.
    final int moved = Math.max(Math.abs(mid.pos.x - start_x),
        Math.abs(mid.pos.y - start_y)) >> Coords.shift;
    assertTrue(moved > 5,
        "snake should writhe, but the mid node moved only " + moved + "px");
  }
}
