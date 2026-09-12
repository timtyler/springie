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
 * A procedural background texture: a dusk sky above, dark grass below,
 * split exactly 50/50. The sun has just gone down, so the sky is deep
 * indigo with an ember-orange band hugging the horizon, a few early
 * stars overhead, and many clouds lit warm from below. The grass is
 * near-black green drawn blade by blade.
 *
 * <p>Generated deterministically from the window size (fixed seed), so
 * the same size always yields the same picture and nothing shimmers
 * between frames. The image is cached and only regenerated when the
 * requested size changes, so per-frame use is a cache hit.
 *
 * <p>The ray-traced renderer samples the texture through
 * {@link #sampleWithPan}, which shifts the lookup by the camera pan so
 * the background behaves like a world-fixed backdrop: panning the view
 * left or right visibly slides the grass blades. The sky uses a small
 * parallax rate (it is distant) and the grass a near-full one. The
 * tiled renderer draws the texture screen-locked.
 */
public final class ScenicBackground {
  private static final long SEED = 0x5EED5EEDL;

  /**
   * Parallax rate for the sky: distant, so it barely moves with a pan.
   */
  public static final double SKY_PARALLAX = 0.15;

  /**
   * Parallax rate for the grass: near, so it moves almost with the
   * model plane when the view is panned.
   */
  public static final double GRASS_PARALLAX = 0.85;

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

  /**
   * Samples the texture for a screen pixel, shifting the lookup by the
   * camera pan so the background reads as fixed in the world: when the
   * view pans, the grass blades slide across the screen. The shift is
   * zoom-aware, matching how far geometry at the model plane moves for
   * the same pan, scaled by the parallax rate ({@link #SKY_PARALLAX}
   * for the sky, {@link #GRASS_PARALLAX} for the grass).
   */
  public static int sampleWithPan(BufferedImage image, int sx, int sy,
      boolean sky) {
    final double zoom = Coords.shift_constant_z;
    if (zoom <= 0) {
      return sampleClamped(image, sx, sy);
    }
    final double rate = sky ? SKY_PARALLAX : GRASS_PARALLAX;
    final int tx =
        (int) Math.round(sx - rate * Coords.shift_constant_x / zoom);
    final int ty =
        (int) Math.round(sy - rate * Coords.shift_constant_y / zoom);
    return sampleClamped(image, tx, ty);
  }

  private static BufferedImage generate(int width, int height) {
    final BufferedImage image = new BufferedImage(width, height,
        BufferedImage.TYPE_INT_RGB);
    final Random random = new Random(SEED ^ (width * 31L + height));
    paintSky(image, width, height, random);
    paintStars(image, width, height, random);
    paintClouds(image, width, height, random);
    paintGrass(image, width, height, random);
    return image;
  }

  // Sky gradient stops: thousandths of the way down the sky half, then
  // r, g, b. Near-black indigo at the zenith, ember orange at the
  // horizon where the sun just set.
  private static final int[][] SKY_STOPS = {
      { 0, 6, 9, 22 },
      { 450, 16, 22, 48 },
      { 700, 38, 32, 66 },
      { 860, 96, 52, 74 },
      { 940, 196, 96, 52 },
      { 1000, 244, 164, 88 },
  };

  private static int skyAt(double t) {
    final int tt = (int) (t * 1000.0);
    for (int s = 0; s < SKY_STOPS.length - 1; s++) {
      final int[] lo = SKY_STOPS[s];
      final int[] hi = SKY_STOPS[s + 1];
      if (tt <= hi[0]) {
        final double f = (double) (tt - lo[0])
            / Math.max(1, hi[0] - lo[0]);
        final int r = (int) (lo[1] + (hi[1] - lo[1]) * f);
        final int g = (int) (lo[2] + (hi[2] - lo[2]) * f);
        final int b = (int) (lo[3] + (hi[3] - lo[3]) * f);
        return (r << 16) | (g << 8) | b;
      }
    }
    final int[] last = SKY_STOPS[SKY_STOPS.length - 1];
    return (last[1] << 16) | (last[2] << 8) | last[3];
  }

  private static void paintSky(BufferedImage image, int width, int height,
      Random random) {
    final int horizon = height / 2;
    for (int y = 0; y < horizon; y++) {
      final double t = (double) y / Math.max(1, horizon - 1);
      final int rgb = skyAt(t);
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, rgb);
      }
    }
  }

  /**
   * A few faint early stars in the upper sky, where it is already dark.
   */
  private static void paintStars(BufferedImage image, int width, int height,
      Random random) {
    final int horizon = height / 2;
    final int stars = Math.min(160, 40 + width * height / 12000);
    for (int s = 0; s < stars; s++) {
      final int x = random.nextInt(width);
      // Only where the sky is dark enough to show them.
      final int y = (int) (random.nextDouble() * horizon * 0.42);
      final int brightness = 90 + random.nextInt(110);
      // Slight colour-temperature variation, warm to cool white.
      final int r = brightness;
      final int g = (int) (brightness * (0.92 + random.nextDouble() * 0.08));
      final int b = (int) (brightness * (0.95 + random.nextDouble() * 0.05));
      final int over = image.getRGB(x, y);
      image.setRGB(x, y, blend(over, (r << 16) | (g << 8) | b, 0.75));
    }
  }

  private static int blend(int base, int over, double alpha) {
    final int br = (base >> 16) & 0xFF;
    final int bg = (base >> 8) & 0xFF;
    final int bb = base & 0xFF;
    final int or = (over >> 16) & 0xFF;
    final int og = (over >> 8) & 0xFF;
    final int ob = over & 0xFF;
    final int r = (int) (br + (or - br) * alpha);
    final int g = (int) (bg + (og - bg) * alpha);
    final int b = (int) (bb + (ob - bb) * alpha);
    return (r << 16) | (g << 8) | b;
  }

  /**
   * Dusk clouds: dark slate bodies with warm sunset-lit undersides.
   * The sun is below the horizon, so the light comes from underneath:
   * each puff is a dark disc, a warm glow offset downwards, and a
   * bright rim along the very bottom edge. Lower clouds catch more
   * light.
   */
  private static void paintClouds(BufferedImage image, int width, int height,
      Random random) {
    final int horizon = height / 2;
    final Graphics2D g = image.createGraphics();
    try {
      final int clouds = Math.min(26, 8 + width * height / 110000);
      for (int c = 0; c < clouds; c++) {
        final double cloud_w = width * (0.10 + random.nextDouble() * 0.12);
        final double cloud_h = cloud_w * (0.32 + random.nextDouble() * 0.18);
        final double cx = random.nextDouble() * width;
        // Clouds may drift down over the horizon glow; their lit rims
        // read as silhouettes against it.
        final double cy = height * 0.04 + random.nextDouble()
            * (horizon - cloud_h * 0.7 - height * 0.04);
        // Lower clouds catch more of the sunset.
        final double warmth = 1.0 - Math.max(0.0,
            Math.min(1.0, cy / Math.max(1, horizon)));
        final int glow_alpha = (int) (90 + 60 * warmth);
        final int rim_alpha = (int) (60 + 50 * warmth);
        final int puffs = 14 + random.nextInt(9);
        for (int p = 0; p < puffs; p++) {
          // Centred triangular distribution: puffs bunch in the middle
          // and merge into one mass instead of separate balls.
          final double px = cx + (triangular(random) * cloud_w);
          final double py = cy + (triangular(random) * cloud_h);
          final double pr = cloud_w * (0.13 + random.nextDouble() * 0.13);
          // Lit from below: the warm glow goes down first and the dark
          // body sits on top of it, so only a lit crescent peeks out
          // underneath; then a thin bright rim along the bottom edge.
          drawPuff(g, px, py + pr * 0.35, pr * 0.95,
              new Color(225, 120, 65, glow_alpha));
          drawPuff(g, px, py, pr, new Color(26, 30, 50, 230));
          drawPuff(g, px, py + pr * 0.75, pr * 0.35,
              new Color(255, 170, 105, rim_alpha));
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

  // Grass gradient stops: thousandths of the way down the grass half,
  // then r, g, b. Near-black green, with a faint warm kiss from the
  // sunset just below the horizon.
  private static final int[][] GRASS_STOPS = {
      { 0, 52, 58, 34 },
      { 120, 30, 42, 24 },
      { 500, 18, 30, 16 },
      { 1000, 10, 16, 10 },
  };

  private static int grassAt(double t) {
    final int tt = (int) (t * 1000.0);
    for (int s = 0; s < GRASS_STOPS.length - 1; s++) {
      final int[] lo = GRASS_STOPS[s];
      final int[] hi = GRASS_STOPS[s + 1];
      if (tt <= hi[0]) {
        final double f = (double) (tt - lo[0])
            / Math.max(1, hi[0] - lo[0]);
        final int r = (int) (lo[1] + (hi[1] - lo[1]) * f);
        final int g = (int) (lo[2] + (hi[2] - lo[2]) * f);
        final int b = (int) (lo[3] + (hi[3] - lo[3]) * f);
        return (r << 16) | (g << 8) | b;
      }
    }
    final int[] last = GRASS_STOPS[GRASS_STOPS.length - 1];
    return (last[1] << 16) | (last[2] << 8) | last[3];
  }

  /**
   * Dark grass with real blade structure: thousands of short tapered
   * strokes, taller towards the bottom of the frame, a few catching
   * the last light. The blades are what the eye tracks when the view
   * pans left or right.
   */
  private static void paintGrass(BufferedImage image, int width, int height,
      Random random) {
    final int horizon = height / 2;
    for (int y = horizon; y < height; y++) {
      final double t = (double) (y - horizon)
          / Math.max(1, height - 1 - horizon);
      final int rgb = grassAt(t);
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, rgb);
      }
    }
    final Graphics2D g = image.createGraphics();
    try {
      final int blades = Math.max(200, width * height / 85);
      for (int i = 0; i < blades; i++) {
        final int x = random.nextInt(width);
        final int base_y = horizon + random.nextInt(height - horizon);
        // Depth cue: blades grow taller towards the viewer.
        final double depth = (double) (base_y - horizon)
            / Math.max(1, height - 1 - horizon);
        final int blade_h = 2 + (int) (random.nextDouble() * (3 + 6 * depth));
        final int lean =
            (int) ((random.nextDouble() - 0.5) * (2 + 3 * depth));
        // One blade in eight catches the last of the light, more often
        // near the horizon where the sunset still reaches.
        final boolean lit = random.nextDouble() < 0.08 + 0.12 * (1 - depth);
        final int r;
        final int gg;
        final int b;
        if (lit) {
          r = 88 + random.nextInt(30);
          gg = 96 + random.nextInt(30);
          b = 48 + random.nextInt(18);
        } else {
          r = 14 + random.nextInt(22);
          gg = 30 + random.nextInt(38);
          b = 12 + random.nextInt(20);
        }
        g.setColor(new Color(r, gg, b));
        g.drawLine(x, base_y, x + lean, base_y - blade_h);
      }
    } finally {
      g.dispose();
    }
  }
}
