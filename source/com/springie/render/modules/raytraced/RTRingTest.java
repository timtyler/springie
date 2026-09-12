// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The selection ring intersection math: a flat annulus in the z = 0
 * plane, normal +z, inner radius 4, outer radius 6.
 */
class RTRingTest {
  private static RTRing ring() {
    return new RTRing(0, 0, 0, 0, 0, 1, 4, 6, 0xFF4040);
  }

  private static Ray ray(double ox, double oy, double oz, double dx,
      double dy, double dz) {
    final Ray ray = new Ray();
    ray.ox = ox;
    ray.oy = oy;
    ray.oz = oz;
    ray.dx = dx;
    ray.dy = dy;
    ray.dz = dz;
    return ray;
  }

  @Test
  void hitThroughAnnulus() {
    final RTRing ring = ring();
    final Hit hit = new Hit();
    assertTrue(ring.intersect(ray(5, 0, -10, 0, 0, 1), hit));
    assertEquals(10.0, hit.t, 1e-9);
    assertSame(ring, hit.primitive);
    // The normal faces back along the incoming ray.
    assertEquals(0.0, hit.nx, 1e-9);
    assertEquals(0.0, hit.ny, 1e-9);
    assertEquals(-1.0, hit.nz, 1e-9);
  }

  @Test
  void normalFacesRayFromBehind() {
    final Hit hit = new Hit();
    assertTrue(ring().intersect(ray(5, 0, 10, 0, 0, -1), hit));
    assertEquals(1.0, hit.nz, 1e-9);
  }

  @Test
  void missThroughHole() {
    assertFalse(ring().intersect(ray(0, 0, -10, 0, 0, 1), new Hit()));
  }

  @Test
  void missOutsideOuterEdge() {
    assertFalse(ring().intersect(ray(7, 0, -10, 0, 0, 1), new Hit()));
  }

  @Test
  void missWhenParallelToPlane() {
    assertFalse(ring().intersect(ray(5, 0, 0, 1, 0, 0), new Hit()));
  }

  @Test
  void respectsCloserHit() {
    final Hit hit = new Hit();
    hit.t = 5.0;
    assertFalse(ring().intersect(ray(5, 0, -10, 0, 0, 1), hit));
    assertEquals(5.0, hit.t, 1e-9);
  }

  @Test
  void isUnlitOverlay() {
    final RTRing ring = ring();
    assertTrue(ring.isUnlit());
    assertEquals(0xFF4040, ring.getColour());
  }
}
