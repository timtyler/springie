// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.world.World;

/**
 * The crawler demo must build a 4-legged walker: a tetrahedral body with
 * four tetrahedral legs, each leg driven by a single muscle (hip-foot),
 * with a trot gait (diagonal legs in phase).
 */
class CrawlerDemoTest {

  private boolean old_muscles_enabled;
  private boolean old_gravity_active;
  private int old_friction;

  @BeforeEach
  void setUp() {
    old_muscles_enabled = Muscles.enabled;
    old_gravity_active = World.gravity_active;
    old_friction = World.ground_friction;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = old_muscles_enabled;
    World.gravity_active = old_gravity_active;
    World.ground_friction = old_friction;
  }

  @Test
  void buildsFourLegsWithMuscles() {
    final Node body = CrawlerDemo.buildAt(0);
    assertNotNull(body);

    final NodeManager nm = ContextManager.getNodeManager();
    // 6 body nodes + 4 legs × 2 nodes (knee + foot) = 14 nodes.
    assertEquals(14, nm.element.size());

    final LinkManager lm = nm.getLinkManager();
    int muscle_count = 0;
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        muscle_count++;
      }
    }
    // 4 legs × 1 muscle each.
    assertEquals(4, muscle_count);
  }

  @Test
  void trotGaitHasDiagonalPairsInPhase() {
    CrawlerDemo.buildAt(0);
    final NodeManager nm = ContextManager.getNodeManager();
    final LinkManager lm = nm.getLinkManager();

    // Collect muscle phases.
    final java.util.List<Integer> phases = new java.util.ArrayList<>();
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof GlobalOscillatorController) {
        phases.add(link.phase);
      }
    }

    assertEquals(4, phases.size());
    // Diagonal pairs (FL+BR, FR+BL) share phases; the pairs differ.
    // FL=0, FR=60, BL=60, BR=0 for the default 120-tick period.
    assertTrue(phases.contains(0));
    assertTrue(phases.contains(60));
  }

  @Test
  void enablesGravityAndFriction() {
    CrawlerDemo.buildAt(0);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
    assertTrue(Muscles.enabled);
  }
}
