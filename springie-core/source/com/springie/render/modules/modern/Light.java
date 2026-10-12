// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

/**
 * A point light source (Tim, 2026-10-04): position as percentages of the
 * viewport half-size, intensity as a percentage, and a configurable color.
 * The world coordinates are derived by LightSource.updateForViewport().
 */
public class Light {
  /** X position as % (0-100, 50=center). */
  public volatile double x_pct;

  /** Y position as % (0-100, 50=center). */
  public volatile double y_pct;

  /** Z position as % (0-100, 50=current -50*scale in front). */
  public volatile double z_pct;

  /** Intensity 0-100%. */
  public volatile int intensity_pct;

  /** Color as 0xRRGGBB. */
  public volatile int colour;

  /** Derived world X (fixed-point, shifted by Coords.shift). */
  public volatile double px;

  /** Derived world Y. */
  public volatile double py;

  /** Derived world Z. */
  public volatile double pz;

  public Light(final double x_pct, final double y_pct, final int intensity_pct,
      final int colour) {
    this(x_pct, y_pct, 50.0, intensity_pct, colour);
  }

  public Light(final double x_pct, final double y_pct, final double z_pct,
      final int intensity_pct, final int colour) {
    this.x_pct = x_pct;
    this.y_pct = y_pct;
    this.z_pct = z_pct;
    this.intensity_pct = intensity_pct;
    this.colour = colour;
    // Initialize derived coords (Tim, 2026-10-06): 0-100% (50=center),
    // using the Coords.getXCoords convention (px = HW*S - scx at center,
    // range HW*PF for exact screen mapping).
    final double scale = (double) (1 << com.springie.render.Coords.shift);
    final double half_w = 400.0 * scale;
    final double half_h = 300.0 * scale;
    this.pz = (50.0 - 2.0 * z_pct) * scale;
    final double pf = (double) (com.springie.render.Coords.shift_constant_z
        + ((int) this.pz >> com.springie.render.Coords.shift_z));
    this.px = (x_pct - 50.0) / 50.0 * half_w * (pf / scale)
        + (half_w - com.springie.render.Coords.shift_constant_x);
    this.py = (y_pct - 50.0) / 50.0 * half_h * (pf / scale)
        + (half_h - com.springie.render.Coords.shift_constant_y);
  }

  /** Copy constructor. */
  public Light(final Light other) {
    this.x_pct = other.x_pct;
    this.y_pct = other.y_pct;
    this.z_pct = other.z_pct;
    this.intensity_pct = other.intensity_pct;
    this.colour = other.colour;
    this.px = other.px;
    this.py = other.py;
    this.pz = other.pz;
  }
}
