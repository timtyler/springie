// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

/**
 * A point light source (Tim, 2026-10-04): position as percentages of the
 * viewport half-size, intensity as a percentage, and a configurable color.
 * The world coordinates are derived by LightSource.updateForViewport().
 */
public class Light {
  /** X position as % of viewport half-width. */
  public volatile double x_pct;

  /** Y position as % of viewport half-height. */
  public volatile double y_pct;

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
    this.x_pct = x_pct;
    this.y_pct = y_pct;
    this.intensity_pct = intensity_pct;
    this.colour = colour;
    // Initialize derived coords to sensible defaults (Tim, 2026-10-04):
    // prevents 0,0,0 which is on the eye plane and makes the dot vanish.
    final double scale = (double) (1 << com.springie.render.Coords.shift);
    this.px = x_pct / 100.0 * 400.0 * scale;
    this.py = y_pct / 100.0 * 300.0 * scale;
    this.pz = -200.0 * scale;
  }

  /** Copy constructor. */
  public Light(final Light other) {
    this.x_pct = other.x_pct;
    this.y_pct = other.y_pct;
    this.intensity_pct = other.intensity_pct;
    this.colour = other.colour;
    this.px = other.px;
    this.py = other.py;
    this.pz = other.pz;
  }
}
