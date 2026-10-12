package com.springie.geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tuple3D implements Cloneable {
  private static final Logger logger = LoggerFactory.getLogger(Tuple3D.class);

  public int x;

  public int y;

  public int z;

  public Tuple3D(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Tuple3D(final Tuple3D t) {
    this.x = t.x;
    this.y = t.y;
    this.z = t.z;
  }

  public void set(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public void set(final Tuple3D t) {
    this.x = t.x;
    this.y = t.y;
    this.z = t.z;
  }

  public void addTuple3D(final Tuple3D delta) {
    this.x += delta.x;
    this.y += delta.y;
    this.z += delta.z;
  }

  public void subtractTuple3D(final Tuple3D delta) {
    this.x -= delta.x;
    this.y -= delta.y;
    this.z -= delta.z;
  }

  public void divideBy(final int number) {
    this.x /= number;
    this.y /= number;
    this.z /= number;
  }

  public void multiplyBy(final int factor) {
    this.x *= factor;
    this.y *= factor;
    this.z *= factor;
  }

  public void multiplyBy(final float factor) {
    this.x *= factor;
    this.y *= factor;
    this.z *= factor;
  }

  public void multiplyBy(final double factor) {
    this.x *= factor;
    this.y *= factor;
    this.z *= factor;
  }

  public boolean equals(final Object o) {
    if (!(o instanceof Tuple3D)) {
      return false;
    }

    final Tuple3D t = (Tuple3D) o;
    if (t.x != this.x) {
      return false;
    }
    if (t.y != this.y) {
      return false;
    }
    if (t.z != this.z) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    return this.x * this.y * this.z;
  }

  public Object clone() {
    try {
      super.clone();
    } catch (CloneNotSupportedException e) {
      logger.error("Unexpected exception", e);
    }
    return new Tuple3D(this.x, this.y, this.z);
  }
}
