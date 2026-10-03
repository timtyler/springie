// This code has been placed into the public domain by its author

package com.springie.render.scene;

/**
 * One node as plain render data: position and radius in world units
 * (pixels), colour as ARGB. No AWT types: this is the seam a JavaFX
 * renderer will consume.
 */
public final class SceneNode {
  public final double x;
  public final double y;
  public final double z;
  public final double radius;
  public final int colour;
  public final boolean selected;

  public SceneNode(final double x, final double y, final double z,
      final double radius, final int colour, final boolean selected) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.radius = radius;
    this.colour = colour;
    this.selected = selected;
  }
}
