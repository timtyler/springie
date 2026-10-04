// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.geometry.Vector3D;

public class LightSource {
  public static final Vector3D source_1 = new Vector3D(-116, -67, -217);

  /**
   * RGB point light positions (Tim, 2026-10-03): viewport-dependent,
   * near the top of the frame, equally spaced. Updated by
   * updateForViewport() before each render.
   */
  public static double red_px, red_py, red_pz;
  public static double green_px, green_py, green_pz;
  public static double blue_px, blue_py, blue_pz;
  public static double white_px, white_py, white_pz;

  /** Directional versions (for compatibility). */
  public static final Vector3D source_red = new Vector3D(-100, -100, -100);
  public static final Vector3D source_green = new Vector3D(0, -100, -100);
  public static final Vector3D source_blue = new Vector3D(100, -100, -100);
  public static final Vector3D source_white = new Vector3D(0, -100, -100);

  /**
   * Positions the RGB lights based on viewport dimensions (Tim, 2026-10-03).
   */
  public static void updateForViewport(final int half_w_pixels, final int half_h_pixels) {
    // Default to 800x600 viewport if not initialized (e.g., in unit tests).
    final int hw = half_w_pixels == 0 ? 400 : half_w_pixels;
    final int hh = half_h_pixels == 0 ? 300 : half_h_pixels;
    // Lights in fixed-point world units (shifted by Coords.shift), to match
    // the geometry coordinates used by both the ray tracer and the polygon
    // renderer. Near the top, equally spaced. The z puts them in front.
    final double scale = (double) (1 << com.springie.render.Coords.shift);
    final double half_w = (double) hw * scale;
    final double half_h = (double) hh * scale;
    final double light_y_top = -half_h * 0.8;
    final double light_y_bottom = half_h * 0.8;
    final double light_z = -800.0 * scale;
    red_px = -half_w * 0.6;
    red_py = light_y_top;
    red_pz = light_z;
    green_px = 0.0;
    green_py = light_y_bottom;
    green_pz = light_z;
    blue_px = half_w * 0.6;
    blue_py = light_y_top;
    blue_pz = light_z;
    // White light (Tim, 2026-10-03): fourth point light, center top.
    white_px = 0.0;
    white_py = light_y_top;
    white_pz = light_z;
  }
}
