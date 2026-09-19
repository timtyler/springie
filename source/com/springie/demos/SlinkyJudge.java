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
 * Truly headless judge that scores the slinky. Does NOT start FrEnd
 * (no GUI, no animation thread) -- single-threaded and deterministic.
 * Needs DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Rules:
 * <ol>
 * <li>Muscle power only: the demo applies no start kick; every pixel of
 * travel comes from the kicker diagonals' ground-contact reflex.</li>
 * <li>The slinky must take steps: a step is 45 degrees of forward flip
 * (one octagon face planting after the next), measured by tracking a
 * reference node's angle about the coil's centre. Fewer than
 * {@link #MIN_STEPS} steps scores 0 -- sliding or hopping in place is not
 * stepping.</li>
 * <li>Score = forward distance (px) of the coil's centre along +x, times
 * a straightness factor: {@code score = forward * (1 - min(1, lateral /
 * max(forward, 1)))}. The slinky must step in a straight line; sideways
 * veering is penalised hard. Non-positive forward travel scores as-is
 * (no bonus).</li>
 * <li>The coil must stay in one piece: any node exceeding
 * {@link #SPEED_CAP_PX_PER_TICK} or any passive link exceeding
 * {@link #STRAIN_CAP} strain (against its adjusted rest length)
 * disqualifies the run (score 0).</li>
 * </ol>
 *
 * <p>Usage: java com.springie.demos.SlinkyJudge [ticks]
 */
public final class SlinkyJudge {
  private SlinkyJudge() {
  }

  /**
   * Speed disqualification bar, px/tick. Stepping moves at a few
   * px/tick; numerical explosions hit hundreds.
   */
  public static final int SPEED_CAP_PX_PER_TICK = 100;
  /**
   * Strain disqualification bar, fraction of adjusted rest length.
   * Applies to passive links only: the skeleton must hold together.
   */
  public static final double STRAIN_CAP = 1.0;
  /** Ticks skipped before measuring, so the build can find its stance. */
  public static final int SETTLE_TICKS = 60;
  /**
   * Minimum counted flip-steps for a valid score. The slinky's whole
   * point is stepping one after another; fewer than this means it slid,
   * hopped in place, or fell over.
   */
  public static final int MIN_STEPS = 3;
  /** Radians of forward flip per counted step (one octagon face). */
  public static final double STEP_RADIANS = Math.PI / 4.0;

  /** Score breakdown for one judged run. */
  public static final class Result {
    /** Forward travel of the coil's centre along +x, pixels. */
    public int distance_px;
    /** Lateral wander of the coil's centre along z, pixels. */
    public int lateral_px;
    /** Straightness factor applied to the score, 0..1. */
    public double straightness;
    /** Counted 45-degree forward flip-steps. */
    public int steps;
    /** Fastest any node moved, px/tick. */
    public int max_speed_px;
    /** Worst strain on a passive (non-muscle) link. */
    public double max_passive_strain;
    /** True when the run was disqualified (exploded). */
    public boolean disqualified;
    /** distance * straightness, or 0 if disqualified / too few steps. */
    public double score;
    /** Total ticks simulated (including settling). */
    public int ticks;

    @Override
    public String toString() {
      return "DISTANCE_PX " + this.distance_px
          + "\nLATERAL_PX " + this.lateral_px
          + "\nSTRAIGHTNESS " + String.format("%.3f", this.straightness)
          + "\nSTEPS " + this.steps
          + "\nMAX_SPEED_PX " + this.max_speed_px
          + "\nMAX_PASSIVE_STRAIN "
          + String.format("%.3f", this.max_passive_strain)
          + "\nDISQUALIFIED " + this.disqualified
          + "\nSCORE " + String.format("%.1f", this.score)
          + "\nTICKS " + this.ticks;
    }
  }

  /**
   * Scores the slinky over the given number of ticks. Deterministic:
   * two calls give identical results, even in a JVM where a GUI test has
   * left the animation thread running.
   */
  public static Result score(int ticks) {
    // Hold the model lock for the whole run (see SidewinderJudge for why).
    synchronized (ContextManager.class) {
      return scoreWithLockHeld(ticks);
    }
  }

  private static Result scoreWithLockHeld(int ticks) {
    resetWorldRandom();
    pinGlobals();

    ContextManager.setNodeManager(new NodeManager());
    final NodeManager node_manager = ContextManager.getNodeManager();

    SlinkyDemo.buildAt(400);
    final Node[] nodes = SlinkyDemo.coil_nodes;
    final Node ref = SlinkyDemo.reference_node;
    final int n = nodes.length;
    final LinkManager link_manager = node_manager.getLinkManager();
    final int n_links = link_manager.element.size();
    final Link[] links = new Link[n_links];
    for (int i = 0; i < n_links; i++) {
      links[i] = (Link) link_manager.element.get(i);
    }

    for (int i = 0; i < SETTLE_TICKS; i++) {
      node_manager.nodeAndLinkUpdate();
    }
    final double start_x = centreX(nodes);
    final double start_z = centreZ(nodes);

    long max_speed_sq = 0;
    double max_passive_strain = 0.0;
    // No tip-over rule for the slinky: end-over-end flipping IS its gait
    // (Tim: "behave like a slinky going downstairs -- lift the rear, raise
    // it high, put it down in front"). It is judged on periodic stepping,
    // straightness, and forward progress instead.
    double prev_angle = refAngle(ref, nodes);
    double forward_angle = 0.0;
    final int measured = ticks - SETTLE_TICKS;
    for (int t = 0; t < measured; t++) {
      node_manager.nodeAndLinkUpdate();

      final double angle = refAngle(ref, nodes);
      double delta = angle - prev_angle;
      if (delta > Math.PI) {
        delta -= 2.0 * Math.PI;
      } else if (delta < -Math.PI) {
        delta += 2.0 * Math.PI;
      }
      if (delta > 0) {
        forward_angle += delta;
      }
      prev_angle = angle;

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

    final double end_x = centreX(nodes);
    final double end_z = centreZ(nodes);
    final int distance_px = (int) ((end_x - start_x) / (1 << Coords.shift));
    final int lateral_px =
        (int) (Math.abs(end_z - start_z) / (1 << Coords.shift));
    final double straightness = distance_px <= 0 ? 1.0
        : 1.0 - Math.min(1.0, lateral_px / (double) Math.max(distance_px, 1));
    final int steps = (int) (forward_angle / STEP_RADIANS);
    final int max_speed_px =
        (int) (Math.sqrt((double) max_speed_sq) / (1 << Coords.shift));
    final boolean disqualified = max_speed_px > SPEED_CAP_PX_PER_TICK
        || max_passive_strain > STRAIN_CAP;
    final double score =
        (disqualified || steps < MIN_STEPS) ? 0.0 : distance_px * straightness;

    final Result result = new Result();
    result.distance_px = distance_px;
    result.lateral_px = lateral_px;
    result.straightness = straightness;
    result.steps = steps;
    result.max_speed_px = max_speed_px;
    result.max_passive_strain = max_passive_strain;
    result.disqualified = disqualified;
    result.score = score;
    result.ticks = ticks;
    return result;
  }

  private static double centreX(Node[] nodes) {
    double x = 0.0;
    for (final Node node : nodes) {
      x += node.pos.x;
    }
    return x / nodes.length;
  }

  private static double centreZ(Node[] nodes) {
    double z = 0.0;
    for (final Node node : nodes) {
      z += node.pos.z;
    }
    return z / nodes.length;
  }

  /**
   * Angle of the reference node about the coil's centre in the x-y
   * plane. Forward flips (top of the coil moving toward +x) increase
   * this angle.
   */
  private static double refAngle(Node ref, Node[] nodes) {
    double cx = 0.0;
    double cy = 0.0;
    for (final Node node : nodes) {
      cx += node.pos.x;
      cy += node.pos.y;
    }
    cx /= nodes.length;
    cy /= nodes.length;
    return Math.atan2(ref.pos.y - cy, ref.pos.x - cx);
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
    // Damping stabilizes the stiff skeleton + muscle combination.
    // Without it the muscle forces ring numerically and explode.
    World.global_temperature = 0;
    com.springie.elements.nodes.Node.viscocity = 2;
    World.ground_friction = 100;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.max_speed = Integer.MAX_VALUE;
    com.springie.FrEnd.three_d = true;
    com.springie.FrEnd.check_collisions = false; // Tim 2026-09-16: no node-node collisions as a crutch -- structure alone.
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
