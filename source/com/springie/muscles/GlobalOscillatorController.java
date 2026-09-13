// This code has been placed into the public domain by its author.

package com.springie.muscles;

import com.springie.elements.links.Link;

/**
 * The first controller behaviour: every muscled link pulses with the global
 * oscillator defined by {@link Muscles#amplitude} and
 * {@link Muscles#period_ticks}.
 *
 * <p>Each link carries its own {@link #phase} offset, in ticks, into the
 * shared cycle - stagger the phases around a model and the pulse travels
 * as a wave instead of breathing in unison.
 */
public class GlobalOscillatorController implements Controller {
  /** Per-link phase offset into the global cycle, in ticks. */
  public int phase;

  public GlobalOscillatorController(int phase) {
    this.phase = phase;
  }

  @Override
  public void update(Link link, long tick) {
    final double radians = 2.0 * Math.PI * (tick + this.phase) / Muscles.period_ticks;
    link.rest_length_scale = Muscles.UNITY + (int) (Muscles.amplitude * Math.sin(radians));
  }
}
