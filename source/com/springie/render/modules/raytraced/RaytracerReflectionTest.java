// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

/**
 * Glossiness and max bounces through the full tile pipeline.
 *
 * <p>The centre pixel's primary ray runs straight down +z from the eye
 * (25600, 25600, -196608), so a sphere centred on (25600, 25600, 0) is
 * hit exactly at its near pole: the mirror reflection then runs straight
 * back towards the camera. No model is loaded, so Fog.applyFog is a
 * no-op and every expected value is exact.
 */
public class RaytracerReflectionTest {
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

  private int saved_max_bounces;

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
    this.saved_depth_is_relative =
        DeepObjectColourCalculator.depth_is_relative;

    this.saved_glossiness = RendererDelegator.glossiness;
    this.saved_max_bounces = RendererDelegator.max_bounces;
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
    RendererDelegator.max_bounces = this.saved_max_bounces;
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
  public void matteSurfacesIgnoreTheBounceLimit() {
    RendererDelegator.glossiness = 0;

    RendererDelegator.max_bounces = 0;
    final int no_bounces = centrePixel(whiteSphere());

    RendererDelegator.max_bounces = 4;
    final int four_bounces = centrePixel(whiteSphere());

    assertEquals(no_bounces, four_bounces,
        "0% gloss must render pure diffuse whatever the bounce limit");
  }

  @Test
  public void fullGlossShowsTheBackgroundWhenNothingIsBehind() {
    RendererDelegator.glossiness = 100;
    RendererDelegator.max_bounces = 1;

    assertEquals(Raytracer.BACKGROUND_RGB, centrePixel(whiteSphere()),
        "a perfect mirror facing empty space must show the background");
  }

  @Test
  public void zeroBouncesDisablesReflection() {
    RendererDelegator.glossiness = 100;
    RendererDelegator.max_bounces = 0;
    final int bounce_capped = centrePixel(whiteSphere());

    RendererDelegator.glossiness = 0;
    RendererDelegator.max_bounces = 4;
    final int matte = centrePixel(whiteSphere());

    assertEquals(matte, bounce_capped,
        "max bounces 0 must render pure diffuse even at 100% gloss");
  }

  @Test
  public void halfGlossBlendsDiffuseWithReflection() {
    RendererDelegator.glossiness = 0;
    final int diffuse = centrePixel(whiteSphere());

    RendererDelegator.glossiness = 50;
    RendererDelegator.max_bounces = 1;
    final int blended = centrePixel(whiteSphere());

    final int bg = Raytracer.BACKGROUND_RGB;
    final int expected = 0xFF000000
        | ((((diffuse >> 16) & 0xFF) + ((bg >> 16) & 0xFF)) / 2 << 16)
        | ((((diffuse >> 8) & 0xFF) + ((bg >> 8) & 0xFF)) / 2 << 8)
        | (((diffuse & 0xFF) + (bg & 0xFF)) / 2);

    assertEquals(expected, blended,
        "50% gloss must average the diffuse and reflected colours");
  }

  /**
   * The white sphere's mirror ray runs back past the camera and meets a
   * red sphere parked behind it. One bounce shows the red sphere's
   * diffuse colour; the second bounce looks back at the white sphere and
   * must reproduce its matte colour exactly.
   */
  @Test
  public void secondBounceSeesAroundTheCamera() {
    final Primitive[] primitives = new Primitive[] {
        new RTSphere(EX, EY, 0.0, 20000.0, 0xFFFFFF),
        new RTSphere(EX, EY, -250000.0, 20000.0, 0xFF0000) };
    RendererDelegator.glossiness = 100;

    RendererDelegator.max_bounces = 0;
    final int matte = centrePixel(primitives);

    RendererDelegator.max_bounces = 1;
    final int one_bounce = centrePixel(primitives);

    RendererDelegator.max_bounces = 2;
    final int two_bounces = centrePixel(primitives);

    assertNotEquals(matte, one_bounce,
        "one bounce must show the red sphere behind the camera");
    assertEquals(matte, two_bounces,
        "the second bounce must land back on the white sphere's near pole");
  }
}
