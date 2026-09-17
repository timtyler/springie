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
 * Truly headless judge that scores the hopper. Does NOT start FrEnd (no
 * GUI, no animation thread) -- single-threaded and deterministic. Needs
 * DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Rules:
 * <ol>
 * <li>Muscle power only: the demo applies no start kick; every hop comes
 * from the leg extensors pushing against the ground.</li>
 * <li>Score = the fraction of ticks fully airborne (every foot off the
 * ground), times a streak bonus for consecutive hops:
 * {@code score = air_fraction * (1 + 0.25 * min(max_streak, 8))}.
 * A hop is an airborne phase of at least {@link #MIN_HOP_TICKS} ticks
 * bookended by ground contact; each hop's touchdown must be feet-first
 * or the streak resets.</li>
 * <li>Feet-first landings only: at every touchdown, every non-foot node
 * must sit at least {@link #FEET_FIRST_SLACK_PX} above the lowest foot.
 * A body-slam landing breaks the streak.</li>
 * <li>The hopper must stay in one piece: any node exceeding
 * {@link #SPEED_CAP_PX_PER_TICK} or any passive link exceeding
 * {@link #STRAIN_CAP} strain (against its adjusted rest length)
 * disqualifies the run (score 0).</li>
 * </ol>
 *
 * <p>Usage: java com.springie.demos.HopperJudge [ticks]
 */
public final class HopperJudge {
  private HopperJudge() {
  }

  /** A foot counts as airborne once it rises this far above the wall. */
  public static final int AIRBORNE_SLACK_PX = 6;
  /**
   * A hop must stay airborne at least this many ticks; shorter
   * airborne blips are jitter, not hops.
   */
  public static final int MIN_HOP_TICKS = 5;
  /**
   * Feet-first bar, px: at touchdown every non-foot node must be at
   * least this far above the lowest foot.
   */
  public static final int FEET_FIRST_SLACK_PX = 8;
  /**
   * Speed disqualification bar, px/tick. Hopping moves at a few
   * px/tick; numerical explosions hit thousands.
   */
  public static final int SPEED_CAP_PX_PER_TICK = 250;
  /**
   * Strain disqualification bar, fraction of adjusted rest length.
   * Applies to passive links only: the skeleton must hold together.
   * (Muscle strain is reported but not disqualifying -- high muscle
   * strain is the hop itself.)
   */
  public static final double STRAIN_CAP = 1.5;
  /** Streak bonus per consecutive hop, capped at 8 hops. */
  public static final double STREAK_BONUS_PER_HOP = 0.25;
  /** Streak cap for the bonus. */
  public static final int STREAK_CAP = 8;

  /** Score breakdown for one judged run. */
  public static final class Result {
    /** Fraction of ticks fully airborne (all feet off the ground). */
    public double air_fraction;
    /** Ticks fully airborne. */
    public int air_ticks;
    /** Number of valid hops (airborne >= MIN_HOP_TICKS, feet-first). */
    public int hops;
    /** Longest run of consecutive valid hops. */
    public int max_streak;
    /** Highest point of the crown marker above its build height, px. */
    public int max_height_px;
    /** Fastest any node moved, px/tick. */
    public int max_speed_px;
    /** Worst strain on a passive (non-muscle) link. */
    public double max_passive_strain;
    /** True when the run was disqualified (exploded). */
    public boolean disqualified;
    /** True when Tim's "not tipping over" rule was violated. */
    public boolean tipped_over;
    /** Lowest/highest marker x seen, pixels (drift check). */
    public int min_marker_x_px;
    public int max_marker_x_px;
    /** air_fraction * (1 + 0.25 * min(max_streak, 8)), or 0 if disqualified. */
    public double score;
    /** Total ticks simulated. */
    public int ticks;

    @Override
    public String toString() {
      return "AIR_FRACTION " + String.format("%.3f", this.air_fraction)
          + "\nAIR_TICKS " + this.air_ticks
          + "\nHOPS " + this.hops
          + "\nMAX_STREAK " + this.max_streak
          + "\nMAX_HEIGHT_PX " + this.max_height_px
          + "\nMAX_SPEED_PX " + this.max_speed_px
          + "\nMAX_PASSIVE_STRAIN "
          + String.format("%.3f", this.max_passive_strain)
          + "\nDISQUALIFIED " + this.disqualified
          + "\nTIPPED_OVER " + this.tipped_over
          + "\nMARKER_X_MIN_PX " + this.min_marker_x_px
          + "\nMARKER_X_MAX_PX " + this.max_marker_x_px
          + "\nSCORE " + String.format("%.4f", this.score)
          + "\nTICKS " + this.ticks;
    }
  }

  /**
   * Scores the hopper over the given number of ticks. Deterministic:
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

    final Node marker = HopperDemo.buildAt(400);
    final Node[] feet = HopperDemo.feet;
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
    final boolean[] is_foot = new boolean[n];
    for (final Node foot : feet) {
      for (int i = 0; i < n; i++) {
        if (nodes[i] == foot) {
          is_foot[i] = true;
        }
      }
    }

    final int ground_wall = Coords.y_pixels << Coords.shift;
    final int slack = AIRBORNE_SLACK_PX << Coords.shift;
    final int feet_first_slack = FEET_FIRST_SLACK_PX << Coords.shift;
    // Tim's "not tipping over" rule: the dorsal top node must stay above
    // the belly reference node for the whole run.
    final TipOverRule tip_over = new TipOverRule(HopperDemo.posture_top_index,
        HopperDemo.posture_bottom_index, HopperDemo.posture_min_separation_px);
    boolean tipped_over = false;
    final int start_y = marker.pos.y;
    int min_marker_y = start_y;
    int min_marker_x = marker.pos.x;
    int max_marker_x = marker.pos.x;
    long max_speed_sq = 0;
    double max_passive_strain = 0.0;

    int air_ticks = 0;
    int hops = 0;
    int streak = 0;
    int max_streak = 0;
    boolean was_airborne = false;
    int hop_start = 0;

    for (int t = 0; t < ticks; t++) {
      node_manager.nodeAndLinkUpdate();

      if (!tipped_over && tip_over.violated(node_manager)) {
        tipped_over = true;
      }

      if (marker.pos.y < min_marker_y) {
        min_marker_y = marker.pos.y;
      }
      if (marker.pos.x < min_marker_x) {
        min_marker_x = marker.pos.x;
      }
      if (marker.pos.x > max_marker_x) {
        max_marker_x = marker.pos.x;
      }

      boolean all_air = true;
      for (final Node foot : feet) {
        if (foot.pos.y >= ground_wall - slack) {
          all_air = false;
          break;
        }
      }

      if (all_air) {
        air_ticks++;
        if (!was_airborne) {
          hop_start = t;
        }
      } else if (was_airborne) {
        // Touchdown: the airborne phase just ended.
        final int duration = t - hop_start;
        if (duration >= MIN_HOP_TICKS && feetFirst(nodes, is_foot, feet_first_slack)) {
          hops++;
          streak++;
          if (streak > max_streak) {
            max_streak = streak;
          }
        } else {
          // Too short to be a hop, or a body-slam: the streak breaks.
          streak = 0;
        }
      }
      was_airborne = all_air;

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
        if (link.controller != null) {
          continue;
        }
        final int rest = link.adjusted_rest_length;
        if (rest == 0) {
          continue;
        }
        final int actual = distance(link.nodes[0], link.nodes[1]);
        final double strain = Math.abs(actual - rest) / (double) rest;
        if (strain > max_passive_strain) {
          max_passive_strain = strain;
        }
      }
    }

    final double air_fraction = (double) air_ticks / ticks;
    final int max_height_px = Math.max(0, (start_y - min_marker_y) >> Coords.shift);
    final int max_speed_px =
        (int) (Math.sqrt((double) max_speed_sq) / (1 << Coords.shift));
    final boolean disqualified = max_speed_px > SPEED_CAP_PX_PER_TICK
        || max_passive_strain > STRAIN_CAP || tipped_over;
    final double score = disqualified ? 0.0
        : air_fraction * (1.0 + STREAK_BONUS_PER_HOP * Math.min(max_streak, STREAK_CAP));

    final Result result = new Result();
    result.air_fraction = air_fraction;
    result.air_ticks = air_ticks;
    result.hops = hops;
    result.max_streak = max_streak;
    result.max_height_px = max_height_px;
    result.max_speed_px = max_speed_px;
    result.max_passive_strain = max_passive_strain;
    result.disqualified = disqualified;
    result.tipped_over = tipped_over;
    result.min_marker_x_px = min_marker_x >> Coords.shift;
    result.max_marker_x_px = max_marker_x >> Coords.shift;
    result.score = score;
    result.ticks = ticks;
    return result;
  }

  /**
   * Feet-first check at touchdown: every non-foot node must sit at
   * least feet_first_slack above the lowest foot (in y-down pixels,
   * non-foot nodes must have smaller y).
   */
  private static boolean feetFirst(Node[] nodes, boolean[] is_foot, int feet_first_slack) {
    int max_foot_y = Integer.MIN_VALUE;
    for (int i = 0; i < nodes.length; i++) {
      if (is_foot[i] && nodes[i].pos.y > max_foot_y) {
        max_foot_y = nodes[i].pos.y;
      }
    }
    for (int i = 0; i < nodes.length; i++) {
      if (!is_foot[i] && nodes[i].pos.y > max_foot_y - feet_first_slack) {
        return false;
      }
    }
    return true;
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
    // The demo pins temp=0 in buildAt (deterministic physics); the
    // judge must not override it afterwards.
    World.global_temperature = 0;
    World.ground_friction = 100;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.max_speed = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.viscocity = 0;
    com.springie.FrEnd.three_d = true;
    com.springie.FrEnd.check_collisions = false;
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
