// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.Muscles;
import com.springie.world.World;

/**
 * Tests for the spider tank demo.
 */
public class SpiderTankDemoTest {
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
  void buildsSixLegsWithMuscles() {
    SpiderTankDemo.buildAt(0);
    final NodeManager node_manager = ContextManager.getNodeManager();
    final LinkManager link_manager = node_manager.getLinkManager();

    // 6 legs × 2 new nodes (knee, foot; hip is a body node) = 12,
    // plus 10 body nodes (6 bottom + 4 ridge), plus 1 turret top = 23 nodes.
    assertEquals(23, node_manager.element.size());

    // Count muscle links (those with a controller).
    int muscles = 0;
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller instanceof com.springie.muscles.GlobalOscillatorController) {
        muscles++;
      }
    }
    // 6 legs × 1 hip-foot muscle each.
    assertEquals(6, muscles);
  }

  @Test
  void tripodGaitHasAlternatingGroups() {
    SpiderTankDemo.buildAt(0);
    final LinkManager link_manager = ContextManager.getNodeManager().getLinkManager();

    final Set<Integer> phases = new HashSet<>();
    for (int i = 0; i < link_manager.element.size(); i++) {
      final Link link = (Link) link_manager.element.get(i);
      if (link.controller instanceof com.springie.muscles.GlobalOscillatorController) {
        phases.add(link.phase);
      }
    }
    // 6 muscles, tripod: 3 at phase 0, 3 at half-period (60).
    assertEquals(2, phases.size());
    assertTrue(phases.contains(0));
    assertTrue(phases.contains(60));
  }

  @Test
  void enablesGravityAndFriction() {
    final Node body = SpiderTankDemo.buildAt(0);
    assertTrue(Muscles.enabled);
    assertTrue(World.gravity_active);
    assertTrue(World.ground_friction > 0);
    // Body node exists.
    assertTrue(body != null);
  }
}
