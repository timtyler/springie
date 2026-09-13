// This code has been placed into the public domain by its author.

package com.springie.muscles;

import com.springie.elements.links.Link;

/**
 * A per-link muscle controller.
 *
 * <p>Each link may carry one controller. It is called once per dynamics
 * step, before link forces are computed, and may both <em>sense</em> (via
 * {@link Sensors}: how compressed or stretched is this link?) and
 * <em>actuate</em>, by writing {@link Link#adjusted_rest_length}.
 *
 * <p>The first behaviour is a global oscillator ({@link GlobalOscillatorController});
 * sensor-driven behaviours - reacting to compression, or to a node being
 * pushed onto the ground - plug in behind this same interface.
 */
public interface Controller {
  /**
   * Advances the controller by one dynamics step.
   *
   * @param link the link under control; write {@code link.adjusted_rest_length}
   *             to change its rest length for this step
   * @param tick the world's dynamics-step counter
   */
  void update(Link link, long tick);
}
