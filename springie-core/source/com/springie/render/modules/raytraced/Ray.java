// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * A ray: an origin plus a normalized direction. Mutable and reused across
 * pixels so the inner render loop allocates nothing.
 */
final class Ray {
  double ox, oy, oz;

  double dx, dy, dz;
}
