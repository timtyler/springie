// This code has been placed into the public domain by its author

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The procedural background must be dark dusk sky above and dark grass
 * below, split exactly 50/50, deterministic per size, with sunlit
 * clouds in the sky and blade structure in the grass.
 */
public class ScenicBackgroundTest {
  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private static int red(int rgb) {
    return (rgb >> 16) & 0xFF;
  }

  private static int green(int rgb) {
    return (rgb >> 8) & 0xFF;
  }

  private static int blue(int rgb) {
    return rgb & 0xFF;
  }

  @BeforeEach
  public void setUp() {
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;
    Coords.shift_constant_x = 0;
    Coords.shift_constant_y = 0;
    Coords.shift_constant_z = 192;
  }

  @AfterEach
  public void tearDown() {
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
  }

  @Test
  public void skyAboveAndGrassBelow() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    assertEquals(800, image.getWidth());
    assertEquals(600, image.getHeight());

    // Statistical: clouds, stars and the ember band mean single pixels
    // prove nothing, but the sky half must read blue overall...
    int blue_dominant = 0;
    int sky_total = 0;
    for (int y = 0; y < 300; y += 2) {
      for (int x = 0; x < 800; x += 2) {
        final int rgb = image.getRGB(x, y);
        sky_total++;
        if (blue(rgb) > red(rgb) && blue(rgb) > green(rgb)) {
          blue_dominant++;
        }
      }
    }
    assertTrue((double) blue_dominant / sky_total > 0.55,
        "sky should be blue-dominant overall");

    // ...and the grass half must read green overall.
    int green_dominant = 0;
    int grass_total = 0;
    for (int y = 300; y < 600; y += 2) {
      for (int x = 0; x < 800; x += 2) {
        final int rgb = image.getRGB(x, y);
        grass_total++;
        if (green(rgb) > red(rgb) && green(rgb) > blue(rgb)) {
          green_dominant++;
        }
      }
    }
    assertTrue((double) green_dominant / grass_total > 0.9,
        "grass should be green-dominant overall");
  }

  @Test
  public void everythingIsDark() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    // Dusk: all but the ember band and the lit cloud undersides is
    // near-black. The old daylight version was bright almost
    // everywhere; this one must be overwhelmingly dark.
    int dark = 0;
    int total = 0;
    for (int y = 0; y < 600; y += 2) {
      for (int x = 0; x < 800; x += 2) {
        final int rgb = image.getRGB(x, y);
        final int mean =
            (red(rgb) + green(rgb) + blue(rgb)) / 3;
        if (mean < 90) {
          dark++;
        }
        total++;
      }
    }
    assertTrue((double) dark / total > 0.85,
        "dusk background should be dark overall");
  }

  @Test
  public void horizonHasEmberGlowAboveAndDarkGrassBelow() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    // Just above the middle: the ember band where the sun went down.
    final int above = image.getRGB(400, 299);
    assertTrue(red(above) > blue(above) && red(above) > 150,
        "pixel above the horizon should glow ember: "
            + Integer.toHexString(above));
    // Just below: dark grass.
    final int below = image.getRGB(400, 300);
    assertTrue(green(below) >= blue(below)
        && red(below) + green(below) + blue(below) < 200,
        "pixel below the horizon should be dark grass: "
            + Integer.toHexString(below));
  }

  @Test
  public void skyHasSunlitClouds() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    int warm = 0;
    for (int y = 0; y < 300; y++) {
      for (int x = 0; x < 800; x++) {
        final int rgb = image.getRGB(x, y);
        // Warm-lit cloud undersides: strong red, clearly warmer than
        // the indigo sky. The gradient alone never gets this warm.
        if (red(rgb) > 150 && red(rgb) > blue(rgb) + 40
            && green(rgb) > 80) {
          warm++;
        }
      }
    }
    assertTrue(warm > 5000,
        "sky should hold sunlit clouds, found " + warm
            + " warm-lit pixels");
  }

  @Test
  public void grassHasBladeStructure() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    // Blade tips read as pixels clearly brighter than the grass just
    // beneath them: vertical structure, not flat noise.
    int tips = 0;
    for (int y = 300; y < 596; y++) {
      for (int x = 0; x < 800; x += 2) {
        final int rgb = image.getRGB(x, y);
        final int under = image.getRGB(x, y + 3);
        final int lum = (red(rgb) + green(rgb) + blue(rgb)) / 3;
        final int lum_under =
            (red(under) + green(under) + blue(under)) / 3;
        if (lum - lum_under > 25) {
          tips++;
        }
      }
    }
    assertTrue(tips > 500,
        "grass should show blade structure, found " + tips
            + " blade-tip pixels");
  }

  @Test
  public void panSlidesTheBackgroundLikeAFixedBackdrop() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    // Pan the view right: geometry moves right on screen, so a
    // world-fixed backdrop must sample left of the screen pixel.
    Coords.shift_constant_x = 1920;
    // Expected lookups computed exactly the way sampleWithPan does.
    final int grass_tx = (int) Math.round(
        400 - ScenicBackground.GRASS_PARALLAX * 1920 / 192);
    final int sky_tx = (int) Math.round(
        400 - ScenicBackground.SKY_PARALLAX * 1920 / 192);
    assertTrue(400 - grass_tx > 400 - sky_tx,
        "near grass must move faster than the distant sky");
    assertTrue(grass_tx < 400 && sky_tx < 400,
        "panning right must sample left of the pixel, so the backdrop "
            + "moves right with the geometry");
    assertEquals(ScenicBackground.sampleClamped(image, grass_tx, 450),
        ScenicBackground.sampleWithPan(image, 400, 450, false),
        "grass must slide with the pan");
    assertEquals(ScenicBackground.sampleClamped(image, sky_tx, 100),
        ScenicBackground.sampleWithPan(image, 400, 100, true),
        "sky must slide with the pan, but less");
  }

  @Test
  public void sameSizeIsDeterministicAndCached() {
    final BufferedImage first = ScenicBackground.imageFor(640, 480);
    final BufferedImage second = ScenicBackground.imageFor(640, 480);
    assertSame(first, second, "same size must hit the cache");
  }

  @Test
  public void imageFollowsTheRequestedSize() {
    final BufferedImage image = ScenicBackground.imageFor(320, 200);
    assertEquals(320, image.getWidth());
    assertEquals(200, image.getHeight());
    // Horizon still at half height for the new size: ember above,
    // dark grass below.
    final int above = image.getRGB(160, 99);
    final int below = image.getRGB(160, 100);
    assertTrue(red(above) > blue(above));
    assertTrue(green(below) >= blue(below));
    // Restore the common size for other tests sharing the cache.
    ScenicBackground.imageFor(800, 600);
  }

  @Test
  public void sampleClampedStaysOnTheImage() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    assertEquals(image.getRGB(0, 0),
        ScenicBackground.sampleClamped(image, -5, -5));
    assertEquals(image.getRGB(799, 599),
        ScenicBackground.sampleClamped(image, 9000, 9000));
    assertEquals(image.getRGB(400, 300),
        ScenicBackground.sampleClamped(image, 400, 300));
  }
}
