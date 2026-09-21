// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.utilities.random.Hortensius32Fast;
import com.springie.world.World;

/**
 * Truly headless judge that scores a rolling design. Does NOT start FrEnd
 * (no GUI, no animation thread) -- single-threaded and deterministic.
 * Needs DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Metric: 2D travel on the floor by the tracked node, multiplied by how
 * well the motion matches pure rolling, minus a penalty for vertical
 * bobbing (which is what legged designs do):
 * score = distance * rollingMatch - 3 * heightStd.
 *
 * <p>rollingMatch = min(1, R * |theta_total| / distance): a smooth roller
 * has distance ~= R * theta (pure rolling ~= 1); sliders, hoppers, and
 * legged walkers score lower. heightStd is the stddev of the centre-of-mass
 * height over the measured ticks -- rollers hold a steady height.
 *
 * <p>Usage: java com.springie.demos.RollingJudge [ticks] [crawler]
 * The optional "crawler" argument scores the legged CrawlerDemo instead,
 * as a sanity check that the metric crushes non-rolling designs.
 * Prints: DISTANCE, THETA_REV, ROLLING_MATCH, HEIGHT_STD, SCORE, TICKS.
 */
public final class RollingJudge {
  private RollingJudge() {
  }

  /** Score breakdown for one judged run. */
  public static final class Result {
    /** 2D travel on the floor by the tracked node, pixels. */
    public int distance_px;
    /** Total unwrapped rotation of the marker about the hub, radians. */
    public double theta_total;
    /** min(1, R*|theta| / distance): 1 for pure rolling. */
    public double rolling_match;
    /** Stddev of centre-of-mass height over measured ticks, pixels. */
    public double height_std_px;
    /** distance * rolling_match - 3 * height_std. */
    public double score;
    /** Total ticks simulated (including settling). */
    public int ticks;
    /**
     * Fraction of measured ticks the posture rule held (1.0 = never tipped;
     * wheel = axle level, crawler = dorsal top above belly). Tim's "not
     * tipping over" disqualification: any tip-over disqualifies the run.
     */
    public double upright_fraction;
    /** True when the posture rule was violated at least once. */
    public boolean tipped_over;
    /** True when disqualified (tipped over). Score is 0 when set. */
    public boolean disqualified;
    /**
     * Hub z displacement over the measured ticks, pixels. A wheel that
     * stays in its rolling plane keeps this near zero; wall-riding or
     * veering shows up here.
     */
    public int z_drift_px;

    @Override
    public String toString() {
      return "DISTANCE " + this.distance_px
          + "\nTHETA_REV " + String.format("%.3f", this.theta_total / (2 * Math.PI))
          + "\nROLLING_MATCH " + String.format("%.3f", this.rolling_match)
          + "\nHEIGHT_STD " + String.format("%.2f", this.height_std_px)
          + "\nUPRIGHT_FRAC " + String.format("%.3f", this.upright_fraction)
          + "\nTIPPED_OVER " + this.tipped_over
          + "\nDISQUALIFIED " + this.disqualified
          + "\nZ_DRIFT " + this.z_drift_px
          + "\nSCORE " + String.format("%.1f", this.score)
          + "\nTICKS " + this.ticks;
    }
  }

  /** Ticks skipped at the start so the model can settle. */
  public static final int SETTLE_TICKS = 60;

  /**
   * Scores the wheel (or, if use_crawler, the legged crawler) over the
   * given number of ticks. Deterministic: two calls give identical results,
   * even in a JVM where a GUI test has left the animation thread running.
   */
  public static Result score(int ticks, boolean use_crawler) {
    // Hold the model lock for the whole run. A GUI test's animation thread
    // never stops: it keeps repainting, and the AWT thread would otherwise
    // step physics on this run's NodeManager concurrently with the loop
    // below (extra ticks plus data races on node positions), so two
    // back-to-back runs diverge. This is the same lock the AWT renderer
    // and the message pump already use (see
    // RendererDelegator.redrawChanged); the physics path never needs the
    // AWT tree lock, so this cannot deadlock.
    synchronized (ContextManager.class) {
      return scoreWithLockHeld(ticks, use_crawler);
    }
  }

  private static Result scoreWithLockHeld(int ticks, boolean use_crawler) {
    // Reset the world's RNG so every scored run starts from identical
    // initial conditions (temperature jitter and node seeds).
    resetWorldRandom();

    // Pin down all global physics state. Earlier tests (especially GUI
    // tests) leak values into these statics; the demos' buildAt methods
    // set only a subset, so two consecutive score() calls could otherwise
    // diverge in a polluted suite.
    // NOTE: temperature is NOT pinned here -- WheelDemo.buildAt sets
    // World.global_temperature = 0 (deterministic build; Caterpillar2Demo
    // and SlinkyDemo do the same). Pinning 6 here would be dead code
    // because buildAt overrides it.
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = 100;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.max_speed = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.viscocity = 0;
    com.springie.FrEnd.three_d = true;
    com.springie.FrEnd.check_collisions = true;
    com.springie.FrEnd.continuously_centre = false;
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

    ContextManager.setNodeManager(new NodeManager());
    final NodeManager node_manager = ContextManager.getNodeManager();

    final Node hub;
    final Node marker;
    final int radius_px;
    if (use_crawler) {
      hub = CrawlerDemo.buildAt(100);
      // Second body node: the body doesn't rotate, so theta stays ~0.
      marker = (Node) node_manager.element.get(1);
      radius_px = CrawlerDemo.body_edge_px;
    } else {
      hub = WheelDemo.buildAt(120);
      // Node 0 is rim0[0] at body angle 0 (see WheelDemo docs).
      marker = (Node) node_manager.element.get(0);
      radius_px = WheelDemo.rim_radius_px;
    }

    for (int i = 0; i < SETTLE_TICKS; i++) {
      node_manager.nodeAndLinkUpdate();
    }
    final int start_x = hub.pos.x;
    final int start_z = hub.pos.z;

    double theta_prev = angleOf(marker, hub);
    double theta_total = 0.0;

    double sum = 0.0;
    double sum2 = 0.0;
    int n = 0;
    int upright_ticks = 0;
    // Tim's "not tipping over" rule: wheel = axle stays level (two axle-end
    // nodes); crawler = dorsal top node stays above the belly reference.
    final AxleLevelRule axle_rule = use_crawler ? null
        : new AxleLevelRule(WheelDemo.posture_axle_left_index,
            WheelDemo.posture_axle_right_index,
            WheelDemo.posture_axle_max_diff_px);
    final TipOverRule tip_rule = !use_crawler ? null
        : new TipOverRule(CrawlerDemo.posture_top_index,
            CrawlerDemo.posture_bottom_index,
            CrawlerDemo.posture_min_separation_px);
    boolean tipped_over = false;

    final int measured = ticks - SETTLE_TICKS;
    for (int i = 0; i < measured; i++) {
      node_manager.nodeAndLinkUpdate();

      final boolean ok;
      if (use_crawler) {
        ok = !tip_rule.violated(node_manager);
      } else {
        ok = !axle_rule.violated(node_manager);
      }
      if (ok) {
        upright_ticks++;
      } else {
        tipped_over = true;
      }

      final double theta = angleOf(marker, hub);
      double delta = theta - theta_prev;
      while (delta > Math.PI) {
        delta -= 2.0 * Math.PI;
      }
      while (delta <= -Math.PI) {
        delta += 2.0 * Math.PI;
      }
      theta_total += delta;
      theta_prev = theta;

      final double h = comHeightPx(node_manager);
      sum += h;
      sum2 += h * h;
      n++;
    }

    // 2D travel on the floor: vertical motion doesn't count.
    final long dx = (long) hub.pos.x - start_x;
    final long dz = (long) hub.pos.z - start_z;
    final int dist_internal = (int) Math.sqrt(dx * dx + dz * dz);
    final int distance_px = dist_internal >> Coords.shift;

    final double mean = sum / n;
    final double variance = Math.max(0.0, sum2 / n - mean * mean);
    final double height_std_px = Math.sqrt(variance);

    final double rolling_match = Math.min(1.0,
        radius_px * Math.abs(theta_total) / Math.max(distance_px, 1));

    final Result result = new Result();
    result.distance_px = distance_px;
    result.theta_total = theta_total;
    result.rolling_match = rolling_match;
    result.height_std_px = height_std_px;
    result.upright_fraction = (double) upright_ticks / measured;
    result.tipped_over = tipped_over;
    result.disqualified = tipped_over;
    result.z_drift_px =
        (hub.pos.z - start_z) >> com.springie.render.Coords.shift;
    result.score = result.disqualified ? 0.0
        : distance_px * rolling_match - 3.0 * height_std_px;
    result.ticks = ticks;
    return result;
  }

  /** Marker angle about the hub in the XY (rolling) plane, radians. */
  private static double angleOf(Node marker, Node hub) {
    return Math.atan2((double) (marker.pos.y - hub.pos.y),
        (double) (marker.pos.x - hub.pos.x));
  }

  /** Mean node height (centre-of-mass height), in pixels. */
  private static double comHeightPx(NodeManager node_manager) {
    final int n = node_manager.element.size();
    long sum = 0;
    for (int i = 0; i < n; i++) {
      sum += ((Node) node_manager.element.get(i)).pos.y;
    }
    return (double) (sum >> Coords.shift) / n;
  }

  /**
   * Resets the world's random number generator to its initial seed.
   * The physics uses this for temperature jitter and node seeds; without
   * a reset, consecutive scored runs diverge (the wheel is chaotic).
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
    final boolean use_crawler = args.length > 1 && "crawler".equals(args[1]);
    final Result result = score(ticks, use_crawler);
    System.out.println(result);
    System.exit(0);
  }
}
