package com.springie.geometry;

public class Point3D extends Tuple3D implements Cloneable {
  public Point3D(final int x, final int y, final int z) {
    super(x, y, z);
  }

  public Point3D(final Tuple3D t) {
    super(t);
  }

  public Object clone() {
    super.clone();
    return new Point3D(this.x, this.y, this.z);
  }
}
