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

  /**
   * Returns the link's compression in fixed point: how far below its
   * (effective) rest length it currently is, as a fraction of rest length.
   * Zero at or above rest length; grows the harder the link is squashed
   * (a strut being compressed, a cable gone slack). Quantified so a
   * controller can react in proportion -- stiffen or contract harder
   * when more compressed -- rather than on a boolean edge.
   */
  public static int compression(Link link) {
    return Math.max(0, -strain(link));
  }

  /**
   * Returns the link's stretch in fixed point: how far above its
   * (effective) rest length it currently is, as a fraction of rest length.
   * Zero at or below rest length; grows the further the link is pulled
   * past rest (a cable under tension). Quantified for the same reason.
   */
  public static int stretch(Link link) {
    return Math.max(0, strain(link));
  }
}
