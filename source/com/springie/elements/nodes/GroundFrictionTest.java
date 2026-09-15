// This code has been placed into the public domain by its author.

package com.springie.elements.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.world.World;

/**
 * Ground friction damps a node's horizontal velocity while it touches
 * the ground (y=0): 0 leaves it alone, 100 stops it dead.
 */
class GroundFrictionTest {

  private int old_friction;

  @BeforeEach
  void setUp() {
    this.old_friction = World.ground_friction;
  }

  @AfterEach
  void tearDown() {
    World.ground_friction = this.old_friction;
  }

  private Node groundNodeWithVelocity(int vx, int vz) {
    final int c = 100 << com.springie.render.Coords.shift;
    final int ground_y = (com.springie.render.Coords.y_pixels << com.springie.render.Coords.shift);
    final Node node = new Node(new com.springie.geometry.Point3D(c, ground_y, c),
        0, new NodeTypeFactory());
    node.pos.y = ground_y; // touching the ground (high-Y wall)
    node.velocity.x = vx;
    node.velocity.z = vz;
    node.velocity.y = 0;
    return node;
  }

  @Test
  void zeroFrictionLeavesHorizontalVelocityAlone() {
    World.ground_friction = 0;
    final Node node = groundNodeWithVelocity(1000, -600);
    node.boundaryCheck();
    assertEquals(1000, node.velocity.x, "vx must survive with no friction");
    assertEquals(-600, node.velocity.z, "vz must survive with no friction");
  }

  @Test
  void fullFrictionStopsHorizontalMotion() {
    World.ground_friction = 100;
    final Node node = groundNodeWithVelocity(1000, -600);
    node.boundaryCheck();
    assertEquals(0, node.velocity.x, "vx must stop with full friction");
    assertEquals(0, node.velocity.z, "vz must stop with full friction");
  }

  @Test
  void halfFrictionHalvesHorizontalVelocity() {
    World.ground_friction = 50;
    final Node node = groundNodeWithVelocity(1000, -600);
    node.boundaryCheck();
    assertEquals(500, node.velocity.x, "vx must halve with 50 friction");
    assertEquals(-300, node.velocity.z, "vz must halve with 50 friction");
  }

  @Test
  void frictionDoesNotAffectAirborneNodes() {
    World.ground_friction = 100;
    final Node node = groundNodeWithVelocity(1000, -600);
    node.pos.y = 100 << com.springie.render.Coords.shift; // well above ground
    node.boundaryCheck();
    assertEquals(1000, node.velocity.x, "airborne vx must be untouched");
    assertEquals(-600, node.velocity.z, "airborne vz must be untouched");
  }
}
