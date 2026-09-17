// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.utilities.random.Hortensius32Fast;
import com.springie.world.World;

/**
 * Truly headless judge that scores the grasshopper high jump. Does NOT
 * start FrEnd (no GUI, no animation thread) -- single-threaded and
 * deterministic. Needs DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Rules:
 * <ol>
 * <li>Muscle power only: the demo applies no start kick; the launch
 * comes from the leg extensors pushing against the ground.</li>
 * <li>Score = highest point the crown marker reaches above its build
 * height, in pixels, over the run.</li>
 * <li>A valid jump goes fully airborne: every foot leaves the ground
 * (rises more than {@link #AIRBORNE_SLACK_PX} above the wall) at some
 * point. Otherwise the score is 0.</li>
 * <li>The jumper must stay in one piece: any node exceeding
 * {@link #SPEED_CAP_PX_PER_TICK} or any link exceeding
 * {@link #STRAIN_CAP} strain (against its adjusted rest length)
 * disqualifies the run (score 0).</li>
 * </ol>
 *
 * <p>Usage: java com.springie.demos.GrasshopperJudge [ticks]
 */
public final class GrasshopperJudge {
  private GrasshopperJudge() {
  }

  /** A foot counts as airborne once it rises this far above the wall. */
  public static final int AIRBORNE_SLACK_PX = 6;
  /**
   * Speed disqualification bar, px/tick. A real launch moves at a few
   * px/tick; numerical explosions hit thousands.
   */
  public static final int SPEED_CAP_PX_PER_TICK = 250;
  /**
   * Strain disqualification bar, fraction of adjusted rest length.
   * Applies to passive links only: the skeleton must hold together.
   * (Muscle strain is reported but not disqualifying -- high muscle
   * strain is the launch itself.)
   */
  public static final double STRAIN_CAP = 1.5;

  /** Score breakdown for one judged run. */
  public static final class Result {
    /** Highest point of the crown marker above its build height, pixels. */
    public int height_px;
    /** True when every foot left the ground at some point. */
    public boolean airborne;
    /** Fastest any node moved, px/tick. */
    public int max_speed_px;
    /** Worst link strain seen (fraction of adjusted rest length). */
    public double max_strain;
    /** Worst strain on a passive (non-muscle) link. */
    public double max_passive_strain;
    /** True when the run was disqualified (exploded). */
    public boolean disqualified;
    /** height_px when airborne and not disqualified, else 0. */
    public int score;
    /** Total ticks simulated. */
    public int ticks;

    @Override
    public String toString() {
      return "HEIGHT_PX " + this.height_px
          + "\nAIRBORNE " + this.airborne
          + "\nMAX_SPEED_PX " + this.max_speed_px
          + "\nMAX_STRAIN " + String.format("%.3f", this.max_strain)
          + "\nMAX_PASSIVE_STRAIN "
          + String.format("%.3f", this.max_passive_strain)
          + "\nDISQUALIFIED " + this.disqualified
          + "\nSCORE " + this.score
          + "\nTICKS " + this.ticks;
    }
  }

  /**
   * Scores the grasshopper over the given number of ticks. Deterministic:
   * two calls give identical results, even in a JVM where a GUI test has
   * left the animation thread running.
   */
  public static Result score(int ticks) {
    // Hold the model lock for the whole run (see SidewinderJudge for why:
    // a GUI test's animation thread never stops and would otherwise step
    // physics on this run's NodeManager concurrently).
    synchronized (ContextManager.class) {
      return scoreWithLockHeld(ticks);
    }
  }

  private static Result scoreWithLockHeld(int ticks) {
    resetWorldRandom();
    pinGlobals();

    ContextManager.setNodeManager(new NodeManager());
    final NodeManager node_manager = ContextManager.getNodeManager();

    final Node marker = GrasshopperDemo.buildAt(400);
    final Node[] feet = GrasshopperDemo.feet;
    final int n = node_manager.element.size();
    final Node[] nodes = new Node[n];
    for (int i = 0; i < n; i++) {
      nodes[i] = (Node) node_manager.element.get(i);
    }
    final LinkManager link_manager = node_manager.getLinkManager();
    final int n_links = link_manager.element.size();
    final Link[] links = new Link[n_links];
    for (int i = 0; i < n_links; i++) {
      links[i] = (Link) link_manager.element.get(i);
    }

    final int ground_wall = Coords.y_pixels << Coords.shift;
    final int start_y = marker.pos.y;
    int min_marker_y = start_y;
    final int[] min_foot_y = new int[feet.length];
    for (int f = 0; f < feet.length; f++) {
      min_foot_y[f] = feet[f].pos.y;
    }
    long max_speed_sq = 0;
    double max_strain = 0.0;
    double max_passive_strain = 0.0;
    // Tim's "not tipping over" rule: dorsal top node stays above the base.
    final TipOverRule tip_over = new TipOverRule(
        GrasshopperDemo.posture_top_index, GrasshopperDemo.posture_bottom_index,
        GrasshopperDemo.posture_min_separation_px);
    boolean tipped_over = false;

    for (int t = 0; t < ticks; t++) {
      node_manager.nodeAndLinkUpdate();

      if (!tipped_over && tip_over.violated(node_manager)) {
        tipped_over = true;
      }

      if (marker.pos.y < min_marker_y) {
        min_marker_y = marker.pos.y;
      }
      for (int f = 0; f < feet.length; f++) {
        if (feet[f].pos.y < min_foot_y[f]) {
          min_foot_y[f] = feet[f].pos.y;
        }
      }
      for (final Node node : nodes) {
        final long vx = node.velocity.x;
        final long vy = node.velocity.y;
        final long vz = node.velocity.z;
        final long sq = vx * vx + vy * vy + vz * vz;
        if (sq > max_speed_sq) {
          max_speed_sq = sq;
        }
      }
      for (final Link link : links) {
        final int rest = link.adjusted_rest_length;
        if (rest == 0) {
          continue;
        }
        final int actual = distance(link.nodes[0], link.nodes[1]);
        final double strain = Math.abs(actual - rest) / (double) rest;
        if (strain > max_strain) {
          max_strain = strain;
        }
        // Muscles are driven to high strain by design (that is the
        // launch); the "stay in one piece" rule applies to the passive
        // skeleton, so track its worst strain separately.
        if (link.controller == null && strain > max_passive_strain) {
          max_passive_strain = strain;
        }
      }
    }

    final int height_px = Math.max(0, (start_y - min_marker_y) >> Coords.shift);
    boolean airborne = true;
    final int slack = AIRBORNE_SLACK_PX << Coords.shift;
    for (final int y : min_foot_y) {
      if (y > ground_wall - slack) {
        airborne = false;
      }
    }
    final int max_speed_px =
        (int) (Math.sqrt((double) max_speed_sq) / (1 << Coords.shift));
    final boolean disqualified = max_speed_px > SPEED_CAP_PX_PER_TICK
        || max_passive_strain > STRAIN_CAP || tipped_over;

    final Result result = new Result();
    result.height_px = height_px;
    result.airborne = airborne;
    result.max_speed_px = max_speed_px;
    result.max_strain = max_strain;
    result.max_passive_strain = max_passive_strain;
    result.disqualified = disqualified;
    result.score = (airborne && !disqualified) ? height_px : 0;
    result.ticks = ticks;
    return result;
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }

  /** Pin down all global physics state so scored runs are identical. */
  private static void pinGlobals() {
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.global_temperature = 0;
    World.ground_friction = 100;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.max_speed = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.viscocity = 0;
    com.springie.FrEnd.three_d = true;
    com.springie.FrEnd.links_disabled = false;
    com.springie.FrEnd.continuously_centre = false;
    com.springie.FrEnd.node_growth = false;
    com.springie.FrEnd.boundaries = true;
    com.springie.FrEnd.explosions = true;
    com.springie.FrEnd.oscd = true;
    com.springie.FrEnd.dragged_element = null;
    com.springie.FrEnd.forces_disabled_during_gesture = false;
    com.springie.FrEnd.paused = false;
    com.springie.FrEnd.frame_frequency = 0;
    // The demo runs with node-node collisions off (the truss holds
    // together through structure alone); the judge pins the same.
    com.springie.FrEnd.check_collisions = false;
    com.springie.muscles.Muscles.enabled = false;
    com.springie.muscles.Muscles.active_oscillator = 0;
    // Pin the universe size: a booted GUI resizes Coords to its canvas,
    // moving the ground walls and changing the absolute score.
    com.springie.render.Coords.x_pixels = 800;
    com.springie.render.Coords.y_pixels = 600;
    com.springie.render.Coords.z_pixels = 1024;
  }

  /**
   * Resets the world's random number generator to its initial seed.
   * The physics uses this for temperature jitter and node seeds; without
   * a reset, consecutive scored runs diverge.
   */
  private static void resetWorldRandom() {
    try {
      final java.lang.reflect.Field field =
          World.class.getDeclaredField("rnd");
      field.setAccessible(true);
      final Hortensius32Fast rnd =
          (Hortensius32Fast) field.get(null);
      // GOOD_SEED = 4357 (the default seed for a new generator).
      rnd.setSeed(4357);
    } catch (Exception e) {
      throw new RuntimeException("Failed to reset World.rnd", e);
    }
  }

  public static void main(String[] args) throws Exception {
    final int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 600;
    final Result result = score(ticks);
    System.out.println(result);
    System.exit(0);
  }
}
