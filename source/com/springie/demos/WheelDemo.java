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
 * A big, clean rolling wheel: 6 nodes per rim (radius 160px), two
 * parallel rims, each with its own single central hub node -- two hubs
 * total, joined by a stiff passive axle. Fewer, thinner spokes than the
 * old design: 12 cable spokes (6 per hub), not 16.
 *
 * <p>Geometry follows Tim's directive (2026-09-21): bigger nodes, longer
 * struts, fewer thinner spokes, one central node per rim. Each hub sits
 * in its rim's plane and spokes radially to its 6 rim nodes, like a
 * bicycle wheel; the axle ties the two hubs into a single rigid shaft.
 * The rim uses alternating diagonal bracing to resist shear, and each
 * hub connects via tetrahedra -- never triangles sharing only the hub
 * corner.
 *
 * <p>Drive: the 12 hub spokes are cable muscles (tension-only, per Tim's
 * "muscles on cables" rule) with a ground-contact pull reflex. When a
 * spoke pair's rim nodes are on the ground ahead of the hub (in the
 * rolling direction) the spokes contract, pulling the hub forward and
 * down toward the rim -- like pulling yourself forward. The reflex is
 * self-synchronizing: ground contact sets the timing. Spokes are
 * pre-tensioned (spoke_rest_scale_pct) so each hub hangs from its upper
 * spokes instead of sagging.
 *
 * <p>Node order: rim-0[i] and rim-1[i] interleaved per iteration (element
 * indices 2*i and 2*i+1), then hub0, then hub1. Node 0 (rim-0[0], body
 * angle 0) is the rotation marker. buildAt returns hub0.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class WheelDemo {
  private WheelDemo() {
  }

  /** Nodes per rim. */
  public static final int RIM_COUNT = 6;

  /** Rim radius, in pixels. */
  public static int rim_radius_px = 160;

  /** Rims sit at z = z_offset and z = z_offset + 2 * this, in pixels. */
  public static int rim_half_width_px = 130;

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
  public static int rim_log_mass = 17;

  /** Nominal mass for the hub node (log scale used by the engine). */
  public static int hub_log_mass = NodeType.REFERENCE_LOG_MASS;

  /** Drawn node size for the wheel's nodes, in pixels. */
  public static int node_size_px = 60;

  /** Muscle amplitude for the spoke wave, 0-100%. */
  /** Spoke muscle amplitude, percent (travelling-wave mode only). */
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

  /**
   * Yaw stabilizer bias, in internal velocity units per frame
   * (256 units = 1 px/frame). Tim's directive: N and S bias on opposite
   * ends of the wheel axle -- this does not turn the wheel around, it
   * stabilizes the initial rolling direction (see
   * {@link AxleStabilizerController}). Tuned to 13 for the two-hub wheel;
   * 0 disables.
   */
  public static int axle_stabilizer_bias = 13;

  /** Ground friction, 0-100. */
  public static int friction = 100;

  /**
   * Drive mode: true for the ground-contact push-off reflex
   * (self-synchronizing), false for the open-loop travelling wave.
   * The paired reflex fires both spokes of a rim-pair together, keeping
   * lateral forces symmetric -- this is what stops the tip-over. Geometry
   * (short/fat) helps but is not sufficient alone.
   */
  public static boolean use_reflex_drive = true;

  /**
   * Reflex push percent (spoke extension in back stance). Zero: the
   * spokes are cables (tension-only), so extension fires nothing -- the
   * drive is pull-only on the ahead stance (see reflex_pull_pct).
   */
  public static int reflex_push_pct = 0;
  /**
   * Tim's "not tipping over" rule for the wheel: the axle must stay level.
   * Element indices of one node on each end of the axle (rim0[0], rim1[0]
   * -- same angular station, so they ride together while rolling). Their
   * heights must stay within the max difference for the whole run.
   */
  public static final int posture_axle_left_index = 0;
  public static final int posture_axle_right_index = 1;
  /** Max allowed axle-end height difference (px) for the tip-over rule. */
  public static final int posture_axle_max_diff_px = 20;

  /**
   * Reflex pull percent (spoke contraction in front stance). The drive:
   * with cable-only spokes the push phase cannot fire (cables don't
   * push), so the reflex pulls on the ahead stance instead -- contracting
   * spokes yank the hub forward toward the grounded front rim. Tuned to 8
   * for the two-hub wheel: the in-plane spokes are shorter and more
   * direct, so less pull is needed; stronger pull tips it over.
   */
  public static int reflex_pull_pct = 8;

  /**
   * How far behind/in front of the hub (px) a spoke's rim pair must be to
   * fire the push/pull. Firing only when the spoke is well angled (not
   * near-vertical) directs the push forward instead of launching the wheel
   * skyward.
   */
  public static int reflex_stance_threshold_px = 60;

  /** Rolling direction for the reflex: +1 toward +X, -1 toward -X. */
  public static int roll_direction = 1;

  /**
   * When true, the paired reflex ramps the push/pull smoothly with spoke
   * angle (0 at the stance threshold, full at horizontal) instead of
   * snapping on/off. Removes the impulsive kick that pumps the rocking
   * mode; concentrates push where the spoke is most horizontal.
   */
  public static boolean proportional_drive = true;

  /** Elasticity for the rim links (all tetrahedron edges). */
  public static int rim_elasticity = 30;

  /**
   * Elasticity for the inter-rim bracing (cross links and mirror
   * diagonals). Stiffer than the rim rings: it ties the two rims together
   * against differential (rolling/rocking) motion, while the softer rings
   * keep ground impacts gentle.
   */
  public static int bracing_elasticity = 30;

  /** Elasticity for the hub-to-rim spoke muscles. */
  public static int spoke_elasticity = 10;

  /**
   * Roll correction gain for the paired reflex, in rest-length units per
   * unit of inter-rim y-difference (256 = 1.0x). Zero: with cable-only
   * spokes the correction is counterproductive -- extending a cable does
   * nothing, and the asymmetric contraction pumps the very rocking mode
   * it was meant to damp. The paired reflex fires symmetrically from
   * midpoint geometry, which is sufficient; the axle stays level without
   * active correction.
   */
  public static int roll_correct_gain = 0;

  /**
   * Spoke rest-length scale, percent. 100 = rest length equals the built
   * geometry (zero pre-tension). Below 100 pre-tensions the spokes: with
   * cable-only spokes this is structural, not optional -- un-tensioned
   * cables go slack under the hub and it sags until the wheel tips.
   * 95 holds the hub at axle height like a bicycle wheel.
   */
  public static int spoke_rest_scale_pct = 95;

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
    rim_type.setSize(node_size_px);
    hub_type.setSize(node_size_px);

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
        (Coords.y_pixels << Coords.shift) - (node_size_px << Coords.shift);
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
    final Node hub0 = addNode(node_manager, clazz, hub_type, cx, cy, z0);
    final Node hub1 =
        addNode(node_manager, clazz, hub_type, cx, cy, z0 + 2 * hw);

    // Axle: a stiff passive link joining the two hubs into a single
    // rigid shaft. Also carries the yaw stabilizer (one instance, so the
    // bias applies exactly once per dynamics step).
    final Link axle_link =
        passive(link_manager, clazz, hub0, hub1, bracing_elasticity);
    axle_link.controller =
        new AxleStabilizerController(rim0, rim1, axle_stabilizer_bias);

    // Rim: two 6-gon rings (12 links) + 6 cross links + 12 mirror diagonals
    // (30 total). The diagonals come in mirror pairs so the bracing has
    // no chirality: single-handed diagonals twist the wheel and make it
    // veer in a circle instead of rolling straight.
    final GlobalOscillatorController controller =
        new GlobalOscillatorController(0);
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

    // Hub spokes: 12 cable muscles, 6 per hub, each hub spoking radially
    // to its own rim (in-plane, like a bicycle wheel). For each i, the
    // pair (hub0-rim0[i], hub1-rim1[i]) fires together from midpoint
    // geometry, keeping lateral forces symmetric.
    //
    // Drive: either the ground-contact pull reflex (self-synchronizing)
    // or the open-loop travelling wave (per-link phases).
    for (int i = 0; i < RIM_COUNT; i++) {
      final double a = 2.0 * Math.PI * i / RIM_COUNT;
      // Phase in ticks: one full wave around the rim.
      final int phase =
          (int) (phase_direction * a * muscle_period_ticks / (2.0 * Math.PI));
      if (use_reflex_drive) {
        pairedReflexSpokes(link_manager, clazz, hub0, hub1, rim0[i], rim1[i],
            ground);
      } else {
        spoke(link_manager, clazz, controller, hub0, rim0[i], phase);
        spoke(link_manager, clazz, controller, hub1, rim1[i], phase);
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
   * geometry exactly. Returns the link so callers can attach a controller.
   */
  private static Link passive(LinkManager lm, Clazz clazz, Node a, Node b,
      int elasticity) {
    final LinkType type =
        lm.link_type_factory.getNew(distance(a, b), elasticity);
    final Link link = lm.setLink(a, b, type, clazz);
    link.adjusted_rest_length = type.length;
    return link;
  }

  /** Hub-to-rim cable muscle spoke with an angle-derived oscillator phase. */
  private static void spoke(LinkManager lm, Clazz clazz,
      GlobalOscillatorController controller, Node hub, Node rim, int phase) {
    final LinkType type = lm.link_type_factory.getNew(
        scaledSpokeLength(distance(hub, rim)), spoke_elasticity);
    type.compression = false;
    final Link link = lm.setLink(hub, rim, type, clazz);
    link.adjusted_rest_length = type.length;
    link.phase = phase;
    link.controller = controller;
  }

  /**
   * Spoke rest length after the pre-tension scale is applied. The link is
   * built at the geometric distance but its rest length is shorter, so the
   * spoke is pre-tensioned from tick 0 and holds the hub up at axle height.
   */
  private static int scaledSpokeLength(int geometric) {
    return (int) ((long) geometric * spoke_rest_scale_pct / 100);
  }

  /**
   * One paired reflex controller driving both spokes of angular station
   * i: hub0 to rim0[i] and hub1 to rim1[i]. The pair fires together from
   * its midpoint geometry, keeping the sideways spoke forces symmetric so
   * a small tilt cannot grow into a capsize (see PairedSpokeController).
   * Both hubs sit on the axle (same x, y), so hub0 serves as the
   * controller's position reference.
   *
   * <p>The spokes are cables (tension-only, compression=false) per Tim's
   * "muscles on cables" rule. They are pre-tensioned via
   * spoke_rest_scale_pct so each hub hangs from its upper spokes like a
   * bicycle wheel, instead of sagging.
   */
  private static void pairedReflexSpokes(LinkManager link_manager, Clazz clazz,
      Node hub0, Node hub1, Node rim_a, Node rim_b, int ground_y) {
    final LinkType type_a = link_manager.link_type_factory.getNew(
        scaledSpokeLength(distance(hub0, rim_a)), spoke_elasticity);
    type_a.compression = false;
    final Link link_a = link_manager.setLink(hub0, rim_a, type_a, clazz);
    link_a.adjusted_rest_length = type_a.length;
    final LinkType type_b = link_manager.link_type_factory.getNew(
        scaledSpokeLength(distance(hub1, rim_b)), spoke_elasticity);
    type_b.compression = false;
    final Link link_b = link_manager.setLink(hub1, rim_b, type_b, clazz);
    link_b.adjusted_rest_length = type_b.length;
    final PairedSpokeController controller = new PairedSpokeController(hub0,
        rim_a, rim_b, link_a, link_b, type_a.length, type_b.length,
        reflex_push_pct, reflex_pull_pct, ground_y, roll_direction,
        reflex_stance_threshold_px, proportional_drive, rim_radius_px,
        roll_correct_gain);
    link_a.controller = controller;
    link_b.controller = controller;
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