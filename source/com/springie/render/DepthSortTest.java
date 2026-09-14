// This code has been placed into the public domain by its author

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Pins the shared depth sort: both key types must sort ascending and
 * stably, and the double variant must produce exactly the order the old
 * O(n^2) node bubble sort produced -- the observable order the
 * original renderer has always drawn in.
 */
class DepthSortTest {

  private static int[] sortedByInts(int[] keys, int[] start) {
    final int size = keys.length;
    final int[] index = start.clone();
    DepthSort.sort(index, keys.clone(), size, new int[size]);
    return index;
  }

  private static int[] sortedByDoubles(double[] keys, int[] start) {
    final int size = keys.length;
    final int[] index = start.clone();
    DepthSort.sort(index, keys.clone(), size, new int[size]);
    return index;
  }

  private static int[] identity(int size) {
    final int[] index = new int[size];
    for (int i = 0; i < size; i++) {
      index[i] = i;
    }
    return index;
  }

  private static int[] shuffled(int size, Random random) {
    final int[] index = identity(size);
    for (int i = size; --i > 0;) {
      final int j = random.nextInt(i + 1);
      final int temp = index[i];
      index[i] = index[j];
      index[j] = temp;
    }
    return index;
  }

  @Test
  void intKeysSortAscendingByKey() {
    // Keys {5,1,4,2,3}: ascending values 1,2,3,4,5 live at indices
    // 1,3,4,2,0.
    assertArrayEquals(new int[] {1, 3, 4, 2, 0},
        sortedByInts(new int[] {5, 1, 4, 2, 3}, identity(5)));
  }

  @Test
  void intKeysAreStable() {
    // Ties keep original order: the three 3s stay 0, 2, 4 and the two
    // 1s stay 1, 5 -- the order the old merge sort produced.
    assertArrayEquals(new int[] {1, 5, 3, 0, 2, 4},
        sortedByInts(new int[] {3, 1, 3, 2, 3, 1}, identity(6)));
  }

  /**
   * Stability is relative to the starting order: equal keys keep the
   * relative order they had on entry, whatever permutation that was.
   * (This is also why the NodeManager sort stays correct across frames:
   * it re-sorts the previous frame's order, exactly as the old bubble
   * sort did.)
   */
  @Test
  void sortIsStableRelativeToItsStartingOrder() {
    final Random random = new Random(4242);
    for (int trial = 0; trial < 50; trial++) {
      final int size = random.nextInt(60);
      final int[] keys = new int[size];
      for (int i = 0; i < size; i++) {
        keys[i] = random.nextInt(7) - 3;
      }
      final int[] start = shuffled(size, random);
      final int[] result = sortedByInts(keys, start);
      // Ascending by key...
      for (int i = 1; i < size; i++) {
        assertTrue(keys[result[i - 1]] <= keys[result[i]],
            "trial " + trial + ": must ascend by key");
      }
      // ...and ties keep the entry order.
      for (int i = 1; i < size; i++) {
        if (keys[result[i - 1]] == keys[result[i]]) {
          assertTrue(positionIn(start, result[i - 1])
              < positionIn(start, result[i]),
              "trial " + trial + ": ties must keep entry order");
        }
      }
    }
  }

  private static int positionIn(int[] array, int value) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] == value) {
        return i;
      }
    }
    throw new AssertionError("not a permutation");
  }

  @Test
  void intKeysMatchStableOracleOnRandomInput() {
    final Random random = new Random(12345);
    for (int trial = 0; trial < 50; trial++) {
      final int size = random.nextInt(60);
      final int[] keys = new int[size];
      for (int i = 0; i < size; i++) {
        keys[i] = random.nextInt(7) - 3;
      }
      final Integer[] expected = new Integer[size];
      for (int i = 0; i < size; i++) {
        expected[i] = i;
      }
      // TimSort is stable: ties keep original index order.
      Arrays.sort(expected, Comparator.comparingInt(i -> keys[i]));
      final int[] actual = sortedByInts(keys, identity(size));
      assertEquals(size, actual.length);
      for (int i = 0; i < size; i++) {
        assertEquals(expected[i].intValue(), actual[i],
            "trial " + trial + " position " + i);
      }
    }
  }

  @Test
  void emptyAndSingleElementAreTrivial() {
    assertArrayEquals(new int[0], sortedByInts(new int[0], new int[0]));
    assertArrayEquals(new int[] {0}, sortedByInts(new int[] {7}, identity(1)));
    assertArrayEquals(new int[0],
        sortedByDoubles(new double[0], new int[0]));
    assertArrayEquals(new int[] {0},
        sortedByDoubles(new double[] {7.5}, identity(1)));
  }

  /**
   * Faithful reimplementation of the old NodeManager bubble sort: stable
   * ascending by z, swapping only on a strict greater-than, starting
   * from the given permutation (the real one re-sorted the previous
   * frame's order).
   */
  private static int[] bubbleSort(double[] keys, int[] start) {
    final int n = keys.length;
    final int[] index = start.clone();
    for (int i = n - 1; --i >= 0;) {
      boolean flipped = false;
      for (int j = 0; j <= i; j++) {
        final int k = j + 1;
        if (keys[index[j]] > keys[index[k]]) {
          final int temp = index[j];
          index[j] = index[k];
          index[k] = temp;
          flipped = true;
        }
      }
      if (!flipped) {
        return index;
      }
    }
    return index;
  }

  @Test
  void doubleKeysMatchTheOldBubbleSortExactly() {
    final Random random = new Random(777);
    for (int trial = 0; trial < 200; trial++) {
      final int size = random.nextInt(50);
      final double[] keys = new double[size];
      for (int i = 0; i < size; i++) {
        switch (random.nextInt(6)) {
          case 0:
            keys[i] = random.nextInt(5) - 2; // heavy ties
            break;
          case 1:
            keys[i] = -random.nextDouble() * 1e6;
            break;
          case 2:
            keys[i] = random.nextDouble() * 1e-9; // near-ties
            break;
          case 3:
            keys[i] = Double.POSITIVE_INFINITY;
            break;
          case 4:
            keys[i] = Double.NEGATIVE_INFINITY;
            break;
          default:
            keys[i] = random.nextGaussian() * 1000;
            break;
        }
      }
      // Both algorithms are stable sorts with the same strict
      // comparison, so from the same start permutation -- identity or
      // shuffled -- they must agree exactly.
      final int[] start = shuffled(size, random);
      assertArrayEquals(bubbleSort(keys, start), sortedByDoubles(keys, start),
          "trial " + trial + " from a shuffled start");
      assertArrayEquals(bubbleSort(keys, identity(size)),
          sortedByDoubles(keys, identity(size)),
          "trial " + trial + " from identity");
    }
  }

  @Test
  void doubleKeysAreStable() {
    final double[] keys = {3.5, 1.5, 3.5, 2.5, 3.5, 1.5};
    assertArrayEquals(new int[] {1, 5, 3, 0, 2, 4},
        sortedByDoubles(keys, identity(keys.length)));
  }

  @Test
  void sortedIndexOrdersPositionsAscending() {
    final Random random = new Random(31337);
    for (int trial = 0; trial < 50; trial++) {
      final int size = 1 + random.nextInt(100);
      final double[] keys = new double[size];
      for (int i = 0; i < size; i++) {
        keys[i] = random.nextGaussian();
      }
      final int[] index = sortedByDoubles(keys, identity(size));
      for (int i = 1; i < size; i++) {
        assertTrue(keys[index[i - 1]] <= keys[index[i]],
            "trial " + trial + ": must ascend by key");
      }
    }
  }
}
