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
 * <p>Metric: distance traveled by the tracked node, multiplied by how
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
    /** 3D distance traveled by the tracked node, pixels. */
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

    @Override
    public String toString() {
      return "DISTANCE " + this.distance_px
          + "\nTHETA_REV " + String.format("%.3f", this.theta_total / (2 * Math.PI))
          + "\nROLLING_MATCH " + String.format("%.3f", this.rolling_match)
          + "\nHEIGHT_STD " + String.format("%.2f", this.height_std_px)
          + "\nSCORE " + String.format("%.1f", this.score)
          + "\nTICKS " + this.ticks;
    }
  }

  /** Ticks skipped at the start so the model can settle. */
  public static final int SETTLE_TICKS = 60;

  /**
   * Scores the wheel (or, if use_crawler, the legged crawler) over the
   * given number of ticks. Deterministic: two calls give identical results.
   */
  public static Result score(int ticks, boolean use_crawler) {
    // Reset the world's RNG so every scored run starts from identical
    // initial conditions (temperature jitter and node seeds).
    resetWorldRandom();

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
    final int start_y = hub.pos.y;
    final int start_z = hub.pos.z;

    double theta_prev = angleOf(marker, hub);
    double theta_total = 0.0;

    double sum = 0.0;
    double sum2 = 0.0;
    int n = 0;

    final int measured = ticks - SETTLE_TICKS;
    for (int i = 0; i < measured; i++) {
      node_manager.nodeAndLinkUpdate();

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

    final long dx = (long) hub.pos.x - start_x;
    final long dy = (long) hub.pos.y - start_y;
    final long dz = (long) hub.pos.z - start_z;
    final int dist_internal = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
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
    result.score = distance_px * rolling_match - 3.0 * height_std_px;
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
