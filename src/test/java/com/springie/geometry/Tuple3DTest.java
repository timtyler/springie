package com.springie.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Tuple3DTest {

  @Test
  void addTuple3DAccumulatesComponents() {
    final Tuple3D t = new Tuple3D(1, 2, 3);
    t.addTuple3D(new Tuple3D(4, -1, 10));
    assertEquals(new Tuple3D(5, 1, 13), t);
  }

  @Test
  void subtractTuple3DSubtractsComponents() {
    final Tuple3D t = new Tuple3D(5, 1, 13);
    t.subtractTuple3D(new Tuple3D(4, -1, 10));
    assertEquals(new Tuple3D(1, 2, 3), t);
  }

  @Test
  void multiplyByIntScalesComponents() {
    final Tuple3D t = new Tuple3D(1, -2, 3);
    t.multiplyBy(3);
    assertEquals(new Tuple3D(3, -6, 9), t);
  }

  @Test
  void divideByIntDividesComponents() {
    final Tuple3D t = new Tuple3D(9, -6, 3);
    t.divideBy(3);
    assertEquals(new Tuple3D(3, -2, 1), t);
  }

  @Test
  void copyConstructorCopiesValues() {
    final Tuple3D t = new Tuple3D(new Tuple3D(7, 8, 9));
    assertEquals(7, t.x);
    assertEquals(8, t.y);
    assertEquals(9, t.z);
  }

  @Test
  void equalsComparesValuesNotIdentity() {
    final Tuple3D a = new Tuple3D(1, 2, 3);
    final Tuple3D b = new Tuple3D(1, 2, 3);
    assertNotSame(a, b);
    assertTrue(a.equals(b));
    assertTrue(b.equals(a));
  }

  @Test
  void equalsRejectsDifferentValuesAndTypes() {
    final Tuple3D a = new Tuple3D(1, 2, 3);
    assertNotEquals(new Tuple3D(1, 2, 4), a);
    assertNotEquals(new Tuple3D(1, 4, 3), a);
    assertNotEquals(new Tuple3D(4, 2, 3), a);
    assertNotEquals(null, a);
    assertNotEquals("not a tuple", a);
  }

  @Test
  void cloneProducesEqualButDistinctTuple() {
    final Tuple3D original = new Tuple3D(1, 2, 3);
    final Tuple3D copy = (Tuple3D) original.clone();
    assertNotSame(original, copy);
    assertEquals(original, copy);
  }
}
