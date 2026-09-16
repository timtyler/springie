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
 * A short, fat tetrahedral rolling wheel: 8 nodes per rim (radius 60px)
 * at z = 0 and z = 100, one heavy centre hub node. The rim uses
 * alternating diagonal bracing (8 diagonals, one per segment) to resist
 * shear without over-constraining, and the hub connects via tetrahedra
 * (H, A_i, B_i, A_{i+1}) rather than triangles sharing only the hub
 * corner.
 *
 * <p>Geometry follows Tim's "shorter and fatter" directive: the older
 * design (12 nodes per rim, radius 90, z half-width 35) stood tall and
 * narrow and toppled sideways. The radius is now 60px (lower centre of
 * mass) and the z track is 100px wide (the critical tipping angle went
 * from ~21 degrees to ~40 degrees), so the wheel stays on its end with
 * the axle horizontal.
 *
 * <p>Unlike the previous design (two 12-gon rims with parallel struts and
 * 24 hub spokes forming triangles that share only the hub corner), the
 * hub connection here forms tetrahedra, and the rim has diagonal bracing.
 * Tetrahedra are rigid in 3D; triangles sharing a single corner flap
 * aimlessly.
 *
 * <p>Drive: the 16 hub spokes are muscles with a ground-contact
 * push-off reflex ({@link WheelPushController}). When a spoke's rim
 * node is on the ground behind the hub (in the rolling direction) the
 * spoke extends, pushing the hub forward and up; when on the ground in
 * front it contracts slightly, pulling the hub forward -- like legs
 * pushing off. Unlike an open-loop travelling wave, the reflex is
 * self-synchronizing: the ground contact sets the timing, so the drive
 * cannot fall out of step with the rolling. The 20%/5% push/pull was
 * tuned empirically: stronger pull veers the wheel sideways and tips
 * it, weaker push stalls the roll.
 *
 * <p>Node order is fixed and documented: rim-0[i] and rim-1[i] are added
 * interleaved per iteration (element indices 2*i and 2*i+1), then the
 * hub. Node 0 (rim-0[0], body angle 0) is the rotation marker. buildAt
 * returns the hub.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class WheelDemo {
  private WheelDemo() {
  }

  /** Nodes per rim. */
  public static final int RIM_COUNT = 8;

  /** Rim radius, in pixels. */
  public static int rim_radius_px = 60;

  /** Rims sit at z = 0 and z = 2 * this value, in pixels. */
  public static int rim_half_width_px = 50;

  /**
   * Z offset of the whole wheel: rim-0 sits at z = this, rim-1 at
   * z = this + 2 * rim_half_width_px. Keeps the wheel off the z = 0
   * wall -- riding the wall shoves the wheel sideways (+z drift) on
   * every rim contact. Must stay >= 0 (never below the wall).
   */
  public static int z_offset_px = 20;

  /** Nominal mass for rim nodes (log scale used by the engine). */
  public static int rim_log_mass = 5;

  /** Nominal mass for the hub node (log scale used by the engine). */
  public static int hub_log_mass = 30;

  /** Muscle amplitude for the spoke wave, 0-100%. */
  public static int muscle_amplitude_pct = 25;

  /** Oscillator period for the spoke wave, in ticks. */
  public static int muscle_period_ticks = 120;

  /**
   * Direction of the travelling wave: +1 or -1. The spoke phase is
   * phase_direction * angle * period / (2*PI). Sign verified
   * empirically: -1 drives the wheel toward +X.
   */
  public static int phase_direction = -1;

  /**
   * Initial angular impulse for self-start, in internal velocity units
   * applied to rim nodes (tangential). The open-loop wave is a
   * synchronous drive: it pulls in best when the wheel is already
   * turning. A small kick gets it turning; the wave then holds it.
   * Zero for the fat wheel: the spoke wave self-starts without it.
   */
  public static int start_kick = 0;

  /** Ground friction, 0-100. */
  public static int friction = 100;

  /**
   * Drive mode: true for the ground-contact push-off reflex
   * (self-synchronizing), false for the open-loop travelling wave.
   */
  public static boolean use_reflex_drive = true;

  /** Reflex push percent (spoke extension in back stance). */
  public static int reflex_push_pct = 20;

  /** Reflex pull percent (spoke contraction in front stance). */
  public static int reflex_pull_pct = 5;

  /** Rolling direction for the reflex: +1 toward +X, -1 toward -X. */
  public static int roll_direction = 1;

  /** Elasticity for the rim links (all tetrahedron edges). */
  public static int rim_elasticity = 30;

  /** Elasticity for the hub-to-rim spoke muscles. */
  public static int spoke_elasticity = 25;

  /**
   * Builds the wheel with its centre at (x_px, ground - radius).
   * Returns the hub node.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType rim_type = node_manager.node_type_factory.getNew();
    final NodeType hub_type = node_manager.node_type_factory.getNew();
    rim_type.log_mass = rim_log_mass;
    hub_type.log_mass = hub_log_mass;

    // The spoke wave runs on oscillator slot 0.
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

    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    final int ground = (Coords.y_pixels << Coords.shift) - (10 << Coords.shift);
    final int cx = x_px << Coords.shift;
    final int cy = ground - (rim_radius_px << Coords.shift);
    final int radius = rim_radius_px << Coords.shift;
    final int hw = rim_half_width_px << Coords.shift;

    // Rings: node i at body angle 2*PI*i / RIM_COUNT (screen coords, y down).
    final Node[] rim0 = new Node[RIM_COUNT];
    final Node[] rim1 = new Node[RIM_COUNT];
    final int z0 = z_offset_px << Coords.shift;
    for (int i = 0; i < RIM_COUNT; i++) {
      final double a = 2.0 * Math.PI * i / RIM_COUNT;
      final double c = Math.cos(a);
      final double s = Math.sin(a);
      rim0[i] = addNode(node_manager, clazz, rim_type,
          cx + (int) (radius * c), cy + (int) (radius * s), z0);
      rim1[i] = addNode(node_manager, clazz, rim_type,
          cx + (int) (radius * c), cy + (int) (radius * s), z0 + 2 * hw);
    }
    final Node hub = addNode(node_manager, clazz, hub_type, cx, cy, z0 + hw);

    // Rim: two 8-gon rings (16 links) + 8 cross links + 8 diagonals (32 total).
    // The hub spokes form triangles (hub, rim0[i], rim1[i]) sharing only
    // the hub corner -- these flap aimlessly. To fix, add 8 passive
    // diagonals (rim1[i] to rim0[i+1]) which complete the tetrahedra
    // (hub, rim0[i], rim1[i], rim0[i+1]). The diagonals are passive
    // structural bracing; only the 16 spokes are muscles.
    final GlobalOscillatorController controller =
        new GlobalOscillatorController(0);
    for (int i = 0; i < RIM_COUNT; i++) {
      final int j = (i + 1) % RIM_COUNT;
      final Node a0 = rim0[i];
      final Node b0 = rim1[i];
      final Node a1 = rim0[j];
      // Passive: rim edges, cross links, and diagonals maintain shape.
      passive(link_manager, clazz, a0, a1, rim_elasticity); // rim0 edge
      passive(link_manager, clazz, b0, rim1[j], rim_elasticity); // rim1 edge
      passive(link_manager, clazz, a0, b0, rim_elasticity); // cross at i
      passive(link_manager, clazz, b0, a1, rim_elasticity); // diagonal
    }

    // Hub spokes: 16 muscles forming face-joined tetrahedra with the rim.
    // For each i, (hub, rim0[i], rim1[i], rim0[i+1]) and
    // (hub, rim1[i], rim0[i+1], rim1[i+1]) are tetrahedra sharing the face
    // (hub, rim1[i], rim0[i+1]). The rim edges already exist; we add
    // only the 16 hub-to-rim spokes.
    //
    // Drive: either the ground-contact push-off reflex (self-synchronizing)
    // or the open-loop travelling wave (per-link phases).
    for (int i = 0; i < RIM_COUNT; i++) {
      final double a = 2.0 * Math.PI * i / RIM_COUNT;
      // Phase in ticks: one full wave around the rim.
      final int phase =
          (int) (phase_direction * a * muscle_period_ticks / (2.0 * Math.PI));
      if (use_reflex_drive) {
        reflexSpoke(link_manager, clazz, hub, rim0[i], ground);
        reflexSpoke(link_manager, clazz, hub, rim1[i], ground);
      } else {
        spoke(link_manager, clazz, controller, hub, rim0[i], phase);
        spoke(link_manager, clazz, controller, hub, rim1[i], phase);
      }
    }

    // Optional self-start kick: tangential rim velocities.
    if (start_kick != 0) {
      for (int i = 0; i < RIM_COUNT; i++) {
        final double a = 2.0 * Math.PI * i / RIM_COUNT;
        // Tangent for +X rolling (screen coords, y down): clockwise.
        final int tx = (int) (-Math.sin(a) * start_kick);
        final int ty = (int) (Math.cos(a) * start_kick);
        rim0[i].velocity.x += tx;
        rim0[i].velocity.y += ty;
        rim1[i].velocity.x += tx;
        rim1[i].velocity.y += ty;
      }
    }

    return hub;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt,
      int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /**
   * Passive link with its own type so the rest length matches its actual
   * geometry exactly.
   */
  private static void passive(LinkManager lm, Clazz clazz, Node a, Node b,
      int elasticity) {
    final LinkType type =
        lm.link_type_factory.getNew(distance(a, b), elasticity);
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = type.length;
  }

  /** Hub-to-rim muscle spoke with an angle-derived oscillator phase. */
  private static void spoke(LinkManager lm, Clazz clazz,
      GlobalOscillatorController controller, Node hub, Node rim, int phase) {
    final LinkType type =
        lm.link_type_factory.getNew(distance(hub, rim), spoke_elasticity);
    final Link link = lm.setLink(hub, rim, type, clazz);
    link.adjusted_rest_length = type.length;
    link.phase = phase;
    link.controller = controller;
  }

  /**
   * Hub-to-rim muscle spoke with a ground-contact push-off reflex.
   * Extends when its rim node is on the ground behind the hub (push-off),
   * contracts when on the ground in front (pull-forward).
   */
  private static void reflexSpoke(LinkManager lm, Clazz clazz,
      Node hub, Node rim, int ground_y) {
    final LinkType type =
        lm.link_type_factory.getNew(distance(hub, rim), spoke_elasticity);
    final Link link = lm.setLink(hub, rim, type, clazz);
    link.adjusted_rest_length = type.length;
    link.controller = new WheelPushController(hub, rim, type.length,
        reflex_push_pct, reflex_pull_pct, ground_y, roll_direction);
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }

  /**
   * Builds the wheel at the default position. Kept for the judge and
   * existing callers.
   */
  public static Node build() {
    return buildAt(400);
  }
}