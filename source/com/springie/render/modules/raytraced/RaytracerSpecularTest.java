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
 * Specular highlights through the full tile pipeline.
 *
 * <p>The centre pixel's primary ray runs straight down +z from the eye
 * (25600, 25600, -196608), so a sphere centred on (25600, 25600, 0) is
 * hit exactly at its near pole. The light sits mostly behind the camera,
 * so the near pole is close to the perfect mirror direction and carries
 * a strong highlight. No model is loaded, so Fog.applyFog is a no-op.
 */
public class RaytracerSpecularTest {
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

  private boolean saved_specular_enabled;

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
    this.saved_specular_enabled = RendererDelegator.specular_enabled;

    RendererDelegator.glossiness = 0;
    RendererDelegator.shadows = false;
    RendererDelegator.specular_enabled = true;
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
    RendererDelegator.specular_enabled = this.saved_specular_enabled;
  }

  private int centrePixel(Primitive[] primitives) {
    final BVH bvh = new BVH(primitives);
    final RayCamera camera = new RayCamera();
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);
    return pixels[100 * 200 + 100];
  }

  private Primitive[] whiteSphere() {
    return new Primitive[] { new RTSphere(EX, EY, 0.0, 20000.0, 0xFFFFFF) };
  }

  @Test
  public void zeroSpecularIsPureDiffuse() {
    RendererDelegator.specular = 0;
    // Diffuse only: |normal . light| = 0.85092 at the near pole,
    // scaled = 236, (255 * 236) >> 8 = 235.
    assertEquals(0xFFEBEBEB, centrePixel(whiteSphere()),
        "specular 0% must leave the diffuse picture untouched");
  }

  @Test
  public void disabledSpecularAddsNothing() {
    RendererDelegator.specular = 0;
    final int plain = centrePixel(whiteSphere());
    RendererDelegator.specular_enabled = false;
    RendererDelegator.specular = 100;
    assertEquals(plain, centrePixel(whiteSphere()),
        "a disabled specular must add nothing, even at 100% strength");
    RendererDelegator.specular_enabled = true;
  }

  @Test
  public void fullSpecularRollsOffSoftly() {
    RendererDelegator.specular = 100;
    // The highlight at the near pole adds ~73 per channel to the 235
    // diffuse; the soft rolloff asymptotes instead of clipping:
    // 255 - (255 - 235) * 255 / (255 + 73) = 240.
    assertEquals(0xFFF0F0F0, centrePixel(whiteSphere()),
        "specular 100% must roll the highlight off softly at the near pole");
  }

  @Test
  public void partialSpecularIsBetween() {
    RendererDelegator.specular = 100;
    final int full = centrePixel(whiteSphere());
    RendererDelegator.specular = 10;
    final int partial = centrePixel(whiteSphere());
    RendererDelegator.specular = 0;
    final int none = centrePixel(whiteSphere());
    assertTrue(partial > none,
        "specular 10% must brighten the pole over 0%");
    assertTrue(full >= partial,
        "specular must grow monotonically with the setting");
  }
}
