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
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;

/**
 * The snake demo must build a chain of face-sharing tetrahedra
 * (4 nodes + 1 new node per additional segment) with a passive strut
 * skeleton plus antagonistic flank muscles driven in a traveling wave.
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
  void buildsATetrahedralChainWithPassiveSkeletonAndFlankMuscles() {
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

    int muscles = 0;
    int struts = 0;
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller != null) {
        muscles++;
        assertTrue(link.controller instanceof GlobalOscillatorController,
            "link " + i + " must follow the global oscillator");
      } else {
        struts++;
      }
    }

    // Most links are passive skeleton struts; a minority are flank muscles.
    assertTrue(muscles > 0, "snake must have flank muscles");
    assertTrue(struts > muscles,
        "skeleton struts (" + struts + ") must outnumber muscles (" + muscles + ")");
    assertTrue(muscles >= 10 && muscles <= 30,
        "expected 10-30 flank muscles, got " + muscles);

    assertTrue(Muscles.enabled, "the demo must enable muscles");
  }

  @Test
  void musclePhaseTravelsAlongTheBody() {
    SnakeDemo.build();

    final LinkManager link_manager =
        ContextManager.getNodeManager().getLinkManager();
    final int period = Muscles.activeOscillator().getPeriodTicks();

    int min_phase = Integer.MAX_VALUE;
    int max_phase = Integer.MIN_VALUE;
    int muscles = 0;
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller == null) {
        continue;
      }
      muscles++;
      assertTrue(link.phase >= 0 && link.phase < period,
          "muscle " + i + " phase must be within one period");
      min_phase = Math.min(min_phase, link.phase);
      max_phase = Math.max(max_phase, link.phase);
    }

    assertTrue(muscles > 0, "need muscles to check phases");
    assertTrue(max_phase - min_phase > period / 2,
        "muscle phases must span most of one period for a traveling wave");
  }

  /**
   * The judge must be deterministic: two runs with the same parameters
   * must produce identical scores.
   */
  @Test
  void judgeIsDeterministic() {
    final SnakeJudge.Result r1 = SnakeJudge.score(600);
    final SnakeJudge.Result r2 = SnakeJudge.score(600);
    assertEquals(r1.distance_px, r2.distance_px);
    assertEquals(r1.length_keep, r2.length_keep, 1e-9);
    assertEquals(r1.xs_keep, r2.xs_keep, 1e-9);
    assertEquals(r1.score, r2.score, 1e-9);
  }

  /**
   * Regression test: the passive skeleton must not gain height on its own.
   * An earlier build placed nodes under the physics ground (and at negative
   * z), so boundaryCheck teleported them every tick and ratcheted the whole
   * body skyward even with zero muscle drive.
   */
  @Test
  void passiveSkeletonDoesNotRise() {
    final int old_amplitude = SnakeDemo.muscle_amplitude_pct;
    SnakeDemo.muscle_amplitude_pct = 0;
    try {
      SnakeDemo.build();
    } finally {
      SnakeDemo.muscle_amplitude_pct = old_amplitude;
    }

    final NodeManager node_manager = ContextManager.getNodeManager();
    final int n = node_manager.element.size();

    long com_y0 = 0;
    for (int i = 0; i < n; i++) {
      com_y0 += ((Node) node_manager.element.get(i)).pos.y;
    }

    final boolean old_paused = FrEnd.paused;
    FrEnd.paused = false;
    try {
      for (int t = 0; t < 120; t++) {
        node_manager.nodeAndLinkUpdate();
      }
    } finally {
      FrEnd.paused = old_paused;
    }

    long com_y1 = 0;
    for (int i = 0; i < n; i++) {
      com_y1 += ((Node) node_manager.element.get(i)).pos.y;
    }

    // y grows downward: the COM must not move up (negative delta).
    final int rise_px = (int) ((com_y0 - com_y1) / n >> Coords.shift);
    assertTrue(rise_px < 20,
        "passive skeleton rose " + rise_px + "px with zero drive");
  }

  /**
   * The snake must hold its tubular shape and slither, not explode into a
   * squirming mess or freeze in place.
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
        "snake should slither, but the mid node moved only " + moved + "px");
  }
}
