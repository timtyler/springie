package com.springie.utilities.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SquareRootTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 10, 99, 100, 101, 1000, 65535, 65536,
      1000000, 123456789, Integer.MAX_VALUE})
  void sqrtMatchesMathSqrt(int x) {
    assertEquals((int) Math.sqrt(x), SquareRoot.sqrt(x), "sqrt(" + x + ")");
  }

  @Test
  void sqrtIsMonotonicOverRange() {
    int previous = -1;
    for (int x = 0; x < 100000; x += 7) {
      final int result = SquareRoot.sqrt(x);
      assertTrue(result >= previous, "not monotonic at x=" + x);
      previous = result;
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 10, 99, 100, 200, 288})
  void fastSqrtIsAccurateBelow289(int x) {
    assertEquals((int) Math.sqrt(x), SquareRoot.fastSqrt(x), "fastSqrt(" + x + ")");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 10, 99, 100, 101, 1000, 65535, 65536,
      1000000, 123456789, Integer.MAX_VALUE})
  void accurateSqrtRoundsToNearest(int x) {
    assertEquals((int) (Math.sqrt(x) + 0.5), SquareRoot.accurateSqrt(x),
        "accurateSqrt(" + x + ")");
  }
}
