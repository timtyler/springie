// This code has been placed into the public domain by its author

package com.springie.render.scene;

/**
 * One face triangle as plain render data: vertices in world units
 * (pixels), colour as opaque ARGB. Faces with more than three nodes are
 * fan-triangulated, like the ray tracer does. No AWT types: this is the
 * seam a future renderer can consume.
 */
public final class SceneFace {
  public final double x1;
  public final double y1;
  public final double z1;
  public final double x2;
  public final double y2;
  public final double z2;
  public final double x3;
  public final double y3;
  public final double z3;
  public final int colour;
  public final boolean selected;

  public SceneFace(final double x1, final double y1, final double z1,
      final double x2, final double y2, final double z2,
      final double x3, final double y3, final double z3,
      final int colour, final boolean selected) {
    this.x1 = x1;
    this.y1 = y1;
    this.z1 = z1;
    this.x2 = x2;
    this.y2 = y2;
    this.z2 = z2;
    this.x3 = x3;
    this.y3 = y3;
    this.z3 = z3;
    this.colour = colour;
    this.selected = selected;
  }
}
