// This code has been placed into the public domain by its author

package com.springie.world;

/**
 * A point light source: position as percentages of the viewport, intensity
 * as a percentage, and a color. Pure data; the render-side Light subclass
 * adds derived world coordinates.
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
  }

  public Light(final Light other) {
    this(other.x_pct, other.y_pct, other.z_pct, other.intensity_pct,
      other.colour);
  }
}
