package com.springie.elements;

import com.springie.context.ContextMananger;
import com.springie.elements.nodes.Range;
import com.springie.render.Coords;

public final class DeepObjectColourCalculator {
  public static int factor = 640;
  public static boolean depth_is_relative = true;

  private DeepObjectColourCalculator() {
    // ...
  }

  public static int getColourOfDeepObject(int colour, int z) {
    if (depth_is_relative) {
      final Range depth_range = ContextMananger.getNodeManager().getDepthRange();

      final int max_minus_min = depth_range.max - depth_range.min;
      final int zz = (depth_range.max - z) >> Coords.shift;
      final int range = max_minus_min >> Coords.shift;
      final int actual_range = range == 0 ? 1 : range;
      // Long arithmetic: an int overflow here would wrap zzz negative and
      // the depth above z_pixels, flashing the object white.
      final int zzz = (int) ((zz * (long) factor) / actual_range);

      final int depth = Coords.z_pixels - zzz;
      return getColourForDepth(colour, depth);
    }
    final int depth = Coords.z_pixels - (((z >> Coords.shift) * factor) >> 10);
    return getColourForDepth(colour, depth);
  }

  private static int getColourForDepth(int colour, final int depth) {
    int r = (colour >> 16) & 0xFF;
    int g = (colour >> 8) & 0xFF;
    int b = (colour >> 0) & 0xFF;
    r = (r * depth) >> 10;
    g = (g * depth) >> 10;
    b = (b * depth) >> 10;
    // Clamp both ends: without the lower clamp, a negative depth (an object
    // beyond the cached depth range) makes the channels negative, and their
    // sign bits then leak into the packed colour, wrapping it to near-white.
    if (r > 255) {
      r = 255;
    } else if (r < 0) {
      r = 0;
    }
    if (g > 255) {
      g = 255;
    } else if (g < 0) {
      g = 0;
    }
    if (b > 255) {
      b = 255;
    } else if (b < 0) {
      b = 0;
    }

    return 0xFF000000 | b | g << 8 | r << 16;
  }
}