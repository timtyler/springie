// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.FrEnd;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.world.World;

/**
 * "Caterpillar 2": a row of six square-based pyramids, square faces down,
 * each sharing a base edge with its neighbours. Each pyramid is a rigid
 * unit: 4 base edges as struts, 2 crossed base diagonals as passive
 * cables (tension-only), and 4 slant edges to the apex as struts. The 6
 * apexes are chained by two parallel solid struts per gap (5 gaps).
 *
 * <p>Fully passive: no muscles anywhere. Tim 2026-09-18 experiment:
 * charge off, node-node collision detection on (overriding the usual
 * no-collisions rule), apex cables replaced by solid struts. The tuning
 * goal is structural soundness -- stable settling, no tip-over, low
 * strain -- as a platform for later actuation.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class Caterpillar2Demo {
  private Caterpillar2Demo() {
    // static-only
  }

  /** Number of pyramids in the row. */
  public static final int PYRAMIDS = 6;

  /** Base side length, in pixels. */
  public static int BASE_SIDE_PX = 80;

  /** Apex height above the base plane, in pixels. */
  public static int APEX_HEIGHT_PX = 35;

  /** Elasticity of the passive skeleton struts. */
  public static int skeleton_elasticity = 20;

  /** Elasticity of the passive base-diagonal cables. */
  public static int diagonal_elasticity = 20;

  /** Gravity strength. */
  public static int gravity_strength = 5;

  /** Ground friction, 0-100. */
  public static int friction = 50;

  /** Belly clearance: base nodes hang this high above the ground, px. */
  public static int base_clearance_px = 2;

  /** Ticks the build settles before the judge starts measuring. */
  public static int settle_ticks = 60;

  /** The apex nodes, set by buildAt (index = pyramid). */
  public static Node[] apexes = new Node[0];

  /** The base nodes, set by buildAt (index = column * 2 + row). */
  public static Node[] bases = new Node[0];

  /**
   * Builds the caterpillar, replacing whatever is there. The row starts
   * at x = x_px pixels, facing +x, bases resting just above the ground.
   */
  public static void buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();

    // Fully passive: no muscles. Tim 2026-09-18 experiment settings.
    node_manager.electrostatic.charge_active = false; // charge off
    FrEnd.check_collisions = true; // node-node collision detection on
    World.gravity_active = true;
    World.gravity_strength = gravity_strength;
    World.ground_friction = friction;
    World.global_temperature = 0;
    // Damping stabilizes the stiff skeleton.
    com.springie.elements.nodes.Node.viscocity = 2;

    // Base grid: 7 columns x 2 rows = 14 nodes; adjacent pyramids share
    // the 2 nodes of their common base edge.
    final int s = BASE_SIDE_PX << Coords.shift;
    final int x0 = x_px << Coords.shift;
    final int ground = Coords.y_pixels << Coords.shift;
    final int y_base = ground - (base_clearance_px << Coords.shift);
    final int zmid = (Coords.z_pixels / 2) << Coords.shift;
    final int half = s / 2;
    final Node[][] base = new Node[PYRAMIDS + 1][2];
    for (int col = 0; col <= PYRAMIDS; col++) {
      for (int row = 0; row < 2; row++) {
        base[col][row] = addNode(node_manager, clazz, node_type,
            x0 + col * s, y_base, zmid + (row == 0 ? -half : half));
      }
    }

    // Apexes: one per pyramid, above the base-square centre.
    final int h = APEX_HEIGHT_PX << Coords.shift;
    apexes = new Node[PYRAMIDS];
    for (int i = 0; i < PYRAMIDS; i++) {
      apexes[i] = addNode(node_manager, clazz, node_type,
          x0 + i * s + s / 2, y_base - h, zmid);
    }
    bases = new Node[(PYRAMIDS + 1) * 2];
    for (int col = 0; col <= PYRAMIDS; col++) {
      bases[col * 2] = base[col][0];
      bases[col * 2 + 1] = base[col][1];
    }

    // Pyramid structure: 4 base edges (struts), 2 crossed diagonals
    // (passive tension-only cables), 4 slants (struts). The shared base
    // edges are built once via the isLinked dedup.
    for (int i = 0; i < PYRAMIDS; i++) {
      final Node a = base[i][0];
      final Node b = base[i + 1][0];
      final Node c = base[i + 1][1];
      final Node d = base[i][1];
      strut(link_manager, clazz, a, b);
      strut(link_manager, clazz, b, c);
      strut(link_manager, clazz, c, d);
      strut(link_manager, clazz, d, a);
      diagonalCable(link_manager, clazz, a, c);
      diagonalCable(link_manager, clazz, b, d);
      final Node apex = apexes[i];
      strut(link_manager, clazz, a, apex);
      strut(link_manager, clazz, b, apex);
      strut(link_manager, clazz, c, apex);
      strut(link_manager, clazz, d, apex);
    }

    // Apex chain: two parallel solid struts per gap (fully passive --
    // the former muscle cables are now plain struts). No dedup here:
    // both parallel struts are wanted.
    for (int i = 0; i < PYRAMIDS - 1; i++) {
      apexStrut(link_manager, clazz, apexes[i], apexes[i + 1]);
      apexStrut(link_manager, clazz, apexes[i], apexes[i + 1]);
    }

    // Let the build find its stance.
    for (int t = 0; t < settle_ticks; t++) {
      node_manager.nodeAndLinkUpdate();
    }
  }

  /** Builds the caterpillar at the default position (top-left). */
  public static void build() {
    buildAt(100);
  }

  private static Node addNode(NodeManager node_manager, Clazz clazz,
      NodeType node_type, int x, int y, int z) {
    return node_manager.addNewAgent(new Point3D(x, y, z), clazz, node_type);
  }

  /** Passive strut with rest length matched to the geometry (no pre-stress). */
  private static void strut(LinkManager link_manager, Clazz clazz,
      Node n1, Node n2) {
    if (isLinked(link_manager, n1, n2)) {
      return;
    }
    makeStrut(link_manager, clazz, n1, n2);
  }

  /**
   * Solid apex-to-apex strut. Parallel pairs are allowed, so unlike
   * strut() this does no dedup.
   */
  private static void apexStrut(LinkManager link_manager, Clazz clazz,
      Node n1, Node n2) {
    makeStrut(link_manager, clazz, n1, n2);
  }

  private static void makeStrut(LinkManager link_manager, Clazz clazz,
      Node n1, Node n2) {
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(n1, n2), skeleton_elasticity);
    final Link link = link_manager.setLink(n1, n2, type, clazz);
    snapRestLength(link);
    link.adjusted_rest_length = type.length;
  }

  /**
   * Passive tension-only cable (muscles pull, never push): braces the
   * base square against shear without adding compression stiffness.
   */
  private static void diagonalCable(LinkManager link_manager, Clazz clazz,
      Node n1, Node n2) {
    if (isLinked(link_manager, n1, n2)) {
      return;
    }
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(n1, n2), diagonal_elasticity);
    type.compression = false;
    type.tension = true;
    final Link link = link_manager.setLink(n1, n2, type, clazz);
    snapRestLength(link);
    link.adjusted_rest_length = type.length;
  }

  /**
   * Snaps a link's rest length to what the engine actually measures for
   * the current node positions. The engine descalces each position delta
   * to whole pixels before the length is computed, so a rest length set
   * from full-precision geometry sits up to ~1px off from the measured
   * value: struts end up permanently pre-compressed and tension-only
   * cables permanently slack (no shear stiffness). Snapping removes the
   * systematic bias; the remaining +/-1px quantization is symmetric.
   */
  private static void snapRestLength(Link link) {
    link.type.length = link.getActualLength();
  }

  private static boolean isLinked(LinkManager link_manager, Node n1, Node n2) {
    final int n_o_l = link_manager.element.size();
    for (int i = n_o_l; --i >= 0;) {
      final Link existing = (Link) link_manager.element.get(i);
      if ((existing.nodes[0] == n1 && existing.nodes[1] == n2)
          || (existing.nodes[0] == n2 && existing.nodes[1] == n1)) {
        return true;
      }
    }
    return false;
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }
}
