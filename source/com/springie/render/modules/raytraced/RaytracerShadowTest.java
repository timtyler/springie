// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Vector3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.LightSource;

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

    RendererDelegator.glossiness = 0;
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
  }

  private int centrePixel(Primitive[] primitives) {
    final BVH bvh = new BVH(primitives);
    final RayCamera camera = new RayCamera();
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);
    return pixels[100 * 200 + 100];
  }

  /**
   * The white sphere plus an occluder on the light axis above the hit
   * pole. The occluder is well clear of the primary ray, so it can only
   * affect the picture through the shadow ray.
   */
  private Primitive[] whiteSphereWithOccluder() {
    final Vector3D source = LightSource.source_1;
    final double length = Math.sqrt(source.x * source.x + source.y * source.y
        + source.z * source.z);
    final double lx = source.x / length;
    final double ly = source.y / length;
    final double lz = source.z / length;
    final double pole_x = EX;
    final double pole_y = EY;
    final double pole_z = -20000.0;
    final double distance = 50000.0;
    final double radius = 20000.0;
    return new Primitive[] {
        new RTSphere(EX, EY, 0.0, 20000.0, 0xFFFFFF),
        new RTSphere(pole_x + lx * distance, pole_y + ly * distance,
            pole_z + lz * distance, radius, 0xFF0000) };
  }

  @Test
  public void occluderDarkensThePoleToAmbientOnly() {
    RendererDelegator.shadows = true;
    final int shadowed = centrePixel(whiteSphereWithOccluder());
    // Ambient only: scaled = 128, (255 * 128) >> 8 = 127.
    assertEquals(0xFF7F7F7F, shadowed,
        "a shadowed white surface must get ambient light only");
  }

  @Test
  public void shadowsOffLeavesThePoleFullyLit() {
    RendererDelegator.shadows = false;
    final int lit = centrePixel(whiteSphereWithOccluder());
    assertTrue(lit > 0xFF7F7F7F,
        "with shadows off the occluder must not darken anything");
  }

  @Test
  public void shadowsOnWithoutOccluderLeavesThePoleFullyLit() {
    RendererDelegator.shadows = true;
    final int lit = centrePixel(
        new Primitive[] { new RTSphere(EX, EY, 0.0, 20000.0, 0xFFFFFF) });
    assertTrue(lit > 0xFF7F7F7F,
        "with nothing to block the light, shadows must change nothing");
  }
}
