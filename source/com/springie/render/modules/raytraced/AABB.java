// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * Axis-aligned bounding box with a branch-safe slab test (handles zero
 * direction components without infinities leaking into the comparison).
 */
final class AABB {
  double min_x = Double.POSITIVE_INFINITY;

  double min_y = Double.POSITIVE_INFINITY;

  double min_z = Double.POSITIVE_INFINITY;

  double max_x = Double.NEGATIVE_INFINITY;

  double max_y = Double.NEGATIVE_INFINITY;

  double max_z = Double.NEGATIVE_INFINITY;

  void addPoint(double x, double y, double z) {
    if (x < this.min_x) {
      this.min_x = x;
    }
    if (x > this.max_x) {
      this.max_x = x;
    }
    if (y < this.min_y) {
      this.min_y = y;
    }
    if (y > this.max_y) {
      this.max_y = y;
    }
    if (z < this.min_z) {
      this.min_z = z;
    }
    if (z > this.max_z) {
      this.max_z = z;
    }
  }

  void add(AABB other) {
    addPoint(other.min_x, other.min_y, other.min_z);
    addPoint(other.max_x, other.max_y, other.max_z);
  }

  double centroidX() {
    return 0.5 * (this.min_x + this.max_x);
  }

  double centroidY() {
    return 0.5 * (this.min_y + this.max_y);
  }

  double centroidZ() {
    return 0.5 * (this.min_z + this.max_z);
  }

  boolean intersect(Ray ray, double t_max) {
    double t0 = 0.0;
    double t1 = t_max;

    if (ray.dx == 0.0) {
      if (ray.ox < this.min_x || ray.ox > this.max_x) {
        return false;
      }
    } else {
      final double inv = 1.0 / ray.dx;
      double ta = (this.min_x - ray.ox) * inv;
      double tb = (this.max_x - ray.ox) * inv;
      if (ta > tb) {
        final double tmp = ta;
        ta = tb;
        tb = tmp;
      }
      if (ta > t0) {
        t0 = ta;
      }
      if (tb < t1) {
        t1 = tb;
      }
      if (t0 > t1) {
        return false;
      }
    }

    if (ray.dy == 0.0) {
      if (ray.oy < this.min_y || ray.oy > this.max_y) {
        return false;
      }
    } else {
      final double inv = 1.0 / ray.dy;
      double ta = (this.min_y - ray.oy) * inv;
      double tb = (this.max_y - ray.oy) * inv;
      if (ta > tb) {
        final double tmp = ta;
        ta = tb;
        tb = tmp;
      }
      if (ta > t0) {
        t0 = ta;
      }
      if (tb < t1) {
        t1 = tb;
      }
      if (t0 > t1) {
        return false;
      }
    }

    if (ray.dz == 0.0) {
      if (ray.oz < this.min_z || ray.oz > this.max_z) {
        return false;
      }
    } else {
      final double inv = 1.0 / ray.dz;
      double ta = (this.min_z - ray.oz) * inv;
      double tb = (this.max_z - ray.oz) * inv;
      if (ta > tb) {
        final double tmp = ta;
        ta = tb;
        tb = tmp;
      }
      if (ta > t0) {
        t0 = ta;
      }
      if (tb < t1) {
        t1 = tb;
      }
      if (t0 > t1) {
        return false;
      }
    }

    return true;
  }
}
