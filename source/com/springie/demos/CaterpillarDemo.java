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
import com.springie.muscles.Oscillator;
import com.springie.render.Coords;
import com.springie.world.World;

/**
 * A caterpillar that crawls with a low floor-hugging peristaltic slide.
 * The body is a flat ribbon: a triangular prism (wide, low) cut into
 * face-sharing tetrahedra (three per segment), flat side down. Every
 * edge is a passive strut with its rest length matched to the geometry,
 * so the skeleton holds its shape with no pre-stress.
 *
 * <p>Drive: all three axial lines carry a travelling contraction wave
 * (one full wavelength tail-to-head). Each segment squeezes in phase
 * as the wave passes, gripping the ground and sliding the ribbon
 * forward -- peristalsis, like an earthworm. The wave direction
 * (wave_sign) sets the crawl direction. Muscle power only: no start kick.
 *
 * <p>Parameters are public fields so the judge can sweep them.
 */
public final class CaterpillarDemo {
  private CaterpillarDemo() {
    // static-only
  }

  /** Number of tube segments. */
  public static final int SEGMENTS = 12;

  /** Segment length (axial), in pixels. */
  public static final int SEGMENT_PX = 15;

  /** Triangle cross-section: bottom edge width, in pixels. */
  public static final int WIDTH_PX = 80;

  /** Triangle cross-section: height, in pixels. Flat ribbon: low. */
  public static final int HEIGHT_PX = 12;

  /** Elasticity of the passive skeleton struts. */
  public static int skeleton_elasticity = 20;

  /** Elasticity of the wave muscles (softer than the skeleton, like
   *  the snake). */
  public static int muscle_elasticity = 15;

  /** Elasticity of the proleg foot muscles. */
  public static int foot_muscle_elasticity = 15;

  /** Muscle amplitude, 0-100%. */
  public static int muscle_amplitude_pct = 20;

  /** Foot muscle amplitude, 0-100%. */
  public static int foot_amplitude_pct = 25;

  /** Oscillator period for the travelling wave, in ticks. */
  public static int muscle_period_ticks = 60;

  /**
   * Wavelength of the travelling wave as a fraction of the body length.
   * 1.0 = one full wave tail-to-head.
   */
  public static double wave_length_fraction = 1.0;

  /**
   * Sign of the travelling wave phase slope. -1 sends the contraction
   * wave tail-to-head (+x); +1 reverses it. (For the wide flat ribbon,
   * -1 drives forward.)
   */
  public static int wave_sign = -1;

  /**
   * When true, the ventral muscles all pulse in phase (inchworm arch).
   * When false (default), a travelling peristaltic wave squeezes along
   * the body tail-to-head, sliding it low over the floor.
   */
  public static boolean standing_wave = false;

  /** Belly clearance: how high the tube hangs above the ground, px.
   *  2 = belly on the floor (floor-hugging slide). */
  public static int belly_clearance_px = 2;

  /** Number of proleg pairs along the body (0 = none, pure peristalsis). */
  public static int proleg_pairs = 0;

  /** Oscillator slot driving the proleg feet (slot 0 drives the body wave). */
  public static final int FOOT_OSCILLATOR = 1;

  /** Proleg phase offset from the local wave phase, in ticks. */
  public static int proleg_phase_offset_ticks = -1; // -1 => half a period

  /** Ground friction, 0-100. */
  public static int friction = 100;

  /** Ticks the build settles before the judge starts measuring. */
  public static int settle_ticks = 60;

  /**
   * When true, mirror the build across the x-y plane (negate z). Used
   * to test whether the crawl direction is set by the geometry.
   */
  public static boolean mirror_build = false;

  /** The proleg foot nodes, set by buildAt (pairs: station-major). */
  public static Node[] feet = new Node[0];

  /**
   * Builds the caterpillar, replacing whatever is there. The body starts
   * at x = x_px pixels, facing +x, hanging low over the ground.
   */
  public static void buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();

    // Tune the oscillators first: the phase assignment below assumes them.
    Muscles.enabled = true;
    Muscles.active_oscillator = 0;
    Muscles.activeOscillator().setAmplitude(
        muscle_amplitude_pct * Muscles.UNITY / 100);
    Muscles.activeOscillator().setPeriodTicks(muscle_period_ticks);
    Muscles.activeOscillator().setPhase(0);
    // The proleg feet run on their own oscillator slot so their depth
    // tunes independently of the body wave.
    if (Muscles.oscillators[FOOT_OSCILLATOR] == null) {
      Muscles.oscillators[FOOT_OSCILLATOR] = new Oscillator();
    }
    Muscles.oscillators[FOOT_OSCILLATOR].setAmplitude(
        foot_amplitude_pct * Muscles.UNITY / 100);
    Muscles.oscillators[FOOT_OSCILLATOR].setPeriodTicks(muscle_period_ticks);
    Muscles.oscillators[FOOT_OSCILLATOR].setPhase(0);
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = friction;
    // No thermal jitter: the long tube's bending modes are excited by
    // even mild temperature, shaking the skeleton apart (20% strain
    // with no gravity at temperature 6). The gait is fully driven.
    World.global_temperature = 0;
    // Damping stabilizes the stiff skeleton + muscle combination.
    com.springie.elements.nodes.Node.viscocity = 2;

    // Cross-section triangles, flat side down, along +x.
    final int n_tri = SEGMENTS + 1;
    final Node[] tops = new Node[n_tri];
    final Node[] lefts = new Node[n_tri];
    final Node[] rights = new Node[n_tri];
    final int seg = SEGMENT_PX << Coords.shift;
    final int wid = WIDTH_PX << Coords.shift;
    final int hgt = HEIGHT_PX << Coords.shift;
    final int x0 = x_px << Coords.shift;
    final int ground = Coords.y_pixels << Coords.shift;
    final int y_bot = ground - (belly_clearance_px << Coords.shift);
    final int y_top = y_bot - hgt;
    final int zmid = (Coords.z_pixels / 2) << Coords.shift;
    for (int i = 0; i < n_tri; i++) {
      final int x = x0 + i * seg;
      tops[i] = addNode(node_manager, clazz, node_type, x, y_top, zmid);
      lefts[i] = addNode(node_manager, clazz, node_type, x, y_bot, zmid - wid / 2);
      rights[i] = addNode(node_manager, clazz, node_type, x, y_bot, zmid + wid / 2);
    }
    if (mirror_build) {
      for (int i = 0; i < n_tri; i++) {
        lefts[i].pos.z = 2 * zmid - lefts[i].pos.z;
        rights[i].pos.z = 2 * zmid - rights[i].pos.z;
        tops[i].pos.z = 2 * zmid - tops[i].pos.z;
      }
    }

    // Prism segments, each cut into three face-sharing tetrahedra.
    // Tet1 (T_i, L_i, R_i, T_{i+1}), Tet2 (L_i, R_i, T_{i+1}, L_{i+1}),
    // Tet3 (R_i, T_{i+1}, L_{i+1}, R_{i+1}).
    for (int i = 0; i < SEGMENTS; i++) {
      linkTetrahedron(link_manager, clazz,
          tops[i], lefts[i], rights[i], tops[i + 1]);
      linkTetrahedron(link_manager, clazz,
          lefts[i], rights[i], tops[i + 1], lefts[i + 1]);
      linkTetrahedron(link_manager, clazz,
          rights[i], tops[i + 1], lefts[i + 1], rights[i + 1]);
    }

    final int period = Muscles.activeOscillator().getPeriodTicks();
    final double body_len_px = SEGMENTS * SEGMENT_PX;
    final double lambda_px =
        Math.max(1.0, wave_length_fraction * body_len_px);
    addVentralMuscles(link_manager, clazz, lefts, rights, tops, period,
        lambda_px);
    addProlegs(node_manager, link_manager, clazz, lefts, rights, tops,
        period, lambda_px);

    // Let the build find its stance with the muscles attached.
    for (int t = 0; t < settle_ticks; t++) {
      node_manager.nodeAndLinkUpdate();
    }
  }

  /** Builds the caterpillar at the default position. */
  public static void build() {
    buildAt(400);
  }

  private static Node addNode(NodeManager node_manager, Clazz clazz,
      NodeType node_type, int x, int y, int z) {
    return node_manager.addNewAgent(new Point3D(x, y, z), clazz, node_type);
  }

  /**
   * Creates the six edges of a tetrahedron as passive struts, each with
   * its own link type whose rest length matches its actual geometry, so
   * the frame carries no pre-stress.
   */
  private static void linkTetrahedron(LinkManager link_manager, Clazz clazz,
      Node p0, Node p1, Node p2, Node p3) {
    strut(link_manager, clazz, p0, p1);
    strut(link_manager, clazz, p0, p2);
    strut(link_manager, clazz, p0, p3);
    strut(link_manager, clazz, p1, p2);
    strut(link_manager, clazz, p1, p3);
    strut(link_manager, clazz, p2, p3);
  }

  private static void strut(LinkManager link_manager, Clazz clazz,
      Node n1, Node n2) {
    if (isLinked(link_manager, n1, n2)) {
      return;
    }
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(n1, n2), skeleton_elasticity);
    final Link link = link_manager.setLink(n1, n2, type, clazz);
    link.adjusted_rest_length = type.length;
  }

  private static boolean isLinked(LinkManager link_manager, Node n1, Node n2) {
    final int n_o_l = link_manager.element.size();
    for (int i = n_o_l; --i >= 0;) {
      final Link existing = (Link) link_manager.element.get(i);
      if ((existing.nodes[0] == n1 && existing.nodes[1] == n2)
          || (existing.nodes[0] == n2 && existing.nodes[1] == n1)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Converts the axial lines into peristaltic muscles. In travelling
   * mode (default) a contraction wave sweeps tail-to-head: each segment
   * squeezes in phase (ventral + dorsal together), gripping and sliding
   * the flat ribbon low over the floor. In standing mode the whole
   * belly pulses (inchworm).
   */
  private static void addVentralMuscles(LinkManager link_manager, Clazz clazz,
      Node[] lefts, Node[] rights, Node[] tops, int period,
      double lambda_px) {
    final GlobalOscillatorController controller =
        new GlobalOscillatorController(Muscles.active_oscillator);
    final Oscillator wave_osc = Muscles.activeOscillator();
    for (int i = 0; i < SEGMENTS; i++) {
      final double s_px = (i + 0.5) * SEGMENT_PX;
      final int phase = standing_wave ? 0
          : wavePhase(s_px, period, lambda_px);
      makeMuscle(link_manager, findLink(link_manager, lefts[i], lefts[i + 1]),
          controller, muscle_elasticity, phase, wave_osc);
      makeMuscle(link_manager, findLink(link_manager, rights[i], rights[i + 1]),
          controller, muscle_elasticity, phase, wave_osc);
      makeMuscle(link_manager, findLink(link_manager, tops[i], tops[i + 1]),
          controller, muscle_elasticity, phase, wave_osc);
    }
  }

  /** Phase in ticks for axial position s_px along the body. */
  private static int wavePhase(double s_px, int period, double lambda_px) {
    final int phase = (int) (wave_sign * s_px * period / lambda_px);
    return ((phase % period) + period) % period;
  }

  /** Finds the link between two nodes (must exist). */
  private static Link findLink(LinkManager link_manager, Node a, Node b) {
    final int n_o_l = link_manager.element.size();
    for (int i = 0; i < n_o_l; i++) {
      final Link link = (Link) link_manager.element.get(i);
      if ((link.nodes[0] == a && link.nodes[1] == b)
          || (link.nodes[0] == b && link.nodes[1] == a)) {
        return link;
      }
    }
    throw new IllegalStateException("no link between nodes");
  }

  /**
   * Adds proleg pairs under the tube. Each foot hangs from a ventral
   * axial edge (the hip edge) plus the top node above it, forming a
   * face-sharing tetrahedron. The three foot links are muscles on the
   * foot oscillator, phased to the local wave so the foot plants while
   * its station is pulled and lifts while the wave passes.
   */
  private static void addProlegs(NodeManager node_manager,
      LinkManager link_manager, Clazz clazz,
      Node[] lefts, Node[] rights, Node[] tops,
      int period, double lambda_px) {
    final int ground = Coords.y_pixels << Coords.shift;
    final int offset = proleg_phase_offset_ticks < 0 ? period / 2
        : proleg_phase_offset_ticks;
    final GlobalOscillatorController controller =
        new GlobalOscillatorController(FOOT_OSCILLATOR);
    final Oscillator foot_osc = Muscles.oscillators[FOOT_OSCILLATOR];
    final NodeType foot_type = node_manager.node_type_factory.getNew();
    final java.util.ArrayList<Node> feet_list = new java.util.ArrayList<>();

    for (int p = 0; p < proleg_pairs; p++) {
      // Spread the pairs along the body.
      final int seg_i = (p + 1) * SEGMENTS / (proleg_pairs + 1);
      final double s_px = (seg_i + 0.5) * SEGMENT_PX;
      // Inchworm (standing wave): rear pairs grip while the belly
      // contracts, front pairs while it extends -- front and rear feet
      // work antiphase. Travelling wave: feet follow the local phase.
      final boolean is_front = p >= proleg_pairs / 2;
      final int base = standing_wave ? (is_front ? period / 2 : 0)
          : wavePhase(s_px, period, lambda_px);
      final int phase = (base + offset) % period;
      final int foot_phase = ((phase % period) + period) % period;
      for (int side = 0; side < 2; side++) {
        final Node ha = side == 0 ? lefts[seg_i] : rights[seg_i];
        final Node hb = side == 0 ? lefts[seg_i + 1] : rights[seg_i + 1];
        // Third node: the top node above the hip edge -- the new
        // tetrahedron shares face (ha, hb, w) with the tube.
        final Node w = tops[seg_i + 1];
        final int fx = (ha.pos.x + hb.pos.x) / 2;
        final int fz = (ha.pos.z + hb.pos.z) / 2;
        final int fy = ground - (2 << Coords.shift);
        final Node foot =
            addNode(node_manager, clazz, foot_type, fx, fy, fz);
        feet_list.add(foot);
        final Link lha = newFootLink(link_manager, clazz, ha, foot);
        final Link lhb = newFootLink(link_manager, clazz, hb, foot);
        final Link lw = newFootLink(link_manager, clazz, w, foot);
        makeMuscle(link_manager, lha, controller,
            foot_muscle_elasticity, foot_phase, foot_osc);
        makeMuscle(link_manager, lhb, controller,
            foot_muscle_elasticity, foot_phase, foot_osc);
        makeMuscle(link_manager, lw, controller,
            foot_muscle_elasticity, foot_phase, foot_osc);
      }
    }
    feet = feet_list.toArray(new Node[0]);
  }

  /** Creates a passive link and returns it for muscle conversion. */
  private static Link newFootLink(LinkManager link_manager, Clazz clazz,
      Node a, Node b) {
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(a, b), foot_muscle_elasticity);
    final Link link = link_manager.setLink(a, b, type, clazz);
    link.adjusted_rest_length = type.length;
    return link;
  }

  /** Converts an existing strut link into a muscle. */
  private static void makeMuscle(LinkManager link_manager, Link link,
      GlobalOscillatorController controller, int elasticity,
      int phase, Oscillator oscillator) {
    final LinkType type = link_manager.link_type_factory.getNew(
        distance(link.nodes[0], link.nodes[1]), elasticity);
    link.type = type;
    link.phase = phase;
    link.controller = controller;
    // Start the muscle at its tick-0 wave value, not at the nominal rest
    // length: otherwise every muscle snaps to its driven length on the
    // first tick and the body kicks violently.
    final int scale = oscillator.getScale(0, phase);
    link.adjusted_rest_length =
        (int) (((long) type.length * scale) >> Coords.shift);
  }

  // Backwards-compatible overload used by addVentralMuscles callers.
  private static void makeMuscle(LinkManager link_manager, Clazz clazz,
      Link link, GlobalOscillatorController controller, int elasticity,
      int phase, Oscillator oscillator) {
    makeMuscle(link_manager, link, controller, elasticity, phase, oscillator);
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }
}
