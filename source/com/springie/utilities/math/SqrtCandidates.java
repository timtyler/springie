package com.springie.utilities.math;

/**
 * Experimental integer square-root candidates used by the SquareRoot
 * profiling work (SquareRootBenchmark / SquareRootAnalysis).
 *
 * <p>Each method computes {@code floor(sqrt(x))} for {@code x >= 0} and throws
 * {@link IllegalArgumentException} for {@code x < 0}, matching the contract of
 * {@link SquareRoot#sqrt(int)}. They are kept here, out of the production
 * class, until the measurements say which (if any) deserve to be promoted.
 */
public final class SqrtCandidates {

  private SqrtCandidates() {
    // static only
  }

  private static void checkNegative(int x) {
    if (x < 0) {
      throw new IllegalArgumentException(
          "Attemt to take the square root of negative number");
    }
  }

  /**
   * The 2003 premise of {@link SquareRoot} was that {@code Math.sqrt} is slow.
   * On modern hardware it compiles to a single {@code sqrtsd} instruction and
   * is correctly rounded, so {@code (int) Math.sqrt(x)} is exactly
   * {@code floor(sqrt(x))} for every non-negative int. This is the candidate
   * that tests whether the hand-rolled integer code is still worth it.
   */
  public static int intrinsicSqrt(int x) {
    checkNegative(x);
    return (int) Math.sqrt(x);
  }

  /**
   * Integer Newton iteration from a bit-length-derived initial guess, with no
   * lookup table and minimal branching. The guess is within a factor of 2 of
   * the true root, so three Newton steps converge, then a short correction
   * fixes any residual error. Uses only integer division, no floating point.
   */
  public static int newtonBitSqrt(int x) {
    checkNegative(x);
    if (x == 0) {
      return 0;
    }
    // 2^ceil(bitlen/2) >= sqrt(x), and < 2*sqrt(x).
    int r = 1 << ((32 - Integer.numberOfLeadingZeros(x) + 1) / 2);
    r = (r + x / r) >> 1;
    r = (r + x / r) >> 1;
    r = (r + x / r) >> 1;
    while ((long) r * r > x) {
      r--;
    }
    while ((long) (r + 1) * (r + 1) <= x) {
      r++;
    }
    return r;
  }

  /**
   * Classic digit-by-digit (binary) integer square root: 16 iterations of
   * shifts, adds and compares, no division, no table, no floating point.
   * Always exact. (Halleck's method; the commented-out sketch in
   * {@link SquareRoot} had a typo in the initial bit, {@code 1 < <30}.)
   */
  public static int digitSqrt(int x) {
    checkNegative(x);
    int squaredBit = 0x40000000;
    int remainder = x;
    int root = 0;
    while (squaredBit > 0) {
      if (remainder >= (squaredBit | root)) {
        remainder -= squaredBit | root;
        root >>= 1;
        root |= squaredBit;
      } else {
        root >>= 1;
      }
      squaredBit >>= 2;
    }
    return root;
  }

  /**
   * Hardware single-precision sqrt plus integer correction. {@code sqrtss} has
   * higher throughput than {@code sqrtsd} on some chips, but a float only
   * carries 24 bits of mantissa, so the correction loop can be long for large
   * inputs. The measurement will show whether the tradeoff pays.
   */
  public static int floatSqrt(int x) {
    checkNegative(x);
    int r = (int) (float) Math.sqrt((float) x);
    while ((long) r * r > x) {
      r--;
    }
    while ((long) (r + 1) * (r + 1) <= x) {
      r++;
    }
    return r;
  }

  /**
   * Middle ground between {@link SquareRoot#fastSqrt(int)} (table lookup only)
   * and {@link SquareRoot#sqrt(int)} (table + up to two Newton steps): reuse
   * the existing 256-entry table for the initial guess, then apply exactly one
   * Newton iteration and correct. The range ladder of {@code sqrt()} is
   * replaced by bit arithmetic: {@code shift} is the largest even shift with
   * {@code (x >>> shift) < 256}, so {@code table[j] << (shift/2) >>> 4}
   * approximates {@code sqrt(x)}.
   */
  public static int tableOneNewtonSqrt(int x) {
    checkNegative(x);
    if (x < 0x100) {
      return SquareRoot.table[x] >> 4;
    }
    final int bitlen = 32 - Integer.numberOfLeadingZeros(x);
    final int shift = (bitlen - 7) & ~1; // largest even shift, (x>>>shift) < 256
    final int j = x >>> shift;
    int xn = (SquareRoot.table[j] << (shift >>> 1)) >>> 4;
    xn = (xn + 1 + x / xn) >> 1; // single Newton step (with round-up tweak)
    while ((long) xn * xn > x) {
      xn--;
    }
    while ((long) (xn + 1) * (xn + 1) <= x) {
      xn++;
    }
    return xn;
  }

  /**
   * Rounding sibling of {@link #intrinsicSqrt}: matches the documented
   * contract of {@link SquareRoot#accurateSqrt(int)},
   * {@code (int)(Math.sqrt(x) + 0.5)}.
   */
  public static int roundIntrinsicSqrt(int x) {
    checkNegative(x);
    return (int) (Math.sqrt(x) + 0.5);
  }
}
