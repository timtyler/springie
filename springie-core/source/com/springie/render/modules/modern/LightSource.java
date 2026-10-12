// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.geometry.Vector3D;
import java.util.ArrayList;
import java.util.List;

public class LightSource {
  public static final Vector3D source_1 = new Vector3D(-116, -67, -217);

  /**
   * The light sources (Tim, 2026-10-04): N lights, each with position
   * (as % of viewport half-size), intensity %, and color. The source
   * of truth; world coordinates are derived by updateForViewport().
   * Percentages survive window resizes and are what we persist.
   * Guarded by the LightSource class lock for add/remove; individual
   * Light fields are volatile for the event/render thread handoff.
   */
  public static final List<Light> lights = new ArrayList<>();

  static {
    resetToDefaults();
  }

  /**
   * Set when a light is dragged (or added/removed): the next frame
   * re-traces (the illumination changed). Cleared by the renderer
   * (Tim, 2026-10-04).
   */
  public static volatile boolean light_moved = false;

  /** Directional versions (for compatibility). */
  public static final Vector3D source_red = new Vector3D(-100, -100, -100);
  public static final Vector3D source_green = new Vector3D(0, -100, -100);
  public static final Vector3D source_blue = new Vector3D(100, -100, -100);
  public static final Vector3D source_white = new Vector3D(0, -100, -100);

  /**
   * Callback run after resetToDefaults() (Tim, 2026-10-05): the UI
   * registers a hook to rebuild the light tabs when a model load
   * resets the lights.
   */
  public static volatile Runnable onLightsReset;

  /**
   * Callback run after a light is dragged (Tim, 2026-10-05): the UI
   * registers a hook to refresh the position sliders.
   */
  public static volatile Runnable onLightMoved;

  /**
   * Resets to the six default lights (Tim, 2026-10-08): cyan, yellow,
   * green, red, blue, magenta at 60-degree intervals around the edge;
   * ambient to 0%.
   */
  public static synchronized void resetToDefaults() {
    lights.clear();
    // Six lights spread uniformly around the edge (Tim, 2026-10-08):
    // hexagon at 60-degree intervals, 0-100% (50 = center).
    // Cyan right, yellow top-right, green top-left, red left,
    // blue bottom-left, magenta bottom-right.
    lights.add(new Light(100.0, 50.0, 100, 0x00FFFF));   // Cyan
    lights.add(new Light(75.0, 6.7, 100, 0xFFFF00));     // Yellow
    lights.add(new Light(25.0, 6.7, 100, 0x00FF00));     // Green
    lights.add(new Light(0.0, 50.0, 100, 0xFF0000));     // Red
    lights.add(new Light(25.0, 93.3, 100, 0x0000FF));    // Blue
    lights.add(new Light(75.0, 93.3, 100, 0xFF00FF));    // Magenta
    // Ambient defaults to 0 (Tim, 2026-10-08).
    com.springie.render.RendererDelegator.ambient_light_pct = 0;
    // Notify the UI to rebuild the light tabs (Tim, 2026-10-05).
    final Runnable hook = onLightsReset;
    if (hook != null) {
      hook.run();
    }
  }

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
    // renderer.
    final double scale = (double) (1 << com.springie.render.Coords.shift);
    final double half_w = (double) hw * scale;
    final double half_h = (double) hh * scale;
    synchronized (LightSource.class) {
      for (final Light light : lights) {
        // Z: 0-100% maps to +50*scale (behind) through -50*scale (the
        // previous fixed front value) to -150*scale (far front). 50% is
        // the default front position (Tim, 2026-10-06).
        light.pz = (50.0 - 2.0 * light.z_pct) * scale;
        // 0-100% (50=center) to world coords (Tim, 2026-10-06). The
        // Coords.getXCoords convention is: sx = HW + (px + scx - HW*S)/PF,
        // so px = HW*S - scx is the screen center. The range is HW*PF
        // for exact screen mapping (PF = perspective factor at the
        // light's depth).
        final double pf = (double) (com.springie.render.Coords.shift_constant_z
            + ((int) light.pz >> com.springie.render.Coords.shift_z));
        light.px = (light.x_pct - 50.0) / 50.0 * half_w * (pf / scale)
            + (half_w - com.springie.render.Coords.shift_constant_x);
        light.py = (light.y_pct - 50.0) / 50.0 * half_h * (pf / scale)
            + (half_h - com.springie.render.Coords.shift_constant_y);
      }
    }
  }
}
