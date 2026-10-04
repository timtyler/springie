// This program has been placed into the public domain by its author.

package com.springie.render;

import com.springie.render.modules.modern.LightSource;
import java.awt.Color;
import java.awt.Graphics;

/**
 * Draws the four point light sources (red, green, blue, white) as small
 * colored circles. (Tim, 2026-10-03)
 *
 * <p>Keeps it cheap: one fillOval per light per frame, no depth test.
 * Positions are clamped inside the viewport so the lights stay visible.
 */
public final class LightSourceDots {
  /** Radius of the light circles in pixels. */
  private static final int RADIUS = 6;

  private LightSourceDots() {
    // Static only.
  }

  /**
   * Draws all four lights. Positions are static (updated by the renderers
   * via LightSource.updateForViewport); just draw them.
   */
  public static void draw(final Graphics g) {
    drawOne(g, LightSource.red_px, LightSource.red_py, LightSource.red_pz,
        Color.red);
    drawOne(g, LightSource.green_px, LightSource.green_py, LightSource.green_pz,
        Color.green);
    drawOne(g, LightSource.blue_px, LightSource.blue_py, LightSource.blue_pz,
        Color.blue);
    drawOne(g, LightSource.white_px, LightSource.white_py, LightSource.white_pz,
        Color.white);
  }

  private static void drawOne(final Graphics g, final double wx,
      final double wy, final double wz, final Color color) {
    final int ix = (int) wx;
    final int iy = (int) wy;
    final int iz = (int) wz;
    // Skip if on the eye plane (projection divides by zero).
    if (Coords.shift_constant_z + (iz >> Coords.shift_z) == 0) {
      return;
    }
    int sx = Coords.getXCoords(ix, iz);
    int sy = Coords.getYCoords(iy, iz);
    // Clamp inside the viewport (Tim, 2026-10-03).
    if (sx < RADIUS) {
      sx = RADIUS;
    } else if (sx > Coords.x_pixels - RADIUS) {
      sx = Coords.x_pixels - RADIUS;
    }
    if (sy < RADIUS) {
      sy = RADIUS;
    } else if (sy > Coords.y_pixels - RADIUS) {
      sy = Coords.y_pixels - RADIUS;
    }
    g.setColor(color);
    g.fillOval(sx - RADIUS, sy - RADIUS, RADIUS * 2, RADIUS * 2);
    g.setColor(Color.black);
    g.drawOval(sx - RADIUS, sy - RADIUS, RADIUS * 2, RADIUS * 2);
  }
}
