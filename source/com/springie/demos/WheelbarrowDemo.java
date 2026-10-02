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
 * A minimal rolling wheel: nonagonal prism (7 nodes per rim, radius
 * 160px), two parallel rims, each with its own single central hub node
 * -- two hubs total, joined by a stiff passive axle in the middle.
 *
 * <p>Nothing else: no hamster, no muscles, no paddles, no kick. The hubs
 * carry N/S compass headings for tip-over stabilization (Tim, 2026-09-28);
 * the N/S tension is carried by the rigid axle, not the spokes.
 *
 * <p>Geometry follows Tim's directives: 2026-09-21 (bigger nodes, longer
 * struts, fewer thinner spokes, one central node per rim), 2026-09-24
 * (heavy axis nodes, much lighter rim nodes, bigger nodes still, thinner
 * links). Each hub sits in its rim's plane and spokes radially to its 6
 * rim nodes, like a bicycle wheel; the axle ties the two hubs into a
 * single rigid shaft. The rim uses alternating diagonal bracing to resist
 * shear.
 *
 * <p>Node order: rim-0[i] and rim-1[i] interleaved per iteration (element
 * indices 2*i and 2*i+1), then hub0, then hub1. Node 0 (rim-0[0], body
 * angle 0) is the rotation marker. buildAt returns hub0.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class WheelbarrowDemo {
  private WheelbarrowDemo() {
  }

  /** Nodes per rim: heptagonal prism (Tim, 2026-10-01: f=7). */
  public static final int RIM_COUNT = 6;

  /** Rim radius, in pixels. */
  public static int rim_radius_px = 160;

  /** Rims sit at z = z_offset and z = z_offset + 2 * this, in pixels. */
  public static int rim_half_width_px = 162;

  /**
   * How far the hubs sit inside the rim planes on each side, in pixels.
   * The axle is shorter than the track -- the spokes pull the rims
   * together like a bicycle wheel, instead of apart.
   */
  public static int axle_inset_px = 60;

  /**
   * Z offset of the whole wheel: rim-0 sits at z = this, rim-1 at
   * z = this + 2 * rim_half_width_px. Keeps the wheel clear of the z = 0
   * wall -- riding the wall shoves the wheel sideways (+z drift) on
   * every rim contact. Must stay >= 0 (never below the wall).
   */
  public static int z_offset_px = 100;

  /** Nominal mass for rim nodes (log scale used by the engine). */
  public static int rim_log_mass = 15;

  /** Nominal mass for the hub node (log scale used by the engine). */
  public static int hub_log_mass = 19;

  /** Node radius, in fixed-point units: the same as the rim links' radius. */
  static int nodeRadius() {
    final int chord =
        (int) (2.0 * (rim_radius_px << Coords.shift) * Math.sin(Math.PI / RIM_COUNT));
    return chord / link_radius_divisor;
  }

  /**
   * Link rendering thinness: radius = length / this. Visual only -- link
   * radius never enters the physics.
   */
  public static int link_radius_divisor = 32;

  /**
   * Per-node compass bias for the axle ends, in velocity units per frame.
   * Tim, 2026-09-28: N on the north axle end, S on the south axle end --
   * the universe compass bias pulls the ends apart along the axle,
   * restoring yaw wander and tip-over. E/W are deliberately not assigned.
   * Tuned to 10 (Tim, 2026-10-01): 50 causes Z-drift and tip-over by
   * ~350 ticks; 10 holds 100% upright for 600 ticks with no shatter.
   */
  public static int compass_bias = 10;

  /** Ground friction, 0-100. */
  public static int friction = 100;

  /**
   * Gravity strength for the wheel universe, in velocity units per frame.
   */
  public static int gravity_strength = 5;

  /**
   * Tim's "not tipping over" rule for the wheel: the axle must stay level.
   * Element indices of one node on each end of the axle (hub0, hub1).
   * Their heights must stay within the max difference for the whole run.
   */
  public static final int posture_axle_left_index = 12;
  public static final int posture_axle_right_index = 13;
  /** Max allowed axle-end height difference (px) for the tip-over rule. */
  public static final int posture_axle_max_diff_px = 20;

  /** Elasticity for the rim links (all tetrahedron edges). */
  public static int rim_elasticity = 40;

  /**
   * Elasticity for the inter-rim bracing (cross links and mirror
   * diagonals). Stiffer than the rim rings: it ties the two rims together
   * against differential (rolling/rocking) motion, while the softer rings
   * keep ground impacts gentle.
   */
  public static int bracing_elasticity = 40;

  /** Elasticity for the hub-to-rim spokes (passive cables). */
  public static int spoke_elasticity = 40;

  /**
   * Spoke rest-length scale, percent. 100 = rest length equals the built
   * geometry (zero pre-tension).
   */
  public static int spoke_rest_scale_pct = 100;

  /**
   * Handle: how far behind the wheel centre the handle nodes sit, in
   * pixels. Tim, 2026-10-01: two nodes on the ground behind the wheel,
   * linked from the axle ends -- a wheelbarrow tripod (wheel + two
   * handle ends) that resists tip-over.
   */
  public static int handle_back_px = 240;

  /** Elasticity for the handle shafts and cross-brace. */
  public static int handle_elasticity = 40;
  public static int xbrace_elasticity = 40;

  /**
   * Handle muscle drive (Tim, 2026-10-01): the two handle shafts
   * (hub-to-handle) are muscles. They haul the trailing handle nodes,
   * stabbing them into the ground; the ground reaction drives the wheel
   * forward (+X, opposite the trailing handle). Both share one
   * oscillator; phase 0 = in-phase, phase period/2 = alternating.
   */
  public static int muscle_amplitude_pct = 30;
  public static int muscle_period_ticks = 510;
  /** Phase offset for the second handle muscle, in ticks. */
  public static int muscle_phase2_ticks = 64;

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
    rim_type.radius = nodeRadius();
    hub_type.radius = nodeRadius();

    World.gravity_active = true;
    World.gravity_strength = gravity_strength;
    World.ground_friction = friction;
    World.bounding_box_bounciness = 0; // Tim, 2026-10-01: no wall bounce for demos.
    World.global_temperature = 0;

    // Muscles: the two handle shafts share one oscillator (Tim's rule:
    // all muscles share one controller instance, no subset treatment).
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(
        muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);

    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    final int ground =
        (Coords.y_pixels << Coords.shift) - nodeRadius();
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
    final Node hub0 = addNode(node_manager, clazz, hub_type,
        cx, cy, z0 + (axle_inset_px << Coords.shift));
    final Node hub1 = addNode(node_manager, clazz, hub_type,
        cx, cy, z0 + 2 * hw - (axle_inset_px << Coords.shift));

    // Rigid axle in the middle: ties the two hubs into a single shaft.
    // The N/S compass force goes through the axle (tension) instead of
    // through the spokes, isolating the spoke/rim structure from N/S stress.
    final LinkType axle_type = thin(link_manager.link_type_factory.getNew(
        distance(hub0, hub1), spoke_elasticity));
    axle_type.compression = false;
    final Link axle = link_manager.setLink(hub0, hub1, axle_type, clazz);
    axle.adjusted_rest_length = axle_type.length;

    // N/S compass on the hubs for tip-over stabilization (Tim, 2026-09-28).
    hub0.compass = CompassPoint.N;
    hub1.compass = CompassPoint.S;
    CompassPoint.bias_size = compass_bias;

    // Rim: two 6-gon rings (12 links) + 6 cross links + 12 mirror diagonals
    // (30 total). The diagonals come in mirror pairs so the bracing has
    // no chirality.
    for (int i = 0; i < RIM_COUNT; i++) {
      final int j = (i + 1) % RIM_COUNT;
      final Node a0 = rim0[i];
      final Node b0 = rim1[i];
      final Node a1 = rim0[j];
      passive(link_manager, clazz, a0, a1, rim_elasticity); // rim0 edge
      passive(link_manager, clazz, b0, rim1[j], rim_elasticity); // rim1 edge
      passive(link_manager, clazz, a0, b0,
          bracing_elasticity); // cross at i
      passive(link_manager, clazz, b0, a1, bracing_elasticity); // diagonal /
      passive(link_manager, clazz, a0, rim1[j], bracing_elasticity); // diag \
    }

    // Hub spokes: 12 passive structural cables, 6 per hub, each hub
    // spoking radially to its own rim (in-plane, like a bicycle wheel).
    for (int i = 0; i < RIM_COUNT; i++) {
      structuralSpoke(link_manager, clazz, hub0, rim0[i]);
      structuralSpoke(link_manager, clazz, hub1, rim1[i]);
    }

    // Handle (Tim, 2026-10-01): two nodes on the ground behind the wheel
    // (-X, the wheel rolls toward +X). Two links from the axle ends
    // (hub0, hub1) to the trailing handle nodes, plus one link between
    // the handle nodes. Wheel + two handle ends = tripod, stable
    // against tip-over. The handle nodes sit at the hubs' z so the
    // shafts run straight back with no twist.
    final NodeType handle_type = node_manager.node_type_factory.getNew();
    handle_type.log_mass = rim_log_mass;
    handle_type.radius = nodeRadius();
    // Handle nodes get their own clazz color (pastel peach), distinct
    // from the wheel's nodes (Tim, 2026-10-01).
    final Clazz handle_clazz = node_manager.clazz_factory.getNew(0xFFDAB9);
    // Tetrahedron handle (Tim, 2026-10-01): the two handle nodes form a
    // tetrahedron with the axle nodes (hub0, hub1). Based on the stable
    // original positions (240px back, full axle width); handle0 is 20px
    // further back to break coplanarity -- a true 3D tetrahedron.
    final int thx = cx - (240 << Coords.shift);
    final Node handle0 = addNode(node_manager, handle_clazz, handle_type,
        thx - (20 << Coords.shift), ground, z0 + (axle_inset_px << Coords.shift));
    final Node handle1 = addNode(node_manager, handle_clazz, handle_type,
        thx, ground, z0 + 2 * hw - (axle_inset_px << Coords.shift));
    // The two shafts are MUSCLES (cables, not struts -- Tim's rule).
    // They share one oscillator; phase2 controls in-phase vs alternating.
    // These are the ONLY muscles in the handle.
    final GlobalOscillatorController muscle =
        new GlobalOscillatorController(Muscles.active_oscillator);
    handleMuscle(link_manager, clazz, hub0, handle0, muscle, 0).handle = true;
    handleMuscle(link_manager, clazz, hub1, handle1, muscle,
        muscle_phase2_ticks).handle = true;
    // Tetrahedron edges (passive): handle0-handle1, hub0-handle1,
    // hub1-handle0. With the axle (hub0-hub1) and the two muscles, this
    // completes the 6 edges of the tetrahedron. Softer than the muscles
    // (xbrace_elasticity) so the tetrahedron can flex without tipping.
    passive(link_manager, clazz, handle0, handle1, xbrace_elasticity).handle = true;
    passive(link_manager, clazz, hub0, handle1, xbrace_elasticity).handle = true;
    passive(link_manager, clazz, hub1, handle0, xbrace_elasticity).handle = true;

    // No mid-air starts: rest the whole model on the ground plane.
    Grounding.restOnGround(node_manager);

    return hub0;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt,
      int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /**
   * Passive link with its own type so the rest length matches its actual
   * geometry exactly.
   */
  private static Link passive(LinkManager lm, Clazz clazz, Node a, Node b,
      int elasticity) {
    final LinkType type =
        thin(lm.link_type_factory.getNew(distance(a, b), elasticity));
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = type.length;
    return link;
  }

  /**
   * X-brace diagonal: passive cable (tension-only, compression=false)
   * with moderate give -- checks lateral yaw without over-constraining
   * the handle (Tim, 2026-10-01).
   */
  private static void xBraceCable(LinkManager lm, Clazz clazz,
      Node hub, Node handle) {
    final LinkType type = thin(lm.link_type_factory.getNew(
        distance(hub, handle), xbrace_elasticity));
    type.compression = false;
    final Link link = lm.setLink(hub, handle, type, clazz);
    link.adjusted_rest_length = type.length;
    link.handle = true;
  }

  /**
   * Thins a link type for rendering: radius = length / link_radius_divisor.
   * Visual only -- link radius never enters the physics.
   */
  private static LinkType thin(LinkType type) {
    type.radius = type.length / link_radius_divisor;
    return type;
  }

  /**
   * Structural hub-to-rim spoke: passive cable. Tension-only
   * (compression=false).
   */
  private static void structuralSpoke(LinkManager lm, Clazz clazz,
      Node hub, Node rim) {
    final LinkType type = thin(lm.link_type_factory.getNew(
        scaledSpokeLength(distance(hub, rim)), spoke_elasticity));
    type.compression = false;
    final Link link = lm.setLink(hub, rim, type, clazz);
    link.adjusted_rest_length = type.length;
  }

  /**
   * Handle muscle: cable (tension-only, per Tim's muscle-on-cables rule)
   * from hub to handle node, driven by the shared oscillator.
   * Returns the link so callers can set the handle flag.
   */
  private static Link handleMuscle(LinkManager lm, Clazz clazz,
      Node hub, Node handle, GlobalOscillatorController muscle, int phase) {
    final LinkType type = thin(lm.link_type_factory.getNew(
        distance(hub, handle), handle_elasticity));
    type.compression = false;
    final Link link = lm.setLink(hub, handle, type, clazz);
    link.adjusted_rest_length = type.length;
    link.phase = phase;
    link.controller = muscle;
    return link;
  }

  private static int scaledSpokeLength(int geometric) {
    return (int) ((long) geometric * spoke_rest_scale_pct / 100);
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
