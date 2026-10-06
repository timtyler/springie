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

  /**
   * True if the user has explicitly configured lights via the UI (Tim,
   * 2026-10-05). When true, resetToDefaults() (called on model load)
   * does not overwrite the user's settings with hardcoded defaults.
   */
  public static volatile boolean user_configured = false;

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
   * Resets to the four default lights (Tim, 2026-10-04).
   */
  public static synchronized void resetToDefaults() {
    // Don't overwrite user-configured lights (Tim, 2026-10-05).
    if (user_configured) {
      return;
    }
    forceResetToDefaults();
  }

  /**
   * Resets to the spread defaults even if user_configured is true.
   * Called once on program startup (Tim, 2026-10-05): lights must
   * start separate, not stacked.
   */
  public static synchronized void forceResetToDefaults() {
    user_configured = false;
    lights.clear();
    // Light one: red, far left. (Tim, 2026-10-04: spread to the sides,
    // not jammed in one corner.)
    lights.add(new Light(-80.0, 0.0, 50, 0xFF0000));
    // Light two: green, far right.
    lights.add(new Light(80.0, 0.0, 50, 0x00FF00));
    // Light three: blue, top.
    lights.add(new Light(0.0, -80.0, 50, 0x0000FF));
    // Light four: white, bottom.
    lights.add(new Light(0.0, 80.0, 50, 0xFFFFFF));
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
    // renderer. The z puts them slightly in front of the model, from the
    // user's perspective (Tim, 2026-10-04): was -200 (too far forward,
    // broke the drag tracking), now -50.
    final double scale = (double) (1 << com.springie.render.Coords.shift);
    final double half_w = (double) hw * scale;
    final double half_h = (double) hh * scale;
    final double light_z = -50.0 * scale;
    synchronized (LightSource.class) {
      for (final Light light : lights) {
        light.px = light.x_pct / 100.0 * half_w;
        light.py = light.y_pct / 100.0 * half_h;
        light.pz = light_z;
      }
    }
  }
}
