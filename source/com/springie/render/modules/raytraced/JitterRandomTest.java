// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * JitterRandom must replicate java.util.Random's sequence bit-for-bit for
 * any seed: the anti-aliasing paths reseed one instance per pixel instead
 * of allocating a Random per pixel, and the golden checksum test pins the
 * pixels identical.
 */
public class JitterRandomTest {
  @Test
  public void replicatesRandomBitForBit() {
    final long[] seeds = { 0L, 1L, -1L, 0x9E3779B9L,
        73856093L ^ 19349663L ^ 0x9E3779B9L, Long.MIN_VALUE,
        Long.MAX_VALUE };
    for (final long seed : seeds) {
      final Random expected = new Random(seed);
      final JitterRandom actual = new JitterRandom();
      actual.setSeed(seed);
      for (int i = 0; i < 50; i++) {
        assertEquals(expected.nextDouble(), actual.nextDouble(), 0.0,
            "diverged at sample " + i + " for seed " + seed);
      }
      // Reseeding restarts the identical sequence.
      actual.setSeed(seed);
      final Random fresh = new Random(seed);
      assertEquals(fresh.nextDouble(), actual.nextDouble(), 0.0);
    }
  }
}
