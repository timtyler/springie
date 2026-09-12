// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * A link, rendered as a stretched sphere: an ellipsoid whose long axis runs
 * from one end of the link to the other and whose waist has the link's
 * radius, so struts bulge in the middle and taper to points at the nodes.
 */
final class RTEllipsoid implements Primitive {
  private final double cx, cy, cz;

  private final double nx, ny, nz;

  private final double halfLength;

  private final double radius;

  private final int colour;

  RTEllipsoid(double ax, double ay, double az, double bx, double by, double bz,
      double radius, int colour) {
    this.cx = (ax + bx) / 2.0;
    this.cy = (ay + by) / 2.0;
    this.cz = (az + bz) / 2.0;
    final double dx = bx - ax;
    final double dy = by - ay;
    final double dz = bz - az;
    this.halfLength = Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0;
    final double inv = 1.0 / (2.0 * this.halfLength);
    this.nx = dx * inv;
    this.ny = dy * inv;
    this.nz = dz * inv;
    this.radius = radius;
    this.colour = colour;
  }

  public int getColour() {
    return this.colour;
  }

  public void writeBounds(AABB out) {
    if (this.halfLength < 1e-9) {
      // Degenerate: a zero-length link contributes its point only, so no
      // NaN from the axis normalization can poison the BVH bounds.
      out.addPoint(this.cx, this.cy, this.cz);
      return;
    }
    final double ex = this.nx * this.halfLength;
    final double ey = this.ny * this.halfLength;
    final double ez = this.nz * this.halfLength;
    out.addPoint(
        Math.min(this.cx - ex, this.cx + ex) - this.radius,
        Math.min(this.cy - ey, this.cy + ey) - this.radius,
        Math.min(this.cz - ez, this.cz + ez) - this.radius);
    out.addPoint(
        Math.max(this.cx - ex, this.cx + ex) + this.radius,
        Math.max(this.cy - ey, this.cy + ey) + this.radius,
        Math.max(this.cz - ez, this.cz + ez) + this.radius);
  }

  public boolean intersect(Ray ray, Hit hit) {
    if (this.halfLength < 1e-9) {
      return false;
    }
    final double wx = ray.ox - this.cx;
    final double wy = ray.oy - this.cy;
    final double wz = ray.oz - this.cz;

    final double dp = ray.dx * this.nx + ray.dy * this.ny + ray.dz
        * this.nz;
    final double wp = wx * this.nx + wy * this.ny + wz * this.nz;

    // Components perpendicular to the long axis.
    final double pdx = ray.dx - dp * this.nx;
    final double pdy = ray.dy - dp * this.ny;
    final double pdz = ray.dz - dp * this.nz;
    final double pwx = wx - wp * this.nx;
    final double pwy = wy - wp * this.ny;
    final double pwz = wz - wp * this.nz;

    final double invL2 = 1.0 / (this.halfLength * this.halfLength);
    final double invR2 = 1.0 / (this.radius * this.radius);

    final double a = dp * dp * invL2
        + (pdx * pdx + pdy * pdy + pdz * pdz) * invR2;
    final double b = 2.0 * (wp * dp * invL2
        + (pwx * pdx + pwy * pdy + pwz * pdz) * invR2);
    final double c = wp * wp * invL2
        + (pwx * pwx + pwy * pwy + pwz * pwz) * invR2 - 1.0;

    final double discriminant = b * b - 4.0 * a * c;
    if (discriminant < 0.0) {
      return false;
    }

    final double root = Math.sqrt(discriminant);
    final double t0 = (-b - root) / (2.0 * a);
    final double t1 = (-b + root) / (2.0 * a);

    final double t = closestValidT(t0, t1, hit.t);
    if (t < 0.0) {
      return false;
    }

    hit.t = t;
    // Surface normal: gradient of (s/L)^2 + (|q|/r)^2 - 1, where s is the
    // axial coordinate and q the perpendicular offset of the hit point.
    final double s = wp + t * dp;
    final double qx = pwx + t * pdx;
    final double qy = pwy + t * pdy;
    final double qz = pwz + t * pdz;
    final double gx = 2.0 * s * invL2 * this.nx + 2.0 * qx * invR2;
    final double gy = 2.0 * s * invL2 * this.ny + 2.0 * qy * invR2;
    final double gz = 2.0 * s * invL2 * this.nz + 2.0 * qz * invR2;
    final double inv = 1.0 / Math.sqrt(gx * gx + gy * gy + gz * gz);
    hit.nx = gx * inv;
    hit.ny = gy * inv;
    hit.nz = gz * inv;
    hit.primitive = this;
    return true;
  }

  /**
   * Returns the nearest root in front of the ray origin and nearer than the
   * recorded hit, or a negative value if neither root qualifies.
   */
  private static double closestValidT(double t0, double t1, double maxT) {
    if (t0 > 1e-9 && t0 < maxT) {
      return t0;
    }
    if (t1 > 1e-9 && t1 < maxT) {
      return t1;
    }
    return -1.0;
  }
}
