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
 * Parametric 4-legged crawler built mostly from tetrahedra.
 *
 * <p>Body: two face-sharing tetrahedra forming a rigid torso.
 * Each leg: a tetrahedron hanging from the body (hip edge on the body,
 * knee out to the side, foot below). Leg edges are muscles; a trot gait
 * (diagonal legs in phase) lifts and swings the feet.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class CrawlerDemo {
  private CrawlerDemo() {
  }

  /** Edge length of body tetrahedra, in pixels. */
  public static int body_edge_px = 40;
  /** Leg length (hip to foot), in pixels. */
  public static int leg_length_px = 50;
  /** Leg splay (hip to knee sideways), in pixels. */
  public static int leg_splay_px = 25;
  /** Muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 10;
  /** Muscle period, in ticks. */
  public static int muscle_period_ticks = 60;
  /** Ground friction, 0-100. */
  public static int friction = 100;

  /** Phase offsets (ticks) for FL, FR, BL, BR legs. */
  public static int[] leg_phases = {0, 30, 30, 0};

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

    final Clazz clazz = node_manager.clazz_factory.getNew(0);
    final NodeType node_type = node_manager.node_type_factory.getNew();
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
    World.ground_friction = friction;

    final int x0 = x_px << Coords.shift;
    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    final int ground = (Coords.y_pixels << Coords.shift) - (10 << Coords.shift);

    // Body: two tetrahedra sharing a face, long axis along X.
    // Bottom face: rectangle of 4 nodes; top: 2 ridge nodes.
    final int bw = body_e; // body width (z)
    final int bl = body_e * 2; // body length (x)
    final int bh = (int) (body_e * 0.8); // body height (y)

    // Bottom face: rectangle of 4 nodes; top: 2 ridge nodes.
    // Y increases downward; ground is at high Y, so "up" is smaller Y.
    final int body_y = ground - (leg_length_px << Coords.shift) - (bh / 2);
    final Node b0 = addNode(node_manager, clazz, node_type, x0, body_y, 0);
    final Node b1 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, 0);
    final Node b2 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, bw);
    final Node b3 = addNode(node_manager, clazz, node_type, x0, body_y, bw);
    // Ridge (top).
    final Node t0 = addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, 0);
    final Node t1 = addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, bw);

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

    // Legs: FL=(b0), FR=(b3), BL=(b1), BR=(b2).
    // Each leg is a tetrahedron (hip, knee, foot) for structural stability,
    // but only the hip-foot edge is a muscle. The other edges are passive.
    final Node[] hips = {b0, b3, b1, b2};
    final int[] sides = {-1, 1, -1, 1}; // splay direction (z)
    for (int leg = 0; leg < 4; leg++) {
      final Node hip = hips[leg];
      final int side = sides[leg];
      final int phase = leg_phases[leg];

      // Knee: out to the side and slightly down from hip (toward ground = +Y).
      final Node knee = addNode(node_manager, clazz, node_type,
          hip.pos.x, hip.pos.y + (leg_length_px << Coords.shift) / 3,
          hip.pos.z + side * (leg_splay_px << Coords.shift));
      // Foot: forward (+X), out to the side, just above the ground.
      final int foot_forward_px = 40;
      final Node foot = addNode(node_manager, clazz, node_type,
          hip.pos.x + (foot_forward_px << Coords.shift),
          ground - (5 << Coords.shift),
          hip.pos.z + side * (leg_splay_px << Coords.shift));

      // Tetrahedron edges: hip-knee and knee-foot are passive cables.
      // Only hip-foot is a muscle.
      link(link_manager, leg_type, clazz, hip, knee, -1);
      link(link_manager, leg_type, clazz, knee, foot, -1);
      muscle(link_manager, leg_type, clazz, hip, foot, phase);
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
