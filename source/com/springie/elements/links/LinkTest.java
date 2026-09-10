package com.springie.elements.links;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Exercises the spring physics core without touching the GUI.
 * Positions are fixed-point: Coords.shift fractional bits.
 */
class LinkTest {

  private static Node nodeAt(int x, int y, int z) {
    return new Node(new Point3D(x, y, z), 42, new NodeTypeFactory());
  }

  private static Link linkBetween(Node a, Node b) {
    // Accessible because this test lives in the same package.
    final LinkType type = new LinkType(100 << Coords.shift, 50);
    return new Link(a, b, type, new Clazz(0));
  }

  @Test
  void actualLengthOfAxisAlignedLink() {
    final int span = 512 << Coords.shift;
    final Link link = linkBetween(nodeAt(0, 0, 0), nodeAt(span, 0, 0));
    assertEquals(span, link.getActualLength());
  }

  @Test
  void actualLengthUsesEuclideanDistance() {
    // 3-4-5 triangle in fixed-point units. fastSqrt is an approximation
    // outside its fully-accurate range, so allow a small tolerance.
    final Link link = linkBetween(
        nodeAt(0, 0, 0),
        nodeAt((300 << Coords.shift), (400 << Coords.shift), 0));
    final int actual = link.getActualLength();
    final int expected = 500 << Coords.shift;
    assertTrue(Math.abs(actual - expected) <= (5 << Coords.shift),
        "expected ~" + expected + " but got " + actual);
  }

  @Test
  void actualLengthIsSymmetric() {
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);
    assertEquals(linkBetween(a, b).getActualLength(),
        linkBetween(b, a).getActualLength());
  }
}
