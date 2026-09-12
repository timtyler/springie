// This code has been placed into the public domain by its author

package com.springie.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A procedural background texture: sky above, grass below, split exactly
 * 50/50, with little fluffy clouds drifting in the sky half. Generated
 * deterministically from the window size (fixed seed), so the same size
 * always yields the same picture and nothing shimmers between frames.
 *
 * <p>The image is cached and only regenerated when the requested size
 * changes, so per-frame use is a cache hit.
 */
public final class ScenicBackground {
  private static final long SEED = 0x5EED5EEDL;

  private static int cached_width = -1;

  private static int cached_height = -1;

  private static BufferedImage cached_image;

  private ScenicBackground() {
    // Static only.
  }

  /**
   * Returns the scenic background for a canvas of the given size,
   * generating and caching it on first use or when the size changes.
   */
  public static synchronized BufferedImage imageFor(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException(
          "width and height must be positive: " + width + "x" + height);
    }
    if (cached_image == null || width != cached_width
        || height != cached_height) {
      cached_image = generate(width, height);
      cached_width = width;
      cached_height = height;
    }
    return cached_image;
  }

  /**
   * Samples the cached image, clamping out-of-range coordinates to the
   * nearest edge pixel. Used by the ray-tracer for sub-pixel background
   * lookups.
   */
  public static int sampleClamped(BufferedImage image, int x, int y) {
    final int w = image.getWidth();
    final int h = image.getHeight();
    if (x < 0) {
      x = 0;
    } else if (x >= w) {
      x = w - 1;
    }
    if (y < 0) {
      y = 0;
    } else if (y >= h) {
      y = h - 1;
    }
    return image.getRGB(x, y);
  }

  private static BufferedImage generate(int width, int height) {
    final BufferedImage image = new BufferedImage(width, height,
        BufferedImage.TYPE_INT_RGB);
    final Random random = new Random(SEED ^ (width * 31L + height));
    paintSky(image, width, height, random);
    paintClouds(image, width, height, random);
    paintGrass(image, width, height, random);
    return image;
  }

  private static void paintSky(BufferedImage image, int width, int height,
      Random random) {
    final int horizon = height / 2;
    // Deep blue at the zenith, pale near the horizon.
    final int top_r = 46;
    final int top_g = 111;
    final int top_b = 216;
    final int horizon_r = 207;
    final int horizon_g = 232;
    final int horizon_b = 245;
    for (int y = 0; y < horizon; y++) {
      final double t = (double) y / Math.max(1, horizon - 1);
      final int r = (int) (top_r + (horizon_r - top_r) * t);
      final int g = (int) (top_g + (horizon_g - top_g) * t);
      final int b = (int) (top_b + (horizon_b - top_b) * t);
      final int rgb = (r << 16) | (g << 8) | b;
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, rgb);
      }
    }
  }

  /**
   * Little fluffy clouds: each is a cluster of soft white puffs (radial
   * gradients, opaque centre fading to transparent) over a faint
   * blue-grey copy offset downwards for a hint of shading.
   */
  private static void paintClouds(BufferedImage image, int width, int height,
      Random random) {
    final int horizon = height / 2;
    final Graphics2D g = image.createGraphics();
    try {
      final int clouds = Math.min(12, 4 + width * height / 300000);
      for (int c = 0; c < clouds; c++) {
        final double cloud_w = width * (0.08 + random.nextDouble() * 0.07);
        final double cloud_h = cloud_w * (0.35 + random.nextDouble() * 0.2);
        final double cx = random.nextDouble() * width;
        // Keep clouds inside the sky half, clear of the horizon.
        final double cy = height * 0.05
            + random.nextDouble() * (horizon - cloud_h - height * 0.08);
        final int puffs = 12 + random.nextInt(9);
        for (int p = 0; p < puffs; p++) {
          // Centred triangular distribution: puffs bunch in the middle
          // and merge into one fluffy mass instead of separate balls.
          final double px = cx + (triangular(random) * cloud_w);
          final double py = cy + (triangular(random) * cloud_h);
          final double pr = cloud_w * (0.14 + random.nextDouble() * 0.14);
          // Soft under-shadow first, then the white puff on top.
          drawPuff(g, px, py + pr * 0.35, pr,
              new Color(170, 190, 215, 90));
          drawPuff(g, px, py, pr, new Color(255, 255, 255, 225));
        }
      }
    } finally {
      g.dispose();
    }
  }

  private static double triangular(Random random) {
    return (random.nextDouble() + random.nextDouble()
        + random.nextDouble()) / 3.0 - 0.5;
  }

  private static void drawPuff(Graphics2D g, double x, double y, double radius,
      Color colour) {
    final float[] fractions = { 0.0f, 0.55f, 1.0f };
    final Color transparent = new Color(colour.getRed(), colour.getGreen(),
        colour.getBlue(), 0);
    final Color[] colours = { colour, colour, transparent };
    final RadialGradientPaint paint = new RadialGradientPaint(
        new Point2D.Double(x, y), (float) radius, fractions, colours,
        MultipleGradientPaint.CycleMethod.NO_CYCLE);
    g.setPaint(paint);
    final int r = (int) Math.ceil(radius);
    g.fillOval((int) Math.round(x - r), (int) Math.round(y - r), r * 2,
        r * 2);
  }

  private static void paintGrass(BufferedImage image, int width, int height,
      Random random) {
    final int horizon = height / 2;
    // Fresh green at the horizon, deep green at the bottom.
    final int top_r = 124;
    final int top_g = 179;
    final int top_b = 66;
    final int bottom_r = 46;
    final int bottom_g = 125;
    final int bottom_b = 50;
    for (int y = horizon; y < height; y++) {
      final double t = (double) (y - horizon)
          / Math.max(1, height - 1 - horizon);
      final int base_r = (int) (top_r + (bottom_r - top_r) * t);
      final int base_g = (int) (top_g + (bottom_g - top_g) * t);
      final int base_b = (int) (top_b + (bottom_b - top_b) * t);
      for (int x = 0; x < width; x++) {
        // Speckle for texture: deterministic per-pixel noise.
        final int noise = (int) (random.nextDouble() * 25.0) - 12;
        final int r = clamp(base_r + noise);
        final int g = clamp(base_g + noise);
        final int b = clamp(base_b + noise);
        image.setRGB(x, y, (r << 16) | (g << 8) | b);
      }
    }
  }

  private static int clamp(int v) {
    return v < 0 ? 0 : (v > 255 ? 255 : v);
  }
}
