// This code has been placed into the public domain by its author

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * The procedural background must be exactly half sky and half grass,
 * deterministic per size, and carry fluffy white clouds in the sky.
 */
public class ScenicBackgroundTest {
  private static int red(int rgb) {
    return (rgb >> 16) & 0xFF;
  }

  private static int green(int rgb) {
    return (rgb >> 8) & 0xFF;
  }

  private static int blue(int rgb) {
    return rgb & 0xFF;
  }

  @Test
  public void skyAboveAndGrassBelow() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    assertEquals(800, image.getWidth());
    assertEquals(600, image.getHeight());

    // Deep in the sky half: blue dominates.
    final int sky = image.getRGB(400, 150);
    assertTrue(blue(sky) > red(sky) && blue(sky) > green(sky),
        "sky pixel should be blue-dominant: "
            + Integer.toHexString(sky));

    // Deep in the grass half: green dominates.
    final int grass = image.getRGB(400, 450);
    assertTrue(green(grass) > red(grass) && green(grass) > blue(grass),
        "grass pixel should be green-dominant: "
            + Integer.toHexString(grass));
  }

  @Test
  public void horizonSplitsTheImageInHalf() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    // Just above the middle: sky. Just below: grass.
    final int above = image.getRGB(400, 299);
    final int below = image.getRGB(400, 300);
    assertTrue(blue(above) > green(above),
        "pixel above the horizon should be sky");
    assertTrue(green(below) > blue(below),
        "pixel below the horizon should be grass");
  }

  @Test
  public void skyHasFluffyWhiteClouds() {
    final BufferedImage image = ScenicBackground.imageFor(800, 600);
    int white = 0;
    for (int y = 0; y < 300; y++) {
      for (int x = 0; x < 800; x++) {
        final int rgb = image.getRGB(x, y);
        final int min = Math.min(red(rgb),
            Math.min(green(rgb), blue(rgb)));
        if (min > 225) {
          white++;
        }
      }
    }
    // The sky gradient alone never gets this bright; the white must
    // come from cloud puffs.
    assertTrue(white > 1000,
        "sky should hold fluffy white clouds, found " + white
            + " near-white pixels");
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
    // Horizon still at half height for the new size.
    final int above = image.getRGB(160, 99);
    final int below = image.getRGB(160, 100);
    assertTrue(blue(above) > green(above));
    assertTrue(green(below) > blue(below));
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
