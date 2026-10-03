// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.geometry.Vector3D;

public class LightSource {
  public static final Vector3D source_1 = new Vector3D(-116, -67, -217);

  /**
   * RGB point light positions for improved realism (Tim, 2026-10-03):
   * near the top of the frame, equally spaced, favoring the front
   * and top of the model. Intensity falls off with distance. Affect
   * both the full and fast ray-traced renderers.
   */
  public static final Vector3D source_red_pos =
      new Vector3D(-500, -500, -800);
  public static final Vector3D source_green_pos =
      new Vector3D(0, -500, -800);
  public static final Vector3D source_blue_pos =
      new Vector3D(500, -500, -800);

  /** Directional versions (for compatibility). */
  public static final Vector3D source_red = new Vector3D(-100, -100, -100);
  public static final Vector3D source_green = new Vector3D(0, -100, -100);
  public static final Vector3D source_blue = new Vector3D(100, -100, -100);
}
