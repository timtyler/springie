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
 * Parametric 4-legged crawler built from tetrahedra.
 *
 * <p>Body: two face-sharing tetrahedra forming a rigid torso.
 * Each leg: a tetrahedron attached to the body via an edge (two nodes),
 * not a single corner. The leg has 4 nodes: hip1, hip2 (body edge),
 * knee, foot. All 6 edges exist; only the hip-foot edges are muscles.
 * A trot gait (diagonal legs in phase) lifts and swings the feet.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class CrawlerDemo {
  private CrawlerDemo() {
  }

  /** Edge length of body tetrahedra, in pixels. */
  public static int body_edge_px = 60;
  /** Leg length (hip to foot), in pixels. */
  public static int leg_length_px = 30;
  /** Leg splay (hip to knee sideways), in pixels. */
  public static int leg_splay_px = 8;
  /** Knee forward offset (+x), in pixels. */
  public static int knee_forward_px = 12;
  /** Foot forward offset (+x), in pixels. */
  public static int foot_forward_px = 25;
  /** Muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 5;
  /** Muscle period, in ticks. */
  public static int muscle_period_ticks = 120;
  /** Ground friction, 0-100. */
  public static int friction = 100;

  /** Phase offsets (ticks) for FL, FR, BL, BR legs. */
  public static int[] leg_phases = {0, 60, 60, 0};

  /**
   * Builds the crawler centred at (x_px, 0) with feet on the ground.
   * Returns the body centre node for tracking.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    final int body_e = body_edge_px << Coords.shift;
    final LinkType body_type = link_manager.link_type_factory.getNew(body_e, 50);
    final LinkType leg_type = link_manager.link_type_factory.getNew(
        leg_length_px << Coords.shift, 30);

    // Muscles.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = 5; // Strong gravity to keep it grounded.
    World.ground_friction = friction;

    final int x0 = x_px << Coords.shift;
    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    // Feet rest ~2px above the true wall; the whole build is shifted
    // +40px in z so no node starts against the z = 0 wall.
    final int ground = (Coords.y_pixels << Coords.shift) - (2 << Coords.shift);
    final int zo = 40 << Coords.shift;

    // Body: two tetrahedra sharing a face, long axis along X.
    // Bottom face: rectangle of 4 nodes; top: 2 ridge nodes.
    final int bw = body_e; // body width (z)
    final int bl = body_e * 2; // body length (x)
    final int bh = (int) (body_e * 0.8); // body height (y)

    // Bottom face: rectangle of 4 nodes; top: 2 ridge nodes.
    // Y increases downward; ground is at high Y, so "up" is smaller Y.
    final int body_y = ground - (leg_length_px << Coords.shift) - (bh / 2);
    final Node b0 = addNode(node_manager, clazz, node_type, x0, body_y, zo);
    final Node b1 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, zo);
    final Node b2 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, bw + zo);
    final Node b3 = addNode(node_manager, clazz, node_type, x0, body_y, bw + zo);
    // Ridge (top).
    final Node t0 = addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, zo);
    final Node t1 = addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, bw + zo);

    // Body edges (rigid frame, no muscles).
    link(link_manager, body_type, clazz, b0, b1, -1);
    link(link_manager, body_type, clazz, b1, b2, -1);
    link(link_manager, body_type, clazz, b2, b3, -1);
    link(link_manager, body_type, clazz, b3, b0, -1);
    link(link_manager, body_type, clazz, b0, t0, -1);
    link(link_manager, body_type, clazz, b1, t0, -1);
    link(link_manager, body_type, clazz, b2, t1, -1);
    link(link_manager, body_type, clazz, b3, t1, -1);
    link(link_manager, body_type, clazz, t0, t1, -1);
    link(link_manager, body_type, clazz, b0, b2, -1); // diagonal brace
    link(link_manager, body_type, clazz, b1, b3, -1); // diagonal brace

    // Legs: each attaches to the body via an EDGE (two nodes), forming
    // a tetrahedron (hip1, hip2, knee, foot). This fixes the "triangles
    // at one corner" problem: the old design had 3-node legs (hip, knee,
    // foot) sharing only the hip node with the body, which flapped.
    // FL: edge (b0,b3), FR: edge (b3,b2), BL: edge (b1,b0), BR: edge (b2,b1).
    final Node[][] hip_edges = {{b0, b3}, {b3, b2}, {b1, b0}, {b2, b1}};
    final int[] sides = {-1, 1, -1, 1}; // splay direction (z)
    for (int leg = 0; leg < 4; leg++) {
      final Node hip1 = hip_edges[leg][0];
      final Node hip2 = hip_edges[leg][1];
      final int side = sides[leg];
      final int phase = leg_phases[leg];

      // Knee: forward (+x) and halfway down; foot further forward at the
      // ground; small z splay clears the hull. The leg flexes in the
      // fore-aft plane about the z-running hip edge. (Sideways-splayed
      // knees form a shallow inverted-V that buckles under load.)
      final int mid_x = (hip1.pos.x + hip2.pos.x) / 2;
      final int mid_y = (hip1.pos.y + hip2.pos.y) / 2;
      final int mid_z = (hip1.pos.z + hip2.pos.z) / 2;
      final Node knee = addNode(node_manager, clazz, node_type,
          mid_x + (knee_forward_px << Coords.shift),
          mid_y + (leg_length_px << Coords.shift) / 2,
          mid_z + side * (leg_splay_px << Coords.shift));
      // Foot: forward (+X), just above the ground.
      final Node foot = addNode(node_manager, clazz, node_type,
          mid_x + (foot_forward_px << Coords.shift),
          ground,
          mid_z + side * (leg_splay_px << Coords.shift));

      // Tetrahedron (hip1, hip2, knee, foot): all 6 edges.
      // hip1-hip2 is a body edge (already exists).
      link(link_manager, leg_type, clazz, hip1, knee, -1);
      link(link_manager, leg_type, clazz, hip2, knee, -1);
      link(link_manager, leg_type, clazz, knee, foot, -1);
      // Hip-foot edges are muscles (drive the gait).
      muscle(link_manager, leg_type, clazz, hip1, foot, phase);
      muscle(link_manager, leg_type, clazz, hip2, foot, phase);
    }

    // Return a body node for tracking.
    return b0;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt, int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /** Passive link (phase < 0 means no muscle). Rest length = actual distance. */
  private static void link(LinkManager lm, LinkType template, Clazz clazz, Node a, Node b, int phase) {
    // Each link gets its own type so the muscle controller's base length
    // (link.type.length) matches this link's actual geometry.
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    final int dist = (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
    final LinkType type = lm.link_type_factory.getNew(dist, template.elasticity);
    type.damping = template.damping;
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
