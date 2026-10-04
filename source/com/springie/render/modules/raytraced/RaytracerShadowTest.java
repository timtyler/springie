// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.context.ContextManager;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.LightSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shadows through the full tile pipeline.
 *
 * <p>The centre pixel's primary ray runs straight down +z from the eye
 * (25600, 25600, -196608), so a sphere centred on (25600, 25600, 0) is
 * hit exactly at its near pole. An occluder sphere parked on the light
 * axis above that pole blocks the shadow ray. No model is loaded, so
 * Fog.applyFog is a no-op and every expected value is exact.
 */
public class RaytracerShadowTest {
  private static final double EX = 100 << Coords.shift;

  private static final double EY = 100 << Coords.shift;

  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private NodeManager saved_manager;

  private boolean saved_depth_is_relative;

  private int saved_glossiness;

  private boolean saved_shadows;

  private int saved_specular;

  private double saved_red_px;

  private double saved_red_py;

  private double saved_red_pz;

  private int saved_green_pct;

  private int saved_blue_pct;

  private int saved_white_pct;

  private boolean saved_freeze;

  @BeforeEach
  public void setUp() {
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;

    Coords.x_pixels = 200;
    Coords.y_pixels = 200;
    Coords.x_pixelso2 = 100;
    Coords.y_pixelso2 = 100;
    Coords.shift_constant_x = 0;
    Coords.shift_constant_y = 0;
    Coords.shift_constant_z = 192;

    this.saved_manager = ContextManager.getNodeManager();
    // Fog is a no-op without a model; without this, a GUI test that
    // booted the app earlier in the same JVM would leave fog active
    // and the exact pixel assertions below would fail.
    ContextManager.setNodeManager(null);
    this.saved_depth_is_relative =
        DeepObjectColourCalculator.depth_is_relative;

    this.saved_glossiness = RendererDelegator.glossiness;
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_specular = RendererDelegator.specular;
    this.saved_red_px = Raytracer.RED_PX;
    this.saved_red_py = Raytracer.RED_PY;
    this.saved_red_pz = Raytracer.RED_PZ;
    this.saved_green_pct = RendererDelegator.green_light_pct;
    this.saved_blue_pct = RendererDelegator.blue_light_pct;
    this.saved_white_pct = RendererDelegator.white_light_pct;
    this.saved_freeze = Raytracer.freeze_lights;

    RendererDelegator.glossiness = 0;
    // Only the red light matters for this test; turn off green/blue/white.
    RendererDelegator.green_light_pct = 0;
    RendererDelegator.blue_light_pct = 0;
    RendererDelegator.white_light_pct = 0;
    RendererDelegator.specular = 0;
  }

  @AfterEach
  public void tearDown() {
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = Coords.x_pixels >> 1;
    Coords.y_pixelso2 = Coords.y_pixels >> 1;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
    ContextManager.setNodeManager(this.saved_manager);
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;

    RendererDelegator.glossiness = this.saved_glossiness;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.specular = this.saved_specular;
    Raytracer.RED_PX = this.saved_red_px;
    Raytracer.RED_PY = this.saved_red_py;
    Raytracer.RED_PZ = this.saved_red_pz;
    RendererDelegator.green_light_pct = this.saved_green_pct;
    RendererDelegator.blue_light_pct = this.saved_blue_pct;
    RendererDelegator.white_light_pct = this.saved_white_pct;
    Raytracer.freeze_lights = this.saved_freeze;
  }

  private int centrePixel(final Primitive[] primitives) {
    final BVH bvh = new BVH(primitives);
    final RayCamera camera = new RayCamera();
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);
    return pixels[100 * 200 + 100];
  }

  /**
   * The white sphere plus an occluder on the red light axis above the hit
   * pole. The occluder is well clear of the primary ray, so it can only
   * affect the picture through the shadow ray. (Tim, 2026-10-03: shadows
   * now come from the RGB point lights, not the old white source_1.)
   */
  private Primitive[] whiteSphereWithOccluder() {
    // Freeze the lights and position red in front of the surface.
    // (Production lights sit behind the camera, which would put the
    // occluder in the primary ray.)
    Raytracer.freeze_lights = true;
    Raytracer.RED_PX = EX - 30000.0;
    Raytracer.RED_PY = EY - 30000.0;
    Raytracer.RED_PZ = 100000.0;
    // Green and blue stay where they are (behind camera); they don't
    // affect this test's pole pixel which faces the red light.
    final double lx = Raytracer.RED_PX;
    final double ly = Raytracer.RED_PY;
    final double lz = Raytracer.RED_PZ;
    final double pole_x = EX;
    final double pole_y = EY;
    final double pole_z = -20000.0;
    // Direction from pole to light.
    double dx = lx - pole_x;
    double dy = ly - pole_y;
    double dz = lz - pole_z;
    final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    dx /= length;
    dy /= length;
    dz /= length;
    final double distance = 50000.0;
    final double radius = 20000.0;
    return new Primitive[] {
        new RTSphere(EX, EY, 0.0, 20000.0, 0xFFFFFF),
        new RTSphere(pole_x + dx * distance, pole_y + dy * distance,
            pole_z + dz * distance, radius, 0xFF0000), };
  }

  @Test
  public void occluderDarkensThePoleToAmbientOnly() {
    RendererDelegator.shadows = true;
    final int shadowed = centrePixel(whiteSphereWithOccluder());
    // Ambient only: (255 * 96) >> 8 = 95 (Tim, 2026-10-03: ambient 128->96,
    // and the scale is applied as (base * scaled) >> 8).
    assertEquals(0xFF5F5F5F, shadowed,
        "a shadowed white surface must get ambient light only");
  }

  @Test
  public void shadowsOffLeavesThePoleFullyLit() {
    RendererDelegator.shadows = false;
    final int lit = centrePixel(whiteSphereWithOccluder());
    assertTrue(lit > 0xFF3F3F3F,
        "with shadows off the occluder must not darken anything");
  }

  @Test
  public void shadowsOnWithoutOccluderLeavesThePoleFullyLit() {
    RendererDelegator.shadows = true;
    final int lit = centrePixel(
        new Primitive[] { new RTSphere(EX, EY, 0.0, 20000.0, 0xFFFFFF) });
    assertTrue(lit > 0xFF3F3F3F,
        "with nothing to block the light, shadows must change nothing");
  }
}
