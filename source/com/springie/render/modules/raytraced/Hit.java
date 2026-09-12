// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.render.Coords;

/**
 * The closest-hit record. Reused across pixels; reset() before each ray.
 */
final class Hit {
  double t = Double.POSITIVE_INFINITY;

  double nx, ny, nz;

  Primitive primitive;

  void reset() {
    this.t = Double.POSITIVE_INFINITY;
    this.primitive = null;
  }
}
