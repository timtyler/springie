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
 * The glossy sheen through the full tile pipeline.
 *
 * <p>The centre pixel's primary ray runs straight down +z from the eye
 * (25600, 25600, -196608), so a sphere centred on (25600, 25600, 0) is
 * hit exactly at its near pole. No model is loaded, so Fog.applyFog is
 * a no-op and every expected value is exact.
 *
 * <p>The sheen is pure shading math on the surface normal -- it must be
 * smooth. The smoothness test scatters small bright spheres where a
 * mirror ray would have hit them chaotically (the old implementation
 * speckled here); the sheen must not care.
 */
public class RaytracerGlossTest {
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

  private boolean saved_glossiness_enabled;

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
    this.saved_glossiness_enabled = RendererDelegator.glossiness_enabled;
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_specular = RendererDelegator.specular;

    RendererDelegator.glossiness_enabled = true;
    RendererDelegator.shadows = false;
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
    RendererDelegator.glossiness_enabled = this.saved_glossiness_enabled;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.specular = this.saved_specular;
  }

  private int[] renderTile(Primitive[] primitives) {
    final BVH bvh = new BVH(primitives);
    final RayCamera camera = new RayCamera();
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);
    return pixels;
  }

  private Primitive[] greySphere() {
    return new Primitive[] { new RTSphere(EX, EY, 0.0, 20000.0, 0x808080) };
  }

  @Test
  public void zeroGlossIsPureDiffuse() {
    RendererDelegator.glossiness = 0;
    // Diffuse only: |normal . light| = 0.85092 at the near pole,
    // scaled = 236, (128 * 236) >> 8 = 118.
    assertEquals(0xFF767676, renderTile(greySphere())[100 * 200 + 100],
        "gloss 0% must leave the diffuse picture untouched");
  }

  @Test
  public void fullGlossRollsOffSoftly() {
    RendererDelegator.glossiness = 100;
    // The broad sheen at the near pole adds ~187 per channel to the
    // 118 diffuse; the soft rolloff asymptotes instead of clipping:
    // 255 - (255 - 118) * 255 / (255 + 187) = 176.
    assertEquals(0xFFB0B0B0, renderTile(greySphere())[100 * 200 + 100],
        "gloss 100% must roll the sheen off softly at the near pole");
  }

  @Test
  public void sheenGrowsMonotonically() {
    final int[] levels = new int[4];
    final int[] settings = { 0, 10, 50, 100 };
    for (int i = 0; i < settings.length; i++) {
      RendererDelegator.glossiness = settings[i];
      levels[i] = renderTile(greySphere())[100 * 200 + 100];
    }
    for (int i = 1; i < levels.length; i++) {
      assertTrue(levels[i] > levels[i - 1],
          "the sheen must grow with the gloss setting");
    }
  }

  @Test
  public void disabledGlossAddsNothing() {
    RendererDelegator.glossiness = 0;
    final int plain = renderTile(greySphere())[100 * 200 + 100];
    RendererDelegator.glossiness_enabled = false;
    RendererDelegator.glossiness = 100;
    assertEquals(plain, renderTile(greySphere())[100 * 200 + 100],
        "a disabled gloss must add nothing, even at 100% strength");
    RendererDelegator.glossiness_enabled = true;
  }

  @Test
  public void sheenIsSmoothInABusyScene() {
    // Small bright spheres parked between the camera and the grey
    // sphere, off the centre axis: a mirror ray from the central disc
    // would strike them chaotically, but the sheen must not notice.
    final Primitive[] scene = new Primitive[] {
        new RTSphere(EX, EY, 0.0, 20000.0, 0x808080),
        new RTSphere(EX + 40000.0, EY + 10000.0, -100000.0, 5000.0,
            0xFFFF00),
        new RTSphere(EX - 35000.0, EY - 20000.0, -100000.0, 5000.0,
            0xFFFF00),
        new RTSphere(EX + 15000.0, EY - 45000.0, -90000.0, 5000.0,
            0xFFFF00),
        new RTSphere(EX - 10000.0, EY + 40000.0, -90000.0, 5000.0,
            0xFFFF00) };
    RendererDelegator.glossiness = 100;
    final int[] pixels = renderTile(scene);
    int worst = 0;
    for (int y = 80; y < 120; y++) {
      for (int x = 80; x < 120; x++) {
        final int p = pixels[y * 200 + x];
        final int right = pixels[y * 200 + x + 1];
        final int down = pixels[(y + 1) * 200 + x];
        worst = Math.max(worst, channelDiff(p, right));
        worst = Math.max(worst, channelDiff(p, down));
      }
    }
    assertTrue(worst < 16,
        "adjacent pixels in the sheen must differ by less than 16 per"
            + " channel, but differed by " + worst);
  }

  private static int channelDiff(int a, int b) {
    int worst = 0;
    for (int shift = 0; shift < 24; shift += 8) {
      final int diff = Math.abs(((a >> shift) & 0xFF)
          - ((b >> shift) & 0xFF));
      worst = Math.max(worst, diff);
    }
    return worst;
  }
}
