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
   * Last drawn screen positions (Tim, 2026-10-05): used to erase the
   * old dots with background color before drawing new ones, preventing
   * trails during drag. Indexed by light list position; -1 = not drawn.
   */
  private static int[] last_sx = new int[0];
  private static int[] last_sy = new int[0];

  /**
   * Currently dragged light (Tim, 2026-10-04): null = none, otherwise
   * the Light object reference. Using the reference (not the index)
   * avoids index mismatches if the list changes mid-drag. Volatile:
   * written on the event thread, read on the render thread.
   */
  public static volatile Light dragging_light = null;

  /**
   * @deprecated Use dragging_light instead (Tim, 2026-10-04).
   */
  @Deprecated
  public static volatile int dragging = -1;

  private LightSourceDots() {
    // Static only.
  }

  /**
   * Hit test (Tim, 2026-10-04): returns the light index if the
   * screen point is within a dot, or -1.
   */
  /**
   * Hit test (Tim, 2026-10-04): returns the Light object if the screen
   * point is within a dot, or null.
   */
  public static Light hitTestLight(final int sx, final int sy) {
    LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    synchronized (LightSource.class) {
      // Defensive: if the list is empty, reset to defaults (Tim, 2026-10-04).
      if (LightSource.lights.isEmpty()) {
        LightSource.resetToDefaults();
        LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
      }
      for (int i = 0; i < LightSource.lights.size(); i++) {
        final Light light = LightSource.lights.get(i);
        if (light.intensity_pct > 0
            && near(sx, sy, light.px, light.py, light.pz)) {
          return light;
        }
      }
    }
    return null;
  }

  /**
   * Hit test (Tim, 2026-10-04): returns the light index (0-N) if the
   * screen point is within a dot, or -1. @deprecated Use hitTestLight.
   */
  @Deprecated
  public static int hitTest(final int sx, final int sy) {
    final Light light = hitTestLight(sx, sy);
    if (light == null) {
      return -1;
    }
    synchronized (LightSource.class) {
      return LightSource.lights.indexOf(light);
    }
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
   * Drag a light to a screen position (Tim, 2026-10-04): uses the
   * inverse projection to convert screen pixels to world coordinates
   * at the light's depth, then stores as percentages. This accounts
   * for perspective; the old linear math had a 0.5x error.
   */
  public static void dragTo(final Light light_ref, final int sx, final int sy) {
    if (light_ref == null) {
      return;
    }
    // Inverse project: screen pixels (fixed-point) -> world coordinates
    // at the light's current depth. The inverse functions expect
    // fixed-point screen coordinates.
    final int sx_fp = sx << Coords.shift;
    final int sy_fp = sy << Coords.shift;
    final int pz = (int) light_ref.pz;
    final int world_x = Coords.inverseXCoords(sx_fp, pz);
    final int world_y = Coords.inverseYCoords(sy_fp, pz);
    // Store as percentages of the viewport half-size (Tim, 2026-10-04):
    // survives window resizes, and what we persist.
    final int hw = Coords.x_pixelso2 == 0 ? 400 : Coords.x_pixelso2;
    final int hh = Coords.y_pixelso2 == 0 ? 300 : Coords.y_pixelso2;
    final double scale = (double) (1 << Coords.shift);
    // Inverse of updateForViewport: 0-100% (50 = center) (Tim, 2026-10-05).
    double x_pct = (double) world_x / ((double) hw * scale) * 50.0 + 50.0;
    double y_pct = (double) world_y / ((double) hh * scale) * 50.0 + 50.0;
    // Clamp to a sane range (Tim, 2026-10-04): prevents the light from
    // going so far off-screen that the projection breaks and the dot
    // disappears.
    if (x_pct < -50.0) {
      x_pct = -50.0;
    } else if (x_pct > 150.0) {
      x_pct = 150.0;
    }
    if (y_pct < -50.0) {
      y_pct = -50.0;
    } else if (y_pct > 150.0) {
      y_pct = 150.0;
    }
    light_ref.x_pct = x_pct;
    light_ref.y_pct = y_pct;
    // The illumination changed: force a re-trace (Tim, 2026-10-04).
    LightSource.light_moved = true;
  }

  /**
   * @deprecated Use dragTo(Light, int, int) instead (Tim, 2026-10-04).
   */
  @Deprecated
  public static void dragTo(final int light, final int sx, final int sy) {
    synchronized (LightSource.class) {
      if (light >= 0 && light < LightSource.lights.size()) {
        dragTo(LightSource.lights.get(light), sx, sy);
      }
    }
  }

  /**
   * Draws all lights at their true 3D positions (Tim, 2026-10-04):
   * a visual representation of the light sources used for shading.
   * Positions are refreshed every draw from the current viewport size,
   * then projected via Coords (matches the raytracer's pinhole camera).
   * Only draws 1 frame in 16 to keep it cheap.
   */
  public static void draw(final Graphics g) {
    // Draw at most 1 frame in 16 (Tim, 2026-10-04): the dots are
    // cheap but the canvas blit isn't; every frame is too much.
    frame_count = (frame_count + 1) % FRAME_SKIP;
    if (frame_count != 0) {
      return;
    }
    // Refresh from the live viewport size (Tim, 2026-10-04): stale
    // zero-size positions collapsed all dots to one point.
    LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    // Erase old dots with background color (Tim, 2026-10-05): prevents
    // trails during drag. Essential in single-buffered mode where the
    // canvas isn't cleared each frame.
    final int bg = com.springie.render.RendererDelegator
        .color_background_number;
    g.setColor(new Color(bg));
    synchronized (LightSource.class) {
      // Resize tracking arrays if light count changed.
      final int n = LightSource.lights.size();
      if (last_sx.length != n) {
        last_sx = new int[n];
        last_sy = new int[n];
        java.util.Arrays.fill(last_sx, -1);
        java.util.Arrays.fill(last_sy, -1);
      }
      for (int i = 0; i < n; i++) {
        if (last_sx[i] >= 0) {
          g.fillOval(last_sx[i] - RADIUS - 1, last_sy[i] - RADIUS - 1,
              (RADIUS + 1) * 2, (RADIUS + 1) * 2);
        }
        last_sx[i] = -1;
        last_sy[i] = -1;
      }
    }
    // Skip lights at 0% intensity (Tim, 2026-10-03).
    // Dots use the configured light colors (Tim, 2026-10-04).
    synchronized (LightSource.class) {
      final int n = LightSource.lights.size();
      for (int i = 0; i < n; i++) {
        final Light light = LightSource.lights.get(i);
        if (light.intensity_pct > 0) {
          final int[] pos = drawOne(g, light.px, light.py, light.pz,
              new Color(light.colour));
          if (pos != null && i < last_sx.length) {
            last_sx[i] = pos[0];
            last_sy[i] = pos[1];
          }
        }
      }
    }
  }

  private static int[] drawOne(final Graphics g, final double wx,
      final double wy, final double wz, final Color color) {
    final int ix = (int) wx;
    final int iy = (int) wy;
    final int iz = (int) wz;
    // Skip if on the eye plane (projection divides by zero).
    if (Coords.shift_constant_z + (iz >> Coords.shift_z) == 0) {
      return null;
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
    return new int[]{sx, sy};
  }
}
