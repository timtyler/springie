// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.geometry.Vector3D;

public class LightSource {
  public static final Vector3D source_1 = new Vector3D(-116, -67, -217);

  /**
   * Light positions as percentages of the viewport half-size (Tim,
   * 2026-10-04): x_pct is % of half-width, y_pct is % of half-height.
   * The source of truth; world coordinates are derived by
   * updateForViewport(). Percentages survive window resizes and are
   * what we'll persist. Volatile: the event thread (via NewMessage)
   * writes, the render thread reads.
   */
  public static volatile double red_x_pct = -60.0;
  public static volatile double red_y_pct = -80.0;
  public static volatile double green_x_pct = 0.0;
  public static volatile double green_y_pct = 80.0;
  public static volatile double blue_x_pct = 60.0;
  public static volatile double blue_y_pct = -80.0;
  public static volatile double white_x_pct = 0.0;
  public static volatile double white_y_pct = -80.0;

  /**
   * Derived world coordinates (fixed-point, shifted by Coords.shift),
   * for the renderers. Recomputed by updateForViewport() from the
   * percentages above.
   */
  public static volatile double red_px, red_py, red_pz;
  public static volatile double green_px, green_py, green_pz;
  public static volatile double blue_px, blue_py, blue_pz;
  public static volatile double white_px, white_py, white_pz;

  /**
   * Set when a light is dragged: the next frame re-traces (the
   * illumination changed). Cleared by the renderer (Tim, 2026-10-04).
   */
  public static volatile boolean light_moved = false;

  /** Directional versions (for compatibility). */
  public static final Vector3D source_red = new Vector3D(-100, -100, -100);
  public static final Vector3D source_green = new Vector3D(0, -100, -100);
  public static final Vector3D source_blue = new Vector3D(100, -100, -100);
  public static final Vector3D source_white = new Vector3D(0, -100, -100);

  /**
   * Derives world coordinates from the percentage positions (Tim,
   * 2026-10-04). Called before each render; the percentages are the
   * source of truth.
   */
  public static void updateForViewport(final int half_w_pixels, final int half_h_pixels) {
    // Default to 800x600 viewport if not initialized (e.g., in unit tests).
    final int hw = half_w_pixels == 0 ? 400 : half_w_pixels;
    final int hh = half_h_pixels == 0 ? 300 : half_h_pixels;
    // Lights in fixed-point world units (shifted by Coords.shift), to match
    // the geometry coordinates used by both the ray tracer and the polygon
    // renderer. The z puts them in front of the model, from the user's
    // perspective (Tim, 2026-10-04).
    final double scale = (double) (1 << com.springie.render.Coords.shift);
    final double half_w = (double) hw * scale;
    final double half_h = (double) hh * scale;
    final double light_z = -200.0 * scale;
    red_px = red_x_pct / 100.0 * half_w;
    red_py = red_y_pct / 100.0 * half_h;
    red_pz = light_z;
    green_px = green_x_pct / 100.0 * half_w;
    green_py = green_y_pct / 100.0 * half_h;
    green_pz = light_z;
    blue_px = blue_x_pct / 100.0 * half_w;
    blue_py = blue_y_pct / 100.0 * half_h;
    blue_pz = light_z;
    white_px = white_x_pct / 100.0 * half_w;
    white_py = white_y_pct / 100.0 * half_h;
    white_pz = light_z;
  }
}
