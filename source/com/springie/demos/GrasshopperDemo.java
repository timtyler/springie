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
 * A high-jump grasshopper built from tetrahedra.
 *
 * <p>Body: two face-sharing tetrahedra forming a compact torso (the same
 * rigid body as the crawler). Each leg is a tetrahedron attached to the
 * body via a z-running bottom edge (two nodes): hip edge, knee, foot --
 * all 6 edges present. The legs are built folded in a deep crouch,
 * grasshopper-style: the knee sits up and away from the hip, the foot
 * rests on the ground directly under the hip. The two hip-foot edges of
 * every leg are extensor muscles; when they lengthen in unison they
 * straighten the legs, driving the feet into the ground and launching
 * the body upward. All four legs push so the launch is level -- a
 * rear-only push pitches the body nose-down and it face-plants.
 *
 * <p>The legs flex in the fore-aft plane about the z-running hip edge
 * (sideways-splayed knees buckle under load), and the leg springs are
 * kept soft to avoid the numerical resonance seen in the first
 * tetrahedral walkers. Muscle power only: no start kick.
 *
 * <p>buildAt returns the crown marker node (the top ridge node); the
 * feet are published in {@link #feet} for the judge's airborne check.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class GrasshopperDemo {
  private GrasshopperDemo() {
  }

  /** Body length (x), in pixels. */
  public static int body_length_px = 70;
  /** Body width (z), in pixels. */
  public static int body_width_px = 36;
  /** Body height (y), in pixels. */
  public static int body_height_px = 45;
  /** Hip height above the ground, in pixels. */
  public static int leg_drop_px = 55;
  /** Hind knee offset behind the hip (+x), in pixels. */
  public static int knee_back_px = 22;
  /** Knee offset above the hip, in pixels. */
  public static int knee_up_px = 38;
  /**
   * Foot offset behind the hip (+x for hind legs), in pixels. Kept near
   * zero so the extensor push is vertical: feet behind the hips pitch
   * the body nose-down and it face-plants.
   */
  public static int foot_back_px = 2;
  /** Foot z splay, in pixels. */
  public static int leg_splay_px = 10;
  /** Knee z splay, in pixels. */
  public static int knee_splay_px = 6;
  /** Front knee offset ahead of the hip (-x), in pixels. */
  public static int front_knee_forward_px = 22;
  /** Front foot offset ahead of the hip (-x), in pixels (near zero: vertical push). */
  public static int front_foot_forward_px = 2;
  /** Extensor muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 67;
  /** Extensor muscle period, in ticks. */
  public static int muscle_period_ticks = 62;
  /** Ground friction, 0-100. */
  public static int friction = 100;
  /** Elasticity of the body frame. */
  public static int body_elasticity = 50;
  /** Elasticity of the passive leg links. */
  public static int leg_elasticity = 30;
  /** Elasticity of the extensor muscles. */
  public static int muscle_elasticity = 25;

  /** The crown marker node (top of the body), set by buildAt. */
  public static Node marker;
  /** The four foot nodes (hind L/R, front L/R), set by buildAt. */
  public static Node[] feet = new Node[4];

  /**
   * Builds the grasshopper with its body starting at x_px, feet on the
   * ground. Returns the crown marker node.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    final LinkType body_type =
        link_manager.link_type_factory.getNew(body_length_px << Coords.shift, body_elasticity);
    final LinkType leg_type =
        link_manager.link_type_factory.getNew(leg_drop_px << Coords.shift, leg_elasticity);
    final LinkType muscle_type =
        link_manager.link_type_factory.getNew(leg_drop_px << Coords.shift, muscle_elasticity);

    // The launch extensors run on oscillator slot 0, all in phase.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = friction;

    final int x0 = x_px << Coords.shift;
    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    // Feet rest ~2px above the true wall; the whole build sits at
    // z >= 40 so no node starts against the z = 0 wall.
    final int ground = (Coords.y_pixels << Coords.shift) - (2 << Coords.shift);
    final int zo = 40 << Coords.shift;

    final int bl = body_length_px << Coords.shift;
    final int bw = body_width_px << Coords.shift;
    final int bh = body_height_px << Coords.shift;
    final int zmid = zo + bw / 2;

    // Body: bottom rectangle of 4 nodes plus 2 ridge nodes (top).
    // Y increases downward; ground is at high Y, so "up" is smaller Y.
    final int body_y = ground - (leg_drop_px << Coords.shift) - bh / 2;
    final Node b0 = addNode(node_manager, clazz, node_type, x0, body_y, zo);
    final Node b1 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, zo);
    final Node b2 = addNode(node_manager, clazz, node_type, x0 + bl, body_y, zo + bw);
    final Node b3 = addNode(node_manager, clazz, node_type, x0, body_y, zo + bw);
    final Node t0 =
        addNode(node_manager, clazz, node_type, x0 + bl / 2, body_y - bh, zo);
    final Node t1 = addNode(node_manager, clazz, node_type,
        x0 + bl / 2, body_y - bh, zo + bw);

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

    final int[] sides = {-1, 1};

    // Hind legs on the rear edge (b1, b2): folded crouch, knee up and
    // behind the hip, foot on the ground directly under the hip. The leg
    // flexes in the fore-aft plane about the z-running hip edge.
    for (int s = 0; s < 2; s++) {
      final int side = sides[s];
      final int hip_x = x0 + bl;
      final int hip_y = body_y;
      final Node knee = addNode(node_manager, clazz, node_type,
          hip_x + (knee_back_px << Coords.shift),
          hip_y - (knee_up_px << Coords.shift),
          zmid + side * (knee_splay_px << Coords.shift));
      final Node foot = addNode(node_manager, clazz, node_type,
          hip_x + (foot_back_px << Coords.shift),
          ground,
          zmid + side * (leg_splay_px << Coords.shift));
      feet[s] = foot;

      // Tetrahedron (b1, b2, knee, foot): all 6 edges.
      // b1-b2 is a body edge (already exists).
      link(link_manager, leg_type, clazz, b1, knee, -1);
      link(link_manager, leg_type, clazz, b2, knee, -1);
      link(link_manager, leg_type, clazz, knee, foot, -1);
      // Hip-foot edges are the launch extensors (in-unison phase 0).
      muscle(link_manager, muscle_type, clazz, b1, foot, 0);
      muscle(link_manager, muscle_type, clazz, b2, foot, 0);
    }

    // Front legs on the front edge (b0, b3): mirrors of the hind legs
    // (knee forward-up, foot under the hip). Their extensors fire with
    // the hind ones so the launch is level: a rear-only push pitches
    // the body nose-down.
    for (int s = 0; s < 2; s++) {
      final int side = sides[s];
      final int hip_x = x0;
      final int hip_y = body_y;
      final Node knee = addNode(node_manager, clazz, node_type,
          hip_x - (front_knee_forward_px << Coords.shift),
          hip_y - (knee_up_px << Coords.shift),
          zmid + side * (knee_splay_px << Coords.shift));
      final Node foot = addNode(node_manager, clazz, node_type,
          hip_x - (front_foot_forward_px << Coords.shift),
          ground,
          zmid + side * (leg_splay_px << Coords.shift));
      feet[2 + s] = foot;

      // Tetrahedron (b0, b3, knee, foot): all 6 edges.
      // b0-b3 is a body edge (already exists).
      link(link_manager, leg_type, clazz, b0, knee, -1);
      link(link_manager, leg_type, clazz, b3, knee, -1);
      link(link_manager, leg_type, clazz, knee, foot, -1);
      // Hip-foot edges are the launch extensors (in-unison phase 0).
      muscle(link_manager, muscle_type, clazz, b0, foot, 0);
      muscle(link_manager, muscle_type, clazz, b3, foot, 0);
    }

    marker = t0;
    return t0;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt,
      int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /** Passive link (phase < 0 means no muscle). Rest length = actual distance. */
  private static void link(LinkManager lm, LinkType template, Clazz clazz,
      Node a, Node b, int phase) {
    // Each link gets its own type so the muscle controller's base length
    // (link.type.length) matches this link's actual geometry.
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    final int dist =
        (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
    final LinkType type = lm.link_type_factory.getNew(dist, template.elasticity);
    type.damping = template.damping;
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = dist;
    if (phase >= 0) {
      link.phase = phase;
      link.controller = new GlobalOscillatorController(Muscles.active_oscillator);
    }
  }

  private static void muscle(LinkManager lm, LinkType type, Clazz clazz,
      Node a, Node b, int phase) {
    link(lm, type, clazz, a, b, phase);
  }
}
