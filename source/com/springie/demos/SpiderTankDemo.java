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
import com.springie.world.World;

/**
 * 6-legged spider tank with a tripod gait, built from tetrahedra.
 *
 * <p>Body: an armored hull (elongated box with ridge, like the 4-legged
 * crawler's body stretched for 3 leg pairs) plus a turret on top.
 * Each leg: a tetrahedron attached to the body via an edge (two nodes),
 * not a single corner. The leg has 4 nodes: hip1, hip2 (body edge),
 * knee, foot. Only the hip-foot edges are muscles. Feet splay
 * spider-style out to the sides.
 *
 * <p>Tripod gait: (LF, RM, LB) in phase; (RF, LM, RB) at half-period.
 * Three legs on the ground while three swing, alternating.
 */
public final class SpiderTankDemo {
  private SpiderTankDemo() {
  }

  /** Edge length of body tetrahedra, in pixels. */
  public static int body_edge_px = 60;
  /** Leg length (hip to foot), in pixels. */
  public static int leg_length_px = 30;
  /** Leg splay (hip to knee sideways), in pixels. */
  public static int leg_splay_px = 45;
  /** Foot forward offset, in pixels. */
  public static int foot_forward_px = 40;
  /** Muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 4;
  /** Muscle period, in ticks. */
  public static int muscle_period_ticks = 120;
  /** Ground friction, 0-100. */
  public static int friction = 100;

  /**
   * Phase offsets for LF, RF, LM, RM, LB, RB.
   * Tripod: (LF, RM, LB) at 0; (RF, LM, RB) at half-period.
   */
  public static int[] leg_phases = {0, 60, 60, 0, 0, 60};

  /**
   * Builds the spider tank centred at (x_px, 0) with feet on the ground.
   * Returns a body node for tracking.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    // Heavy body (harder to launch), light legs.
    final NodeType body_node_type = node_manager.node_type_factory.getNew();
    body_node_type.setMass(24);
    final int body_e = body_edge_px << Coords.shift;
    final LinkType body_type = link_manager.link_type_factory.getNew(body_e, 50);
    final LinkType leg_type = link_manager.link_type_factory.getNew(
        leg_length_px << Coords.shift, 50);

    // Muscles.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = friction;

    final int x0 = x_px << Coords.shift;
    final int ground = (Coords.y_pixels << Coords.shift) - (10 << Coords.shift);

    // Body: elongated hull, 3 segments. 6 bottom nodes (3 left, 3 right),
    // 2 ridge nodes on top. Y increases downward; up is smaller Y.
    final int bw = body_e; // width (z)
    final int seg = body_e; // segment length (x)
    final int bh = (int) (body_e * 0.8); // height (y)
    final int body_y = ground - (leg_length_px << Coords.shift) - (bh / 2);

    // Bottom nodes: left side (z=0), right side (z=bw), 3 segments.
    final Node[] left = new Node[3];
    final Node[] right = new Node[3];
    for (int s = 0; s < 3; s++) {
      left[s] = addNode(node_manager, clazz, body_node_type, x0 + s * seg, body_y, 0);
      right[s] = addNode(node_manager, clazz, body_node_type, x0 + s * seg, body_y, bw);
    }
    // Ridge nodes (top).
    final Node t0 = addNode(node_manager, clazz, body_node_type, x0 + seg / 2, body_y - bh, 0);
    final Node t1 = addNode(node_manager, clazz, body_node_type, x0 + seg / 2, body_y - bh, bw);
    final Node t2 = addNode(node_manager, clazz, body_node_type, x0 + seg * 3 / 2, body_y - bh, 0);
    final Node t3 = addNode(node_manager, clazz, body_node_type, x0 + seg * 3 / 2, body_y - bh, bw);

    // Hull frame.
    for (int s = 0; s < 3; s++) {
      link(link_manager, body_type, clazz, left[s], right[s], -1); // cross
      if (s < 2) {
        link(link_manager, body_type, clazz, left[s], left[s + 1], -1);
        link(link_manager, body_type, clazz, right[s], right[s + 1], -1);
        // Diagonal braces.
        link(link_manager, body_type, clazz, left[s], right[s + 1], -1);
        link(link_manager, body_type, clazz, right[s], left[s + 1], -1);
      }
    }
    // Ridge.
    link(link_manager, body_type, clazz, t0, t1, -1);
    link(link_manager, body_type, clazz, t2, t3, -1);
    link(link_manager, body_type, clazz, t0, t2, -1);
    link(link_manager, body_type, clazz, t1, t3, -1);
    // Connect ridge to hull.
    link(link_manager, body_type, clazz, left[0], t0, -1);
    link(link_manager, body_type, clazz, left[1], t0, -1);
    link(link_manager, body_type, clazz, left[1], t2, -1);
    link(link_manager, body_type, clazz, left[2], t2, -1);
    link(link_manager, body_type, clazz, right[0], t1, -1);
    link(link_manager, body_type, clazz, right[1], t1, -1);
    link(link_manager, body_type, clazz, right[1], t3, -1);
    link(link_manager, body_type, clazz, right[2], t3, -1);

    // Turret: small tetrahedron on top center (light, decorative).
    final Node turret_top = addNode(node_manager, clazz, node_type,
        x0 + seg, body_y - bh - (20 << Coords.shift), bw / 2);
    link(link_manager, body_type, clazz, turret_top, t0, -1);
    link(link_manager, body_type, clazz, turret_top, t1, -1);
    link(link_manager, body_type, clazz, turret_top, t2, -1);
    link(link_manager, body_type, clazz, turret_top, t3, -1);

    // Legs: each attaches to the body via an EDGE (two nodes), forming
    // a tetrahedron (hip1, hip2, knee, foot). LF/RF share the front cross
    // edge (left[0],right[0]); LM/RM the middle; LB/RB the back.
    // The old 3-node legs (hip,knee,foot) shared only the hip corner.
    final Node[][] hip_edges = {
        {left[0], right[0]}, {right[0], left[0]},
        {left[1], right[1]}, {right[1], left[1]},
        {left[2], right[2]}, {right[2], left[2]}};
    final int[] sides = {-1, 1, -1, 1, -1, 1};
    for (int leg = 0; leg < 6; leg++) {
      final Node hip1 = hip_edges[leg][0];
      final Node hip2 = hip_edges[leg][1];
      final int side = sides[leg];
      final int phase = leg_phases[leg];

      final int mid_x = (hip1.pos.x + hip2.pos.x) / 2;
      final int mid_y = (hip1.pos.y + hip2.pos.y) / 2;
      final int mid_z = (hip1.pos.z + hip2.pos.z) / 2;
      final Node knee = addNode(node_manager, clazz, node_type,
          mid_x, mid_y + (leg_length_px << Coords.shift) / 3,
          mid_z + side * (leg_splay_px << Coords.shift));
      final Node foot = addNode(node_manager, clazz, node_type,
          mid_x + (foot_forward_px << Coords.shift),
          ground - (5 << Coords.shift),
          mid_z + side * (leg_splay_px << Coords.shift));

      // Tetrahedron (hip1, hip2, knee, foot): all 6 edges.
      // hip1-hip2 is a body edge (already exists).
      link(link_manager, leg_type, clazz, hip1, knee, -1);
      link(link_manager, leg_type, clazz, hip2, knee, -1);
      link(link_manager, leg_type, clazz, knee, foot, -1);
      muscle(link_manager, leg_type, clazz, hip1, foot, phase);
      muscle(link_manager, leg_type, clazz, hip2, foot, phase);
    }

    return left[1];
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt, int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  private static void link(LinkManager lm, LinkType template, Clazz clazz, Node a, Node b, int phase) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    final int dist = (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
    final LinkType type = lm.link_type_factory.getNew(dist, template.elasticity);
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = dist;
    if (phase >= 0) {
      link.phase = phase;
      link.controller = new GlobalOscillatorController(Muscles.active_oscillator);
    }
  }

  private static void muscle(LinkManager lm, LinkType type, Clazz clazz, Node a, Node b, int phase) {
    link(lm, type, clazz, a, b, phase);
  }
}
