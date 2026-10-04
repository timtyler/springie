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
   * Draws all four lights at their true 3D positions (Tim, 2026-10-04):
   * a visual representation of the light sources used for shading.
   * Positions are refreshed every draw from the current viewport size,
   * then projected via Coords (matches the raytracer's pinhole camera).
   * Only draws 1 frame in 16 to keep it cheap.
   */
  public static void draw(final Graphics g) {
    frame_count = (frame_count + 1) % FRAME_SKIP;
    if (frame_count != 0) {
      return;
    }
    // Refresh from the live viewport size (Tim, 2026-10-04): stale
    // zero-size positions collapsed all dots to one point.
    LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    // Skip lights at 0% intensity (Tim, 2026-10-03).
    // Dots use the configured light colors (Tim, 2026-10-04).
    if (RendererDelegator.red_light_pct > 0) {
      drawOne(g, LightSource.red_px, LightSource.red_py, LightSource.red_pz,
          new Color(RendererDelegator.red_light_colour));
    }
    if (RendererDelegator.green_light_pct > 0) {
      drawOne(g, LightSource.green_px, LightSource.green_py,
          LightSource.green_pz,
          new Color(RendererDelegator.green_light_colour));
    }
    if (RendererDelegator.blue_light_pct > 0) {
      drawOne(g, LightSource.blue_px, LightSource.blue_py,
          LightSource.blue_pz,
          new Color(RendererDelegator.blue_light_colour));
    }
    if (RendererDelegator.white_light_pct > 0) {
      drawOne(g, LightSource.white_px, LightSource.white_py,
          LightSource.white_pz,
          new Color(RendererDelegator.white_light_colour));
    }
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
