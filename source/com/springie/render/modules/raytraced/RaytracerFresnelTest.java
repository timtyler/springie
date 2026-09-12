// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * The Fresnel rim through the full tile pipeline: nothing head-on,
 * brighter towards the silhouette, following Schlick's approximation.
 *
 * <p>The centre pixel's primary ray runs straight down +z from the eye,
 * so a sphere centred on that ray is hit exactly at its near pole --
 * cosine 1, rim 0. The silhouette pixels are hit at grazing angles --
 * cosine near 0, rim near full strength.
 */
public class RaytracerFresnelTest {
  private static final int SIZE = 200;

  private static final int CENTRE = 100;

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

  private int saved_antialiasing;

  @BeforeEach
  public void setUp() {
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;

    Coords.x_pixels = SIZE;
    Coords.y_pixels = SIZE;
    Coords.x_pixelso2 = SIZE / 2;
    Coords.y_pixelso2 = SIZE / 2;
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
    this.saved_antialiasing = RendererDelegator.antialiasing;

    RendererDelegator.glossiness = 0;
    RendererDelegator.shadows = false;
    RendererDelegator.specular = 0;
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
    RendererDelegator.antialiasing = this.saved_antialiasing;
  }

  private int[] render(int fresnel) {
    RendererDelegator.fresnel = fresnel;
    // White sphere, 60-pixel radius, centred on the middle row.
    final Primitive[] primitives = new Primitive[] { new RTSphere(
        CENTRE << Coords.shift, CENTRE << Coords.shift, 0.0, 60.0 * 192,
        0xFFFFFF) };
    final BVH bvh = new BVH(primitives);
    final int[] pixels = new int[SIZE * SIZE];
    Raytracer.renderTile(0, 0, SIZE, SIZE, new RayCamera(), bvh, pixels);
    return pixels;
  }

  private static int backgroundRgb() {
    return 0xFF000000 | RendererDelegator.color_background_number;
  }

  /**
   * The last hit pixel on the middle row, walking right from the
   * centre: the silhouette, hit at a grazing angle.
   */
  private static int silhouetteIndex(int[] pixels) {
    for (int x = CENTRE; x < SIZE; x++) {
      if (pixels[CENTRE * SIZE + x] == backgroundRgb()) {
        return CENTRE * SIZE + x - 1;
      }
    }
    throw new AssertionError("no silhouette found on the middle row");
  }

  private static int channel(int rgb, int shift) {
    return (rgb >> shift) & 0xFF;
  }

  @Test
  public void zeroFresnelIsPureDiffuse() {
    // |normal . light| at the near pole is 0.85092: scaled = 236,
    // (255 * 236) >> 8 = 235. The rim must add nothing when off.
    assertEquals(0xFFEBEBEB, render(0)[CENTRE * SIZE + CENTRE],
        "fresnel 0% must leave the diffuse picture untouched");
  }

  @Test
  public void silhouetteBrightensWithFresnel() {
    final int[] plain = render(0);
    final int[] rimmed = render(100);
    final int sil = silhouetteIndex(plain);
    assertNotEquals(backgroundRgb(), plain[sil]);

    // The near pole is hit head-on: cosine 1, Schlick 0, no rim.
    assertEquals(plain[CENTRE * SIZE + CENTRE],
        rimmed[CENTRE * SIZE + CENTRE],
        "fresnel must not touch the head-on near pole");

    // The silhouette is hit at a grazing angle: the rim must lift
    // every channel.
    for (final int shift : new int[] { 16, 8, 0 }) {
      assertTrue(channel(rimmed[sil], shift) > channel(plain[sil], shift),
          "fresnel 100% must brighten the silhouette, channel " + shift
              + ": " + Integer.toHexString(plain[sil]) + " -> "
              + Integer.toHexString(rimmed[sil]));
    }
  }

  @Test
  public void fresnelGrowsMonotonically() {
    final int[] none = render(0);
    final int[] half = render(50);
    final int[] full = render(100);
    // The rim saturates to white at the silhouette itself, so measure
    // one step inside, where nothing clamps.
    final int inner = unclampedIndex(none, full);
    for (final int shift : new int[] { 16, 8, 0 }) {
      final int c0 = channel(none[inner], shift);
      final int c50 = channel(half[inner], shift);
      final int c100 = channel(full[inner], shift);
      assertTrue(c50 > c0 && c50 < c100,
          "fresnel must grow monotonically inside the silhouette: " + c0
              + " < " + c50 + " < " + c100);
    }
  }

  /**
   * The first pixel walking in from the silhouette whose full-strength
   * rim does not saturate to white on any channel.
   */
  private static int unclampedIndex(int[] plain, int[] full) {
    final int sil = silhouetteIndex(plain);
    for (int i = sil; i >= CENTRE * SIZE + CENTRE; i--) {
      final int p = full[i];
      if (channel(p, 16) < 255 && channel(p, 8) < 255
          && channel(p, 0) < 255) {
        return i;
      }
    }
    throw new AssertionError("no unclamped pixel inside the silhouette");
  }
}
