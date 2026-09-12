// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * A ray-traceable scene primitive. Implementations are immutable after
 * construction so worker threads can share them freely.
 *
 * <p>Colour is packed the way the rest of the app packs it: 0xRRGGBB
 * (red in the high byte).
 */
interface Primitive {
  /**
   * If the ray hits this primitive closer than hit.t, updates hit and
   * returns true.
   */
  boolean intersect(Ray ray, Hit hit);

  void writeBounds(AABB out);

  int getColour();
}
