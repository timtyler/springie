// This code has been placed into the public domain by its author

package com.springie.render.scene;

/**
 * One link segment as plain render data: endpoints and radius in world
 * units (pixels), colour as ARGB. A link with more than two nodes
 * produces one SceneLink per consecutive pair, like the renderers draw.
 * No AWT types: this is the seam a JavaFX renderer will consume.
 */
public final class SceneLink {
  public final double x1;
  public final double y1;
  public final double z1;
  public final double x2;
  public final double y2;
  public final double z2;
  public final double radius;
  public final int colour;
  /** True for a compression member (strut), false for a cable. */
  public final boolean strut;
  public final boolean selected;

  public SceneLink(final double x1, final double y1, final double z1,
      final double x2, final double y2, final double z2,
      final double radius, final int colour, final boolean strut,
      final boolean selected) {
    this.x1 = x1;
    this.y1 = y1;
    this.z1 = z1;
    this.x2 = x2;
    this.y2 = y2;
    this.z2 = z2;
    this.radius = radius;
    this.colour = colour;
    this.strut = strut;
    this.selected = selected;
  }
}
