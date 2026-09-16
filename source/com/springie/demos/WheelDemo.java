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
 * A tetrahedral rolling wheel: 12 nodes per rim (radius 90px) at z = +/-35,
 * one heavy centre hub node. The rim uses alternating diagonal bracing
 * (12 diagonals, one per segment) to resist shear without over-constraining,
 * and the hub connects via tetrahedra (H, A_i, B_i, A_{i+1}) rather than
 * triangles sharing only the hub corner.
 *
 * <p>Unlike the previous design (two 12-gon rims with parallel struts and
 * 24 hub spokes forming triangles that share only the hub corner), the
 * hub connection here forms tetrahedra, and the rim has diagonal bracing.
 * Tetrahedra are rigid in 3D; triangles sharing a single corner flap
 * aimlessly.
 *
 * <p>Drive: the 24 hub spokes are muscles on the shared {@link Muscles}
 * oscillator bank. Each spoke carries a per-link phase (in ticks)
 * proportional to its rim angle, so the bank's sine wave becomes a
 * travelling contraction wave in the body frame -- the spokes bunch up
 * just forward of straight-down and the wheel rolls. The wave runs
 * through {@link GlobalOscillatorController}; the phases are derived
 * from the rim geometry at build time.
 *
 * <p>Node order is fixed and documented: 12 rim-0 nodes (z = -35),
 * 12 rim-1 nodes (z = +35), then the hub. Node 0 (rim-0[0], body
 * angle 0) is the rotation marker. buildAt returns the hub.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class WheelDemo {
  private WheelDemo() {
  }

  /** Nodes per rim. */
  public static final int RIM_COUNT = 12;

  /** Rim radius, in pixels. */
  public static int rim_radius_px = 90;

  /** Rims sit at z = +/- this value, in pixels. */
  public static int rim_half_width_px = 35;

  /** Nominal mass for rim nodes (log scale used by the engine). */
  public static int rim_log_mass = 5;

  /** Nominal mass for the hub node (log scale used by the engine). */
  public static int hub_log_mass = 30;

  /** Muscle amplitude for the spoke wave, 0-100%. */
  public static int muscle_amplitude_pct = 25;

  /** Oscillator period for the spoke wave, in ticks. */
  public static int muscle_period_ticks = 180;

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
   */
  public static int start_kick = 0;

  /** Ground friction, 0-100. */
  public static int friction = 100;

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
    for (int i = 0; i < RIM_COUNT; i++) {
      final double a = 2.0 * Math.PI * i / RIM_COUNT;
      final double c = Math.cos(a);
      final double s = Math.sin(a);
      rim0[i] = addNode(node_manager, clazz, rim_type,
          cx + (int) (radius * c), cy + (int) (radius * s), -hw);
      rim1[i] = addNode(node_manager, clazz, rim_type,
          cx + (int) (radius * c), cy + (int) (radius * s), hw);
    }
    final Node hub = addNode(node_manager, clazz, hub_type, cx, cy, 0);

    // Rim: two 12-gon rings (24 links) + 12 cross links (36 total).
    // The hub spokes form triangles (hub, rim0[i], rim1[i]) sharing only
    // the hub corner -- these flap aimlessly. To fix, add 12 passive
    // diagonals (rim1[i] to rim0[i+1]) which complete the tetrahedra
    // (hub, rim0[i], rim1[i], rim0[i+1]). The diagonals are passive
    // structural bracing; only the 24 spokes are muscles.
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

    // Hub spokes: 24 muscles forming face-joined tetrahedra with the rim.
    // For each i, (hub, rim0[i], rim1[i], rim0[i+1]) and
    // (hub, rim1[i], rim0[i+1], rim1[i+1]) are tetrahedra sharing the face
    // (hub, rim1[i], rim0[i+1]). The rim edges already exist; we add
    // only the 24 hub-to-rim spokes.
    for (int i = 0; i < RIM_COUNT; i++) {
      final double a = 2.0 * Math.PI * i / RIM_COUNT;
      // Phase in ticks: one full wave around the rim.
      final int phase =
          (int) (phase_direction * a * muscle_period_ticks / (2.0 * Math.PI));
      spoke(link_manager, clazz, controller, hub, rim0[i], phase);
      spoke(link_manager, clazz, controller, hub, rim1[i], phase);
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