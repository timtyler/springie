package com.springie.geometry;

public class Vector3D extends Tuple3D {
  public Vector3D(int x, int y, int z) {
    super(x, y, z);
  }

  public Vector3D(Tuple3D t) {
    super(t);
  }

  public Vector3D(Tuple3D a, Tuple3D b) {
    super(a.x - b.x, a.y - b.y, a.z - b.z);
  }

  int getLength() {
    return 0;
  }

  long getLengthSquared() {
    return 0;
  }

  public Object clone() {
    super.clone();
    return new Vector3D(this.x, this.y, this.z);
  }

  public int dot(Vector3D v2) {
    return this.x * v2.x + this.y * v2.y + this.z * v2.z;
  }

  public double length() {
    final double dx = this.x;
    final double dy = this.y;
    final double dz = this.z;
    final double sum_sq = dx * dx + dy * dy + dz * dz;

    if (sum_sq < 1) {
      return Integer.MAX_VALUE;
    }

    return Math.sqrt(sum_sq);
  }

  public Vector3D crossProduct(Vector3D b) {
    final int nx = this.y * b.z - this.z * b.y;
    final int ny = this.z * b.x - this.x * b.z;
    final int nz = this.x * b.y - this.y * b.x;

    return new Vector3D(nx, ny, nz);
  }
}
