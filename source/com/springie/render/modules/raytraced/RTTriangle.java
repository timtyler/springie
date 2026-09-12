// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * One triangle of a face. Double-sided: the normal is flipped to face the
 * incoming ray, matching the default renderer's |normal . light| shading.
 */
final class RTTriangle implements Primitive {
  private final double ax, ay, az;

  private final double e1x, e1y, e1z;

  private final double e2x, e2y, e2z;

  private final double nx, ny, nz;

  private final boolean degenerate;

  private final int colour;

  RTTriangle(double ax, double ay, double az, double bx, double by, double bz,
      double cx, double cy, double cz, int colour) {
    this.ax = ax;
    this.ay = ay;
    this.az = az;
    this.e1x = bx - ax;
    this.e1y = by - ay;
    this.e1z = bz - az;
    this.e2x = cx - ax;
    this.e2y = cy - ay;
    this.e2z = cz - az;
    final double nx = this.e1y * this.e2z - this.e1z * this.e2y;
    final double ny = this.e1z * this.e2x - this.e1x * this.e2z;
    final double nz = this.e1x * this.e2y - this.e1y * this.e2x;
    final double area2 = Math.sqrt(nx * nx + ny * ny + nz * nz);
    // A zero-area triangle can never be hit; flag it so no NaN normal is
    // ever produced or shaded.
    this.degenerate = area2 < 1e-12;
    final double inv = this.degenerate ? 0.0 : 1.0 / area2;
    this.nx = nx * inv;
    this.ny = ny * inv;
    this.nz = nz * inv;
    this.colour = colour;
  }

  public int getColour() {
    return this.colour;
  }

  public boolean isUnlit() {
    return false;
  }

  public void writeBounds(AABB out) {
    out.addPoint(this.ax, this.ay, this.az);
    out.addPoint(this.ax + this.e1x, this.ay + this.e1y, this.az + this.e1z);
    out.addPoint(this.ax + this.e2x, this.ay + this.e2y, this.az + this.e2z);
  }

  public boolean intersect(Ray ray, Hit hit) {
    if (this.degenerate) {
      return false;
    }
    // Moller-Trumbore, accepting hits from either side.
    final double px = ray.dy * this.e2z - ray.dz * this.e2y;
    final double py = ray.dz * this.e2x - ray.dx * this.e2z;
    final double pz = ray.dx * this.e2y - ray.dy * this.e2x;
    final double det = this.e1x * px + this.e1y * py + this.e1z * pz;
    if (det > -1e-12 && det < 1e-12) {
      return false;
    }
    final double inv_det = 1.0 / det;

    final double sx = ray.ox - this.ax;
    final double sy = ray.oy - this.ay;
    final double sz = ray.oz - this.az;
    final double u = (sx * px + sy * py + sz * pz) * inv_det;
    if (u < 0.0 || u > 1.0) {
      return false;
    }

    final double qx = sy * this.e1z - sz * this.e1y;
    final double qy = sz * this.e1x - sx * this.e1z;
    final double qz = sx * this.e1y - sy * this.e1x;
    final double v = (ray.dx * qx + ray.dy * qy + ray.dz * qz) * inv_det;
    if (v < 0.0 || u + v > 1.0) {
      return false;
    }

    final double t = (this.e2x * qx + this.e2y * qy + this.e2z * qz)
        * inv_det;
    if (t < 1e-9 || t >= hit.t) {
      return false;
    }

    hit.t = t;
    final double facing = this.nx * ray.dx + this.ny * ray.dy + this.nz
        * ray.dz;
    if (facing > 0.0) {
      hit.nx = -this.nx;
      hit.ny = -this.ny;
      hit.nz = -this.nz;
    } else {
      hit.nx = this.nx;
      hit.ny = this.ny;
      hit.nz = this.nz;
    }
    hit.primitive = this;
    return true;
  }
}
