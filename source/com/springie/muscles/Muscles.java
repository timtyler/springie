// This code has been placed into the public domain by its author.

package com.springie.muscles;

import com.springie.render.Coords;

/**
 * Global muscle configuration.
 *
 * <p>Each link carries its own {@link Controller}, which senses the link and
 * drives its rest length. This class holds the shared settings: a bank of
 * {@link Oscillator}s, exactly one of which is active. Controllers are
 * linked to an oscillator by index; amplitude, period and phase live in
 * the oscillator, so links carry no oscillator state of their own.
 */
public final class Muscles {
  private Muscles() {
    // static-only
  }

  /** Unity rest-length scale, in fixed point. */
  public static final int UNITY = 1 << Coords.shift;

  /**
   * Master switch. While false, controllers are not updated and every link
   * uses its default rest length, so disabled muscles cost a single
   * static check per dynamics step.
   */
  public static boolean enabled = false;

  /**
   * The oscillator bank. Slots beyond the active one start empty, ready
   * for experiments (and, later, evolution) to fill.
   */
  public static final int NUMBER_OF_OSCILLATORS = 8;

  /** The oscillators; only the active slot is guaranteed to exist. */
  public static final Oscillator[] oscillators = new Oscillator[NUMBER_OF_OSCILLATORS];

  /** Index of the active oscillator: the one new controllers link to. */
  public static int active_oscillator = 0;

  static {
    final Oscillator first = new Oscillator();
    first.amplitude = (int) (0.85 * UNITY);
    first.period_ticks = 12;
    first.phase = 0;
    oscillators[0] = first;
  }

  /** Returns the currently-active oscillator. */
  public static Oscillator activeOscillator() {
    return oscillators[active_oscillator];
  }
}
