// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.utilities.random.Hortensius32Fast;
import com.springie.world.World;

/**
 * Truly headless judge that scores a crawler design. Does NOT start FrEnd
 * (no GUI, no animation thread) -- single-threaded and deterministic.
 * Needs DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Metric: signed 2D travel on the floor along the model's declared
 * compass heading (pixels). Sideways crabbing scores nothing; walking
 * backwards scores negative; vertical motion doesn't count.
 *
 * <p>Disqualifications (score 0): tipping over (Tim's rule: the dorsal
 * top node must stay above the belly reference), a starting shove
 * (initial velocity along the heading), an unbalanced compass-bias
 * layout (the bias must net to zero), or the body's centre of gravity
 * riding too low (it must stay well off the floor all run -- a
 * low-riding body fails even if it never quite touches).
 *
 * <p>Usage: java com.springie.demos.CrawlerJudge [ticks]
 * Prints the Result fields, one per line.
 */
public final class CrawlerJudge {
  private CrawlerJudge() {
  }

  /** Ticks skipped at the start so the model can settle. */
  public static final int SETTLE_TICKS = 60;

  /** Score breakdown for one judged run. */
  public static final class Result {
    /**
     * Signed progress along the declared compass heading, pixels.
     * 0 when disqualified.
     */
    public int distance_px;
    /**
     * Fraction of measured ticks the posture rule held (1.0 = never
     * tipped). Tim's "not tipping over" disqualification: any tip-over
     * disqualifies the run.
     */
    public double upright_frac;
    /** True when the posture rule was violated at least once. */
    public boolean tipped_over;
    /**
     * Minimum clearance (px) of the body's centre of gravity above the
     * floor over the measured ticks.
     */
    public int cog_min_clearance_px;
    /** True when the CoG rode below the allowed clearance. */
    public boolean cog_violated;
    /**
     * Compass-bias balance violation, or null when the layout is
     * balanced (equal N/S and E/W, so the net bias is zero).
     */
    public String bias_violation;
    /**
     * Mean node velocity along the heading at build time, px/frame.
     * Must be ~0: a starting shove disqualifies the run.
     */
    public double initial_velocity_px_per_frame;
    /** True when the model started with velocity along the heading. */
    public boolean shoved;
    /** True when disqualified. Score is 0 when set. */
    public boolean disqualified;
    /** Signed distance, or 0 when disqualified. */
    public int score;
    /** Total ticks simulated (including settling). */
    public int ticks;

    @Override
    public String toString() {
      return "DISTANCE " + this.distance_px
          + "\nUPRIGHT_FRAC " + String.format("%.3f", this.upright_frac)
          + "\nTIPPED_OVER " + this.tipped_over
          + "\nCOG_MIN_CLEARANCE " + this.cog_min_clearance_px
          + "\nCOG_VIOLATED " + this.cog_violated
          + "\nBIAS_VIOLATION " + this.bias_violation
          + "\nINITIAL_VELOCITY "
          + String.format("%.4f", this.initial_velocity_px_per_frame)
          + "\nSHOVED " + this.shoved
          + "\nDISQUALIFIED " + this.disqualified
          + "\nSCORE " + this.score
          + "\nTICKS " + this.ticks;
    }
  }

  /**
   * Scores the crawler over the given number of ticks. Deterministic:
   * two calls give identical results, even in a JVM where a GUI test
   * has left the animation thread running.
   */
  public static Result score(int ticks) {
    // Hold the model lock for the whole run (see RollingJudge for why).
    synchronized (ContextManager.class) {
      return scoreWithLockHeld(ticks);
    }
  }

  private static Result scoreWithLockHeld(int ticks) {
    // Reset the world's RNG so every scored run starts from identical
    // initial conditions (temperature jitter and node seeds).
    resetWorldRandom();

    // Pin down all global physics state. Earlier tests (especially GUI
    // tests) leak values into these statics; the demo's buildAt sets
    // only a subset, so two consecutive score() calls could otherwise
    // diverge in a polluted suite.
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = 100;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    World.global_temperature = 6;
    Node.max_speed = Integer.MAX_VALUE;
    Node.viscocity = 0;
    com.springie.FrEnd.three_d = true;
    com.springie.FrEnd.check_collisions = false;
    com.springie.FrEnd.continuously_centre_x = false;
    com.springie.FrEnd.continuously_centre_y = false;
    com.springie.FrEnd.continuously_centre_z = false;
    com.springie.FrEnd.boundaries = true;
    com.springie.FrEnd.explosions = false;
    com.springie.FrEnd.oscd = true;
    com.springie.FrEnd.dragged_element = null;
    com.springie.FrEnd.forces_disabled_during_gesture = false;
    com.springie.FrEnd.paused = false;
    com.springie.FrEnd.frame_frequency = 0;
    com.springie.muscles.Muscles.enabled = false;
    com.springie.muscles.Muscles.active_oscillator = 0;
    // Pin the universe size: a booted GUI resizes Coords to its canvas,
    // moving the ground walls and changing the absolute score.
    Coords.x_pixels = 800;
    Coords.y_pixels = 600;
    Coords.z_pixels = 1024;

    ContextManager.setNodeManager(new NodeManager());
    final NodeManager node_manager = ContextManager.getNodeManager();

    final Node tracked = CrawlerDemo.buildAt(0);

    // No free shove: the model must start at rest along its heading.
    // Checked before the first tick.
    final double initial_velocity = InitialVelocityCheck.meanAlong(
        node_manager, CrawlerDemo.compassHeading());
    final boolean shoved = !InitialVelocityCheck.atRestAlong(node_manager,
        CrawlerDemo.compassHeading());

    // No net bias: the compass-bias layout must carry equal numbers of
    // N and S, and of E and W, so the stabilizer can never propel the
    // model. The layout is part of the demo definition.
    final String bias_violation =
        CompassBalance.checkBalanced(CrawlerDemo.last_bias_layout);

    for (int i = 0; i < SETTLE_TICKS; i++) {
      node_manager.nodeAndLinkUpdate();
    }
    final int start_x = tracked.pos.x;
    final int start_z = tracked.pos.z;

    final TipOverRule tip_rule = new TipOverRule(
        CrawlerDemo.posture_top_index, CrawlerDemo.posture_bottom_index,
        CrawlerDemo.posture_min_separation_px);
    boolean tipped_over = false;
    int upright_ticks = 0;

    // The body's centre of gravity must stay well off the floor all
    // run: track the minimum clearance of the body-node mean height
    // above the ground wall. Feet touch the ground, so they are
    // excluded -- this is the body riding high, not grazing.
    final int floor_internal = Coords.y_pixels << Coords.shift;
    int cog_min_clearance = Integer.MAX_VALUE;

    final int measured = ticks - SETTLE_TICKS;
    for (int i = 0; i < measured; i++) {
      node_manager.nodeAndLinkUpdate();

      if (tip_rule.violated(node_manager)) {
        tipped_over = true;
      } else {
        upright_ticks++;
      }

      final int clearance = bodyClearancePx(node_manager, floor_internal);
      if (clearance < cog_min_clearance) {
        cog_min_clearance = clearance;
      }
    }

    // Signed progress along the declared compass heading, in pixels.
    final long dx = (long) tracked.pos.x - start_x;
    final long dz = (long) tracked.pos.z - start_z;
    final int distance_px = (int) (CrawlerDemo.compassHeading()
        .progress(dx, dz) >> Coords.shift);

    final Result result = new Result();
    result.distance_px = distance_px;
    result.upright_frac = (double) upright_ticks / measured;
    result.tipped_over = tipped_over;
    result.cog_min_clearance_px = cog_min_clearance;
    result.cog_violated =
        cog_min_clearance < CrawlerDemo.cog_min_clearance_px;
    result.bias_violation = bias_violation;
    result.initial_velocity_px_per_frame = initial_velocity;
    result.shoved = shoved;
    result.disqualified = tipped_over || shoved || result.cog_violated
        || bias_violation != null;
    result.score = result.disqualified ? 0 : distance_px;
    result.ticks = ticks;
    return result;
  }

  /**
   * Clearance of the body's centre of gravity above the floor, in
   * pixels. The body nodes are element indices 0-5 (the leg nodes are
   * excluded: feet touch the ground by design).
   */
  private static int bodyClearancePx(NodeManager node_manager,
      int floor_internal) {
    long sum_y = 0;
    final int body_nodes = 6;
    for (int i = 0; i < body_nodes; i++) {
      sum_y += ((Node) node_manager.element.get(i)).pos.y;
    }
    final long mean_y = sum_y / body_nodes;
    return (int) ((floor_internal - mean_y) >> Coords.shift);
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
    System.out.println(score(ticks));
    System.exit(0);
  }
}
