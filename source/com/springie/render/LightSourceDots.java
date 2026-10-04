// This program has been placed into the public domain by its author.

package com.springie.render;

import com.springie.render.modules.modern.Light;
import com.springie.render.modules.modern.LightSource;
import java.awt.Color;
import java.awt.Graphics;

/**
 * Draws the point light sources as small colored circles. (Tim, 2026-10-03)
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
   * Currently dragged light (Tim, 2026-10-04): -1 = none, otherwise
   * the index into LightSource.lights.
   */
  public static int dragging = -1;

  private LightSourceDots() {
    // Static only.
  }

  /**
   * Hit test (Tim, 2026-10-04): returns the light index if the
   * screen point is within a dot, or -1.
   */
  public static int hitTest(final int sx, final int sy) {
    LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    synchronized (LightSource.class) {
      for (int i = 0; i < LightSource.lights.size(); i++) {
        final Light light = LightSource.lights.get(i);
        if (light.intensity_pct > 0
            && near(sx, sy, light.px, light.py, light.pz)) {
          return i;
        }
      }
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
   * percentages of the viewport half-size.
   */
  public static void dragTo(final int light, final int sx, final int sy) {
    // Store as percentages of the viewport half-size (Tim, 2026-10-04):
    // survives window resizes, and what we persist.
    final int hw = Coords.x_pixelso2 == 0 ? 400 : Coords.x_pixelso2;
    final int hh = Coords.y_pixelso2 == 0 ? 300 : Coords.y_pixelso2;
    double x_pct = (double) (sx - Coords.x_pixelso2) / (double) hw * 100.0;
    double y_pct = (double) (sy - Coords.y_pixelso2) / (double) hh * 100.0;
    // Clamp to a sane range (Tim, 2026-10-04): prevents the light from
    // going so far off-screen that the projection breaks and the dot
    // disappears.
    if (x_pct < -200.0) {
      x_pct = -200.0;
    } else if (x_pct > 200.0) {
      x_pct = 200.0;
    }
    if (y_pct < -200.0) {
      y_pct = -200.0;
    } else if (y_pct > 200.0) {
      y_pct = 200.0;
    }
    synchronized (LightSource.class) {
      if (light >= 0 && light < LightSource.lights.size()) {
        final Light l = LightSource.lights.get(light);
        l.x_pct = x_pct;
        l.y_pct = y_pct;
      }
    }
    // The illumination changed: force a re-trace (Tim, 2026-10-04).
    LightSource.light_moved = true;
  }

  /**
   * Draws all lights at their true 3D positions (Tim, 2026-10-04):
   * a visual representation of the light sources used for shading.
   * Positions are refreshed every draw from the current viewport size,
   * then projected via Coords (matches the raytracer's pinhole camera).
   * Only draws 1 frame in 16 to keep it cheap.
   */
  public static void draw(final Graphics g) {
    // During an active drag, draw every frame for immediate feedback
    // (Tim, 2026-10-04: the FRAME_SKIP made dragged lights lag/disappear).
    final boolean dragging_active = dragging >= 0;
    if (!dragging_active) {
      frame_count = (frame_count + 1) % FRAME_SKIP;
      if (frame_count != 0) {
        return;
      }
    }
    // Refresh from the live viewport size (Tim, 2026-10-04): stale
    // zero-size positions collapsed all dots to one point.
    LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    // Skip lights at 0% intensity (Tim, 2026-10-03).
    // Dots use the configured light colors (Tim, 2026-10-04).
    synchronized (LightSource.class) {
      for (final Light light : LightSource.lights) {
        if (light.intensity_pct > 0) {
          drawOne(g, light.px, light.py, light.pz,
              new Color(light.colour));
        }
      }
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
