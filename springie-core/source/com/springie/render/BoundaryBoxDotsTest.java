// This program has been placed into the public domain by its author.

package com.springie.render;

import com.springie.FrEnd;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryBoxDotsTest {
  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_x_pixelso2;

  private int saved_y_pixelso2;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private boolean saved_show_boundary_box;

  @BeforeEach
  void setUp() {
    // Pin the box to the test image: with a centred viewport every
    // drawable dot projects inside it, whatever the ambient Coords or
    // the shared dot counter (advanced by the app's animation thread in
    // other tests' JVMs) happen to be.
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_x_pixelso2 = Coords.x_pixelso2;
    this.saved_y_pixelso2 = Coords.y_pixelso2;
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;
    Coords.x_pixels = 800;
    Coords.y_pixels = 600;
    Coords.x_pixelso2 = 400;
    Coords.y_pixelso2 = 300;
    Coords.shift_constant_x = 0;
    Coords.shift_constant_y = 0;
    Coords.shift_constant_z = 192;
    this.saved_show_boundary_box = FrEnd.show_boundary_box;
  }

  @AfterEach
  void resetFlag() {
    FrEnd.show_boundary_box = this.saved_show_boundary_box;
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = this.saved_x_pixelso2;
    Coords.y_pixelso2 = this.saved_y_pixelso2;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
  }

  private static int dotPixels(final BufferedImage img) {
    // Bulk getRGB: per-pixel getRGB(x,y) is a synchronized method call,
    // ~10x slower than a single bulk read into an array. The test loops
    // calling this after each drawOneDot, so the speedup compounds.
    final int w = img.getWidth();
    final int h = img.getHeight();
    final int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
    final int gray = Color.gray.getRGB();
    int n = 0;
    for (int p : pixels) {
      if (p == gray) {
        n++;
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
    // Reset the shared dot counter: dot 0 is on a visible edge with the
    // pinned test Coords, so one draw plots one visible dot. No loop.
    BoundaryBoxDots.resetForTest();
    final int before = dotPixels(img);
    BoundaryBoxDots.drawOneDot(g);
    final int afterOne = dotPixels(img);
    g.dispose();
    assertTrue(afterOne - before > 0 && afterOne - before <= 4,
        "one call plots a single 2x2 dot, got " + (afterOne - before));

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
