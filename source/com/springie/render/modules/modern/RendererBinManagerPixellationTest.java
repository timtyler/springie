// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * The pixellated blit path scales the coarse (1/px resolution) tile up to
 * the full bin with a nearest-neighbour blit: each coarse pixel becomes
 * one flat px-by-px block. The tile scrub is snapped out to whole coarse
 * blocks, because the 1/px tile transform would otherwise leave the edge
 * coarse pixels unscrubbed -- stale content streaking right and down.
 */
public class RendererBinManagerPixellationTest {

  private static BufferedImage image(int w, int h, int[] pixels) {
    final BufferedImage img =
        new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, w, h, pixels, 0, w);
    return img;
  }

  @Test
  public void paintPixellatedReplicatesEachPixel() {
    final BufferedImage coarse = image(2, 2, new int[] {
        0xFFFF0000, 0xFF00FF00,
        0xFF0000FF, 0xFFFFFFFF });
    final BufferedImage dst =
        new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    RendererBinManager.paintPixellated(dst.getGraphics(), coarse, 0, 0, 4,
        4);
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
  public void paintPixellatedRestoresTheInterpolationHint() {
    final BufferedImage coarse = image(1, 1, new int[] { 0xFF123456 });
    final BufferedImage dst =
        new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = (Graphics2D) dst.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    RendererBinManager.paintPixellated(g, coarse, 0, 0, 2, 2);
    assertEquals(RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        g.getRenderingHint(RenderingHints.KEY_INTERPOLATION));
    g.dispose();
  }

  private static RectangleInt snap(int min_x, int min_y, int max_x,
      int max_y, int bin_min_x, int bin_min_y, int px) {
    final RectangleInt out = new RectangleInt(0, 0, 0, 0);
    RendererBinManager.snapScrubToCoarseBlocks(
        new RectangleInt(min_x, min_y, max_x, max_y), bin_min_x, bin_min_y,
        px, out);
    return out;
  }

  @Test
  public void scrubSnapRoundsMaxUpAndMinDown() {
    // px = 2, bin at screen origin: max_x = 7 is not on a block
    // boundary, so it snaps up to 8; min_x = 1 snaps down to 0.
    final RectangleInt snapped = snap(1, 1, 7, 7, 0, 0, 2);
    assertEquals(0, snapped.min_x);
    assertEquals(0, snapped.min_y);
    assertEquals(8, snapped.max_x);
    assertEquals(8, snapped.max_y);
  }

  @Test
  public void scrubSnapLeavesAlignedEdgesAlone() {
    final RectangleInt snapped = snap(0, 0, 8, 8, 0, 0, 2);
    assertEquals(0, snapped.min_x);
    assertEquals(0, snapped.min_y);
    assertEquals(8, snapped.max_x);
    assertEquals(8, snapped.max_y);
  }

  @Test
  public void scrubSnapAlignsToTheBinOrigin() {
    // Bins start at multiples of the divisor (340), which is not a
    // multiple of 3: the blocks must align to the bin origin, not the
    // screen origin. Screen x 347 sits in block [346, 349).
    final RectangleInt snapped = snap(341, 0, 347, 8, 340, 0, 3);
    assertEquals(340, snapped.min_x);
    assertEquals(349, snapped.max_x);
  }

  @Test
  public void snappedScrubCoversTheFractionalEdgePixel() {
    // Regression test for the right/bottom streaks: with px = 2 the
    // scrub runs through a 1/2 scale transform, and a union max_x = 7
    // leaves coarse pixel 3 (screen x 6-7) unscrubbed -- stale content
    // the upsampler streaks. After snapping, the edge pixel is covered.
    final RectangleInt snapped = snap(0, 0, 7, 8, 0, 0, 2);
    final BufferedImage tile =
        new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = (Graphics2D) tile.getGraphics();
    g.scale(0.5, 0.5);
    g.setClip(snapped.min_x, snapped.min_y,
        snapped.max_x - snapped.min_x, snapped.max_y - snapped.min_y);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, 8, 8);
    g.dispose();
    for (int y = 0; y < 4; y++) {
      for (int x = 0; x < 4; x++) {
        assertEquals(0xFFFFFFFF, tile.getRGB(x, y),
            "coarse pixel not scrubbed at " + x + "," + y);
      }
    }
  }
}
