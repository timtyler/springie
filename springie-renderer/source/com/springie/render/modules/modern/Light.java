// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

/**
 * A point light source (Tim, 2026-10-04): position as percentages of the
 * viewport half-size, intensity as a percentage, and a configurable color.
 * The world coordinates are derived by LightSource.updateForViewport().
 *
 * <p>Extends the engine-side {@code com.springie.world.Light} (pure data)
 * with the derived world coordinates.
 */
public class Light extends com.springie.world.Light {
  /** Derived world X (fixed-point, shifted by Coords.shift). */
  public volatile double px;

  /** Derived world Y. */
  public volatile double py;

  /** Derived world Z. */
  public volatile double pz;

  public Light(final double x_pct, final double y_pct, final int intensity_pct,
      final int colour) {
    super(x_pct, y_pct, intensity_pct, colour);
    initDerived();
  }

  public Light(final double x_pct, final double y_pct, final double z_pct,
      final int intensity_pct, final int colour) {
    super(x_pct, y_pct, z_pct, intensity_pct, colour);
    initDerived();
  }

  private void initDerived() {
    // Initialize derived coords (Tim, 2026-10-06): 0-100% (50=center),
    // using the Coords.getXCoords convention (px = HW*S - scx at center,
    // range HW*PF for exact screen mapping).
    final double scale = (double) (1 << com.springie.geometry.Coords.shift);
    final double half_w = 400.0 * scale;
    final double half_h = 300.0 * scale;
    this.pz = (50.0 - 2.0 * this.z_pct) * scale;
    final double pf = (double) (com.springie.geometry.Coords.shift_constant_z
        + ((int) this.pz >> com.springie.geometry.Coords.shift_z));
    this.px = (this.x_pct - 50.0) / 50.0 * half_w * (pf / scale)
        + (half_w - com.springie.geometry.Coords.shift_constant_x);
    this.py = (this.y_pct - 50.0) / 50.0 * half_h * (pf / scale)
        + (half_h - com.springie.geometry.Coords.shift_constant_y);
  }

  /** Copy constructor. */
  public Light(final Light other) {
    super(other);
    this.px = other.px;
    this.py = other.py;
    this.pz = other.pz;
  }

  /** Copy from an engine-side light (data only; derived coords recomputed). */
  public Light(final com.springie.world.Light other) {
    super(other);
    initDerived();
  }
}
