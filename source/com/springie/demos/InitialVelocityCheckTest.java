// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the "no free shove" initial-velocity check used by the
 * locomotion judges.
 */
class InitialVelocityCheckTest {
  private NodeManager node_manager;

  @BeforeEach
  void setUp() {
    ContextManager.setNodeManager(new NodeManager());
    this.node_manager = ContextManager.getNodeManager();
  }

  private Node addNode() {
    final NodeType type = this.node_manager.node_type_factory.getNew();
    final Clazz clazz = this.node_manager.clazz_factory.getNew(0xFFFFFFFF);
    return this.node_manager.addNewAgent(new Point3D(0, 0, 0), clazz, type);
  }

  @Test
  void freshNodesPassAsAtRest() {
    // The engine spawns nodes with a tiny uniform (3, 4, 5) velocity;
    // that must not trip the check.
    addNode();
    addNode();
    addNode();
    assertTrue(InitialVelocityCheck.atRestAlong(this.node_manager,
        CompassPoint.E));
    assertTrue(InitialVelocityCheck.atRestOnFloor(this.node_manager));
    assertTrue(InitialVelocityCheck.atRestVertically(this.node_manager));
  }

  @Test
  void translationalShoveAlongHeadingFails() {
    final Node a = addNode();
    final Node b = addNode();
    // 1 px/frame shove toward E on every node.
    a.velocity.x = 256;
    b.velocity.x = 256;
    assertEquals(1.0,
        InitialVelocityCheck.meanAlong(this.node_manager, CompassPoint.E),
        1e-9);
    assertFalse(InitialVelocityCheck.atRestAlong(this.node_manager,
        CompassPoint.E));
    // The same shove reads negative from the opposite heading.
    assertEquals(-1.0,
        InitialVelocityCheck.meanAlong(this.node_manager, CompassPoint.W),
        1e-9);
  }

  @Test
  void perpendicularShovePassesAlongHeading() {
    final Node a = addNode();
    // 1 px/frame toward S: nothing along the E/W axis.
    a.velocity.x = 0; // isolate: drop the engine's (3,4,5) spawn velocity.
    a.velocity.z = 256;
    assertEquals(0.0,
        InitialVelocityCheck.meanAlong(this.node_manager, CompassPoint.E),
        1e-9);
    assertTrue(InitialVelocityCheck.atRestAlong(this.node_manager,
        CompassPoint.E));
    // ...but the floor-speed check catches a shove in any direction.
    assertFalse(InitialVelocityCheck.atRestOnFloor(this.node_manager));
  }

  @Test
  void tangentialSpinKickNetsToZero() {
    // A pure spin (the wheel's tangential self-start shape) has no net
    // velocity along any heading, so it is not a shove.
    final int n = 8;
    final Node[] nodes = new Node[n];
    for (int i = 0; i < n; i++) {
      nodes[i] = addNode();
    }
    final int kick = 512; // 2 px/frame tangential.
    for (int i = 0; i < n; i++) {
      final double a = 2.0 * Math.PI * i / n;
      nodes[i].velocity.x = (int) (-Math.sin(a) * kick);
      nodes[i].velocity.y = (int) (Math.cos(a) * kick);
    }
    assertTrue(InitialVelocityCheck.atRestAlong(this.node_manager,
        CompassPoint.E),
        "mean=" + InitialVelocityCheck.meanAlong(this.node_manager,
            CompassPoint.E));
  }

  @Test
  void verticalCheckCatchesUpwardShove() {
    final Node a = addNode();
    // Upward shove (negative y in screen coords): 1 px/frame.
    a.velocity.y = -256;
    assertEquals(-1.0,
        InitialVelocityCheck.meanVertical(this.node_manager), 1e-9);
    assertFalse(InitialVelocityCheck.atRestVertically(this.node_manager));
  }
}
