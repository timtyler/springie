// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.links.Link;
import com.springie.muscles.Controller;
import com.springie.render.Coords;

/**
 * A stance-gated sine drive for the hopper's hip-foot crouch cables,
 * with a postural stabilizing reflex.
 *
 * <p>One controller drives all eight hip-foot muscles (two per leg).
 * Cables only pull, so the hop is a smooth two-phase catapult: during
 * stance the cables follow a raised-cosine yank (smoothly shortening
 * to haul the body down and load the passive knee springs, then
 * smoothly releasing so the springs fire, the legs extend, and the
 * body launches). In flight the cables hold their natural rest length
 * (slack).
 *
 * <p>Stability: a free-running symmetric yank lets pitch/roll modes
 * grow (the touchdown is never perfectly level). Each leg's yank is
 * therefore tilt-corrected: legs whose hip sits HIGHER than the mean
 * yank harder, pulling the high side down; legs on the low side yank
 * softer. This is a postural servo (local to the legs) that damps
 * pitch and roll without a touchdown trigger.
 */
public final class HopperStanceController implements Controller {
  private final HopperDemo.Leg[] legs;
  private final int[] base_lengths;
  private final int[] pull_amounts;
  private final int ground_y;
  private final int period_ticks;
  /** Postural gain: extra yank percent per px the hip sits above mean. */
  private final int tilt_gain_pct_per_px = 4;

  /**
   * @param legs the four legs (hip edge nodes, foot, crouch muscles)
   * @param pull_pct peak percent to shorten during the yank
   * @param period_ticks the yank-release rhythm, in ticks
   * @param ground_y the ground level in internal units
   */
  public HopperStanceController(java.util.List<HopperDemo.Leg> legs,
      int pull_pct, int period_ticks, int ground_y) {
    this.legs = legs.toArray(new HopperDemo.Leg[0]);
    this.ground_y = ground_y;
    this.period_ticks = period_ticks;
    int n = 0;
    for (final HopperDemo.Leg leg : this.legs) {
      n += leg.muscles.length;
    }
    this.base_lengths = new int[n];
    this.pull_amounts = new int[n];
    int i = 0;
    for (final HopperDemo.Leg leg : this.legs) {
      for (final Link link : leg.muscles) {
        final int base = link.type.length;
        this.base_lengths[i] = base;
        this.pull_amounts[i] = base * pull_pct / 100;
        i++;
      }
    }
  }

  @Override
  public void update(Link link, long tick) {
    // Stance gate: any foot on the ground.
    boolean in_stance = false;
    final int slack = 6 << Coords.shift;
    for (final HopperDemo.Leg leg : legs) {
      if (leg.foot.pos.y >= ground_y - slack) {
        in_stance = true;
        break;
      }
    }
    // Mean hip height for the tilt reflex.
    long hip_sum = 0;
    int hip_n = 0;
    for (final HopperDemo.Leg leg : legs) {
      hip_sum += leg.hip_a.pos.y;
      hip_sum += leg.hip_b.pos.y;
      hip_n += 2;
    }
    final long hip_mean = hip_sum / hip_n;
    // Raised-cosine yank: 0 -> peak -> 0 over one period.
    final double phase =
        2.0 * Math.PI * (tick % period_ticks) / period_ticks;
    final double yank = 0.5 - 0.5 * Math.cos(phase);
    int i = 0;
    for (final HopperDemo.Leg leg : legs) {
      // Tilt reflex: the high side yanks harder (pulls itself down).
      // Hip y grows downward, so (mean - hip) is positive when high.
      final long hip_y = (leg.hip_a.pos.y + leg.hip_b.pos.y) / 2;
      final int err_px = (int) ((hip_mean - hip_y) >> Coords.shift);
      final int corr_pct = 100 + tilt_gain_pct_per_px * err_px;
      for (final Link muscle : leg.muscles) {
        if (in_stance) {
          final int pull =
              (int) (pull_amounts[i] * yank * corr_pct / 100);
          muscle.adjusted_rest_length = base_lengths[i] - pull;
        } else {
          muscle.adjusted_rest_length = base_lengths[i];
        }
        i++;
      }
    }
  }
}
