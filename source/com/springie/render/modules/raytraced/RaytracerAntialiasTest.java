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
 * Anti-aliasing (RendererDelegator.antialiasing): a 2x2 or 3x3 grid of
 * sub-pixel rays per pixel, box-filtered. Flat regions must render
 * identically to 1x1; silhouette edges must soften into blends.
 */
public class RaytracerAntialiasTest {
  private static final int SIZE = 200;

  private static final int CENTRE_X = 140;

  private static final int CENTRE_Y = 100;

  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_x_pixelso2;

  private int saved_y_pixelso2;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private NodeManager saved_manager;

  private boolean saved_depth_is_relative;

  private int saved_antialiasing;

  private int saved_glossiness;

  private boolean saved_shadows;

  private int saved_specular;

  @BeforeEach
  public void setUp() {
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_x_pixelso2 = Coords.x_pixelso2;
    this.saved_y_pixelso2 = Coords.y_pixelso2;
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

    // Fog.applyFog is a no-op only with no model loaded.
    this.saved_manager = ContextManager.getNodeManager();
    ContextManager.setNodeManager(null);
    this.saved_depth_is_relative =
        DeepObjectColourCalculator.depth_is_relative;
    DeepObjectColourCalculator.depth_is_relative = false;

    // Deterministic smooth shading: no sheen, no shadows, no sparkle.
    this.saved_glossiness = RendererDelegator.glossiness;
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_specular = RendererDelegator.specular;
    RendererDelegator.glossiness = 0;
    RendererDelegator.shadows = false;
    RendererDelegator.specular = 0;

    this.saved_antialiasing = RendererDelegator.antialiasing;
  }

  @AfterEach
  public void tearDown() {
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = this.saved_x_pixelso2;
    Coords.y_pixelso2 = this.saved_y_pixelso2;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
    ContextManager.setNodeManager(this.saved_manager);
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;
    RendererDelegator.glossiness = this.saved_glossiness;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.specular = this.saved_specular;
    RendererDelegator.antialiasing = this.saved_antialiasing;
  }

  /**
   * One large white sphere; pixel radius ~= world_radius / divisor.
   */
  private BVH sphereScene() {
    final double radius = 60.0 * 192;
    final Primitive[] primitives = new Primitive[] { new RTSphere(
        CENTRE_X << Coords.shift, CENTRE_Y << Coords.shift, 0.0, radius,
        0xFFFFFF) };
    return new BVH(primitives);
  }

  private int[] render(int aa) {
    RendererDelegator.antialiasing = aa;
    final int[] pixels = new int[SIZE * SIZE];
    Raytracer.renderTile(0, 0, SIZE, SIZE, new RayCamera(), sphereScene(),
        pixels);
    return pixels;
  }

  private static int backgroundRgb() {
    return 0xFF000000 | RendererDelegator.color_background_number;
  }

  private static int channel(int rgb, int shift) {
    return (rgb >> shift) & 0xFF;
  }

  @Test
  public void flatRegionsAreUnchangedBy2x2() {
    final int[] plain = render(1);
    final int[] aa = render(2);

    // Background: no sub-ray can hit, so the average is exact.
    assertEquals(backgroundRgb(), plain[0]);
    assertEquals(plain[0], aa[0]);

    // Deep interior: sub-rays land within a pixel of the 1x1 ray on
    // smoothly shaded surface; fog and diffuse barely vary.
    final int centre = CENTRE_Y * SIZE + CENTRE_X;
    assertNotEquals(backgroundRgb(), plain[centre]);
    for (final int shift : new int[] { 16, 8, 0 }) {
      final int delta = Math.abs(
          channel(plain[centre], shift) - channel(aa[centre], shift));
      assertTrue(delta <= 3,
          "interior channel drifted by " + delta + " at 2x2");
    }
  }

  @Test
  public void twoByTwoSoftensTheSilhouette() {
    final int[] plain = render(1);
    final int[] aa = render(2);

    // Interior reference: fully covered, smoothly lit.
    final int interior = plain[CENTRE_Y * SIZE + CENTRE_X];
    assertNotEquals(backgroundRgb(), interior);

    // Some pixel must be a strict blend: every channel strictly between
    // the background and the fully-lit interior -- impossible for a
    // single ray, only for averaged sub-rays straddling the edge.
    boolean found = false;
    for (int i = 0; i < aa.length; i++) {
      final int rgb = aa[i];
      boolean blend = true;
      for (final int shift : new int[] { 16, 8, 0 }) {
        final int c = channel(rgb, shift);
        if (c <= channel(backgroundRgb(), shift)
            || c >= channel(interior, shift)) {
          blend = false;
          break;
        }
      }
      if (blend) {
        found = true;
        assertNotEquals(plain[i], rgb,
            "2x2 left a silhouette pixel untouched at index " + i);
        break;
      }
    }
    assertTrue(found, "2x2 produced no blended silhouette pixel");
  }

  @Test
  public void threeByThreeRendersABlend() {
    RendererDelegator.antialiasing = 3;
    final int[] pixels = new int[SIZE * SIZE];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(0, 0, SIZE, SIZE, new RayCamera(), sphereScene(),
        pixels, stats);

    assertEquals(backgroundRgb(), pixels[0]);

    final int interior = pixels[CENTRE_Y * SIZE + CENTRE_X];
    assertNotEquals(backgroundRgb(), interior);

    boolean found = false;
    for (final int rgb : pixels) {
      boolean blend = true;
      for (final int shift : new int[] { 16, 8, 0 }) {
        final int c = channel(rgb, shift);
        if (c <= channel(backgroundRgb(), shift)
            || c >= channel(interior, shift)) {
          blend = false;
          break;
        }
      }
      if (blend) {
        found = true;
        break;
      }
    }
    assertTrue(found, "3x3 produced no blended silhouette pixel");

    // Partially covered pixels still count as hits for the overlay.
    assertTrue(stats.hits > 0, "no hits recorded at 3x3");
  }
}
