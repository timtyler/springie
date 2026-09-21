// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.muscles.Controller;
import com.springie.muscles.Muscles;
import com.springie.muscles.Oscillator;
import com.springie.render.Coords;

/**
 * A stance-gated hop controller. While airborne the cables hold their
 * build length (the legs stay rigid trusses, never flailing); while a
 * foot is in contact each cable shortens in proportion to its own leg's
 * compression VELOCITY -- a stretch reflex. A slow quasi-static settle
 * (like the dead-start pre-stress loading the springs) gets almost no
 * pump, while a genuine touchdown impact gets a strong one; the pump
 * cuts out during extension, so it can't fight the launch or run away
 * into monster hops. The springs' rebound launches the next hop, and
 * the pull only replaces the energy the lossy bounce dissipates, so
 * the rhythm is the leg's natural pogo frequency.
 *
 * <p>On top of the stance pull a proportional attitude trim levels the
 * body: the cables pull the hips DOWN toward the feet, so to level the
 * body the LOW side is slackened and the HIGH side tightened.
 * (Since the symmetric force fix the takeoff is exactly level, so
 * pitch/roll stay at zero and the trim is inert -- kept as a safety
 * net.)
 */
public class HopperGaitController implements Controller {
  /** Index into {@link Muscles#oscillators} (kept for the UI readout). */
  public int oscillator_index;
  /** Pixels above the ground wall that counts as "foot in contact". */
  public static int stance_threshold_px = 8;
  /**
   * Attitude trim gain: cable-length px per px of hip-height error.
   * 0.5 means a 10px nose-down pitch slackens the front cables by 5px
   * and tightens the hind cables by 5px.
   */
  public static double trim_gain = 0.5;
  /**
   * Stance pull gain: fraction of cable rest length shortened per px of
   * leg compression. (Currently inert -- the static pre-stress does the
   * work. Kept for future tuning.)
   */
  public static double stance_pull_per_px = 0.01;
  /** Cap on the compression-proportional stance pull. */
  public static double max_stance_pull = 0.025;
  /**
   * Flight crouch: fraction the cable shortens while airborne. (Currently
   * 0 -- a flight crouch made the chaining worse.)
   */
  public static double flight_crouch = 0.0;
  /**
   * Stance pre-tension: fraction the cable shortens while in contact,
   * on top of the compression-proportional pull. Pre-tensioning at
   * touchdown stiffens the leg for the impact.
   */
  public static double stance_pretension = 0.0;
  /**
   * Velocity pump gain: fraction of cable rest length shortened per
   * px/tick of leg compression velocity. When > 0, the stance pull is
   * proportional to how FAST the leg is compressing (a stretch reflex),
   * not how far -- so a slow quasi-static settle (like the dead-start
   * pre-stress loading) gets almost no pump, while a genuine touchdown
   * impact gets a strong one. The pump naturally cuts out during
   * extension, so it can't fight the launch or run away. 0 disables it
   * (magnitude-proportional pull is used).
   */
  public static double velocity_pump_gain = 0.01;

  private final Node[] feet;
  private final int ground_y;
  private final Node b0;
  private final Node b1;
  private final Node b2;
  private final Node b3;
  /** Natural leg length (hip-to-foot at build), in fixed-point. */
  private int natural_leg_length = 0;

  public HopperGaitController(int oscillator_index, Node[] feet, int ground_y,
      Node b0, Node b1, Node b2, Node b3) {
    this.oscillator_index = oscillator_index;
    this.feet = feet;
    this.ground_y = ground_y;
    this.b0 = b0;
    this.b1 = b1;
    this.b2 = b2;
    this.b3 = b3;
  }

  @Override
  public void update(Link link, long tick) {
    if (this.oscillator_index < 0
        || this.oscillator_index >= Muscles.oscillators.length) {
      return;
    }
    final Oscillator oscillator = Muscles.oscillators[this.oscillator_index];
    if (oscillator == null) {
      return;
    }
    boolean in_stance = false;
    for (final Node foot : this.feet) {
      if (this.ground_y - foot.pos.y <= (stance_threshold_px << Coords.shift)) {
        in_stance = true;
        break;
      }
    }
    // Attitude: front hips (b0, b3) vs hind hips (b1, b2); left hips
    // (b0, b1, at z=zo) vs right hips (b2, b3). Y grows downward, so a
    // larger Y means lower.
    final long front_y = (long) this.b0.pos.y + this.b3.pos.y;
    final long hind_y = (long) this.b1.pos.y + this.b2.pos.y;
    final long left_y = (long) this.b0.pos.y + this.b1.pos.y;
    final long right_y = (long) this.b2.pos.y + this.b3.pos.y;
    final long pitch = front_y - hind_y; // >0 means nose-down
    final long roll = left_y - right_y; // >0 means left-down
    // Which corner does this cable belong to?
    final Node a = link.nodes[0];
    final Node b = link.nodes[1];
    final boolean is_front =
        a == this.b0 || b == this.b0 || a == this.b3 || b == this.b3;
    final boolean is_left =
        a == this.b0 || b == this.b0 || a == this.b1 || b == this.b1;
    // Trim in fixed-point px: slacken (positive) the low side,
    // tighten (negative) the high side.
    final long trim_fp =
        (long) (trim_gain * (is_front ? pitch : -pitch)
            + trim_gain * (is_left ? roll : -roll));
    final int trim = (int) (trim_fp >> Coords.shift);
    final int base = link.type.length;
    if (!in_stance) {
      // Flight: crouch (pre-tension) plus the trim. Clear the
      // velocity pump's previous-length so touchdown starts fresh.
      link.ctrl_prev_length = 0;
      final int scale = Muscles.UNITY - (int) (flight_crouch * Muscles.UNITY);
      link.adjusted_rest_length =
          (int) (((long) base * scale) >> Coords.shift) + trim;
      return;
    }
    // Stance: pull proportionally to this leg's compression from its
    // natural length. The cable is pre-stressed so it's always in
    // tension and can do work through the whole stance.
    final Node n0 = link.nodes[0];
    final Node n1 = link.nodes[1];
    final int dx = n0.pos.x - n1.pos.x;
    final int dy = n0.pos.y - n1.pos.y;
    final int dz = n0.pos.z - n1.pos.z;
    final int actual =
        (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
    if (this.natural_leg_length == 0) {
      this.natural_leg_length = actual;
    }
    int comp_px = (this.natural_leg_length - actual) >> Coords.shift;
    if (comp_px < 0) {
      comp_px = 0;
    }
    double pull;
    if (velocity_pump_gain > 0.0) {
      // Stretch reflex: pump proportional to compression velocity.
      // (prev - actual) > 0 means the leg is shortening this tick.
      final int prev = link.ctrl_prev_length;
      if (prev != 0) {
        final int comp_vel_px = (prev - actual) >> Coords.shift;
        pull = velocity_pump_gain * Math.max(0, comp_vel_px);
      } else {
        pull = 0.0;
      }
    } else {
      pull = stance_pull_per_px * comp_px;
    }
    link.ctrl_prev_length = actual;
    if (pull > max_stance_pull) {
      pull = max_stance_pull;
    }
    pull += stance_pretension;
    final int scale = Muscles.UNITY - (int) (pull * Muscles.UNITY);
    link.adjusted_rest_length =
        (int) (((long) base * scale) >> Coords.shift) + trim;
  }
}
