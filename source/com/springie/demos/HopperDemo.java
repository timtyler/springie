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
import com.springie.muscles.Controller;
import com.springie.muscles.Muscles;
import com.springie.render.Coords;
import com.springie.FrEnd;
import com.springie.world.Grounding;
import com.springie.world.World;

/**
 * A hopping hopper built from tetrahedra -- a pogo-like creature tuned
 * for rhythm rather than max height.
 *
 * <p>Body: two face-sharing tetrahedra forming a compact torso (the same
 * rigid body as the crawler). Each leg is a wedge
 * pogo: a tetrahedron hung from a z-running hip edge (two body nodes)
 * down to two splayed feet -- hip edge + two feet = 4 nodes, all 6
 * edges present. There are NO knees: the folded-knee design could
 * collapse under pitch load and vault the body over. A wedge is a
 * rigid column that compresses axially like a pogo spring but cannot
 * fold, lock, or collapse. The feet sit wide in z and fore-aft in x,
 * giving a stable table-like base.
 *
 * <p>Drive: each hop cable is a tension-only muscle cable in parallel
 * with a passive strut diagonal (four cables total, two per leg), all
 * sharing one stance-gated gait controller (HopperGaitController).
 * While airborne the cables hold their build length (the legs stay
 * rigid trusses, never flailing); in stance each cable shortens in
 * proportion to its own leg's compression, gently replacing the
 * energy the lossy bounce dissipates, so the rhythm is the leg's
 * natural pogo frequency. A proportional attitude trim levels the
 * body (inert under the now-symmetric takeoff -- kept as a safety
 * net). The cables are pre-stressed 5px below their geometric length
 * so they never go slack. Muscle power only: no start kick.
 *
 * <p>The legs flex in the fore-aft plane about the z-running hip edge
 * (sideways-splayed knees buckle under load), and the leg springs are
 * kept soft to avoid the numerical resonance seen in the first
 * tetrahedral walkers.
 *
 * <p>buildAt returns the crown marker node (the top ridge node); the
 * feet are published in {@link #feet} for the judge's airborne check.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class HopperDemo {
  private HopperDemo() {
  }

  /** Body length (x), in pixels. */
  public static int body_length_px = 70;
  /** Body width (z), in pixels. */
  public static int body_width_px = 36;
  /** Body height (y), in pixels. */
  public static int body_height_px = 45;
  /** Hip height above the ground, in pixels. */
  public static int leg_drop_px = 25;
  /** Hind knee offset behind the hip (+x), in pixels. Deep crouch:
   * the knee must sit well clear of the hip-foot line so the cable's
   * pull folds the knee (storing spring energy) instead of locking
   * the triangle and fighting the struts head-on (which resonates
   * and explodes). */
  public static int knee_back_px = 45;
  /** Knee offset above the hip, in pixels (deep crouch -- see above). */
  public static int knee_up_px = 55;
  /**
   * Foot offset behind the hip (+x for hind legs), in pixels. The feet
   * sit under the knees, not the hips: the leg is a folded V, and the
   * launch pushes up along the tibia. Front/back stay symmetric so
   * the fore-aft components cancel and the hop is vertical.
   */
  public static int foot_back_px = 30;
  /** Foot z splay, in pixels. Wide for roll stability. */
  public static int leg_splay_px = 18;
  /** Knee z splay, in pixels. Follows the feet out for a wider leg. */
  public static int knee_splay_px = 14;
  /** Front knee offset ahead of the hip (-x), in pixels (deep crouch). */
  public static int front_knee_forward_px = 45;
  /** Front foot offset ahead of the hip (-x), in pixels (under the knee). */
  public static int front_foot_forward_px = 30;
  /**
   * Crouch yank baseline amplitude, 0-100%. The gait controller adapts
   * the yank around this baseline (see HopperGaitController.yank_base);
   * this sets the oscillator slot the UI reads.
   */
  public static int muscle_amplitude_pct = 2;
  /**
   * Tim's "not tipping over" rule: element indices of the dorsal "top"
   * node (ridge t0) and the "bottom" reference node (base b0). The top
   * must stay above the bottom for the whole run.
   */
  public static final int posture_top_index = 4;
  public static final int posture_bottom_index = 0;
  /** Min top-above-bottom separation (px) for the tip-over rule. */
  public static final int posture_min_separation_px = 15;
  /** Yank half-sine duration, in ticks. Snappy: fits inside a stance. */
  public static int muscle_period_ticks = 6;
  /** Ground friction, 0-100. */
  public static int friction = 0;
  /** Gravity strength for the hop tuning. */
  public static int gravity = 5;
  /** Elasticity of the body frame. */
  public static int body_elasticity = 30;
  /** Elasticity of the passive leg links. */
  public static int leg_elasticity = 20;
  /** Damping of the passive leg links (0-255). Higher damps the bounce. */
  public static int leg_damping = 91;
  /** Damping of the body frame (0-255). Damps the ridge flop that caused body-slam landings. */
  public static int body_damping = 95;
  /** Elasticity of the extensor muscles. */
  public static int muscle_elasticity = 25;
  /**
   * Cable pre-stress, in px. Each muscle cable's rest length is built
   * this far BELOW its geometric length, so the cable is always in
   * tension: a tension-only cable in parallel with a strut goes slack
   * the instant the leg compresses and can never pump the pogo. The
   * pre-stress pre-loads the leg springs instead. Narrow optimum.
   */
  public static double muscle_prestress_px = 5.0;

  /** The crown marker node (top of the body), set by buildAt. */
  public static Node marker;
  /** The four foot nodes (hind L/R, front L/R), set by buildAt. */
  public static Node[] feet = new Node[4];

  /**
   * Builds the hopper with its body starting at x_px, feet on the
   * ground. Returns the crown marker node.
   */
  public static Node buildAt(int x_px) {
    FrEnd.check_collisions = false;
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    final LinkType body_type =
        link_manager.link_type_factory.getNew(body_length_px << Coords.shift, body_elasticity);
    body_type.damping = body_damping;
    final LinkType leg_type =
        link_manager.link_type_factory.getNew(leg_drop_px << Coords.shift, leg_elasticity);
    leg_type.damping = leg_damping;
    final LinkType muscle_type =
        link_manager.link_type_factory.getNew(leg_drop_px << Coords.shift, muscle_elasticity);

    // The launch rhythm runs on oscillator slot 0 (kept for the UI's
    // muscle readout); all four hop cables share the stance-gated
    // controller for a level hop.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = gravity;
    World.ground_friction = friction;
    // Deterministic physics: thermal jitter makes identical builds
    // diverge per RNG seed (the wheel's lesson). The judge and the UI
    // both build through here, so both see the same motion.
    World.global_temperature = 0;

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
    link(link_manager, body_type, clazz, b0, t1, -1); // front face diagonal
    link(link_manager, body_type, clazz, b1, t1, -1); // rear face diagonal
    link(link_manager, body_type, clazz, b2, t0, -1); // cross brace
    link(link_manager, body_type, clazz, b3, t0, -1); // cross brace

    // Stance-gated hop controller (shared by all four cables): it
    // phases one half-sine yank to each touchdown and holds the legs
    // rigid in flight, so every touchdown is symmetric. Created before
    // the legs; the feet array is populated below.
    final Controller gait = new HopperGaitController(
        Muscles.active_oscillator, feet, ground, b0, b1, b2, b3);

    // Legs: two wedge pogos (front and hind), each a proper tetrahedron
    // (hip edge + two feet = 4 nodes, 6 edges). There are NO knees --
    // the folded-knee design could collapse under pitch load and vault
    // the body over (the flip that killed every earlier version). A
    // wedge is a rigid column: it compresses axially like a pogo
    // spring, but it cannot fold, lock, or collapse. The feet are
    // splayed outside the hips in z and fore-aft in x, giving a wide,
    // stable base like a table.
    final int foot_x_spread = 10;
    final int foot_z_splay = 26;

    // Front wedge on the front hip edge (b0, b3). Both feet sit 10px
    // BEHIND the hip edge (same x offset): if one foot is forward and
    // the other back, the diagonal hop cables pull asymmetrically
    // (left-back vs right-forward), yawing the body and pumping roll.
    final Node ff1 = addNode(node_manager, clazz, node_type,
        x0 - (foot_x_spread << Coords.shift), ground,
        zmid - (foot_z_splay << Coords.shift));
    final Node ff2 = addNode(node_manager, clazz, node_type,
        x0 - (foot_x_spread << Coords.shift), ground,
        zmid + (foot_z_splay << Coords.shift));
    feet[0] = ff1;
    feet[1] = ff2;
    // Tetrahedron (b0, b3, ff1, ff2): all 6 edges (b0-b3 exists).
    link(link_manager, leg_type, clazz, ff1, ff2, -1);
    link(link_manager, leg_type, clazz, b0, ff1, -1);
    link(link_manager, leg_type, clazz, b3, ff1, -1);
    link(link_manager, leg_type, clazz, b0, ff2, -1);
    link(link_manager, leg_type, clazz, b3, ff2, -1);
    // Hop cables in parallel with two diagonals: when they shorten
    // they work against the struts, loading the wedge springs.
    muscle(link_manager, muscle_type, clazz, b0, ff1, gait);
    muscle(link_manager, muscle_type, clazz, b3, ff2, gait);

    // Hind wedge on the rear hip edge (b1, b2). Both feet sit 10px
    // AHEAD of the hip edge, mirroring the front wedge's symmetry.
    final Node hf1 = addNode(node_manager, clazz, node_type,
        x0 + bl + (foot_x_spread << Coords.shift), ground,
        zmid - (foot_z_splay << Coords.shift));
    final Node hf2 = addNode(node_manager, clazz, node_type,
        x0 + bl + (foot_x_spread << Coords.shift), ground,
        zmid + (foot_z_splay << Coords.shift));
    feet[2] = hf1;
    feet[3] = hf2;
    // Tetrahedron (b1, b2, hf1, hf2): all 6 edges (b1-b2 exists).
    link(link_manager, leg_type, clazz, hf1, hf2, -1);
    link(link_manager, leg_type, clazz, b1, hf1, -1);
    link(link_manager, leg_type, clazz, b2, hf1, -1);
    link(link_manager, leg_type, clazz, b1, hf2, -1);
    link(link_manager, leg_type, clazz, b2, hf2, -1);
    muscle(link_manager, muscle_type, clazz, b1, hf1, gait);
    muscle(link_manager, muscle_type, clazz, b2, hf2, gait);

    // No mid-air starts: rest the whole model on the ground plane.
    Grounding.restOnGround(node_manager);

    marker = t0;
    return t0;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt,
      int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /** Passive link. Rest length = actual distance. */
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
    link.phase = phase;
  }

  private static void muscle(LinkManager lm, LinkType type, Clazz clazz,
      Node a, Node b, Controller controller) {
    final int n_o_l = lm.element.size();
    link(lm, type, clazz, a, b, -1);
    if (lm.element.size() != n_o_l + 1) {
      throw new IllegalStateException("muscle() must add exactly one link");
    }
    final Link link = (Link) lm.element.get(n_o_l);
    // Muscle cable: tension-only (compression members stay passive).
    link.type.compression = false;
    // Pre-stress: build the cable's rest length below its geometric
    // length so it never goes slack (see muscle_prestress_px).
    final int pre = (int) (muscle_prestress_px * (1 << Coords.shift));
    link.type.length -= pre;
    link.adjusted_rest_length = link.type.length;
    link.phase = 0;
    link.controller = controller;
  }
}
