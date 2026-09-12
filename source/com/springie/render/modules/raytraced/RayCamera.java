// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.render.Coords;

/**
 * Pinhole camera that reproduces the default renderer's projection exactly.
 *
 * <p>The default renderer projects a world point (X, Y, Z) in internal
 * fixed-point units to pixel
 * <pre>
 *   sx = x_pixelso2 + (X + A) / (D + Z / 1024)
 *   sy = y_pixelso2 + (Y + B) / (D + Z / 1024)
 * </pre>
 * where A = shift_constant_x - (x_pixelso2 &lt;&lt; 8),
 * B = shift_constant_y - (y_pixelso2 &lt;&lt; 8) and D = shift_constant_z.
 * For a fixed pixel the pre-image is the ray
 * <pre>
 *   (u * D - A, v * D - B, 0) + s * (u / 1024, v / 1024, 1)
 * </pre>
 * with u = sx - x_pixelso2, v = sy - y_pixelso2. All such rays pass through
 * the eye E = (-A, -B, -1024 * D), so the projection is a pinhole camera
 * with its eye at E.
 */
final class RayCamera {
  private final double ex, ey, ez;

  private final int x_pixelso2, y_pixelso2;

  RayCamera() {
    this.x_pixelso2 = Coords.x_pixelso2;
    this.y_pixelso2 = Coords.y_pixelso2;
    this.ex = (Coords.x_pixelso2 << Coords.shift) - Coords.shift_constant_x;
    this.ey = (Coords.y_pixelso2 << Coords.shift) - Coords.shift_constant_y;
    this.ez = -1024.0 * Coords.shift_constant_z;
  }

  double getEyeX() {
    return this.ex;
  }

  double getEyeY() {
    return this.ey;
  }

  double getEyeZ() {
    return this.ez;
  }

  void makeRay(int sx, int sy, Ray ray) {
    makeRay((double) sx, (double) sy, ray);
  }

  void makeRay(double sx, double sy, Ray ray) {
    final double dx = (sx - this.x_pixelso2) / 1024.0;
    final double dy = (sy - this.y_pixelso2) / 1024.0;
    final double inverse_length = 1.0 / Math.sqrt(dx * dx + dy * dy + 1.0);
    ray.ox = this.ex;
    ray.oy = this.ey;
    ray.oz = this.ez;
    ray.dx = dx * inverse_length;
    ray.dy = dy * inverse_length;
    ray.dz = inverse_length;
  }
}
