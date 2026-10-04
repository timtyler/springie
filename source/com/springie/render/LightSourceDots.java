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

  /** Draw every Nth frame (Tim, 2026-10-03). */
  private static final int FRAME_SKIP = 16;

  private static int frame_count = 0;

  private LightSourceDots() {
    // Static only.
  }

  /**
   * Draws all four lights. Positions are static (updated by the renderers
   * via LightSource.updateForViewport); just draw them.
   * Only draws 1 frame in 16 to keep it cheap.
   */
  public static void draw(final Graphics g) {
    frame_count = (frame_count + 1) % FRAME_SKIP;
    if (frame_count != 0) {
      return;
    }
    // Ensure positions are initialized (Tim, 2026-10-03): the renderers
    // update them, but the first paint may happen before any render.
    if (LightSource.red_px == 0.0 && LightSource.red_py == 0.0) {
      LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    }
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
