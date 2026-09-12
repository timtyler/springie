// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * The anti-aliased blit path box-filters each aa-by-aa block of the
 * supersampled tile into one pixel. Exact integer averaging.
 */
public class RendererBinManagerAntialiasTest {

  private static BufferedImage image(int size, int[] pixels) {
    final BufferedImage img =
        new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, size, size, pixels, 0, size);
    return img;
  }

  @Test
  public void downsample2x2AveragesQuadrants() {
    final BufferedImage src = image(2, new int[] {
        0xFFFF0000, 0xFF00FF00,
        0xFF0000FF, 0xFFFFFFFF });
    final BufferedImage dst =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.downsampleTile(src, dst, 2);
    // (255+0+0+255)/4 = 127 per channel.
    assertEquals(0xFF7F7F7F, dst.getRGB(0, 0));
  }

  @Test
  public void downsample3x3AveragesNine() {
    final int[] pixels = new int[9];
    for (int i = 0; i < 9; i++) {
      pixels[i] = 0xFFFFFFFF;
    }
    pixels[4] = 0xFF000000;
    final BufferedImage src = image(3, pixels);
    final BufferedImage dst =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.downsampleTile(src, dst, 3);
    // 8*255/9 = 226 (integer division truncates 226.67).
    assertEquals(0xFFE2E2E2, dst.getRGB(0, 0));
  }

  @Test
  public void downsampleOfAFlatTileIsIdentity() {
    final int[] pixels = new int[16];
    for (int i = 0; i < 16; i++) {
      pixels[i] = 0xFF123456;
    }
    final BufferedImage src = image(4, pixels);
    final BufferedImage dst =
        new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.downsampleTile(src, dst, 2);
    for (int y = 0; y < 2; y++) {
      for (int x = 0; x < 2; x++) {
        assertEquals(0xFF123456, dst.getRGB(x, y),
            "flat tile drifted at " + x + "," + y);
      }
    }
  }

  @Test
  public void downsamplePreservesOpaqueAlpha() {
    final BufferedImage src = image(2, new int[] {
        0xFFFF0000, 0xFF00FF00,
        0xFF0000FF, 0xFFFFFFFF });
    final BufferedImage dst =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.downsampleTile(src, dst, 2);
    assertEquals(0xFF, (dst.getRGB(0, 0) >> 24) & 0xFF);
  }
}
