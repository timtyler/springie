// This code has been placed into the public domain by its author.

package com.springie.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.springie.context.ContextManager;
import com.springie.demos.WheelDemo;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The general no-mid-air-starts mechanism: {@link Grounding#restOnGround}
 * must set a floating model down onto the ground plane, lift a buried one
 * up onto it, and leave a grounded model (or an empty one) alone.
 */
public class GroundingTest {

  private boolean old_muscles_enabled;
  private boolean old_gravity_active;
  private int old_friction;
  private int old_temperature;

  @BeforeEach
  void setUp() {
    this.old_muscles_enabled = Muscles.enabled;
    this.old_gravity_active = World.gravity_active;
    this.old_friction = World.ground_friction;
    this.old_temperature = World.global_temperature;
  }

  @AfterEach
  void tearDown() {
    Muscles.enabled = this.old_muscles_enabled;
    World.gravity_active = this.old_gravity_active;
    World.ground_friction = this.old_friction;
    World.global_temperature = this.old_temperature;
  }

  private static int lowestExtent(NodeManager node_manager) {
    int lowest = Integer.MIN_VALUE;
    final int n = node_manager.element.size();
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      final int extent = node.pos.y + node.type.radius;
      if (extent > lowest) {
        lowest = extent;
      }
    }
    return lowest;
  }

  private static void shiftAllY(NodeManager node_manager, int dy) {
    final int n = node_manager.element.size();
    for (int i = 0; i < n; i++) {
      ((Node) node_manager.element.get(i)).pos.y += dy;
    }
  }

  private static NodeManager freshWheel() {
    ContextManager.setNodeManager(new NodeManager());
    WheelDemo.buildAt(120);
    return ContextManager.getNodeManager();
  }

  @Test
  void floatingModelIsSetDownOnTheGround() {
    final NodeManager node_manager = freshWheel();
    final int ground = Coords.y_pixels << Coords.shift;
    assertEquals(ground, lowestExtent(node_manager),
        "the demo build must already rest on the ground");

    // Hoist the whole model into mid-air (y grows downward).
    shiftAllY(node_manager, -(200 << Coords.shift));
    assertTrue(lowestExtent(node_manager) < ground);

    Grounding.restOnGround(node_manager);

    assertEquals(ground, lowestExtent(node_manager),
        "a floating model must be set down exactly on the ground plane");
  }

  @Test
  void buriedModelIsLiftedOntoTheGround() {
    final NodeManager node_manager = freshWheel();
    final int ground = Coords.y_pixels << Coords.shift;

    // Push the whole model below the ground plane.
    shiftAllY(node_manager, 200 << Coords.shift);
    assertTrue(lowestExtent(node_manager) > ground);

    Grounding.restOnGround(node_manager);

    assertEquals(ground, lowestExtent(node_manager),
        "a buried model must be lifted exactly onto the ground plane");
  }

  @Test
  void groundedModelKeepsItsExactPositions() {
    final NodeManager node_manager = freshWheel();
    final int n = node_manager.element.size();
    final int[] before = new int[n];
    for (int i = 0; i < n; i++) {
      before[i] = ((Node) node_manager.element.get(i)).pos.y;
    }

    Grounding.restOnGround(node_manager);

    for (int i = 0; i < n; i++) {
      assertEquals(before[i], ((Node) node_manager.element.get(i)).pos.y,
          "a grounded model must not move, node " + i);
    }
  }

  @Test
  void emptyModelIsANoOp() {
    // Must not throw.
    Grounding.restOnGround(new NodeManager());
  }
}
