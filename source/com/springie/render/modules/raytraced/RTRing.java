// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * The selection indicator for a node: a flat ring (annulus) in the plane
 * through the node's centre, perpendicular to the ray from the camera eye
 * to that centre -- so it always faces the viewer, exactly like the
 * default renderer's screen-space selection circle.
 *
 * <p>The ring is unlit: it renders in its flat red at full strength from
 * any angle, fogged for depth like everything else. It lives outside the
 * BVH in a small side array, so it can never cast shadows or slow down
 * the shadow rays; when nothing is selected the array is empty and the
 * per-ray cost is a single length check.
 */
final class RTRing implements Primitive {
  private final double cx, cy, cz;

  /** Unit normal of the ring plane, pointing back towards the eye. */
  private final double nx, ny, nz;

  private final double inner_sq, outer_sq;

  private final double outer;

  private final int colour;

  /**
   * @param nx ny nz the (already normalized) plane normal, eye-ward
   * @param inner the annulus inner radius, in world units
   * @param outer the annulus outer radius, in world units
   */
  RTRing(double cx, double cy, double cz, double nx, double ny, double nz,
      double inner, double outer, int colour) {
    this.cx = cx;
    this.cy = cy;
    this.cz = cz;
    this.nx = nx;
    this.ny = ny;
    this.nz = nz;
    this.inner_sq = inner * inner;
    this.outer_sq = outer * outer;
    this.outer = outer;
    this.colour = colour;
  }

  public int getColour() {
    return this.colour;
  }

  public boolean isUnlit() {
    return true;
  }

  public void writeBounds(AABB out) {
    // Conservative cube around the ring; the ring never enters the BVH,
    // so this is only here to satisfy the interface.
    out.addPoint(this.cx - this.outer, this.cy - this.outer,
        this.cz - this.outer);
    out.addPoint(this.cx + this.outer, this.cy + this.outer,
        this.cz + this.outer);
  }

  public boolean intersect(Ray ray, Hit hit) {
    final double denom = ray.dx * this.nx + ray.dy * this.ny + ray.dz * this.nz;
    if (denom > -1e-9 && denom < 1e-9) {
      return false;
    }
    final double t = ((this.cx - ray.ox) * this.nx + (this.cy - ray.oy) * this.ny
        + (this.cz - ray.oz) * this.nz) / denom;
    if (t < 1e-9 || t >= hit.t) {
      return false;
    }
    final double px = ray.ox + ray.dx * t - this.cx;
    final double py = ray.oy + ray.dy * t - this.cy;
    final double pz = ray.oz + ray.dz * t - this.cz;
    final double dist_sq = px * px + py * py + pz * pz;
    if (dist_sq < this.inner_sq || dist_sq > this.outer_sq) {
      return false;
    }
    hit.t = t;
    if (denom > 0.0) {
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
