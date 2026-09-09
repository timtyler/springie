package com.springie.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Vector3DTest {

  @Test
  void dotProductOfPerpendicularVectorsIsZero() {
    assertEquals(0, new Vector3D(1, 0, 0).dot(new Vector3D(0, 1, 0)));
  }

  @Test
  void dotProductSumsComponentProducts() {
    assertEquals(1 * 4 + 2 * 5 + 3 * 6, new Vector3D(1, 2, 3).dot(new Vector3D(4, 5, 6)));
  }

  @Test
  void crossProductOfBasisVectors() {
    final Vector3D x = new Vector3D(1, 0, 0);
    final Vector3D y = new Vector3D(0, 1, 0);
    assertEquals(new Vector3D(0, 0, 1), x.crossProduct(y));
    assertEquals(new Vector3D(0, 0, -1), y.crossProduct(x));
  }

  @Test
  void crossProductOfParallelVectorsIsZero() {
    assertEquals(new Vector3D(0, 0, 0), new Vector3D(2, 4, 6).crossProduct(new Vector3D(1, 2, 3)));
  }

  @Test
  void twoTupleConstructorFormsDifferenceVector() {
    final Vector3D v = new Vector3D(new Tuple3D(5, 7, 9), new Tuple3D(1, 2, 3));
    assertEquals(new Vector3D(4, 5, 6), v);
  }

  @Test
  void lengthMatchesEuclideanLength() {
    assertEquals(5.0, new Vector3D(3, 4, 0).length(), 1e-9);
  }

  @Test
  void lengthOfZeroVectorIsMaxInt() {
    // Historic behaviour: degenerate vectors report Integer.MAX_VALUE.
    assertEquals(Integer.MAX_VALUE, new Vector3D(0, 0, 0).length(), 1e-9);
  }

  @Test
  void cloneProducesEqualVector() {
    final Vector3D original = new Vector3D(1, 2, 3);
    final Vector3D copy = (Vector3D) original.clone();
    assertEquals(original, copy);
  }
}
