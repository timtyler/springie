// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * The BVH must agree with brute force on the nearest hit for every ray.
 */
public class BVHTest {
  private static boolean bruteForce(Primitive[] primitives, Ray ray,
      Hit hit) {
    boolean found = false;
    for (final Primitive p : primitives) {
      if (p.intersect(ray, hit)) {
        found = true;
      }
    }
    return found;
  }

  @Test
  public void emptySceneNeverHits() {
    final BVH bvh = new BVH(new Primitive[0]);
    assertTrue(bvh.isEmpty());
    final Ray ray = new Ray();
    ray.oz = -1000.0;
    ray.dz = 1.0;
    assertFalse(bvh.intersect(ray, new Hit(), new int[64]));
  }

  @Test
  public void agreesWithBruteForce() {
    final Random random = new Random(1234);
    final Primitive[] primitives = new Primitive[60];
    for (int i = 0; i < primitives.length; i++) {
      final double cx = random.nextDouble() * 2000.0 - 1000.0;
      final double cy = random.nextDouble() * 2000.0 - 1000.0;
      final double cz = random.nextDouble() * 2000.0;
      final double r = 20.0 + random.nextDouble() * 80.0;
      final int kind = i % 3;
      if (kind == 0) {
        primitives[i] = new RTSphere(cx, cy, cz, r, i);
      } else if (kind == 1) {
        primitives[i] = new RTCylinder(cx, cy, cz, cx + 200.0, cy - 100.0,
            cz + 300.0, r * 0.5, i);
      } else {
        primitives[i] = new RTTriangle(cx, cy, cz, cx + r, cy, cz, cx,
            cy + r, cz, i);
      }
    }

    final BVH bvh = new BVH(primitives);
    final int[] stack = new int[64];

    for (int i = 0; i < 500; i++) {
      final Ray ray = new Ray();
      ray.ox = random.nextDouble() * 2000.0 - 1000.0;
      ray.oy = random.nextDouble() * 2000.0 - 1000.0;
      ray.oz = -1500.0;
      double dx = random.nextDouble() - 0.5;
      double dy = random.nextDouble() - 0.5;
      final double dz = 1.0;
      final double inv = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
      ray.dx = dx * inv;
      ray.dy = dy * inv;
      ray.dz = dz * inv;

      final Hit expected = new Hit();
      final boolean expected_hit = bruteForce(primitives, ray, expected);
      final Hit actual = new Hit();
      final boolean actual_hit = bvh.intersect(ray, actual, stack);

      assertEquals(expected_hit, actual_hit, "hit mismatch on ray " + i);
      if (expected_hit) {
        assertEquals(expected.t, actual.t, 1e-9,
            "t mismatch on ray " + i);
        assertTrue(expected.primitive == actual.primitive,
            "primitive mismatch on ray " + i);
      }
    }
  }

  @Test
  public void singlePrimitiveScene() {
    final Primitive[] primitives = new Primitive[] {
        new RTSphere(0, 0, 500, 100, 0xFFFFFF) };
    final BVH bvh = new BVH(primitives);
    final Ray ray = new Ray();
    ray.oz = -1000.0;
    ray.dz = 1.0;
    final Hit hit = new Hit();
    assertTrue(bvh.intersect(ray, hit, new int[64]));
    assertEquals(1400.0, hit.t, 1e-9);
  }
}
