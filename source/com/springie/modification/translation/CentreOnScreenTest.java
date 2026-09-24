// This program has been placed into the public domain by its author.
package com.springie.modification.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.Coords;

/**
 * Per-axis continuous centering must translate node positions so the
 * model's bounding box sits central on each selected axis, leave the
 * other axes alone, and never touch velocities.
 */
class CentreOnScreenTest {

  private NodeManager manager;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    // Off-centre on every axis.
    this.manager.element.add(node(1000, 3000, 5000));
    this.manager.element.add(node(2000, 4000, 6000));
  }

  private static Node node(int x, int y, int z) {
    final Node node = new Node(new Point3D(x, y, z), 42,
        new NodeTypeFactory());
    node.velocity = new Vector3D(7, 8, 9);
    return node;
  }

  private static int screenX() {
    return Coords.getInternalFromPixelCoords(Coords.x_pixels);
  }

  @Test
  void centreXOnlyMovesX() {
    CentreOnScreen.centreOnAxes(this.manager, true, false, false);

    final int offset_x = (screenX() - 1000 - 2000) >> 1;
    assertEquals(1000 + offset_x, node(0).pos.x, "node 0 x centred");
    assertEquals(2000 + offset_x, node(1).pos.x, "node 1 x centred");

    // The bounding-box centre now sits on the screen centre (to the
    // pixel, given the integer shift).
    final int centre_x = (node(0).pos.x + node(1).pos.x) >> 1;
    assertEquals(screenX() >> 1, centre_x, "bbox centre x on screen centre");

    // Y and Z untouched.
    assertEquals(3000, node(0).pos.y);
    assertEquals(4000, node(1).pos.y);
    assertEquals(5000, node(0).pos.z);
    assertEquals(6000, node(1).pos.z);

    // Velocities untouched.
    assertEquals(7, node(0).velocity.x);
    assertEquals(8, node(0).velocity.y);
    assertEquals(9, node(0).velocity.z);
  }

  @Test
  void centreAllAxes() {
    CentreOnScreen.centreOnAxes(this.manager, true, true, true);

    final int screen_y = Coords
        .getInternalFromPixelCoords(Coords.y_pixels);
    final int screen_z = Coords
        .getInternalFromPixelCoords(Coords.z_pixels);
    assertEquals(screenX() >> 1, (node(0).pos.x + node(1).pos.x) >> 1,
        "x centred");
    assertEquals(screen_y >> 1, (node(0).pos.y + node(1).pos.y) >> 1,
        "y centred");
    assertEquals(screen_z >> 1, (node(0).pos.z + node(1).pos.z) >> 1,
        "z centred");

    assertEquals(7, node(1).velocity.x, "velocity untouched");
  }

  @Test
  void noAxesSelectedMovesNothing() {
    CentreOnScreen.centreOnAxes(this.manager, false, false, false);

    assertEquals(1000, node(0).pos.x);
    assertEquals(4000, node(1).pos.y);
    assertEquals(6000, node(1).pos.z);
  }

  private Node node(int index) {
    return (Node) this.manager.element.get(index);
  }
}
