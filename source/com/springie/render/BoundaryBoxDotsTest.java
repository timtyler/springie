// This program has been placed into the public domain by its author.

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;

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
    // Draw until a dot lands visibly: depth-edge dots near the front
    // face can project off-screen, and the shared dot counter may start
    // anywhere, so the first plotted dot is not necessarily visible.
    final int before = dotPixels(img);
    int afterOne = before;
    for (int i = 0; i < 1000 && afterOne == before; i++) {
      BoundaryBoxDots.drawOneDot(g);
      afterOne = dotPixels(img);
    }
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
