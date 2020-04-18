// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.geometry.Point3D;

public class Double3D {
  public double x;

  public double y;

  public double z;

  public Double3D(double x, double y, double z) {
    super();
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Double3D(Point3D point) {
    this.x = point.x;
    this.y = point.y;
    this.z = point.z;
  }

  public Double3D(Double3D point) {
    this.x = point.x;
    this.y = point.y;
    this.z = point.z;
  }

  public Double3D subtract(Double3D d) {
    return new Double3D(this.x - d.x, this.y - d.y, this.z - d.z);
  }

  public Double3D crossProduct(Double3D b) {
    final double nx = this.y * b.z - this.z * b.y;
    final double ny = this.z * b.x - this.x * b.z;
    final double nz = this.x * b.y - this.y * b.x;
    return new Double3D(nx, ny, nz);
  }

  public void normalize() {
    double len = length();
    this.x /= len;
    this.y /= len;
    this.z /= len;
  }

  public double length() {
    return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
  }

  public void divideBy(int denominator) {
    this.x /= denominator;
    this.y /= denominator;
    this.z /= denominator;
  }

  public void add(Double3D value) {
    this.x += value.x;
    this.y += value.y;
    this.z += value.z;
  }

  public boolean allSmallerThan(Double3D target) {
    if (this.x < target.x) {
      if (this.y < target.y) {
        if (this.z < target.z) {
          return true;
        }
      }
    }
    return false;
  }
}
