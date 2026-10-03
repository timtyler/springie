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
  public static final Vector3D source_red_pos = new Vector3D();
  public static final Vector3D source_green_pos = new Vector3D();
  public static final Vector3D source_blue_pos = new Vector3D();

  /** Directional versions (for compatibility). */
  public static final Vector3D source_red = new Vector3D(-100, -100, -100);
  public static final Vector3D source_green = new Vector3D(0, -100, -100);
  public static final Vector3D source_blue = new Vector3D(100, -100, -100);

  /**
   * Positions the RGB lights based on viewport dimensions (Tim, 2026-10-03).
   */
  public static void updateForViewport(final int half_w_pixels, final int half_h_pixels) {
    final double half_w = (double) (half_w_pixels << com.springie.render.Coords.shift);
    final double half_h = (double) (half_h_pixels << com.springie.render.Coords.shift);
    final double light_y = -half_h * 0.8;
    final double light_z = -800.0;
    source_red_pos.set(-half_w * 0.6, light_y, light_z);
    source_green_pos.set(0.0, light_y, light_z);
    source_blue_pos.set(half_w * 0.6, light_y, light_z);
  }
}
