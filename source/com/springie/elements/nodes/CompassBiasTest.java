// This code has been placed into the public domain by its author.

package com.springie.elements.nodes;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.demos.CompassPoint;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.world.World;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The universe compass bias nudges each node's velocity along its own
 * N/S/E/W heading every frame: bias_size velocity units per frame, the
 * same units gravity adds to velocity.y. A null heading means no bias.
 */
class CompassBiasTest {

  private NodeManager manager;
  private int saved_bias;
  private int saved_gravity;
  private int saved_temperature;
  private boolean saved_gesture;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.saved_bias = CompassPoint.bias_size;
    this.saved_gravity = World.gravity_strength;
    this.saved_temperature = World.global_temperature;
    this.saved_gesture = FrEnd.forces_disabled_during_gesture;
    CompassPoint.bias_size = 5;
    World.gravity_strength = 0;
    World.global_temperature = 0;
    FrEnd.forces_disabled_during_gesture = false;
    this.manager = new NodeManager();
  }

  @AfterEach
  void tearDown() {
    CompassPoint.bias_size = this.saved_bias;
    World.gravity_strength = this.saved_gravity;
    World.global_temperature = this.saved_temperature;
    FrEnd.forces_disabled_during_gesture = this.saved_gesture;
  }

  private Node headingNode(final CompassPoint heading) {
    final Node node = new Node(new Point3D(0, 0, 0), 0,
        new NodeTypeFactory());
    node.compass = heading;
    node.velocity = new Vector3D(0, 0, 0); // constructor seeds (3, 4, 5)
    this.manager.element.add(node);
    return node;
  }

  private void runOneFrame() {
    final NodeManager saved = ContextManager.getNodeManager();
    ContextManager.setNodeManager(this.manager);
    try {
      this.manager.applyAcceleration();
    } finally {
      ContextManager.setNodeManager(saved);
    }
  }

  @Test
  void eastBiasPushesPlusX() {
    final Node node = headingNode(CompassPoint.E);
    runOneFrame();
    assertEquals(5, node.velocity.x, "E heading must gain +bias on x");
    assertEquals(0, node.velocity.z, "E heading must not move on z");
    assertEquals(0, node.velocity.y, "bias must not touch the vertical");
  }

  @Test
  void westBiasPushesMinusX() {
    final Node node = headingNode(CompassPoint.W);
    runOneFrame();
    assertEquals(-5, node.velocity.x, "W heading must gain -bias on x");
  }

  @Test
  void northBiasPushesMinusZ() {
    final Node node = headingNode(CompassPoint.N);
    runOneFrame();
    assertEquals(-5, node.velocity.z, "N heading must gain -bias on z");
    assertEquals(0, node.velocity.x, "N heading must not move on x");
  }

  @Test
  void southBiasPushesPlusZ() {
    final Node node = headingNode(CompassPoint.S);
    runOneFrame();
    assertEquals(5, node.velocity.z, "S heading must gain +bias on z");
  }

  @Test
  void nullHeadingGetsNoBias() {
    final Node node = headingNode(null);
    runOneFrame();
    assertEquals(0, node.velocity.x, "null heading must not move on x");
    assertEquals(0, node.velocity.z, "null heading must not move on z");
  }

  @Test
  void zeroBiasDisables() {
    CompassPoint.bias_size = 0;
    final Node node = headingNode(CompassPoint.E);
    runOneFrame();
    assertEquals(0, node.velocity.x, "zero bias must leave velocity alone");
    assertEquals(0, node.velocity.z, "zero bias must leave velocity alone");
  }
}
