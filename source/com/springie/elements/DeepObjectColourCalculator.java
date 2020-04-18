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
      final int zzz = (zz * factor) / actual_range;

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
    if (r > 255) {
      r = 255;
    }
    if (g > 255) {
      g = 255;
    }
    if (b > 255) {
      b = 255;
    }

    return 0xFF000000 | b | g << 8 | r << 16;
  }
}