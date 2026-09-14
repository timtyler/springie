// This code has been placed into the public domain by its author

package com.springie.render;

/**
 * Stable bottom-up merge sorts over an index array, ascending by key.
 *
 * Both variants sort the index so that keys ascend; equal keys keep
 * their original relative order (on a tie the merge takes from the left
 * run), which makes the result identical to a stable bubble sort using
 * the same strict comparison -- the observable order the renderers
 * have always produced, without the O(n^2) worst case.
 *
 * The caller owns the buffers: on entry index must hold a permutation
 * of 0..size-1 (it need not be the identity -- any permutation is
 * sorted), keys must hold the sort key per position, and scratch must
 * hold at least size entries. Buffers are reused across frames; nothing
 * is allocated here.
 */
public final class DepthSort {
  private DepthSort() {
    // Static utility.
  }

  /**
   * Sorts index ascending by the int keys.
   */
  public static void sort(int[] index, int[] keys, int size, int[] scratch) {
    if (size < 2) {
      return;
    }
    int[] src = index;
    int[] dst = scratch;
    for (int width = 1; width < size; width <<= 1) {
      final int step = width << 1;
      for (int lo = 0; lo < size; lo += step) {
        int mid = lo + width;
        if (mid > size) {
          mid = size;
        }
        int hi = lo + step;
        if (hi > size) {
          hi = size;
        }
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
          // Strictly-less takes from the right run; ties take from the
          // left run, which keeps the sort stable.
          if (keys[src[j]] < keys[src[i]]) {
            dst[k++] = src[j++];
          } else {
            dst[k++] = src[i++];
          }
        }
        while (i < mid) {
          dst[k++] = src[i++];
        }
        while (j < hi) {
          dst[k++] = src[j++];
        }
      }
      final int[] temp = src;
      src = dst;
      dst = temp;
    }
    if (src != index) {
      System.arraycopy(src, 0, index, 0, size);
    }
  }

  /**
   * Sorts index ascending by the double keys. Same stability contract
   * as the int variant; NaN keys are not expected (a NaN position
   * breaks coordinate projection long before the sort matters).
   */
  public static void sort(int[] index, double[] keys, int size,
      int[] scratch) {
    if (size < 2) {
      return;
    }
    int[] src = index;
    int[] dst = scratch;
    for (int width = 1; width < size; width <<= 1) {
      final int step = width << 1;
      for (int lo = 0; lo < size; lo += step) {
        int mid = lo + width;
        if (mid > size) {
          mid = size;
        }
        int hi = lo + step;
        if (hi > size) {
          hi = size;
        }
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
          if (keys[src[j]] < keys[src[i]]) {
            dst[k++] = src[j++];
          } else {
            dst[k++] = src[i++];
          }
        }
        while (i < mid) {
          dst[k++] = src[i++];
        }
        while (j < hi) {
          dst[k++] = src[j++];
        }
      }
      final int[] temp = src;
      src = dst;
      dst = temp;
    }
    if (src != index) {
      System.arraycopy(src, 0, index, 0, size);
    }
  }
}
