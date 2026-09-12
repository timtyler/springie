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
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

/**
 * The fill light through the full tile pipeline: a weak second light
 * from the front-right that lifts surfaces turned away from the key,
 * shadow-independent.
 *
 * <p>The centre pixel's primary ray runs straight down +z, so a white
 * sphere centred on that ray is hit exactly at its near pole. The fill
 * direction mirrors the key light's azimuth, so |normal . fill| at the
 * pole equals |normal . light|: 0.85092. No model is loaded, so
 * Fog.applyFog is a no-op.
 */
public class RaytracerFillLightTest {
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

  private int saved_fresnel;

  private int saved_fill_light;

  private boolean saved_fill_light_enabled;

  private int saved_antialiasing;

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
    this.saved_fresnel = RendererDelegator.fresnel;
    this.saved_fill_light = RendererDelegator.fill_light;
    this.saved_fill_light_enabled = RendererDelegator.fill_light_enabled;
    this.saved_antialiasing = RendererDelegator.antialiasing;

    RendererDelegator.glossiness = 0;
    RendererDelegator.shadows = false;
    RendererDelegator.specular = 0;
    RendererDelegator.fresnel = 0;
    RendererDelegator.fill_light_enabled = true;
    RendererDelegator.antialiasing = 1;
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
    RendererDelegator.fresnel = this.saved_fresnel;
    RendererDelegator.fill_light = this.saved_fill_light;
    RendererDelegator.fill_light_enabled = this.saved_fill_light_enabled;
    RendererDelegator.antialiasing = this.saved_antialiasing;
  }

  private int nearPole(int fill) {
    RendererDelegator.fill_light = fill;
    final Primitive[] primitives = new Primitive[] { new RTSphere(EX, EY,
        0.0, 20000.0, 0xFFFFFF) };
    final BVH bvh = new BVH(primitives);
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, new RayCamera(), bvh, pixels);
    return pixels[100 * 200 + 100];
  }

  private static int channel(int rgb, int shift) {
    return (rgb >> shift) & 0xFF;
  }

  @Test
  public void zeroFillIsPureDiffuse() {
    // Diffuse only: |normal . light| = 0.85092 at the near pole,
    // scaled = 236, (255 * 236) >> 8 = 235.
    assertEquals(0xFFEBEBEB, nearPole(0),
        "fill 0% must leave the diffuse picture untouched");
  }

  @Test
  public void fillLiftsTheNearPole() {
    final int plain = nearPole(0);
    final int filled = nearPole(100);
    for (final int shift : new int[] { 16, 8, 0 }) {
      assertTrue(channel(filled, shift) > channel(plain, shift),
          "fill 100% must lift the near pole, channel " + shift + ": "
              + Integer.toHexString(plain) + " -> "
              + Integer.toHexString(filled));
    }
  }

  @Test
  public void disabledFillAddsNothing() {
    final int plain = nearPole(0);
    RendererDelegator.fill_light_enabled = false;
    assertEquals(plain, nearPole(100),
        "a disabled fill light must add nothing, even at 100% strength");
    RendererDelegator.fill_light_enabled = true;
  }

  @Test
  public void fillGrowsMonotonically() {
    final int none = nearPole(0);
    final int some = nearPole(30);
    final int full = nearPole(100);
    for (final int shift : new int[] { 16, 8, 0 }) {
      final int c0 = channel(none, shift);
      final int c30 = channel(some, shift);
      final int c100 = channel(full, shift);
      assertTrue(c30 > c0 && c30 < c100,
          "fill must grow monotonically at the near pole: " + c0
              + " < " + c30 + " < " + c100);
    }
  }

  @Test
  public void fullFillAddsAtMostHalfStrength() throws Exception {
    // 100% fill is capped at half the key light's punch: with the hit
    // normal exactly aligned to the fill direction the raw add must be
    // (int)(100 * 1.275) = 127, not 255.
    final java.lang.reflect.Method fillLight = Raytracer.class
        .getDeclaredMethod("fillLight", Hit.class);
    fillLight.setAccessible(true);
    final java.lang.reflect.Field fx_field = Raytracer.class
        .getDeclaredField("FILL_X");
    final java.lang.reflect.Field fy_field = Raytracer.class
        .getDeclaredField("FILL_Y");
    final java.lang.reflect.Field fz_field = Raytracer.class
        .getDeclaredField("FILL_Z");
    fx_field.setAccessible(true);
    fy_field.setAccessible(true);
    fz_field.setAccessible(true);
    final double fx = fx_field.getDouble(null);
    final double fy = fy_field.getDouble(null);
    final double fz = fz_field.getDouble(null);
    final Hit hit = new Hit();
    hit.nx = fx;
    hit.ny = fy;
    hit.nz = fz;
    RendererDelegator.fill_light = 100;
    final int added = (Integer) fillLight.invoke(null, hit);
    assertEquals(127, added,
        "100% fill at perfect alignment must add exactly 127");
  }
}
