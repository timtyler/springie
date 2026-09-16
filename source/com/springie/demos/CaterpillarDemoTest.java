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
 * The caterpillar demo must build a flat ribbon of face-sharing
 * tetrahedra (3 nodes per cross-section) with a passive strut skeleton
 * plus peristaltic axial muscles driven in a traveling wave.
 */
class CaterpillarDemoTest {

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
  void buildsAFlatRibbonOfTetrahedraWithAxialMuscles() {
    CaterpillarDemo.build();

    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    // 3 nodes per cross-section (left, right, top), SEGMENTS+1 sections.
    assertEquals(3 * (CaterpillarDemo.SEGMENTS + 1), node_manager.element.size(),
        "caterpillar must have 3*(SEGMENTS+1) nodes");

    // Skeleton struts plus the axial muscle links.
    assertEquals(111, link_manager.element.size(),
        "caterpillar must have 111 links");

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

    // 3 axial lines x SEGMENTS = 36 peristaltic muscles.
    assertEquals(36, muscles, "caterpillar must have 36 axial muscles");
    assertTrue(struts > muscles,
        "skeleton struts (" + struts + ") must outnumber muscles (" + muscles + ")");

    assertTrue(Muscles.enabled, "the demo must enable muscles");
  }

  @Test
  void musclePhaseTravelsAlongTheBody() {
    CaterpillarDemo.build();

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
    final CaterpillarJudge.Result r1 = CaterpillarJudge.score(600);
    final CaterpillarJudge.Result r2 = CaterpillarJudge.score(600);
    assertEquals(r1.forward_px, r2.forward_px);
    assertEquals(r1.lateral_px, r2.lateral_px);
    assertEquals(r1.score, r2.score, 1e-9);
  }

  /**
   * The caterpillar must crawl forward (not backward, not in place) and
   * hold its shape (low passive strain, no DQ).
   */
  @Test
  void crawlsForwardWithStructuralIntegrity() {
    final CaterpillarJudge.Result r = CaterpillarJudge.score(600);
    assertTrue(!r.disqualified, "caterpillar must not DQ (explode or over-strain)");
    assertTrue(r.forward_px > 50,
        "caterpillar must crawl forward, got " + r.forward_px + "px");
    assertTrue(r.max_passive_strain < 0.5,
        "passive strain must stay low, got " + r.max_passive_strain);
  }
}
