// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * A link, rendered as a finite open-ended cylinder (the tube ends stay open,
 * matching the default renderer's convention).
 */
final class RTCylinder implements Primitive {
  private final double ax, ay, az;

  private final double nx, ny, nz;

  private final double length;

  private final double radius;

  private final int colour;

  RTCylinder(double ax, double ay, double az, double bx, double by, double bz,
      double radius, int colour) {
    this.ax = ax;
    this.ay = ay;
    this.az = az;
    final double dx = bx - ax;
    final double dy = by - ay;
    final double dz = bz - az;
    this.length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    final double inv = 1.0 / this.length;
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
    if (this.length < 1e-9) {
      // Degenerate: a zero-length link contributes its point only, so no
      // NaN from the axis normalization can poison the BVH bounds.
      out.addPoint(this.ax, this.ay, this.az);
      return;
    }
    final double bx = this.ax + this.nx * this.length;
    final double by = this.ay + this.ny * this.length;
    final double bz = this.az + this.nz * this.length;
    out.addPoint(Math.min(this.ax, bx) - this.radius,
        Math.min(this.ay, by) - this.radius,
        Math.min(this.az, bz) - this.radius);
    out.addPoint(Math.max(this.ax, bx) + this.radius,
        Math.max(this.ay, by) + this.radius,
        Math.max(this.az, bz) + this.radius);
  }

  public boolean intersect(Ray ray, Hit hit) {
    if (this.length < 1e-9) {
      return false;
    }
    final double aox = ray.ox - this.ax;
    final double aoy = ray.oy - this.ay;
    final double aoz = ray.oz - this.az;

    final double d_dot_n = ray.dx * this.nx + ray.dy * this.ny + ray.dz
        * this.nz;
    final double ao_dot_n = aox * this.nx + aoy * this.ny + aoz * this.nz;

    // Components perpendicular to the axis.
    final double pdx = ray.dx - d_dot_n * this.nx;
    final double pdy = ray.dy - d_dot_n * this.ny;
    final double pdz = ray.dz - d_dot_n * this.nz;
    final double pax = aox - ao_dot_n * this.nx;
    final double pay = aoy - ao_dot_n * this.ny;
    final double paz = aoz - ao_dot_n * this.nz;

    final double a = pdx * pdx + pdy * pdy + pdz * pdz;
    if (a < 1e-18) {
      return false;
    }
    final double b = 2.0 * (pax * pdx + pay * pdy + paz * pdz);
    final double c = pax * pax + pay * pay + paz * paz - this.radius
        * this.radius;
    final double discriminant = b * b - 4.0 * a * c;
    if (discriminant < 0.0) {
      return false;
    }

    final double root = Math.sqrt(discriminant);
    final double t0 = (-b - root) / (2.0 * a);
    final double t1 = (-b + root) / (2.0 * a);

    final double t = closestValidT(ray, t0, t1);
    if (t < 0.0 || t >= hit.t) {
      return false;
    }

    hit.t = t;
    final double px = ray.ox + t * ray.dx;
    final double py = ray.oy + t * ray.dy;
    final double pz = ray.oz + t * ray.dz;
    final double mx = px - this.ax;
    final double my = py - this.ay;
    final double mz = pz - this.az;
    final double m_dot_n = mx * this.nx + my * this.ny + mz * this.nz;
    double nx = mx - m_dot_n * this.nx;
    double ny = my - m_dot_n * this.ny;
    double nz = mz - m_dot_n * this.nz;
    final double inv = 1.0
        / Math.sqrt(nx * nx + ny * ny + nz * nz);
    hit.nx = nx * inv;
    hit.ny = ny * inv;
    hit.nz = nz * inv;
    hit.primitive = this;
    return true;
  }

  /**
   * Returns the nearest root whose hit point lies on the finite segment,
   * or a negative value if neither does.
   */
  private double closestValidT(Ray ray, double t0, double t1) {
    if (t0 > 1e-9 && onSegment(ray, t0)) {
      return t0;
    }
    if (t1 > 1e-9 && onSegment(ray, t1)) {
      return t1;
    }
    return -1.0;
  }

  private boolean onSegment(Ray ray, double t) {
    final double s = ((ray.ox + t * ray.dx - this.ax) * this.nx
        + (ray.oy + t * ray.dy - this.ay) * this.ny
        + (ray.oz + t * ray.dz - this.az) * this.nz)
        / this.length;
    return s >= 0.0 && s <= 1.0;
  }
}
