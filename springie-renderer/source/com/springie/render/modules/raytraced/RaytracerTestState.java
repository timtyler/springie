// Shared state pinning for the raytracer pixel tests (Tim, 2026-11-08).

package com.springie.render.modules.raytraced;

import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.Light;
import com.springie.render.modules.modern.LightSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Pins the renderer/lighting statics the ray-traced shade() path reads
 * live, so a test class sees the same renderer state no matter which
 * tests ran before it in the JVM.
 *
 * <p>The state pinned here is the state the exact-pixel assertions were
 * written against (Oct 2026, before the six-light reset of e885ff4):
 * the four lights the shade() path reads by slot (red left, green
 * right, blue top, white bottom, all at 100%), colored lighting on,
 * ambient 25% (ambientBase 96: the shadow test's ambient-only
 * constant (255*96)>>8 = 95 holds only there, and the diffuse-only
 * constants saturate identically at 25%), coarse-to-fine OFF so
 * every pixel is traced exactly (the coarse path fills blocks whose
 * samples agree within 3 counts per channel, which would make the
 * exact-pixel assertions statements about the fill heuristic rather
 * than about shade()), simple lighting off, all shading features
 * off, and a black background.
 *
 * <p>The feature flags (gloss/specular/fresnel/shadows/AA/...) are
 * deliberately NOT pinned: each test class sets the ones it exercises
 * in its own setUp and restores them in tearDown. Pinning them here
 * too caused order fights -- this pin runs first in setUp, so a
 * class's own assignments must be free to override it, and restore()
 * runs last in tearDown, after the class has put its own values back.
 * Tests call pin() first in setUp and restore() last in tearDown.
 */
final class RaytracerTestState {
  private int saved_ambient;

  private boolean saved_colored;

  private int saved_coarse_to_fine;

  private boolean saved_simple_lighting;

  private int saved_background;

  private List<Light> saved_lights;

  void pin() {
    this.saved_ambient = RendererDelegator.ambient_light_pct;
    this.saved_colored = LightSource.colored_lighting;
    this.saved_coarse_to_fine = RendererDelegator.coarse_to_fine;
    this.saved_simple_lighting = RendererDelegator.simple_lighting;
    this.saved_background = RendererDelegator.color_background_number;
    synchronized (LightSource.class) {
      this.saved_lights = new ArrayList<>();
      for (final Light light : LightSource.lights) {
        this.saved_lights.add(new Light(light));
      }
    }

    LightSource.colored_lighting = true;
    // Install the four lights the assertions were written against
    // (the pre-e885ff4 resetToDefaults set): red left, green right,
    // blue top, white bottom, all 100%, z defaulting to 50 (front).
    // Keep the UI's onLightsReset hook out of it: in a JVM where a GUI
    // test booted the app, reset paths can fire a hook that posts UI
    // rebuilds to the EDT and blocks against a half-torn-down panel.
    synchronized (LightSource.class) {
      LightSource.lights.clear();
      LightSource.lights.add(new Light(10.0, 50.0, 100, 0xFF0000));
      LightSource.lights.add(new Light(90.0, 50.0, 100, 0x00FF00));
      LightSource.lights.add(new Light(50.0, 10.0, 100, 0x0000FF));
      LightSource.lights.add(new Light(50.0, 90.0, 100, 0xFFFFFF));
    }
    RendererDelegator.ambient_light_pct = 25;
    RendererDelegator.coarse_to_fine = 0;
    RendererDelegator.simple_lighting = false;
    RendererDelegator.color_background_number = 0;
  }

  void restore() {
    RendererDelegator.ambient_light_pct = this.saved_ambient;
    LightSource.colored_lighting = this.saved_colored;
    RendererDelegator.coarse_to_fine = this.saved_coarse_to_fine;
    RendererDelegator.simple_lighting = this.saved_simple_lighting;
    RendererDelegator.color_background_number = this.saved_background;
    synchronized (LightSource.class) {
      LightSource.lights.clear();
      for (final Light light : this.saved_lights) {
        LightSource.lights.add(new Light(light));
      }
    }
  }
}
