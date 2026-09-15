// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;

/**
 * Builds a snake from a chain of tetrahedra: each new node forms a
 * tetrahedron with the previous end face, so the body is a 3D tube with
 * a triangular cross-section. Every edge is a muscle; a phase wave
 * travels along the body, making it writhe.
 *
 * <p>The repeating primitive is two tetrahedra sharing a face (a
 * triangular bipyramid).
 */
public final class SnakeDemo {
  private SnakeDemo() {
    // static-only
  }

  /** Number of tetrahedra in the chain. */
  public static final int SEGMENTS = 20;

  /** Edge length of the tetrahedra, in pixels. */
  public static final int EDGE_PIXELS = 30;

  /**
   * Builds the snake in the current model, replacing whatever is there.
   * Enables muscles and tunes the active oscillator for a slow crawl.
   */
  public static void build() {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initialSetUp();
    final LinkManager link_manager = node_manager.getLinkManager();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    final LinkType link_type = link_manager.link_type_factory.getNew(
        EDGE_PIXELS << Coords.shift, 50);

    // Tune the oscillator first: the phase step below assumes it.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude((int) (0.25 * Muscles.UNITY));
    Muscles.activeOscillator().setPeriodTicks(60);
    Muscles.activeOscillator().setPhase(0);
    final int period = Muscles.activeOscillator().getPeriodTicks();

    final int e = EDGE_PIXELS << Coords.shift;
    final double sqrt3 = Math.sqrt(3.0);
    final double sqrt6 = Math.sqrt(6.0);

    // First tetrahedron: base triangle in the XZ plane, apex above.
    // Centred at the origin, lifted above the ground.
    final int y0 = 100 << Coords.shift;
    final Node p0 = addNode(node_manager, clazz, node_type, 0, y0, 0);
    final Node p1 = addNode(node_manager, clazz, node_type, e, y0, 0);
    final Node p2 = addNode(node_manager, clazz, node_type,
        (int) (e / 2.0), y0, (int) (e * sqrt3 / 2.0));
    final Node p3 = addNode(node_manager, clazz, node_type,
        (int) (e / 2.0), y0 + (int) (e * sqrt6 / 3.0), (int) (e * sqrt3 / 6.0));

    linkTetrahedron(link_manager, link_type, clazz, p0, p1, p2, p3, 0, period);

    // Chain: each new node forms a tetrahedron with the previous end face.
    // The end face rotates through the tetrahedron's faces, giving a
    // straight chain with a triangular cross-section.
    Node a = p0;
    Node b = p1;
    Node c = p2;
    Node d = p3;
    for (int i = 1; i < SEGMENTS; i++) {
      // New node opposite 'a' across face (b, c, d): reflect 'a' through
      // the face plane to get a regular tetrahedron on the far side.
      final Node next = reflectAcrossFace(node_manager, clazz, node_type, a, b, c, d);
      linkTetrahedron(link_manager, link_type, clazz, b, c, d, next,
          (i * period) / SEGMENTS, period);
      // Advance: the new end face drops the oldest node.
      a = b;
      b = c;
      c = d;
      d = next;
    }
  }

  private static Node addNode(NodeManager node_manager, Clazz clazz,
      NodeType node_type, int x, int y, int z) {
    return node_manager.addNewAgent(new Point3D(x, y, z), clazz, node_type);
  }

  /**
   * Creates the six edges of a tetrahedron as muscle links, all sharing
   * the given phase (the segment's position in the traveling wave).
   */
  private static void linkTetrahedron(LinkManager link_manager, LinkType link_type,
      Clazz clazz, Node p0, Node p1, Node p2, Node p3, int phase, int period) {
    muscleLink(link_manager, link_type, clazz, p0, p1, phase);
    muscleLink(link_manager, link_type, clazz, p0, p2, phase);
    muscleLink(link_manager, link_type, clazz, p0, p3, phase);
    muscleLink(link_manager, link_type, clazz, p1, p2, phase);
    muscleLink(link_manager, link_type, clazz, p1, p3, phase);
    muscleLink(link_manager, link_type, clazz, p2, p3, phase);
  }

  private static void muscleLink(LinkManager link_manager, LinkType link_type,
      Clazz clazz, Node n1, Node n2, int phase) {
    // Skip if these nodes are already linked (shared faces reuse edges).
    final int n_o_l = link_manager.element.size();
    for (int i = n_o_l; --i >= 0;) {
      final Link existing = (Link) link_manager.element.get(i);
      if ((existing.nodes[0] == n1 && existing.nodes[1] == n2)
          || (existing.nodes[0] == n2 && existing.nodes[1] == n1)) {
        return;
      }
    }
    final Link link = link_manager.setLink(n1, n2, link_type, clazz);
    link.phase = phase;
    link.controller = new GlobalOscillatorController(Muscles.active_oscillator);
    link.adjusted_rest_length = link.type.length;
  }

  /**
   * Returns a new node positioned so that (b, c, d, next) is a regular
   * tetrahedron: the reflection of 'a' across the plane of face (b, c, d).
   */
  private static Node reflectAcrossFace(NodeManager node_manager, Clazz clazz,
      NodeType node_type, Node a, Node b, Node c, Node d) {
    final Point3D pa = a.pos;
    final Point3D pb = b.pos;
    final Point3D pc = c.pos;
    final Point3D pd = d.pos;

    // Face normal: (c - b) x (d - b), in doubles.
    final double ux = pc.x - pb.x;
    final double uy = pc.y - pb.y;
    final double uz = pc.z - pb.z;
    final double vx = pd.x - pb.x;
    final double vy = pd.y - pb.y;
    final double vz = pd.z - pb.z;
    double nx = uy * vz - uz * vy;
    double ny = uz * vx - ux * vz;
    double nz = ux * vy - uy * vx;
    final double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
    nx /= len;
    ny /= len;
    nz /= len;

    // Signed distance from 'a' to the plane; reflect across it.
    final double dist = (pa.x - pb.x) * nx + (pa.y - pb.y) * ny + (pa.z - pb.z) * nz;
    final int x = (int) (pa.x - 2.0 * dist * nx);
    final int y = (int) (pa.y - 2.0 * dist * ny);
    final int z = (int) (pa.z - 2.0 * dist * nz);

    return addNode(node_manager, clazz, node_type, x, y, z);
  }
}
