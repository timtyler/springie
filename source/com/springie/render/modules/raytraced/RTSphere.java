// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * A node, rendered as a sphere.
 */
final class RTSphere implements Primitive {
  private final double cx, cy, cz, radius;

  private final int colour;

  RTSphere(double cx, double cy, double cz, double radius, int colour) {
    this.cx = cx;
    this.cy = cy;
    this.cz = cz;
    this.radius = radius;
    this.colour = colour;
  }

  public int getColour() {
    return this.colour;
  }

  public void writeBounds(AABB out) {
    out.addPoint(this.cx - this.radius, this.cy - this.radius,
        this.cz - this.radius);
    out.addPoint(this.cx + this.radius, this.cy + this.radius,
        this.cz + this.radius);
  }

  public boolean intersect(Ray ray, Hit hit) {
    if (this.radius <= 0.0) {
      return false;
    }
    final double ox = ray.ox - this.cx;
    final double oy = ray.oy - this.cy;
    final double oz = ray.oz - this.cz;

    // Direction is normalized, so a = 1.
    final double b = ox * ray.dx + oy * ray.dy + oz * ray.dz;
    final double c = ox * ox + oy * oy + oz * oz - this.radius * this.radius;
    final double discriminant = b * b - c;
    if (discriminant < 0.0) {
      return false;
    }

    final double root = Math.sqrt(discriminant);
    double t = -b - root;
    if (t < 1e-9) {
      t = -b + root;
      if (t < 1e-9) {
        return false;
      }
    }
    if (t >= hit.t) {
      return false;
    }

    hit.t = t;
    final double inv_r = 1.0 / this.radius;
    hit.nx = (ox + t * ray.dx) * inv_r;
    hit.ny = (oy + t * ray.dy) * inv_r;
    hit.nz = (oz + t * ray.dz) * inv_r;
    hit.primitive = this;
    return true;
  }
}
