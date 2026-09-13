// This code has been placed into the public domain by its author.

package com.springie.muscles;

import com.springie.elements.links.Link;
import com.springie.render.Coords;

/**
 * Read-only sensing helpers for {@link Controller}s.
 *
 * <p>Ground-contact sensing (is a node being pushed onto the ground?) will
 * join these once the world has a ground plane to sense against.
 */
public final class Sensors {
  private Sensors() {
    // static-only
  }

  /**
   * Returns the link's strain in fixed point: {@code (actual - rest) / rest}.
   * Negative means the link is compressed (a strut being squashed, a cable
   * gone slack); positive means it is stretched (a cable under tension).
   * The rest length used is the effective one, with the controller's
   * current scale applied.
   */
  public static int strain(Link link) {
    final int rest = link.getEffectiveRestLength();
    if (rest <= 0) {
      return 0;
    }
    final int actual = link.getActualLength();
    return (int) (((long) (actual - rest) << Coords.shift) / rest);
  }

  /** True when the link is shorter than its (effective) rest length. */
  public static boolean isCompressed(Link link) {
    return strain(link) < 0;
  }

  /** True when the link is longer than its (effective) rest length. */
  public static boolean isStretched(Link link) {
    return strain(link) > 0;
  }
}
