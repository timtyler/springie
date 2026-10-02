// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;

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
 * <p>rollingMatch: 1 when distance ~= R * theta (pure rolling), falling
 * off on both sides -- sliding (too little spin) and churning (too much
 * spin) both score down. heightStd is the stddev of the centre-of-mass
 * height over the measured ticks -- rollers hold a steady height.
 *
 * <p>Usage: java com.springie.demos.RollingJudge [ticks] [crawler]
 * The optional "crawler" argument scores the legged CrawlerDemo instead,
 * as a sanity check that the metric crushes non-rolling designs.
 * Prints: DISTANCE, THETA_REV, ROLLING_MATCH, HEIGHT_STD, SCORE, TICKS.
 */
public final class RollingJudge extends HeadlessJudge {
  private RollingJudge() {
  }

  /** Score breakdown for one judged run. */
  public static final class Result {
    /** 2D travel on the floor by the tracked node, pixels. */
    public int distance_px;
    /** Total unwrapped rotation of the marker about the hub, radians. */
    public double theta_total;
    /**
     * min(1, R*|theta| / distance), symmetric: 1 for pure rolling, falling
     * on both sides. Too little spin is sliding; too much spin is
     * churning in place (a wheel that spins 10x more than it travels is
     * not rolling, and the old one-sided cap scored it a perfect 1).
     */
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
    /** True when any node flew >400px from the hub (fell apart). */
    public boolean shattered;
    /** True when disqualified (tipped over). Score is 0 when set. */
    public boolean disqualified;
    /**
     * True when the hub reversed direction (moved -X) during the run.
     * Tim: reversals kill the marathon bonus -- a marathon runner doesn't
     * run backwards.
     */
    public boolean reversed_direction;
    /**
     * Sliding distance in pixels: max(0, distance - spin). How far the
     * wheel traveled without rolling. Pure rolling gives 0.
     */
    public double sliding_px;
    /**
     * Hub z displacement over the measured ticks, pixels. A wheel that
     * stays in its rolling plane keeps this near zero; wall-riding or
     * veering shows up here.
     */
    public int z_drift_px;
    /**
     * Mean node velocity toward +X at build time, px/frame. The wheel is
     * driven toward +X, so this is the target direction: it must be ~0
     * (a starting shove disqualifies the run). A pure spin kick nets to
     * zero and passes.
     */
    public double initial_velocity_px_per_frame;

    @Override
    public String toString() {
      return "DISTANCE " + this.distance_px
          + "\nTHETA_REV " + String.format("%.3f", this.theta_total / (2 * Math.PI))
          + "\nROLLING_MATCH " + String.format("%.3f", this.rolling_match)
          + "\nHEIGHT_STD " + String.format("%.2f", this.height_std_px)
          + "\nUPRIGHT_FRAC " + String.format("%.3f", this.upright_fraction)
          + "\nTIPPED_OVER " + this.tipped_over
          + "\nSHATTERED " + this.shattered
          + "\nREVERSED " + this.reversed_direction
          + "\nSLIDING_PX " + String.format("%.1f", this.sliding_px)
          + "\nDISQUALIFIED " + this.disqualified
          + "\nINITIAL_VELOCITY "
          + String.format("%.4f", this.initial_velocity_px_per_frame)
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
    return withModelLock(() -> scoreWithLockHeld(ticks, use_crawler));
  }

  private static Result scoreWithLockHeld(int ticks, boolean use_crawler) {
    final NodeManager node_manager = newJudgedRun();

    final Node hub;
    final Node marker;
    final int radius_px;
    if (use_crawler) {
      hub = CrawlerDemo.buildAt(100);
      // Second body node: the body doesn't rotate, so theta stays ~0.
      marker = (Node) node_manager.element.get(1);
      radius_px = CrawlerDemo.body_length_px;
    } else {
      // Built clear of the side walls (rim spans 140-460px of the 800px
      // world): the wheel must roll from its own gait, never from a
      // tick-1 kick off the left wall. Tim: runners must not touch the
      // side walls.
      hub = WheelbarrowDemo.buildAt(300);
      // Node 0 is rim0[0] at body angle 0 (see WheelbarrowDemo docs).
      marker = (Node) node_manager.element.get(0);
      radius_px = WheelbarrowDemo.rim_radius_px;
    }

    // No free shove: the model must start at rest in the target
    // direction (+X for both the wheel and the crawler sanity check).
    // Checked before the first tick; a violation disqualifies the run.
    final double initial_velocity = InitialVelocityCheck.meanAlong(
        node_manager, CompassPoint.E);
    final boolean shoved = !InitialVelocityCheck.atRestAlong(node_manager,
        CompassPoint.E);

    settle(node_manager, SETTLE_TICKS);
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
        : new AxleLevelRule(WheelbarrowDemo.posture_axle_left_index,
            WheelbarrowDemo.posture_axle_right_index,
            WheelbarrowDemo.posture_axle_max_diff_px);
    final TipOverRule tip_rule = !use_crawler ? null
        : new TipOverRule(CrawlerDemo.posture_top_index,
            CrawlerDemo.posture_bottom_index,
            CrawlerDemo.posture_min_separation_px);
    boolean tipped_over = false;
    boolean shattered = false;
    boolean reversed_direction = false;
    // Windowed reversal detection (Tim): the hub's average velocity over
    // a 60-tick window must stay positive. A hexagonal wheel jitters
    // backward 2-3px at each face transition -- that's polygonal gait,
    // not a reversal. A real direction flip drags the windowed average
    // negative. 60 ticks is ~1/8 of a 510-tick muscle period.
    final int REV_WINDOW = 60;
    final int[] x_history = new int[REV_WINDOW];
    int hist_idx = 0;
    int hist_filled = 0;

    final int measured = ticks - SETTLE_TICKS;
    for (int i = 0; i < measured; i++) {
      node_manager.nodeAndLinkUpdate();

      // Direction reversal (Tim): 60-tick windowed average velocity
      // must stay positive. Polygonal gait jitter averages out; a real
      // flip drags the window negative.
      final int cur_x = hub.pos.x >> com.springie.render.Coords.shift;
      x_history[hist_idx] = cur_x;
      hist_idx = (hist_idx + 1) % REV_WINDOW;
      if (hist_filled < REV_WINDOW) {
        hist_filled++;
      } else {
        final int oldest = x_history[hist_idx];
        if (cur_x < oldest) {
          reversed_direction = true;
        }
      }

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
      // A model that falls apart (nodes flung far from the hub) is not
      // a valid run, even if the hub happens to stay put and level --
      // without this, a shattered wheel can score "upright 1.0".
      if (!shattered && i % 60 == 0
          && shattered(node_manager, hub, 400)) {
        shattered = true;
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
    final int distance_px = distance2DPx(start_x, start_z, hub.pos.x, hub.pos.z);

    final double mean = sum / n;
    final double variance = Math.max(0.0, sum2 / n - mean * mean);
    final double height_std_px = Math.sqrt(variance);

    // Symmetric: 1 for pure rolling, punished both for sliding (too
    // little spin for the distance) and for churning (spinning far more
    // than the travel needs). The old min(1, spin/dist) capped at 1, so
    // a wheel spinning 10x too fast scored a perfect match.
    final double spin_px = radius_px * Math.abs(theta_total);
    final double rolling_match;
    if (distance_px < 1) {
      rolling_match = spin_px < 1 ? 1.0 : 0.0;
    } else if (spin_px < 1) {
      rolling_match = 0.0;
    } else {
      rolling_match =
          Math.min(spin_px / distance_px, distance_px / spin_px);
    }

    final Result result = new Result();
    result.distance_px = distance_px;
    result.theta_total = theta_total;
    result.rolling_match = rolling_match;
    result.height_std_px = height_std_px;
    result.upright_fraction = (double) upright_ticks / measured;
    result.tipped_over = tipped_over;
    result.shattered = shattered;
    result.reversed_direction = reversed_direction;
    // Sliding: distance traveled without rolling. Penalize explicitly.
    result.sliding_px = Math.max(0.0, distance_px - spin_px);
    result.disqualified = tipped_over || shoved || shattered;
    result.initial_velocity_px_per_frame = initial_velocity;
    result.z_drift_px =
        (hub.pos.z - start_z) >> com.springie.render.Coords.shift;
    // Score: rolling match rewards pure rolling; sliding penalty punishes
    // sliding; reversals kill the marathon bonus (score halved).
    double base = distance_px * rolling_match - 3.0 * height_std_px
        - result.sliding_px;
    if (reversed_direction) {
      base *= 0.5;
    }
    result.score = result.disqualified ? 0.0 : Math.max(0.0, base);
    result.ticks = ticks;
    return result;
  }

  /** Marker angle about the hub in the XY (rolling) plane, radians. */
  private static double angleOf(Node marker, Node hub) {
    return Math.atan2((double) (marker.pos.y - hub.pos.y),
        (double) (marker.pos.x - hub.pos.x));
  }

  public static void main(String[] args) throws Exception {
    final int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 600;
    final boolean use_crawler = args.length > 1 && "crawler".equals(args[1]);
    final Result result = score(ticks, use_crawler);
    System.out.println(result);
    System.exit(0);
  }
}
