// This program has been placed into the public domain by its author.

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;

class BoundaryBoxDotsTest {
  @AfterEach
  void resetFlag() {
    FrEnd.show_boundary_box = false;
  }

  private static int dotPixels(BufferedImage img) {
    int n = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (img.getRGB(x, y) == Color.gray.getRGB()) {
          n++;
        }
      }
    }
    return n;
  }

  @Test
  void offByDefaultDrawsNothing() {
    final BufferedImage img = new BufferedImage(64, 64,
        BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = img.createGraphics();
    FrEnd.show_boundary_box = false;
    BoundaryBoxDots.drawOneDot(g);
    g.dispose();
    assertEquals(0, dotPixels(img), "flag off must draw nothing");
  }

  @Test
  void oneDotPerCallAndItWraps() {
    final BufferedImage img = new BufferedImage(800, 600,
        BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = img.createGraphics();
    FrEnd.show_boundary_box = true;
    BoundaryBoxDots.drawOneDot(g);
    g.dispose();
    final int afterOne = dotPixels(img);
    assertTrue(afterOne > 0 && afterOne <= 4,
        "one call plots a single 2x2 dot, got " + afterOne);

    // Cycling well past the full outline must not throw or misbehave.
    final Graphics2D g2 = img.createGraphics();
    for (int i = 0; i < 2000; i++) {
      BoundaryBoxDots.drawOneDot(g2);
    }
    g2.dispose();
    assertTrue(dotPixels(img) > afterOne,
        "more frames must accumulate more dots");
  }
}
