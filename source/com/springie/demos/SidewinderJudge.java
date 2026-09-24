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
 * Truly headless judge that scores a sidewinder design. Does NOT start FrEnd
 * (no GUI, no animation thread) -- single-threaded and deterministic.
 * Needs DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Metric: 2D travel on the floor by the mid-body node, weighted by how well
 * the body holds its tubular shape:
 * score = distance * length_keep * xs_keep,
 * where length_keep is the head-to-tail length at the end of the run as a
 * fraction of the build-time length, and xs_keep is the same for the
 * mean cross-section radius about the body axis. A sidewinder that collapses
 * into a squirming ball scores near zero even if it travels.
 *
 * <p>No tip-over rule for the sidewinder: its triangular cross-section
 * rolls about the body axis as part of the gait (the top node cycles
 * through the cross-section vertices), so a top-stays-on-top rule would
 * false-positive on healthy locomotion. It is judged on distance and
 * shape retention instead.
 *
 * <p>Usage: java com.springie.demos.SidewinderJudge [ticks]
 * Prints: DISTANCE, LENGTH_KEEP, XS_KEEP, STRAIN_P10, STRAIN_P20,
 * SCORE, TICKS.
 */
public final class SidewinderJudge {
  private SidewinderJudge() {
  }

  /** Score breakdown for one judged run. */
  public static final class Result {
    /** 2D travel on the floor by the mid-body node, pixels. */
    public int distance_px;
    /** End head-to-tail length / build head-to-tail length. */
    public double length_keep;
    /** End cross-section radius / build cross-section radius. */
    public double xs_keep;
    /** Fraction of links >10% off their (adjusted) rest length at the end. */
    public double strain_p10;
    /** Fraction of links >20% off their (adjusted) rest length at the end. */
    public double strain_p20;
    /**
     * Mean node speed in the floor plane at build time, px/frame. The
     * sidewinder scores unsigned 2D travel, so a shove in any floor
     * direction cheats: this must be ~0, else the run is disqualified.
     */
    public double initial_velocity_px_per_frame;
    /** True when the run was disqualified (starting shove). */
    public boolean disqualified;
    /** distance * length_keep * xs_keep, or 0 if disqualified. */
    public double score;
    /** Total ticks simulated (including settling). */
    public int ticks;

    @Override
    public String toString() {
      return "DISTANCE " + this.distance_px
          + "\nLENGTH_KEEP " + String.format("%.3f", this.length_keep)
          + "\nXS_KEEP " + String.format("%.3f", this.xs_keep)
          + "\nSTRAIN_P10 " + String.format("%.3f", this.strain_p10)
          + "\nSTRAIN_P20 " + String.format("%.3f", this.strain_p20)
          + "\nDISQUALIFIED " + this.disqualified
          + "\nINITIAL_VELOCITY "
          + String.format("%.4f", this.initial_velocity_px_per_frame)
          + "\nSCORE " + String.format("%.1f", this.score)
          + "\nTICKS " + this.ticks;
    }
  }

  /** Ticks skipped at the start so the model can settle. */
  public static final int SETTLE_TICKS = 60;

  /** Shape snapshot: head/tail centroids and cross-section radius. */
  private static final class Shape {
    double length;
    double xs;
  }

  /**
   * Scores the sidewinder over the given number of ticks. Deterministic: two
   * calls give identical results, even in a JVM where a GUI test has left
   * the animation thread running.
   */
  public static Result score(int ticks) {
    // Hold the model lock for the whole run. A GUI test's animation thread
    // never stops: it keeps repainting, and the AWT thread would otherwise
    // step physics on this run's NodeManager concurrently with the loop
    // below (extra ticks plus data races on node positions), so two
    // back-to-back runs diverge. This is the same lock the AWT renderer
    // and the message pump already use (see
    // RendererDelegator.redrawChanged); the physics path never needs the
    // AWT tree lock, so this cannot deadlock.
    synchronized (ContextManager.class) {
      return scoreWithLockHeld(ticks);
    }
  }

  private static Result scoreWithLockHeld(int ticks) {
    resetWorldRandom();
    pinGlobals();

    ContextManager.setNodeManager(new NodeManager());
    final NodeManager node_manager = ContextManager.getNodeManager();

    SidewinderDemo.buildAt(400);
    final int n = node_manager.element.size();
    final Node[] nodes = new Node[n];
    for (int i = 0; i < n; i++) {
      nodes[i] = (Node) node_manager.element.get(i);
    }
    // No free shove: the sidewinder scores unsigned 2D travel, so it
    // must start at rest in the floor plane. Checked before the first
    // tick; a violation disqualifies the run.
    final double initial_velocity =
        InitialVelocityCheck.meanFloorSpeed(node_manager);
    final boolean shoved =
        !InitialVelocityCheck.atRestOnFloor(node_manager);

    final Shape build_shape = measureShape(nodes);
    final Node mid = nodes[n / 2];

    for (int i = 0; i < SETTLE_TICKS; i++) {
      node_manager.nodeAndLinkUpdate();
    }
    final int start_x = mid.pos.x;
    final int start_z = mid.pos.z;

    final int measured = ticks - SETTLE_TICKS;
    for (int i = 0; i < measured; i++) {
      node_manager.nodeAndLinkUpdate();
    }

    // 2D travel on the floor: vertical motion doesn't count.
    final long dx = (long) mid.pos.x - start_x;
    final long dz = (long) mid.pos.z - start_z;
    final int dist_internal =
        (int) Math.sqrt((double) dx * dx + (double) dz * dz);
    final int distance_px = dist_internal >> Coords.shift;

    final Shape end_shape = measureShape(nodes);
    final double length_keep = end_shape.length / build_shape.length;
    final double xs_keep = end_shape.xs / build_shape.xs;

    final LinkManager link_manager = node_manager.getLinkManager();
    final int n_links = link_manager.element.size();
    int over10 = 0;
    int over20 = 0;
    for (int i = 0; i < n_links; i++) {
      final Link link = (Link) link_manager.element.get(i);
      final int rest = link.adjusted_rest_length;
      if (rest == 0) {
        continue;
      }
      final int actual = distance(link.nodes[0], link.nodes[1]);
      final double strain = Math.abs(actual - rest) / (double) rest;
      if (strain > 0.10) {
        over10++;
      }
      if (strain > 0.20) {
        over20++;
      }
    }

    final Result result = new Result();
    result.distance_px = distance_px;
    result.length_keep = length_keep;
    result.xs_keep = xs_keep;
    result.strain_p10 = (double) over10 / n_links;
    result.strain_p20 = (double) over20 / n_links;
    result.initial_velocity_px_per_frame = initial_velocity;
    result.disqualified = shoved;
    result.score = shoved ? 0.0 : distance_px * length_keep * xs_keep;
    result.ticks = ticks;
    return result;
  }

  /** Head-to-tail length and mean cross-section radius about the axis. */
  private static Shape measureShape(Node[] nodes) {
    final int n = nodes.length;
    final double[] c1 = centroid(nodes, 0, 4);
    final double[] c2 = centroid(nodes, n - 4, n);
    double dx = c2[0] - c1[0];
    double dy = c2[1] - c1[1];
    double dz = c2[2] - c1[2];
    final double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
    final Shape shape = new Shape();
    shape.length = len / (1 << Coords.shift);
    if (len < 1e-9) {
      shape.xs = 0.0;
      return shape;
    }
    dx /= len;
    dy /= len;
    dz /= len;
    double sum = 0.0;
    for (final Node node : nodes) {
      final double ox = node.pos.x - c1[0];
      final double oy = node.pos.y - c1[1];
      final double oz = node.pos.z - c1[2];
      final double along = ox * dx + oy * dy + oz * dz;
      final double px = ox - along * dx;
      final double py = oy - along * dy;
      final double pz = oz - along * dz;
      sum += Math.sqrt(px * px + py * py + pz * pz);
    }
    shape.xs = sum / n / (1 << Coords.shift);
    return shape;
  }

  private static double[] centroid(Node[] nodes, int from, int to) {
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    for (int i = from; i < to; i++) {
      x += nodes[i].pos.x;
      y += nodes[i].pos.y;
      z += nodes[i].pos.z;
    }
    final int count = to - from;
    return new double[] {x / count, y / count, z / count};
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
    World.global_temperature = 6;
    World.ground_friction = 100;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.max_speed = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.viscocity = 0;
    com.springie.FrEnd.three_d = true;
    com.springie.FrEnd.check_collisions = true;
    com.springie.FrEnd.continuously_centre_x = false;
    com.springie.FrEnd.continuously_centre_y = false;
    com.springie.FrEnd.continuously_centre_z = false;
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
