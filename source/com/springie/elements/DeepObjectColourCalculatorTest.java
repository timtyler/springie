package com.springie.elements;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * Regression test for the depth-fog wrap bug: a negative depth (an object
 * beyond the cached depth range) used to wrap round to near-white instead
 * of fading to black, because the colour channels were clamped at 255
 * but never at 0.
 */
class DeepObjectColourCalculatorTest {

  private static int colourForDepth(int colour, int depth) throws Exception {
    final Method m = DeepObjectColourCalculator.class.getDeclaredMethod(
        "getColourForDepth", int.class, int.class);
    m.setAccessible(true);
    return (int) m.invoke(null, colour, depth);
  }

  @Test
  void normalDepthsAreUnchanged() throws Exception {
    assertEquals(0xFFFFFFFF, colourForDepth(0xFFFFFF, 1024));
    assertEquals(0xFF000000, colourForDepth(0xFFFFFF, 0));
    // 384/1024 brightness: 255 * 384 >> 10 = 95.
    assertEquals(0xFF5F5F5F, colourForDepth(0xFFFFFF, 384));
    // Upper clamp still holds.
    assertEquals(0xFFFFFFFF, colourForDepth(0xFFFFFF, 2048));
  }

  @Test
  void negativeDepthFadesToBlackInsteadOfWrappingToWhite() throws Exception {
    assertEquals(0xFF000000, colourForDepth(0xFFFFFF, -1));
    assertEquals(0xFF000000, colourForDepth(0xFFFFFF, -76));
    assertEquals(0xFF000000, colourForDepth(0xFFFFFF, -2000));
    assertEquals(0xFF000000, colourForDepth(0xFF804020, -500));
  }
}
