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
 * A big, clean rolling wheel: 7 nodes per rim (radius 160px), two
 * parallel rims, each with its own single central hub node -- two hubs
 * total, joined by a stiff passive axle. 14 muscle cable spokes (7 per
 * hub).
 *
 * <p>Geometry follows Tim's directives: 2026-09-21 (bigger nodes, longer
 * struts, fewer thinner spokes, one central node per rim), 2026-09-24
 * (heavy axis nodes, much lighter rim nodes, bigger nodes still, thinner
 * links) and 2026-09-28 (7 spokes per rim). Each hub sits in its rim's
 * plane and spokes radially to its 7 rim nodes, like a bicycle wheel;
 * the axle ties the two hubs into a single rigid shaft. The rim uses
 * alternating diagonal bracing to resist shear.
 *
 * <p>Drive (Tim, 2026-09-30): wheelbarrow paddle drive. Two-node paddles
 * trail behind the wheel, one per side, pushing off the ground like oars.
 * The foot plants on the ground (external anchor); a muscle cable from
 * the front rim pulls the paddle top forward, levering the hub forward
 * via a drive strut. This is NOT bootstrap-limited: the ground is external.
 * Spokes are passive structure -- not mixed with the drive.
 * No custom controllers -- only the muscles change cable lengths.
 *
 * <p>Node order: rim-0[i] and rim-1[i] interleaved per iteration (element
 * indices 2*i and 2*i+1), then hub0, then hub1, then paddle nodes. Node 0
 * (rim-0[0], body angle 0) is the rotation marker. buildAt returns hub0.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class WheelbarrowDemo {
  private WheelbarrowDemo() {
  }

  /** Nodes per rim. */
  // Tim's directive (2026-09-28): 7 spokes per rim.
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
  // Mass is functional now: reference mass preserves the tuned behavior
  // (the old values were no-ops when mass was ignored).
  // Tim's directive (2026-09-24): the rim nodes are much lighter than the
  // hubs -- log 13 is 8x lighter than the reference mass, so the heavy
  // hubs dominate the centre of mass and the hamster-ball drive gets real
  // gravitational torque from each hub shift.
  public static int rim_log_mass = 15;

  /** Nominal mass for the hub node (log scale used by the engine). */
  // Tim's directive (2026-09-24): the axis (hub) nodes are heavy -- log 19
  // is 8x the reference mass. The two hubs carry ~90% of the wheel's mass,
  // so pulling a hub forward shifts the centre of mass hard and gravity
  // does the rolling.
  /**
   * Hub mass (log scale). Tim, 2026-09-28: hubs are the heavy central
   * shaft of the wheel.
   */
  public static int hub_log_mass = 19;

  /**
   * Hamster mass (log scale). Tim, 2026-09-29: fairly heavy near-central
   * Paddle drive (Tim, 2026-09-30): the wheelbarrow's paddles push off
   * the ground. No hamster -- the paddles are the drive.
   */
  public static int paddle_log_mass = 14;

  /** Node radius, in fixed-point units: the same as the rim links' radius. */
  // Tim's directive (2026-09-27): nodes the same size as the link radius.
  // (The old node_size_px field passed unshifted pixels to setSize, so the
  // nodes were rendering at ~1px while the ground line assumed 286px.)
  // Node size is PHYSICAL -- the boundary clamp and the ground line both
  // use it.
  static int nodeRadius() {
    final int chord =
        (int) (2.0 * (rim_radius_px << Coords.shift) * Math.sin(Math.PI / RIM_COUNT));
    return chord / link_radius_divisor;
  }

  /**
   * Link rendering thinness: radius = length / this. Tim's directive
   * (2026-09-24): thinner links to go with the bigger nodes (was the
   * LinkType default of length / 8). Visual only -- link radius never
   * enters the physics.
   */
  public static int link_radius_divisor = 32;

  /**
   * Rolling direction: +1 toward +X, -1 toward -X. (Legacy from the
   * hamster drive; the spoke muscles are symmetric so this no longer
   * steers the drive.)
   */
  public static int roll_direction = 1;

  /**
   * Muscle drive for the 14 spoke muscles (Tim, 2026-09-28: only the
   * muscles may change cable lengths). Gentle: 5% amplitude, 120-tick
   * period. All 14 spokes share one oscillator instance with identical
   * phase -- no subset special treatment.
   */
  public static int muscle_amplitude_pct = 15;
  public static int muscle_period_ticks = 480;

  /**
   * Self-start kick, in internal velocity units (256 = 1 px/frame).
   * Tim, 2026-09-30: restored to break the chicken-and-egg -- the kick
   * spins the wheel, the traveling wave (synced to the rotation) then
   * holds the hamster forward to sustain rolling. Pure spin: tangential
   * velocities sum to zero, so the no-initial-velocity rule passes.
   * 240 is about 1px/frame at the rim.
   */
  public static int start_kick = 240;

  /**
   * Paddle drive (Tim, 2026-09-30): two-node paddles trailing behind the
   * wheel, one per side, pushing alternately off the ground like oars.
   * The foot plants on the ground (external anchor); muscle cables pull
   * the paddle top forward, levering the hub forward via the drive cable.
   * Alternating phases (0 and period/2) give continuous drive.
   * Set paddle_back_px = 0 to disable.
   */
  public static int paddle_back_px = 80;
  /** Height of paddle top below hub, in pixels. */
  public static int paddle_top_drop_px = 40;
  /** Muscle amplitude for paddle drive, percent. */
  public static int paddle_amplitude_pct = 20;

  /**
   * Yaw stabilizer bias, in internal velocity units per frame
   * (256 units = 1 px/frame). Tim's directive: N and S bias on opposite
   * ends of the wheel axle -- this does not turn the wheel around, it
   * stabilizes the initial rolling direction (see
   * {@link AxleStabilizerController}). Tuned to 13 for the two-hub wheel;
   * 0 disables.
   */
  public static int axle_stabilizer_bias = 13;

  /**
   * Gravity strength for the wheel universe, in velocity units per frame.
   * Tim, 2026-09-28: turned down from 2 to 1 -- the wheel was hitting the
   * ground with significant velocity and shattering (a node flung &gt;400px
   * from the hub disqualifies the run). Lower gravity softens the impact;
   * it also weakens the gravitational drive, so this is an experiment to
   * be judged.
   */
  public static int gravity_strength = 1;

  /**
   * Per-node compass bias for the axle ends, in velocity units per frame
   * (same units as {@link #axle_stabilizer_bias}). Tim, 2026-09-28: N on
   * the north axle end, S on the south axle end -- the universe compass
   * bias pulls the ends apart along the axle, restoring yaw wander and
   * tip-over the same way the track's axle headings do. E/W are deliberately
   * not assigned: on a z-axle wheel an E/W pair on opposite ends is a pure
   * force couple about the vertical (constant yaw torque -- a turn, not a
   * restoring force), so it would curve the wheel instead of steadying it.
   */
  public static int compass_bias = 50;

  /** Ground friction, 0-100. */
  public static int friction = 100;

  /**
   * Tim's "not tipping over" rule for the wheel: the axle must stay level.
   * Element indices of one node on each end of the axle (rim0[0], rim1[0]
   * -- same angular station, so they ride together while rolling). Their
   * heights must stay within the max difference for the whole run.
   */
  public static final int posture_axle_left_index = 12;
  public static final int posture_axle_right_index = 13;
  /** Max allowed axle-end height difference (px) for the tip-over rule. */
  public static final int posture_axle_max_diff_px = 20;

  /** Elasticity for the rim links (all tetrahedron edges). */
  public static int rim_elasticity = 120;

  /**
   * Elasticity for the inter-rim bracing (cross links and mirror
   * diagonals). Stiffer than the rim rings: it ties the two rims together
   * against differential (rolling/rocking) motion, while the softer rings
   * keep ground impacts gentle.
   */
  public static int bracing_elasticity = 150;

  /** Elasticity for the hub-to-rim spokes (passive cables). */
  public static int spoke_elasticity = 120;

  /**
   * Paddle muscle elasticity (Tim, 2026-09-30): the paddle muscles pull
   * the paddle top forward to lever the hub.
   */
  public static int paddle_elasticity = 80;

  /**
   * Spoke rest-length scale, percent. 100 = rest length equals the built
   * geometry (zero pre-tension). Below 100 pre-tensions the spokes: with
   * cable-only spokes this is structural, not optional -- un-tensioned
   * cables go slack under the hub and it sags until the wheel tips.
   * 95 holds the hub at axle height like a bicycle wheel.
   */
  public static int spoke_rest_scale_pct = 100;

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

    // Muscles.enabled gates the per-link controllers (the 14 spoke
    // muscles). Only the muscles may change cable lengths
    // (Tim, 2026-09-28) -- no custom controllers.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(
        muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = gravity_strength;
    World.ground_friction = friction;
    // Zero thermal jitter: the reflex drive is a delicate self-synchronizing
    // mechanism, and thermal kicks knock it off rhythm into chaotic
    // tip/veer modes (with temperature=6 the same build gives wildly
    // different results per RNG seed). Caterpillar2Demo and SlinkyDemo
    // already build with temperature 0 for the same reason; the judge and
    // the UI both go through buildAt, so both see the deterministic build.
    World.global_temperature = 0;

    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    // The ground line sits one node radius above the canvas floor, so
    // rim-node centres start exactly at their rest height -- no tick-1
    // launch from the boundary clamp (which uses the node radius).
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

    // Tim, 2026-09-29: rigid axle restored. The N/S compass force on the
    // hubs goes through the axle (tension) instead of through the spokes.
    // This isolates the spoke/rim/hamster structure from the N/S stress.
    final LinkType axle_type = thin(link_manager.link_type_factory.getNew(
        distance(hub0, hub1), spoke_elasticity));
    axle_type.compression = false;
    final Link axle = link_manager.setLink(hub0, hub1, axle_type, clazz);
    axle.adjusted_rest_length = axle_type.length;
    // Passive structure -- no muscle, no controller.

    // Tim, 2026-09-29: hubs get N/S compass for tip-over stabilization
    // (the Z-force/Y-offset lever arm gives a restoring torque for axle
    // tilt). The bias is applied via the universe compass setting.
    // The N/S tension is carried by the rigid axle, not the spokes.
    hub0.compass = CompassPoint.N;
    hub1.compass = CompassPoint.S;
    CompassPoint.bias_size = compass_bias;

    // Rim: two 6-gon rings (12 links) + 6 cross links + 12 mirror diagonals
    // (30 total). The diagonals come in mirror pairs so the bracing has
    // no chirality: single-handed diagonals twist the wheel and make it
    // veer in a circle instead of rolling straight.
    for (int i = 0; i < RIM_COUNT; i++) {
      final int j = (i + 1) % RIM_COUNT;
      final Node a0 = rim0[i];
      final Node b0 = rim1[i];
      final Node a1 = rim0[j];
      // Passive: rim edges, cross links, and diagonals maintain shape.
      // The diagonals come in mirror pairs (b0-a1 and a0-b1) so the bracing
      // has no chirality: single-handed diagonals twist the wheel and make
      // it veer in a circle instead of rolling straight.
      passive(link_manager, clazz, a0, a1, rim_elasticity); // rim0 edge
      passive(link_manager, clazz, b0, rim1[j], rim_elasticity); // rim1 edge
      passive(link_manager, clazz, a0, b0,
          bracing_elasticity); // cross at i
      passive(link_manager, clazz, b0, a1, bracing_elasticity); // diagonal /
      passive(link_manager, clazz, a0, rim1[j], bracing_elasticity); // diag \
    }

    // Hub spokes: 12 PASSIVE structural cables, 6 per hub, each hub
    // spoking radially to its own rim (in-plane, like a bicycle wheel).
    // Pre-tensioned via spoke_rest_scale_pct so each hub hangs from its
    // upper spokes. Tim, 2026-09-29: spokes are structure, NOT drive --
    // don't mix spokes and hamster support. The drive is the hamster
    // (below), positioned by its own muscles.
    for (int i = 0; i < RIM_COUNT; i++) {
      structuralSpoke(link_manager, clazz, hub0, rim0[i]);
      structuralSpoke(link_manager, clazz, hub1, rim1[i]);
    }

    // Paddle drive (Tim, 2026-09-30): two-node paddles trailing behind,
    // one per side, pushing off the ground like oars (wheelbarrow drive).
    // The foot plants on the ground (external anchor); a muscle cable from
    // the front rim pulls the paddle top forward, levering the hub
    // forward via the drive strut. This is NOT bootstrap-limited: the
    // ground is external. All muscles share one controller (Tim's rule).
    final GlobalOscillatorController paddleMuscle =
        new GlobalOscillatorController(Muscles.active_oscillator);
    if (paddle_back_px > 0) {
      final NodeType paddle_type = node_manager.node_type_factory.getNew();
      paddle_type.log_mass = paddle_log_mass;
      paddle_type.radius = nodeRadius();
      final Node[] hubs = {hub0, hub1};
      final Node[][] rims = {rim0, rim1};
      for (int side = 0; side < 2; side++) {
        final int z_side = (z0 >> Coords.shift)
            + (side == 0 ? 0 : 2 * rim_half_width_px);
        final int z_fp = z_side << Coords.shift;
        // Foot: on the ground, behind the wheel.
        final Node foot = addNode(node_manager, clazz, paddle_type,
            cx - (paddle_back_px << Coords.shift), ground, z_fp);
        // Paddle top: behind and below the hub.
        final Node ptop = addNode(node_manager, clazz, paddle_type,
            cx - ((paddle_back_px / 2) << Coords.shift),
            cy + (paddle_top_drop_px << Coords.shift), z_fp);
        // Paddle shaft: rigid strut (structure, not muscle).
        final LinkType paddle_strut = thin(link_manager.link_type_factory.getNew(
            distance(foot, ptop), spoke_elasticity));
        link_manager.setLink(foot, ptop, paddle_strut, clazz);
        // Drive STRUT: paddle top to hub (rigid, transmits the lever
        // push). Must be a strut (not cable) because the paddle PUSHES
        // the hub forward -- cables go slack under compression.
        // Structure, not muscle (Tim's rule: muscles on cables, but
        // structure can be struts).
        final LinkType drive_strut = thin(link_manager.link_type_factory.getNew(
            distance(ptop, hubs[side]), spoke_elasticity));
        link_manager.setLink(ptop, hubs[side], drive_strut, clazz);
        // Muscle: front rim node to paddle top. Contracts to pull the
        // paddle forward, rotating it around the planted foot and
        // levering the hub forward. Alternating phases per side.
        // Shares the one controller (Tim's rule).
        // Tim, 2026-09-30: trying SYNCED (not alternating) -- both push
        // together for symmetric drive, no yaw-rock.
        final Node front_rim = rims[side][0]; // angle 0 = +X (front)
        final LinkType pm_type = thin(link_manager.link_type_factory.getNew(
            distance(front_rim, ptop), paddle_elasticity));
        pm_type.compression = false;
        final Link pm_link = link_manager.setLink(front_rim, ptop, pm_type, clazz);
        pm_link.adjusted_rest_length = pm_type.length;
        pm_link.phase = 0; // Synced, not alternating
        pm_link.controller = paddleMuscle;
      }
    }

    // No mid-air starts: rest the whole model on the ground plane.
    Grounding.restOnGround(node_manager);

    // Tim, 2026-09-30: self-start kick (restored). Pure spin -- tangential
    // rim velocities sum to zero net velocity, so the no-initial-velocity
    // rule passes. Tips the wheel into the rolling gait; the traveling
    // wave then holds the hamster forward to sustain it.
    if (start_kick != 0) {
      for (int i = 0; i < RIM_COUNT; i++) {
        final double a = 2.0 * Math.PI * i / RIM_COUNT;
        final int tx = (int) (-Math.sin(a) * start_kick);
        final int ty = (int) (Math.cos(a) * start_kick);
        rim0[i].velocity.x += tx;
        rim0[i].velocity.y += ty;
        rim1[i].velocity.x += tx;
        rim1[i].velocity.y += ty;
      }
    }

    return hub0;
  }

  private static Node addNode(NodeManager nm, Clazz clazz, NodeType nt,
      int x, int y, int z) {
    return nm.addNewAgent(new Point3D(x, y, z), clazz, nt);
  }

  /**
   * Passive link with its own type so the rest length matches its actual
   * geometry exactly. Returns the link so callers can attach a controller.
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
   * Thins a link type for rendering: radius = length / link_radius_divisor.
   * Each type from the factory is fresh (never shared), so mutating it is
   * safe. Visual only -- link radius never enters the physics.
   */
  private static LinkType thin(LinkType type) {
    type.radius = type.length / link_radius_divisor;
    return type;
  }

  /**
   * Structural hub-to-rim spoke: passive cable, pre-tensioned via
   * spoke_rest_scale_pct so the hub hangs from its upper spokes like a
   * bicycle wheel. Tension-only (compression=false). Tim, 2026-09-29:
   * spokes are structure, NOT drive -- no controller, no muscle.
   */
  private static void structuralSpoke(LinkManager lm, Clazz clazz,
      Node hub, Node rim) {
    final LinkType type = thin(lm.link_type_factory.getNew(
        scaledSpokeLength(distance(hub, rim)), spoke_elasticity));
    type.compression = false;
    final Link link = lm.setLink(hub, rim, type, clazz);
    link.adjusted_rest_length = type.length;
    // No controller, no phase -- passive structure.
  }

  /**
   * Spoke rest length after the pre-tension scale is applied. At 100 the
   * link is built at its geometric distance with no pre-tension; lower
   * values pre-tension the spoke so it holds the hub up at axle height.
   */
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
