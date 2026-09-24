// This program has been placed into the public domain by its author.
package com.springie.elements.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Olympic mode -- a demo model running under the follow-cam
 * (continuous centering) -- lives in a boundless world: no side walls
 * and no ceiling for the runners to hit. The ground always stays.
 */
class NodeBoundaryCheckTest {

  private static final int RADIUS = 10;

  private boolean saved_demo;
  private boolean saved_x;
  private boolean saved_y;
  private boolean saved_z;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.saved_demo = FrEnd.demo_model;
    this.saved_x = FrEnd.continuously_centre_x;
    this.saved_y = FrEnd.continuously_centre_y;
    this.saved_z = FrEnd.continuously_centre_z;
  }

  @AfterEach
  void tearDown() {
    FrEnd.demo_model = this.saved_demo;
    FrEnd.continuously_centre_x = this.saved_x;
    FrEnd.continuously_centre_y = this.saved_y;
    FrEnd.continuously_centre_z = this.saved_z;
  }

  private static Node node(int x, int y, int z) {
    final Node node = new Node(new Point3D(x, y, z), 42,
        new NodeTypeFactory());
    node.type.radius = RADIUS;
    return node;
  }

  private static int wallX() {
    return Coords.x_pixels << Coords.shift;
  }

  private static int wallY() {
    return Coords.y_pixels << Coords.shift;
  }

  private static int wallZ() {
    return Coords.z_pixels << Coords.shift;
  }

  private static void olympicMode() {
    FrEnd.demo_model = true;
    FrEnd.continuously_centre_x = true;
    FrEnd.continuously_centre_y = false;
    FrEnd.continuously_centre_z = false;
  }

  @Test
  void sideWallAppliesOutsideOlympicMode() {
    FrEnd.demo_model = false;
    FrEnd.continuously_centre_x = true;
    final Node node = node(wallX() + 1000, wallY() / 2, wallZ() / 2);
    node.velocity.x = 500;

    node.boundaryCheck();

    assertEquals(wallX() - RADIUS, node.pos.x, "x wall clamps");
    assertEquals(-475, node.velocity.x, "x velocity bounces");
  }

  @Test
  void noXWallInOlympicModeWithXCentering() {
    olympicMode();
    final Node node = node(wallX() + 1000, wallY() / 2, wallZ() / 2);
    node.velocity.x = 500;

    node.boundaryCheck();

    assertEquals(wallX() + 1000, node.pos.x, "no x wall for runners");
    assertEquals(500, node.velocity.x, "velocity untouched");
  }

  @Test
  void xWallAppliesInOlympicModeWithoutXCentering() {
    // The follow-cam is not tracking x, so the old wall keeps the
    // creature in view instead of letting it run off-screen for good.
    FrEnd.demo_model = true;
    FrEnd.continuously_centre_x = false;
    FrEnd.continuously_centre_y = true;
    FrEnd.continuously_centre_z = false;
    final Node node = node(wallX() + 1000, wallY() / 2, wallZ() / 2);

    node.boundaryCheck();

    assertEquals(wallX() - RADIUS, node.pos.x,
        "x wall stays without x centering");
  }

  @Test
  void depthWallStillAppliesInOlympicMode() {
    // The depth glass stays: the demos are tuned against it, and the
    // renderer cannot see past the eye plane.
    olympicMode();
    final Node node = node(wallX() / 2, wallY() / 2, wallZ() + 1000);
    node.velocity.z = 500;

    node.boundaryCheck();

    assertEquals(wallZ() - RADIUS, node.pos.z, "z wall stays");
    assertEquals(-475, node.velocity.z, "z velocity bounces");
  }

  @Test
  void groundStillAppliesInOlympicMode() {
    olympicMode();
    final Node node = node(wallX() / 2, wallY() + 1000, wallZ() / 2);
    node.velocity.y = 500;

    node.boundaryCheck();

    assertEquals(wallY() - RADIUS, node.pos.y, "the ground stays");
  }

  @Test
  void noCeilingInOlympicMode() {
    olympicMode();
    final Node node = node(wallX() / 2, RADIUS - 5, wallZ() / 2);
    node.velocity.y = -500;

    node.boundaryCheck();

    assertEquals(RADIUS - 5, node.pos.y, "no ceiling for jumpers");
    assertEquals(-500, node.velocity.y, "velocity untouched");
  }

  @Test
  void wallsReturnWhenCenteringOff() {
    FrEnd.demo_model = true;
    FrEnd.continuously_centre_x = false;
    FrEnd.continuously_centre_y = false;
    FrEnd.continuously_centre_z = false;
    final Node node = node(wallX() + 1000, wallY() / 2, wallZ() / 2);

    node.boundaryCheck();

    assertEquals(wallX() - RADIUS, node.pos.x,
        "bounded world without the follow-cam");
  }
}
