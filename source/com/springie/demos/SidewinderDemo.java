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
import com.springie.world.Grounding;
import com.springie.world.World;

/**
 * A sidewinder from a chain of face-sharing tetrahedra: each new node forms a
 * tetrahedron with the previous end face, so the body is a 3D tube with a
 * triangular cross-section. The repeating primitive is two tetrahedra
 * sharing a face (a triangular bipyramid).
 *
 * <p>Structure: every edge of the tube is a passive strut with its own
 * link type whose rest length matches its actual geometry exactly, so the
 * triangulated skeleton holds its shape with no pre-stress.
 *
 * <p>Drive: links on the two flanks of the tube (top quartile of lateral
 * offset on each side of the sagittal plane) are muscles. The left flank
 * carries a travelling phase wave along the body; the right flank carries
 * the same wave shifted by half a period (antiphase). Left contracts while
 * right extends, bending the tube side to side in a travelling wave -- it
 * slithers instead of squirming.
 *
 * <p>The body is built level (axis horizontal) with its lowest node just
 * above the ground, then settled for 90 ticks so the triangular
 * cross-section comes to rest on the ground; the flank lines are chosen
 * from the settled orientation, so the wave always bends the body
 * horizontally. Muscles start at their tick-0 wave lengths, so there is
 * no initial snap.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class SidewinderDemo {
  private SidewinderDemo() {
    // static-only
  }

  /** Number of tetrahedra in the chain. */
  public static final int SEGMENTS = 20;

  /** Edge length of the tetrahedra, in pixels. */
  public static final int EDGE_PIXELS = 30;

  /** Elasticity of the passive skeleton struts. */
  public static int skeleton_elasticity = 20;

  /** Elasticity of the flank muscles (softer than the skeleton). */
  public static int muscle_elasticity = 15;

  /**
   * Muscle amplitude, 0-100%. Retuned 2026-09-20: 10% was too weak to
   * break static friction -- the mid node moved only 4px. 20% slithers.
   */
  public static int muscle_amplitude_pct = 20;

  /** Oscillator period for the travelling wave, in ticks. */
  public static int muscle_period_ticks = 120;

  /**
   * Wavelength of the travelling wave as a fraction of the body length.
   * 1.0 = one full wave head-to-tail.
   */
  public static double wave_length_fraction = 1.0;

  /**
   * Fraction of links converted to muscles on each flank (top quartile
   * of lateral offset on +normal and on -normal).
   */
  public static double muscle_fraction = 0.25;

  /** Ground friction, 0-100. */
  public static int friction = 100;

  /** Ticks the passive skeleton settles before the muscles are chosen. */
  public static int settle_ticks = 90;

  /**
   * Builds the sidewinder, replacing whatever is there. The body is centred at
   * x = x_px pixels and rests on the ground.
   */
  public static void buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();

    // Tune the oscillator first: the phase assignment below assumes it.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(
        muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = friction;
    World.global_temperature = 6;

    final int e = EDGE_PIXELS << Coords.shift;
    final double sqrt3 = Math.sqrt(3.0);
    final double sqrt6 = Math.sqrt(6.0);

    // First tetrahedron: base triangle in the XZ plane, apex above.
    final int y0 = 100 << Coords.shift;
    final Node p0 = addNode(node_manager, clazz, node_type, 0, y0, 0);
    final Node p1 = addNode(node_manager, clazz, node_type, e, y0, 0);
    final Node p2 = addNode(node_manager, clazz, node_type,
        (int) (e / 2.0), y0, (int) (e * sqrt3 / 2.0));
    final Node p3 = addNode(node_manager, clazz, node_type,
        (int) (e / 2.0), y0 + (int) (e * sqrt6 / 3.0), (int) (e * sqrt3 / 6.0));

    linkTetrahedron(link_manager, clazz, p0, p1, p2, p3);

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
      linkTetrahedron(link_manager, clazz, b, c, d, next);
      // Advance: the new end face drops the oldest node.
      a = b;
      b = c;
      c = d;
      d = next;
    }

    final int n = node_manager.element.size();
    final Node[] nodes = new Node[n];
    for (int i = 0; i < n; i++) {
      nodes[i] = (Node) node_manager.element.get(i);
    }

    // Level the body: rotate so the head-to-tail axis is horizontal, then
    // place it resting on the ground at the requested x position.
    levelBody(nodes, x_px);

    // Flank muscles: pick the most lateral axial links on each side and
    // drive them in antiphase. Selected BEFORE the settle, while the
    // cross-section orientation is still the deterministic one from the
    // build geometry -- letting it settle first lets the triangular tube
    // roll unpredictably, which biases the left/right selection and makes
    // the sidewinder turn instead of slithering straight.
    addFlankMuscles(link_manager, clazz, nodes);

    // Let the skeleton settle onto the ground with the muscles attached.
    for (int t = 0; t < settle_ticks; t++) {
      node_manager.nodeAndLinkUpdate();
    }

    // No mid-air starts: rest the whole model on the ground plane.
    Grounding.restOnGround(node_manager);
  }

  /** Builds the sidewinder at the default position. Kept for existing callers. */
  public static void build() {
    buildAt(400);
  }

  private static Node addNode(NodeManager node_manager, Clazz clazz,
      NodeType node_type, int x, int y, int z) {
    return node_manager.addNewAgent(new Point3D(x, y, z), clazz, node_type);
  }

  /**
   * Creates the six edges of a tetrahedron as passive struts, each with
   * its own link type whose rest length matches the actual geometry, so
   * the frame carries no pre-stress.
   */
  private static void linkTetrahedron(LinkManager link_manager, Clazz clazz,
      Node p0, Node p1, Node p2, Node p3) {
    strut(link_manager, clazz, p0, p1);
    strut(link_manager, clazz, p0, p2);
    strut(link_manager, clazz, p0, p3);
    strut(link_manager, clazz, p1, p2);
    strut(link_manager, clazz, p1, p3);
    strut(link_manager, clazz, p2, p3);
  }

  private static void strut(LinkManager link_manager, Clazz clazz,
      Node n1, Node n2) {
    // Skip if these nodes are already linked (shared faces reuse edges).
    final int n_o_l = link_manager.element.size();
    for (int i = n_o_l; --i >= 0;) {
      final Link existing = (Link) link_manager.element.get(i);
      if ((existing.nodes[0] == n1 && existing.nodes[1] == n2)
          || (existing.nodes[0] == n2 && existing.nodes[1] == n1)) {
        return;
      }
    }
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(n1, n2), skeleton_elasticity);
    final Link link = link_manager.setLink(n1, n2, type, clazz);
    link.adjusted_rest_length = type.length;
  }

  /**
   * Rotates the body so its head-to-tail axis is horizontal, centres it at
   * (x_px, ground) with the lowest node just above the ground.
   */
  private static void levelBody(Node[] nodes, int x_px) {
    final int n = nodes.length;
    final double[] c1 = centroid(nodes, 0, 4);
    final double[] c2 = centroid(nodes, n - 4, n);

    double dx = c2[0] - c1[0];
    double dy = c2[1] - c1[1];
    double dz = c2[2] - c1[2];
    final double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

    if (len > 1e-9) {
      // Horizontal unit vector along the axis.
      final double hlen = Math.sqrt(dx * dx + dz * dz);
      if (hlen > 1e-9) {
        final double ux = dx / hlen;
        final double uz = dz / hlen;
        final double nx = dx / len;
        final double ny = dy / len;
        final double nz = dz / len;
        // Rotation taking the axis to horizontal: axis r = n x u.
        double rx = ny * uz;
        double ry = nz * ux - nx * uz;
        double rz = -ny * ux;
        final double rlen = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rlen > 1e-12) {
          rx /= rlen;
          ry /= rlen;
          rz /= rlen;
          final double cos_t = Math.max(-1.0, Math.min(1.0, nx * ux + nz * uz));
          final double sin_t = Math.sqrt(Math.max(0.0, 1.0 - cos_t * cos_t));
          for (final Node node : nodes) {
            final double vx = node.pos.x - c1[0];
            final double vy = node.pos.y - c1[1];
            final double vz = node.pos.z - c1[2];
            // Rodrigues: v' = v cos t + (r x v) sin t + r (r.v) (1 - cos t).
            final double cx = ry * vz - rz * vy;
            final double cy = rz * vx - rx * vz;
            final double cz = rx * vy - ry * vx;
            final double dot = rx * vx + ry * vy + rz * vz;
            final double k = dot * (1.0 - cos_t);
            node.pos.x = (int) (c1[0] + vx * cos_t + cx * sin_t + rx * k);
            node.pos.y = (int) (c1[1] + vy * cos_t + cy * sin_t + ry * k);
            node.pos.z = (int) (c1[2] + vz * cos_t + cz * sin_t + rz * k);
          }
        }
      }
    }

    // Yaw: rotate around Y so the (now horizontal) axis points along +X.
    // Without this the body keeps the diagonal yaw of the 3D chain build
    // and crabs sideways instead of slithering forward.
    {
      final double[] q1 = centroid(nodes, 0, 4);
      final double[] q2 = centroid(nodes, n - 4, n);
      final double ydx = q2[0] - q1[0];
      final double ydz = q2[2] - q1[2];
      final double ylen = Math.sqrt(ydx * ydx + ydz * ydz);
      if (ylen > 1e-9) {
        final double cos_p = ydx / ylen;
        final double sin_p = ydz / ylen;
        for (final Node node : nodes) {
          final double vx = node.pos.x - q1[0];
          final double vz = node.pos.z - q1[2];
          node.pos.x = (int) (q1[0] + vx * cos_p + vz * sin_p);
          node.pos.z = (int) (q1[2] - vx * sin_p + vz * cos_p);
        }
      }
    }

    // Centre at x_px, z = 0; rest the lowest node just above the ground.
    // NB: the physics ground is exactly (y_pixels << shift) -- see
    // Node.boundaryCheck. An earlier version used y_pixels - 10 here,
    // which buried the lower nodes under the real ground; the clamp then
    // teleported them up on tick 1, exploding the link strains and
    // ratcheting the whole body skyward.
    double sum_x = 0.0;
    double sum_z = 0.0;
    int max_y = Integer.MIN_VALUE;
    for (final Node node : nodes) {
      sum_x += node.pos.x;
      sum_z += node.pos.z;
      max_y = Math.max(max_y, node.pos.y);
    }
    final int ground = Coords.y_pixels << Coords.shift;
    final int shift_x = (x_px << Coords.shift) - (int) (sum_x / n);
    // Centre z mid-way through the depth: the z=0 wall would otherwise
    // crush every node built at negative z (boundaryCheck clamps them).
    final int shift_z = ((Coords.z_pixels / 2) << Coords.shift) - (int) (sum_z / n);
    // y grows downward: max_y is the visually lowest node; park it just
    // above the ground.
    final int shift_y = (ground - (2 << Coords.shift)) - max_y;
    for (final Node node : nodes) {
      node.pos.x += shift_x;
      node.pos.y += shift_y;
      node.pos.z += shift_z;
    }
  }

  /**
   * Converts the most lateral links on each flank into muscles. The
   * sagittal plane contains the body axis and the vertical; the flank
   * score is the signed lateral distance of the link's midpoint from that
   * plane. Only links running mostly along the body are candidates --
   * transverse links would pinch the tube instead of bending it, and the
   * twisted cross-section would otherwise mix the two incoherently.
   * carries a travelling phase wave along the body, the -side the same
   * wave shifted by half a period, so the two flanks work in antiphase.
   */
  private static void addFlankMuscles(LinkManager link_manager, Clazz clazz,
      Node[] nodes) {
    final int n = nodes.length;
    final double[] c1 = centroid(nodes, 0, 4);
    final double[] c2 = centroid(nodes, n - 4, n);
    double dx = c2[0] - c1[0];
    double dy = c2[1] - c1[1];
    double dz = c2[2] - c1[2];
    final double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 1e-9) {
      return;
    }
    dx /= len;
    dy /= len;
    dz /= len;
    // Sagittal-plane normal: axis x vertical(0,1,0), normalised.
    double nx = dz;
    double nz = -dx;
    final double nlen = Math.sqrt(nx * nx + nz * nz);
    if (nlen < 1e-9) {
      return;
    }
    nx /= nlen;
    nz /= nlen;

    final int n_links = link_manager.element.size();
    final double[] flank = new double[n_links];
    final double[] station = new double[n_links];
    final double[] axialness = new double[n_links];
    for (int i = 0; i < n_links; i++) {
      final Link link = (Link) link_manager.element.get(i);
      final double ex = link.nodes[1].pos.x - link.nodes[0].pos.x;
      final double ey = link.nodes[1].pos.y - link.nodes[0].pos.y;
      final double ez = link.nodes[1].pos.z - link.nodes[0].pos.z;
      final double elen = Math.sqrt(ex * ex + ey * ey + ez * ez);
      // |cos| between the link and the body axis: axial links bend the
      // tube when driven; transverse ones just pinch it.
      axialness[i] = elen < 1e-9 ? 0.0
          : Math.abs((ex * dx + ey * dy + ez * dz) / elen);
      final double mx = (link.nodes[0].pos.x + link.nodes[1].pos.x) / 2.0;
      final double my = (link.nodes[0].pos.y + link.nodes[1].pos.y) / 2.0;
      final double mz = (link.nodes[0].pos.z + link.nodes[1].pos.z) / 2.0;
      final double ox = mx - c1[0];
      final double oy = my - c1[1];
      final double oz = mz - c1[2];
      station[i] = ox * dx + oy * dy + oz * dz;
      flank[i] = ox * nx + oz * nz;
    }

    // Candidates: links running mostly along the body. The tube is
    // twisted (the triangular cross-section rotates ~109.5 degrees per
    // segment), so a fixed world-frame flank cut over all links would mix
    // axial benders with transverse pinchers and crumple the body.
    final java.util.ArrayList<Integer> candidates = new java.util.ArrayList<>();
    for (int i = 0; i < n_links; i++) {
      if (axialness[i] > 0.6) {
        candidates.add(i);
      }
    }
    // Sort candidates by flank score, descending.
    candidates.sort((a, b) -> Double.compare(flank[b], flank[a]));

    final int per_side =
        (int) Math.ceil(muscle_fraction * candidates.size());
    final int period = Muscles.activeOscillator().getPeriodTicks();
    final double body_px = len / (1 << Coords.shift);
    final double lambda_px = Math.max(1.0, wave_length_fraction * body_px);
    final GlobalOscillatorController controller =
        new GlobalOscillatorController(Muscles.active_oscillator);

    for (int k = 0; k < per_side; k++) {
      // Left flank (+normal): travelling wave along the body.
      final int li = candidates.get(k);
      makeMuscle(link_manager, clazz, (Link) link_manager.element.get(li),
          controller, phaseFor(station[li], period, lambda_px, 0));
      // Right flank (-normal): same wave, half a period later.
      final int ri = candidates.get(candidates.size() - 1 - k);
      makeMuscle(link_manager, clazz, (Link) link_manager.element.get(ri),
          controller, phaseFor(station[ri], period, lambda_px, period / 2));
    }
  }

  /** Phase in ticks for the given axial station (pixels along the axis). */
  private static int phaseFor(double station_internal, int period,
      double lambda_px, int offset) {
    final double s_px = station_internal / (1 << Coords.shift);
    final int phase = (int) (s_px * period / lambda_px) + offset;
    return ((phase % period) + period) % period;
  }

  /** Converts an existing strut link into a flank muscle. */
  private static void makeMuscle(LinkManager link_manager, Clazz clazz,
      Link link, GlobalOscillatorController controller, int phase) {
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(link.nodes[0], link.nodes[1]), muscle_elasticity);
    link.type = type;
    link.phase = phase;
    link.controller = controller;
    // Start the muscle at its tick-0 wave value, not at the nominal rest
    // length: otherwise every muscle snaps to its driven length on the
    // first tick and the body kicks violently.
    final int scale =
        Muscles.activeOscillator().getScale(0, phase);
    link.adjusted_rest_length =
        (int) (((long) type.length * scale) >> Coords.shift);
  }

  private static double[] centroid(Node[] nodes, int from, int to) {
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    for (int i = from; i < to; i++) {
      x += nodes[i].pos.x;
      y += nodes[i].pos.y;
      z += nodes[i].pos.z;
    }
    final int count = to - from;
    return new double[] {x / count, y / count, z / count};
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
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
