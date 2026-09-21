// This code has been placed into the public domain by its author.

package com.springie.demos;

/**
 * Compass heading for a demo model: its intended direction of travel on the
 * floor plane. Judges score signed progress <em>along</em> the declared
 * heading, so sideways crabbing no longer scores as travel.
 *
 * <p>Axis mapping (Tim-approved): E = +x, W = -x, S = +z, N = -z.
 * Y grows downward and is the vertical axis, so the floor plane is x/z.
 */
public enum CompassPoint {
  N,
  S,
  E,
  W;

  /**
   * Compass bias size: how many velocity units to add per frame along the
   * heading. Part of the universe settings (Universe tab); 0 disables the
   * bias. Same units as World.gravity_strength, which adds that many
   * velocity units per frame to velocity.y.
   */
  public static int bias_size = 0;

  /**
   * Signed progress along this heading from a floor-plane displacement
   * (dx, dz) in internal units. Positive means travel toward the heading;
   * negative means travel away from it.
   */
  public long progress(long dx, long dz) {
    switch (this) {
      case E:
        return dx;
      case W:
        return -dx;
      case S:
        return dz;
      case N:
        return -dz;
      default:
        throw new AssertionError(this);
    }
  }

  /**
   * Absolute drift perpendicular to this heading from a floor-plane
   * displacement (dx, dz) in internal units.
   */
  public long lateral(long dx, long dz) {
    switch (this) {
      case E:
      case W:
        return Math.abs(dz);
      case N:
      case S:
        return Math.abs(dx);
      default:
        throw new AssertionError(this);
    }
  }

  /** The heading opposite this one. */
  public CompassPoint opposite() {
    switch (this) {
      case N:
        return S;
      case S:
        return N;
      case E:
        return W;
      case W:
        return E;
      default:
        throw new AssertionError(this);
    }
  }

  /**
   * The x-component, in internal velocity units per frame, of the compass
   * bias along this heading for the current {@link #bias_size}. E adds
   * +bias, W adds -bias, N/S add nothing on x.
   */
  public int biasDx() {
    switch (this) {
      case E:
        return bias_size;
      case W:
        return -bias_size;
      default:
        return 0;
    }
  }

  /**
   * The z-component, in internal velocity units per frame, of the compass
   * bias along this heading for the current {@link #bias_size}. S adds
   * +bias, N adds -bias, E/W add nothing on z.
   */
  public int biasDz() {
    switch (this) {
      case S:
        return bias_size;
      case N:
        return -bias_size;
      default:
        return 0;
    }
  }
}
