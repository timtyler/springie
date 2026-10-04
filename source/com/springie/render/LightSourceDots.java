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

  /**
   * Currently dragged light (Tim, 2026-10-04): -1 = none, 0 = light one,
   * 1 = light two, 2 = light three, 3 = light four.
   */
  public static int dragging = -1;

  private LightSourceDots() {
    // Static only.
  }

  /**
   * Hit test (Tim, 2026-10-04): returns the light index (0-3) if the
   * screen point is within a dot, or -1.
   */
  public static int hitTest(final int sx, final int sy) {
    LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    if (RendererDelegator.red_light_pct > 0
        && near(sx, sy, LightSource.red_px, LightSource.red_py,
            LightSource.red_pz)) {
      return 0;
    }
    if (RendererDelegator.green_light_pct > 0
        && near(sx, sy, LightSource.green_px, LightSource.green_py,
            LightSource.green_pz)) {
      return 1;
    }
    if (RendererDelegator.blue_light_pct > 0
        && near(sx, sy, LightSource.blue_px, LightSource.blue_py,
            LightSource.blue_pz)) {
      return 2;
    }
    if (RendererDelegator.white_light_pct > 0
        && near(sx, sy, LightSource.white_px, LightSource.white_py,
            LightSource.white_pz)) {
      return 3;
    }
    return -1;
  }

  private static boolean near(final int sx, final int sy, final double wx,
      final double wy, final double wz) {
    final int ix = (int) wx;
    final int iy = (int) wy;
    final int iz = (int) wz;
    if (Coords.shift_constant_z + (iz >> Coords.shift_z) == 0) {
      return false;
    }
    int dx = Coords.getXCoords(ix, iz);
    int dy = Coords.getYCoords(iy, iz);
    // Clamp like drawOne.
    if (dx < RADIUS) {
      dx = RADIUS;
    } else if (dx > Coords.x_pixels - RADIUS) {
      dx = Coords.x_pixels - RADIUS;
    }
    if (dy < RADIUS) {
      dy = RADIUS;
    } else if (dy > Coords.y_pixels - RADIUS) {
      dy = Coords.y_pixels - RADIUS;
    }
    final int ddx = sx - dx;
    final int ddy = sy - dy;
    return ddx * ddx + ddy * ddy <= (RADIUS * 2) * (RADIUS * 2);
  }

  /**
   * Drag a light to a screen position (Tim, 2026-10-04): converts to
   * world X/Y at the light's Z, stores as custom position.
   */
  public static void dragTo(final int light, final int sx, final int sy) {
    final double wz;
    if (light == 0) {
      wz = LightSource.red_pz;
    } else if (light == 1) {
      wz = LightSource.green_pz;
    } else if (light == 2) {
      wz = LightSource.blue_pz;
    } else {
      wz = LightSource.white_pz;
    }
    // Inverse of Coords.getXCoords/getYCoords.
    final int iz = (int) wz;
    final double denom =
        (double) (Coords.shift_constant_z + (iz >> Coords.shift_z));
    final double wx = (sx - Coords.x_pixelso2) * denom
        - Coords.shift_constant_x + (Coords.x_pixelso2 << Coords.shift);
    final double wy = (sy - Coords.y_pixelso2) * denom
        - Coords.shift_constant_y + (Coords.y_pixelso2 << Coords.shift);
    if (light == 0) {
      LightSource.red_custom = true;
      LightSource.red_custom_x = wx;
      LightSource.red_custom_y = wy;
    } else if (light == 1) {
      LightSource.green_custom = true;
      LightSource.green_custom_x = wx;
      LightSource.green_custom_y = wy;
    } else if (light == 2) {
      LightSource.blue_custom = true;
      LightSource.blue_custom_x = wx;
      LightSource.blue_custom_y = wy;
    } else {
      LightSource.white_custom = true;
      LightSource.white_custom_x = wx;
      LightSource.white_custom_y = wy;
    }
    // The illumination changed: force a re-trace (Tim, 2026-10-04).
    LightSource.light_moved = true;
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
