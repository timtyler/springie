// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * The pixellated blit path nearest-neighbour upsamples the coarse tile:
 * each coarse pixel is replicated across a px-by-px block of the full
 * bin-size tile. Exact replication, no blending.
 */
public class RendererBinManagerPixellationTest {

  private static BufferedImage image(int w, int h, int[] pixels) {
    final BufferedImage img =
        new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, w, h, pixels, 0, w);
    return img;
  }

  @Test
  public void upsample2x2ReplicatesEachPixel() {
    final BufferedImage src = image(2, 2, new int[] {
        0xFFFF0000, 0xFF00FF00,
        0xFF0000FF, 0xFFFFFFFF });
    final BufferedImage dst =
        new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.upsampleTile(src, dst, 2);
    final int[] expected = {
        0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00,
        0xFFFF0000, 0xFFFF0000, 0xFF00FF00, 0xFF00FF00,
        0xFF0000FF, 0xFF0000FF, 0xFFFFFFFF, 0xFFFFFFFF,
        0xFF0000FF, 0xFF0000FF, 0xFFFFFFFF, 0xFFFFFFFF };
    for (int y = 0; y < 4; y++) {
      for (int x = 0; x < 4; x++) {
        assertEquals(expected[y * 4 + x], dst.getRGB(x, y),
            "pixel differed at " + x + "," + y);
      }
    }
  }

  @Test
  public void upsample1x1IsIdentity() {
    final BufferedImage src = image(2, 3, new int[] {
        0xFFFF0000, 0xFF00FF00,
        0xFF0000FF, 0xFFFFFFFF,
        0xFF123456, 0xFF654321 });
    final BufferedImage dst =
        new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.upsampleTile(src, dst, 1);
    for (int y = 0; y < 3; y++) {
      for (int x = 0; x < 2; x++) {
        assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
      }
    }
  }

  @Test
  public void upsampleClampsAtNonMultipleEdges() {
    // 2x2 coarse, 5x5 destination, px = 2: the last row/column have no
    // full block, so they clamp to the last coarse row/column.
    final BufferedImage src = image(2, 2, new int[] {
        0xFFFF0000, 0xFF00FF00,
        0xFF0000FF, 0xFFFFFFFF });
    final BufferedImage dst =
        new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.upsampleTile(src, dst, 2);
    assertEquals(0xFFFF0000, dst.getRGB(0, 0));
    assertEquals(0xFF00FF00, dst.getRGB(2, 1));
    assertEquals(0xFF00FF00, dst.getRGB(4, 0),
        "last column must clamp to the last coarse column");
    assertEquals(0xFFFFFFFF, dst.getRGB(4, 4),
        "last row/column must clamp to the last coarse pixel");
    assertEquals(0xFF0000FF, dst.getRGB(0, 4),
        "last row must clamp to the last coarse row");
  }

  @Test
  public void upsample4x4FillsFromOnePixel() {
    final BufferedImage src =
        image(1, 1, new int[] { 0xFFABCDEF });
    final BufferedImage dst =
        new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.upsampleTile(src, dst, 4);
    for (int y = 0; y < 4; y++) {
      for (int x = 0; x < 4; x++) {
        assertEquals(0xFFABCDEF, dst.getRGB(x, y));
      }
    }
  }

  @Test
  public void upsamplePreservesOpaqueAlpha() {
    final BufferedImage src = image(2, 2, new int[] {
        0xFFFF0000, 0xFF00FF00,
        0xFF0000FF, 0xFFFFFFFF });
    final BufferedImage dst =
        new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.upsampleTile(src, dst, 2);
    assertEquals(0xFF, (dst.getRGB(3, 3) >> 24) & 0xFF);
  }
}
