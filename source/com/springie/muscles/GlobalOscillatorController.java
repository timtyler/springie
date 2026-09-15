// This code has been placed into the public domain by its author.

package com.springie.muscles;

import com.springie.elements.links.Link;
import com.springie.render.Coords;

/**
 * The first controller behaviour: the link follows one of the global
 * oscillators ({@link Muscles#oscillators}). Every dynamics step the
 * link's adjusted rest length is rewritten from the oscillator's sine
 * wave, shifted by the link's own phase ({@link Link#phase}).
 *
 * <p>The controller holds only the oscillator's index -- amplitude and
 * period live in the oscillator, phase lives in the link.
 */
public class GlobalOscillatorController implements Controller {
  /** Index into {@link Muscles#oscillators}: the oscillator this link follows. */
  public int oscillator_index;

  public GlobalOscillatorController(int oscillator_index) {
    this.oscillator_index = oscillator_index;
  }

  @Override
  public void update(Link link, long tick) {
    if (this.oscillator_index < 0 || this.oscillator_index >= Muscles.oscillators.length) {
      return;
    }
    final Oscillator oscillator = Muscles.oscillators[this.oscillator_index];
    if (oscillator == null) {
      return;
    }
    final int scale = oscillator.getScale(tick, link.phase);
    link.adjusted_rest_length = (int) (((long) link.type.length * scale) >> Coords.shift);
  }
}
