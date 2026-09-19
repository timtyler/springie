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
 * Truly headless judge that scores Caterpillar 2. Does NOT start FrEnd
 * (no GUI, no animation thread) -- single-threaded and deterministic.
 * Needs DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Rules:
 * <ol>
 * <li>The skeleton is all struts. The ONLY muscles are the side-rail
 * cable muscles: a travelling contraction wave on each side rail, the
 * two sides 90 degrees out of phase, so the body undulates laterally
 * like a swimming eel and crawls forward on ground friction. Muscle
 * power only: no start kick.</li>
 * <li>Score = forward distance (px) of the body centroid along +x, times
 * a straightness factor: {@code score = forward * (1 - min(1, lateral /
 * max(forward, 1)))}. Non-positive forward travel scores as-is.</li>
 * <li>The crawler must stay in one piece: any node exceeding
 * {@link #SPEED_CAP_PX_PER_TICK} or any passive link exceeding
 * {@link #STRAIN_CAP} strain (against its adjusted rest length)
 * disqualifies the run (score 0).</li>
 * <li>Tim's "not tipping over" rule: every apex must stay above its base
 * square (apex y above the base-centre y) for the whole run.</li>
 * </ol>
 *
 * <p>Usage: java com.springie.demos.Caterpillar3Judge [ticks]
 */
public final class Caterpillar3Judge {
  private Caterpillar3Judge() {
  }

  /**
   * Speed disqualification bar, px/tick. Crawling moves at a few
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

  /** Score breakdown for one judged run. */
  public static final class Result {
    /** Forward travel of the body centroid along +x, pixels. */
    public int forward_px;
    /** Lateral wander of the body centroid along z, pixels. */
    public int lateral_px;
    /** Straightness factor applied to the score, 0..1. */
    public double straightness;
    /** Fastest any node moved, px/tick. */
    public int max_speed_px;
    /** Worst strain on a passive (non-muscle) link. */
    public double max_passive_strain;
    /** True when an apex dropped to or below its base square. */
    public boolean tipped_over;
    /** True when the run was disqualified (exploded or tipped). */
    public boolean disqualified;
    /** forward * straightness, or 0 if disqualified. */
    public double score;
    /** Total ticks simulated (including settling). */
    public int ticks;

    @Override
    public String toString() {
      return "FORWARD_PX " + this.forward_px
          + "\nLATERAL_PX " + this.lateral_px
          + "\nSTRAIGHTNESS " + String.format("%.3f", this.straightness)
          + "\nMAX_SPEED_PX " + this.max_speed_px
          + "\nMAX_PASSIVE_STRAIN "
          + String.format("%.3f", this.max_passive_strain)
          + "\nTIPPED_OVER " + this.tipped_over
          + "\nDISQUALIFIED " + this.disqualified
          + "\nSCORE " + String.format("%.1f", this.score)
          + "\nTICKS " + this.ticks;
    }
  }

  /**
   * Scores Caterpillar 2 over the given number of ticks. Deterministic:
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

    Caterpillar3Demo.buildAt(100);
    final Node[] apexes = Caterpillar3Demo.apexes;
    final Node[] bases = Caterpillar3Demo.bases;
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

    for (int i = 0; i < SETTLE_TICKS; i++) {
      node_manager.nodeAndLinkUpdate();
    }
    final double[] start = centroid(nodes);

    long max_speed_sq = 0;
    double max_passive_strain = 0.0;
    boolean tipped_over = false;
    final int measured = ticks - SETTLE_TICKS;
    for (int t = 0; t < measured; t++) {
      node_manager.nodeAndLinkUpdate();

      if (!tipped_over) {
        // Apex i sits over base square i: columns i and i+1.
        for (int i = 0; i < Caterpillar3Demo.PYRAMIDS; i++) {
          final long base_y = (long) bases[i * 2].pos.y
              + bases[i * 2 + 1].pos.y
              + bases[i * 2 + 2].pos.y
              + bases[i * 2 + 3].pos.y;
          // Screen coords: smaller y = higher. Apex must stay above
          // the base-square centre (with a 10px tolerance band).
          if (apexes[i].pos.y >= base_y / 4 - (10L << Coords.shift)) {
            tipped_over = true;
            break;
          }
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

    final double[] end = centroid(nodes);
    final int forward_px =
        (int) ((end[0] - start[0]) / (1 << Coords.shift));
    final int lateral_px =
        (int) (Math.abs(end[2] - start[2]) / (1 << Coords.shift));
    final double straightness = forward_px <= 0 ? 1.0
        : 1.0 - Math.min(1.0, lateral_px / (double) Math.max(forward_px, 1));
    final int max_speed_px =
        (int) (Math.sqrt((double) max_speed_sq) / (1 << Coords.shift));
    final boolean disqualified = max_speed_px > SPEED_CAP_PX_PER_TICK
        || max_passive_strain > STRAIN_CAP || tipped_over;
    final double score = disqualified ? 0.0 : forward_px * straightness;

    final Result result = new Result();
    result.forward_px = forward_px;
    result.lateral_px = lateral_px;
    result.straightness = straightness;
    result.max_speed_px = max_speed_px;
    result.max_passive_strain = max_passive_strain;
    result.tipped_over = tipped_over;
    result.disqualified = disqualified;
    result.score = score;
    result.ticks = ticks;
    return result;
  }

  /** Centroid of all nodes, in internal units. */
  private static double[] centroid(Node[] nodes) {
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    for (final Node node : nodes) {
      x += node.pos.x;
      y += node.pos.y;
      z += node.pos.z;
    }
    final int count = nodes.length;
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
    World.gravity_strength = Caterpillar3Demo.gravity_strength;
    // Damping stabilizes the stiff skeleton.
    World.global_temperature = 0;
    com.springie.elements.nodes.Node.viscocity = 2;
    World.ground_friction = Caterpillar3Demo.friction;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.max_speed = Integer.MAX_VALUE;
    com.springie.FrEnd.three_d = true;
    // Tim 2026-09-18: collision detection OFF for this experiment
    // (the usual no-collisions rule is back).
    com.springie.FrEnd.check_collisions = false;
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
