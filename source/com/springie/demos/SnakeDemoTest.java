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

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;

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
}
