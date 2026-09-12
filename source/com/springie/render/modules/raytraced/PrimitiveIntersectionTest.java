// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Known-answer tests for the three primitive intersection routines.
 */
public class PrimitiveIntersectionTest {
  private static final double EPS = 1e-9;

  private static Ray ray(double ox, double oy, double oz, double dx,
      double dy, double dz) {
    final Ray ray = new Ray();
    ray.ox = ox;
    ray.oy = oy;
    ray.oz = oz;
    final double inv = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
    ray.dx = dx * inv;
    ray.dy = dy * inv;
    ray.dz = dz * inv;
    return ray;
  }

  @Test
  public void sphereHeadOnHit() {
    final Primitive sphere = new RTSphere(0, 0, 0, 100, 0xFFFFFF);
    final Hit hit = new Hit();
    assertTrue(sphere.intersect(ray(0, 0, -1000, 0, 0, 1), hit));
    assertEquals(900.0, hit.t, EPS);
    assertEquals(0.0, hit.nx, EPS);
    assertEquals(0.0, hit.ny, EPS);
    assertEquals(-1.0, hit.nz, EPS);
  }

  @Test
  public void sphereMiss() {
    final Primitive sphere = new RTSphere(0, 0, 0, 100, 0xFFFFFF);
    final Hit hit = new Hit();
    assertFalse(sphere.intersect(ray(500, 0, -1000, 0, 0, 1), hit));
  }

  @Test
  public void sphereKeepsClosestHit() {
    final Primitive sphere = new RTSphere(0, 0, 0, 100, 0xFFFFFF);
    final Hit hit = new Hit();
    hit.t = 500.0;
    // Would hit at t = 900, which is further than the recorded hit.
    assertFalse(sphere.intersect(ray(0, 0, -1000, 0, 0, 1), hit));
    assertEquals(500.0, hit.t, EPS);
  }

  @Test
  public void ellipsoidWaistHit() {
    final Primitive ellipsoid = new RTEllipsoid(0, 0, 0, 0, 0, 1000, 50,
        0xFFFFFF);
    final Hit hit = new Hit();
    assertTrue(ellipsoid.intersect(ray(200, 0, 500, -1, 0, 0), hit));
    assertEquals(150.0, hit.t, EPS);
    assertEquals(1.0, hit.nx, EPS);
    assertEquals(0.0, hit.ny, EPS);
    assertEquals(0.0, hit.nz, EPS);
  }

  @Test
  public void ellipsoidTipHit() {
    // The ends taper to points: a ray down the axis hits the tip, which
    // the old open-ended cylinder could never do.
    final Primitive ellipsoid = new RTEllipsoid(0, 0, 0, 0, 0, 1000, 50,
        0xFFFFFF);
    final Hit hit = new Hit();
    assertTrue(ellipsoid.intersect(ray(0, 0, 1200, 0, 0, -1), hit));
    assertEquals(200.0, hit.t, EPS);
    assertEquals(0.0, hit.nx, EPS);
    assertEquals(0.0, hit.ny, EPS);
    assertEquals(1.0, hit.nz, EPS);
  }

  @Test
  public void ellipsoidTapersTowardEnds() {
    // At z = 900 the cross-section has shrunk to 50 * 0.6 = 30, so the
    // side hit lands nearer than the waist hit would.
    final Primitive ellipsoid = new RTEllipsoid(0, 0, 0, 0, 0, 1000, 50,
        0xFFFFFF);
    final Hit hit = new Hit();
    assertTrue(ellipsoid.intersect(ray(100, 0, 900, -1, 0, 0), hit));
    assertEquals(70.0, hit.t, EPS);
    assertEquals(0.9912, hit.nx, 1e-4);
    assertEquals(0.0, hit.ny, EPS);
    assertEquals(0.1322, hit.nz, 1e-4);
  }

  @Test
  public void ellipsoidMissesBeyondEnd() {
    final Primitive ellipsoid = new RTEllipsoid(0, 0, 0, 0, 0, 1000, 50,
        0xFFFFFF);
    final Hit hit = new Hit();
    // Passes beside the ellipsoid, beyond its far tip.
    assertFalse(ellipsoid.intersect(ray(200, 0, 1500, -1, 0, 0), hit));
  }

  @Test
  public void ellipsoidMissesParallelOffset() {
    final Primitive ellipsoid = new RTEllipsoid(0, 0, 0, 0, 0, 1000, 50,
        0xFFFFFF);
    final Hit hit = new Hit();
    assertFalse(ellipsoid.intersect(ray(200, 0, -100, 0, 0, 1), hit));
  }

  @Test
  public void triangleFrontHit() {
    final Primitive triangle = new RTTriangle(0, 0, 0, 100, 0, 0, 0, 100,
        0, 0xFFFFFF);
    final Hit hit = new Hit();
    assertTrue(triangle.intersect(ray(10, 10, -50, 0, 0, 1), hit));
    assertEquals(50.0, hit.t, EPS);
    assertEquals(0.0, hit.nx, EPS);
    assertEquals(0.0, hit.ny, EPS);
    assertEquals(-1.0, hit.nz, EPS);
  }

  @Test
  public void triangleBackHitFlipsNormal() {
    final Primitive triangle = new RTTriangle(0, 0, 0, 100, 0, 0, 0, 100,
        0, 0xFFFFFF);
    final Hit hit = new Hit();
    assertTrue(triangle.intersect(ray(10, 10, 50, 0, 0, -1), hit));
    assertEquals(50.0, hit.t, EPS);
    assertEquals(0.0, hit.nx, EPS);
    assertEquals(0.0, hit.ny, EPS);
    assertEquals(1.0, hit.nz, EPS);
  }

  @Test
  public void triangleMiss() {
    final Primitive triangle = new RTTriangle(0, 0, 0, 100, 0, 0, 0, 100,
        0, 0xFFFFFF);
    final Hit hit = new Hit();
    assertFalse(triangle.intersect(ray(500, 500, -50, 0, 0, 1), hit));
  }

  @Test
  public void zeroRadiusSphereNeverHits() {
    final Primitive sphere = new RTSphere(0, 0, 0, 0, 0xFFFFFF);
    final Hit hit = new Hit();
    assertFalse(sphere.intersect(ray(0, 0, -1000, 0, 0, 1), hit));
  }

  @Test
  public void zeroLengthEllipsoidNeverHitsAndHasPointBounds() {
    final Primitive ellipsoid = new RTEllipsoid(10, 20, 30, 10, 20, 30, 50,
        0xFFFFFF);
    final Hit hit = new Hit();
    // Aimed straight at the point: still no hit, and no NaN anywhere.
    assertFalse(ellipsoid.intersect(ray(10, 20, -1000, 0, 0, 1), hit));
    final AABB bounds = new AABB();
    ellipsoid.writeBounds(bounds);
    assertFalse(Double.isNaN(bounds.min_x + bounds.max_x + bounds.min_y
        + bounds.max_y + bounds.min_z + bounds.max_z));
  }

  @Test
  public void degenerateTriangleNeverHits() {
    // Collinear points: zero area.
    final Primitive triangle = new RTTriangle(0, 0, 0, 100, 0, 0, 200, 0,
        0, 0xFFFFFF);
    final Hit hit = new Hit();
    assertFalse(triangle.intersect(ray(100, 0, -50, 0, 0, 1), hit));
  }
}
