// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * A ray: an origin plus a normalized direction. Mutable and reused across
 * pixels so the inner render loop allocates nothing.
 *
 * <p>The direction's inverse is precomputed by setDirection (2026-11-17):
 * the BVH slab test needs 1/dx, 1/dy, 1/dz for every box it visits, and
 * recomputing the divisions per box dwarfed their cost. Always set the
 * direction through setDirection so the inverses cannot go stale; a
 * zero component yields an infinite inverse, which the slab test never
 * uses (it branches on the zero first).
 */
final class Ray {
  double ox, oy, oz;

  double dx, dy, dz;

  double inv_dx, inv_dy, inv_dz;

  /** Sets the direction and its precomputed inverse. */
  void setDirection(final double dx, final double dy, final double dz) {
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.inv_dx = 1.0 / dx;
    this.inv_dy = 1.0 / dy;
    this.inv_dz = 1.0 / dz;
  }
}
