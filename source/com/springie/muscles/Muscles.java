// This code has been placed into the public domain by its author.

package com.springie.muscles;

import com.springie.render.Coords;

/**
 * Global muscle configuration.
 *
 * <p>Each link carries its own {@link Controller}, which senses the link and
 * drives its rest length. This class holds the shared settings; the first
 * behaviour is a global oscillator (see {@link GlobalOscillatorController}),
 * so every muscled link pulses in time, offset by its own phase.
 */
public final class Muscles {
  private Muscles() {
    // static-only
  }

  /** Unity rest-length scale, in fixed point. */
  public static final int UNITY = 1 << Coords.shift;

  /**
   * Master switch. While false, controllers are not updated and every link
   * uses its unscaled rest length, so disabled muscles cost a single
   * static check per dynamics step.
   */
  public static boolean enabled = false;

  /**
   * Pulse depth, as a fixed-point fraction of the rest length:
   * 0.25 (that is, {@code (int) (0.25 * UNITY)}) shortens and lengthens
   * each muscled link by up to 25%.
   */
  public static int amplitude = (int) (0.25 * UNITY);

  /** Oscillator period, in dynamics ticks. */
  public static int period_ticks = 120;
}
