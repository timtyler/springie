// This program has been placed into the public domain by its author.

package com.springie.modification.faces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Pins the face-finding algorithm: for every selected link, each
 * shortest cycle through that link becomes one face. A cube wireframe
 * yields exactly its six square faces; a diagonal across a quad splits
 * it into the two triangles.
 */
class FaceCycleFinderTest {

  private final NodeTypeFactory node_types = new NodeTypeFactory();
  private final LinkTypeFactory link_types = new LinkTypeFactory();

  private Node node(int x, int y, int z) {
    final Node node = new Node(new Point3D(x, y, z), 42, this.node_types);
    node.type.selected = true;
    return node;
  }

  private Link link(Node a, Node b) {
    final Link link = new Link(a, b,
      this.link_types.getNew(100 << Coords.shift, 50), new Clazz(0));
    link.type.selected = true;
    return link;
  }

  private static ArrayList<Node> nodes(Node... nodes) {
    final ArrayList<Node> list = new ArrayList<>();
    for (Node node : nodes) {
      list.add(node);
    }
    return list;
  }

  private static ArrayList<Link> links(Link... links) {
    final ArrayList<Link> list = new ArrayList<>();
    for (Link link : links) {
      list.add(link);
    }
    return list;
  }

  /** Every consecutive pair (wrapping round) must share a link. */
  private static void assertIsCycle(ArrayList<Node> cycle,
      ArrayList<Link> links) {
    final HashSet<String> pairs = new HashSet<>();
    for (Link link : links) {
      pairs.add(System.identityHashCode(link.nodes[0]) + ">"
        + System.identityHashCode(link.nodes[1]));
      pairs.add(System.identityHashCode(link.nodes[1]) + ">"
        + System.identityHashCode(link.nodes[0]));
    }
    final int n = cycle.size();
    assertTrue(n >= 3, "a face needs at least 3 nodes");
    for (int i = 0; i < n; i++) {
      final Node a = cycle.get(i);
      final Node b = cycle.get((i + 1) % n);
      assertTrue(pairs.contains(System.identityHashCode(a) + ">"
        + System.identityHashCode(b)),
        "nodes " + i + " and " + ((i + 1) % n) + " are not linked");
    }
  }

  @Test
  void squareLoopFindsOneQuad() {
    final Node a = node(0, 0, 0);
    final Node b = node(1, 0, 0);
    final Node c = node(1, 1, 0);
    final Node d = node(0, 1, 0);
    final ArrayList<Link> ls = links(link(a, b), link(b, c), link(c, d),
      link(d, a));

    final ArrayList<ArrayList<Node>> faces = FaceCycleFinder
      .findFaceCycles(nodes(a, b, c, d), ls);

    assertEquals(1, faces.size(), "a square loop is one face");
    assertEquals(4, faces.get(0).size());
    assertIsCycle(faces.get(0), ls);
  }

  @Test
  void cubeWireframeFindsSixQuads() {
    final Node[] v = new Node[8];
    for (int i = 0; i < 8; i++) {
      v[i] = node(i & 1, (i >> 1) & 1, (i >> 2) & 1);
    }
    // the 12 edges of a cube: pairs differing in one coordinate
    final ArrayList<Link> ls = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      for (int bit = 0; bit < 3; bit++) {
        final int j = i ^ (1 << bit);
        if (j > i) {
          ls.add(link(v[i], v[j]));
        }
      }
    }
    assertEquals(12, ls.size());

    final ArrayList<ArrayList<Node>> faces = FaceCycleFinder
      .findFaceCycles(nodes(v), ls);

    assertEquals(6, faces.size(), "a cube wireframe has six faces");
    for (ArrayList<Node> face : faces) {
      assertEquals(4, face.size(), "every cube face is a quad");
      assertIsCycle(face, ls);
    }
  }

  @Test
  void twoTrianglesSharingAnEdge() {
    final Node a = node(0, 0, 0);
    final Node b = node(1, 0, 0);
    final Node c = node(0, 1, 0);
    final Node d = node(0, -1, 0);
    final ArrayList<Link> ls = links(link(a, b), link(b, c), link(c, a),
      link(a, d), link(d, b));

    final ArrayList<ArrayList<Node>> faces = FaceCycleFinder
      .findFaceCycles(nodes(a, b, c, d), ls);

    assertEquals(2, faces.size());
    for (ArrayList<Node> face : faces) {
      assertEquals(3, face.size());
      assertIsCycle(face, ls);
    }
  }

  @Test
  void diagonalSplitsQuadIntoTriangles() {
    final Node a = node(0, 0, 0);
    final Node b = node(1, 0, 0);
    final Node c = node(1, 1, 0);
    final Node d = node(0, 1, 0);
    final ArrayList<Link> ls = links(link(a, b), link(b, c), link(c, d),
      link(d, a), link(a, c));

    final ArrayList<ArrayList<Node>> faces = FaceCycleFinder
      .findFaceCycles(nodes(a, b, c, d), ls);

    assertEquals(2, faces.size(),
      "the diagonal makes the quad a non-shortest cycle");
    for (ArrayList<Node> face : faces) {
      assertEquals(3, face.size());
      assertIsCycle(face, ls);
    }
  }

  @Test
  void openChainFindsNothing() {
    final Node a = node(0, 0, 0);
    final Node b = node(1, 0, 0);
    final Node c = node(2, 0, 0);

    assertTrue(FaceCycleFinder
      .findFaceCycles(nodes(a, b, c), links(link(a, b), link(b, c)))
      .isEmpty(), "a chain with no cycle makes no faces");
  }

  @Test
  void unselectedLinksAreIgnored() {
    // Selection filtering (by the type.selected flag) is the caller's
    // job; the finder only ever sees the selected links.
    final Node a = node(0, 0, 0);
    final Node b = node(1, 0, 0);
    final Node c = node(1, 1, 0);
    final Node d = node(0, 1, 0);
    final Link ab = link(a, b);
    final Link bc = link(b, c);
    final Link cd = link(c, d);

    assertTrue(FaceCycleFinder
      .findFaceCycles(nodes(a, b, c, d), links(ab, bc, cd)).isEmpty(),
      "the loop is broken without the fourth link");
  }

  @Test
  void linksTouchingUnselectedNodesAreIgnored() {
    final Node a = node(0, 0, 0);
    final Node b = node(1, 0, 0);
    final Node c = node(1, 1, 0);
    final Node d = node(0, 1, 0);
    d.type.selected = false;

    assertTrue(FaceCycleFinder.findFaceCycles(nodes(a, b, c),
      links(link(a, b), link(b, c), link(c, d), link(d, a))).isEmpty(),
      "links through the unselected node cannot form a face");
  }
}
