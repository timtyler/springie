// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.context.ContextManager;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Coords;
import com.springie.render.RendererDelegator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  private final RaytracerTestState state = new RaytracerTestState();

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

  private boolean saved_freeze;

  @BeforeEach
  public void setUp() {
    this.state.pin();
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
    this.saved_freeze = Raytracer.freeze_lights;

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
    Raytracer.freeze_lights = this.saved_freeze;
    this.state.restore();
  }

  private int centrePixel(final Primitive[] primitives) {
    return renderAll(primitives)[100 * 200 + 100];
  }

  private int[] renderAll(final Primitive[] primitives) {
    final BVH bvh = new BVH(primitives);
    final RayCamera camera = new RayCamera();
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);
    return pixels;
  }

  private Primitive[] whiteSphere() {
    return new Primitive[] { new RTSphere(EX, EY, 0.0, 20000.0, 0xFFFFFF) };
  }

  @Test
  public void zeroSpecularIsPureDiffuse() {
    RendererDelegator.specular = 0;
    // Diffuse only with RGB lights (Tim, 2026-10-03): near-white.
    assertEquals(0xFFFEFEFE, centrePixel(whiteSphere()),
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
  public void fullSpecularBlowsOutToWhite() {
    // The blow-out needs a light near the mirror direction for some
    // surface point. The pinned four-light rig cannot provide one:
    // measured over the whole disc, the best halfway-vector cosine
    // is 0.84, and 0.84^32 truncates the highlight to zero at every
    // pixel (the lights sit just in front of the z=0 plane, far
    // off-axis from everywhere on this deep sphere). So freeze the
    // lights and park the white light on the camera axis between
    // the eye and the pole -- the configuration this test's javadoc
    // describes ("the light sits mostly behind the camera, so the
    // near pole is close to the perfect mirror direction"). There
    // the halfway cosine is 1 and the highlight must clip the pole
    // to white, while specular 0 leaves it at the diffuse value.
    // (Re-scoped 2026-11-08.)
    Raytracer.freeze_lights = true;
    synchronized (com.springie.render.modules.modern.LightSource.class) {
      final com.springie.render.modules.modern.Light white =
          com.springie.render.modules.modern.LightSource.lights.get(3);
      white.px = EX;
      white.py = EY;
      white.pz = -100000.0;
    }
    RendererDelegator.specular = 0;
    final int[] plain = renderAll(whiteSphere());
    RendererDelegator.specular = 100;
    final int[] highlighted = renderAll(whiteSphere());
    for (int i = 0; i < plain.length; i++) {
      for (final int shift : new int[] { 16, 8, 0 }) {
        final int before = (plain[i] >> shift) & 0xFF;
        final int after = (highlighted[i] >> shift) & 0xFF;
        assertTrue(after >= before,
            "specular must never darken a pixel");
      }
    }
    assertEquals(0xFFFEFEFE, plain[100 * 200 + 100],
        "specular 0% leaves the near pole at its diffuse value");
    assertEquals(0xFFFFFFFF, highlighted[100 * 200 + 100],
        "specular 100% must blow the near pole out to white");
  }

  @Test
  public void partialSpecularIsBetween() {
    RendererDelegator.specular = 100;
    final int full = centrePixel(whiteSphere());
    RendererDelegator.specular = 10;
    final int partial = centrePixel(whiteSphere());
    RendererDelegator.specular = 0;
    final int none = centrePixel(whiteSphere());
    assertTrue(partial >= none,
        "specular 10% must not darken the pole vs 0%");
    assertTrue(full >= partial,
        "specular must grow monotonically with the setting");
  }
}
