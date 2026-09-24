// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

/**
 * Golden checksums of rendered tiles across the feature matrix: every
 * ray-tracer cleanup must leave the pixels bit-identical. The scene
 * exercises every primitive type with shadows, gloss, specular,
 * fresnel and fill light all on.
 *
 * <p>Run once to print the checksums, then bake them into the
 * EXPECTED_* constants below.
 */
public class RaytracerGoldenChecksumTest {
  private static final int SIZE = 200;

  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_x_pixelso2;

  private int saved_y_pixelso2;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private NodeManager saved_manager;

  private boolean saved_depth_is_relative;

  private boolean saved_scenic_background;

  private boolean saved_glossiness_enabled;

  private int saved_glossiness;

  private boolean saved_shadows;

  private boolean saved_specular_enabled;

  private int saved_specular;

  private boolean saved_fresnel_enabled;

  private int saved_fresnel;

  private boolean saved_fill_light_enabled;

  private int saved_fill_light;

  private int saved_antialiasing;

  private int saved_pixellation;

  // Golden FNV-1a checksums, printed by a first run and baked in.
  private static final long EXPECTED_AA1_PX1 = 3939426083293932359L;

  private static final long EXPECTED_AA2_PX1 = 2253347649951664564L;

  private static final long EXPECTED_AA4_PX1 = 3657819474376892389L;

  private static final long EXPECTED_AA1_PX2 = 8394894569135578073L;

  private static final long EXPECTED_AA2_PX2 = -3565213677650432699L;

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

    // Every shading feature on, so the checksum covers them all.
    this.saved_scenic_background = RendererDelegator.scenic_background;
    this.saved_glossiness_enabled = RendererDelegator.glossiness_enabled;
    this.saved_glossiness = RendererDelegator.glossiness;
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_specular_enabled = RendererDelegator.specular_enabled;
    this.saved_specular = RendererDelegator.specular;
    this.saved_fresnel_enabled = RendererDelegator.fresnel_enabled;
    this.saved_fresnel = RendererDelegator.fresnel;
    this.saved_fill_light_enabled = RendererDelegator.fill_light_enabled;
    this.saved_fill_light = RendererDelegator.fill_light;
    this.saved_antialiasing = RendererDelegator.antialiasing;
    this.saved_pixellation = RendererDelegator.pixellation;

    RendererDelegator.scenic_background = false;
    RendererDelegator.glossiness_enabled = true;
    RendererDelegator.glossiness = 50;
    RendererDelegator.shadows = true;
    RendererDelegator.specular_enabled = true;
    RendererDelegator.specular = 100;
    RendererDelegator.fresnel_enabled = true;
    RendererDelegator.fresnel = 50;
    RendererDelegator.fill_light_enabled = true;
    RendererDelegator.fill_light = 50;
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
    RendererDelegator.scenic_background = this.saved_scenic_background;
    RendererDelegator.glossiness_enabled = this.saved_glossiness_enabled;
    RendererDelegator.glossiness = this.saved_glossiness;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.specular_enabled = this.saved_specular_enabled;
    RendererDelegator.specular = this.saved_specular;
    RendererDelegator.fresnel_enabled = this.saved_fresnel_enabled;
    RendererDelegator.fresnel = this.saved_fresnel;
    RendererDelegator.fill_light_enabled = this.saved_fill_light_enabled;
    RendererDelegator.fill_light = this.saved_fill_light;
    RendererDelegator.antialiasing = this.saved_antialiasing;
    RendererDelegator.pixellation = this.saved_pixellation;
  }

  private BVH mixedScene() {
    final double s = 1 << Coords.shift;
    final Primitive[] primitives = new Primitive[] {
        new RTSphere(140 * s, 100 * s, 0.0, 60.0 * 192, 0xD0D0D0),
        new RTEllipsoid(60 * s, 150 * s, 0.0, 140 * s, 60 * s, 0.0,
            8.0 * 192, 0xC04040),
        new RTCylinder(40 * s, 40 * s, 0.0, 160 * s, 40 * s, 0.0,
            4.0 * 192, 0x40C040),
        new RTTriangle(60 * s, 120 * s, 0.0, 120 * s, 180 * s, 0.0,
            180 * s, 120 * s, 0.0, 0x4040C0), };
    return new BVH(primitives);
  }

  private static long checksum(int[] pixels) {
    long hash = 0xCBF29CE484222325L;
    for (int i = 0; i < pixels.length; i++) {
      hash ^= pixels[i] & 0xFFFFFFFFL;
      hash *= 0x100000001B3L;
    }
    return hash;
  }

  private long renderChecksum(int aa, int px) {
    RendererDelegator.antialiasing = aa;
    RendererDelegator.pixellation = px;
    final int[] pixels = new int[SIZE * SIZE];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(0, 0, SIZE, SIZE, new RayCamera(), mixedScene(),
        pixels, stats);
    final long sum = checksum(pixels);
    System.out.println(
        "golden aa=" + aa + " px=" + px + " checksum=" + sum + "L");
    return sum;
  }

  @Test
  public void goldenChecksumsAreStable() {
    assertEquals(EXPECTED_AA1_PX1, renderChecksum(1, 1));
    assertEquals(EXPECTED_AA2_PX1, renderChecksum(2, 1));
    assertEquals(EXPECTED_AA4_PX1, renderChecksum(4, 1));
    assertEquals(EXPECTED_AA1_PX2, renderChecksum(1, 2));
    assertEquals(EXPECTED_AA2_PX2, renderChecksum(2, 2));
  }
}
