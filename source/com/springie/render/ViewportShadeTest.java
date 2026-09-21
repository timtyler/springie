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

/**
 * The area outside the viewpoint (the projected boundary box) is shaded
 * grey on every paint -- and not at all when the box covers the canvas.
 */
class ViewportShadeTest {
  private int saved_x_pixels;
  private int saved_y_pixels;
  private int saved_shift_x;
  private int saved_shift_y;
  private int saved_shift_z;

  @BeforeEach
  void saveCoords() {
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_shift_x = Coords.shift_constant_x;
    this.saved_shift_y = Coords.shift_constant_y;
    this.saved_shift_z = Coords.shift_constant_z;
    Coords.x_pixels = 400;
    Coords.y_pixels = 300;
    Coords.x_pixelso2 = 200;
    Coords.y_pixelso2 = 150;
  }

  @AfterEach
  void restoreCoords() {
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.shift_constant_x = this.saved_shift_x;
    Coords.shift_constant_y = this.saved_shift_y;
    Coords.shift_constant_z = this.saved_shift_z;
    Coords.x_pixelso2 = Coords.x_pixels >> 1;
    Coords.y_pixelso2 = Coords.y_pixels >> 1;
  }

  private static BufferedImage shadeWith(int shift_x) {
    Coords.shift_constant_x = shift_x;
    Coords.shift_constant_y = 0;
    final BufferedImage img =
        new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = img.createGraphics();
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, 400, 300);
    ViewportShade.shadeOutsideBox(g);
    g.dispose();
    return img;
  }

  @Test
  void centredViewHasNoShading() {
    final BufferedImage img = shadeWith(0);
    // The box covers the canvas: every pixel stays black.
    assertEquals(Color.BLACK.getRGB(), img.getRGB(0, 0));
    assertEquals(Color.BLACK.getRGB(), img.getRGB(399, 299));
    assertEquals(Color.BLACK.getRGB(), img.getRGB(200, 150));
  }

  @Test
  void pannedViewShadesTheOutsideStrip() {
    // Pan right: the box moves right, the left strip is outside it.
    final BufferedImage img = shadeWith(40 << 11);
    assertEquals(Color.GRAY.getRGB(), img.getRGB(0, 150));
    // The box interior (shifted right) is untouched.
    assertEquals(Color.BLACK.getRGB(), img.getRGB(399, 150));
  }

  @Test
  void shadeIsCachedAcrossCalls() {
    // Two calls with the same viewpoint: the second must paint the same
    // pixels (the rect is cached, not recomputed differently).
    final BufferedImage a = shadeWith(40 << 11);
    final BufferedImage b = shadeWith(40 << 11);
    assertTrue(a.getRGB(0, 150) == b.getRGB(0, 150)
        && a.getRGB(0, 150) == Color.GRAY.getRGB());
  }
}
