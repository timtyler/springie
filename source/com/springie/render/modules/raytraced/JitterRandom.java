// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

/**
 * A reusable stand-in for {@code new Random(seed)} in the anti-aliasing
 * jitter paths: it replicates java.util.Random's sequence bit-for-bit
 * (same seed scrambling, same 48-bit LCG, same nextDouble) so the pixels
 * are identical, but one instance is reseeded per pixel instead of
 * allocating a Random per pixel.
 */
final class JitterRandom {
  private static final long MULTIPLIER = 0x5DEECE66DL;

  private static final long ADDEND = 0xBL;

  private static final long MASK = (1L << 48) - 1;

  private static final double DOUBLE_UNIT = 1.0 / (1L << 53);

  private long seed;

  void setSeed(long seed) {
    this.seed = (seed ^ MULTIPLIER) & MASK;
  }

  private int next(int bits) {
    this.seed = (this.seed * MULTIPLIER + ADDEND) & MASK;
    return (int) (this.seed >>> (48 - bits));
  }

  double nextDouble() {
    return (((long) next(26) << 27) + next(27)) * DOUBLE_UNIT;
  }
}
