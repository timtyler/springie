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
 * The selection ring through the full tile pipeline: a selected node
 * renders its own colour with a flat red billboard ring around it --
 * the ray-traced version of the default renderer's screen-space
 * selection circle.
 *
 * <p>Geometry mirrors RaytracerGlossTest: the centre pixel's ray runs
 * straight down +z from the eye (25600, 25600, -196608), so pixel
 * (100 + k, 100) pierces the ring plane k * 192 world units off axis.
 * The sphere has radius 512; the ring spans 512 + 5*192 = 1472 to
 * 512 + 7*192 = 1856, so pixel (108, 100) at 1536 is ring, and pixel
 * (120, 100) at 3840 is background.
 */
public class RaytracerSelectionRingTest {
  private static final double CX = 100 << Coords.shift;

  private static final double CY = 100 << Coords.shift;

  private static final double RADIUS = 512.0;

  private static final double WORLD_PER_PIXEL = 192.0;

  private static final int RING_RED = 0xFFFF4040;

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

  private boolean saved_specular_enabled;

  private boolean saved_fresnel_enabled;

  private boolean saved_fill_light_enabled;

  private int saved_antialiasing;

  private boolean saved_scenic_background;

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
    this.saved_specular_enabled = RendererDelegator.specular_enabled;
    this.saved_fresnel_enabled = RendererDelegator.fresnel_enabled;
    this.saved_fill_light_enabled = RendererDelegator.fill_light_enabled;
    this.saved_antialiasing = RendererDelegator.antialiasing;
    this.saved_scenic_background = RendererDelegator.scenic_background;

    RendererDelegator.glossiness_enabled = false;
    RendererDelegator.shadows = false;
    RendererDelegator.specular_enabled = false;
    RendererDelegator.fresnel_enabled = false;
    RendererDelegator.fill_light_enabled = false;
    RendererDelegator.antialiasing = 1;
    RendererDelegator.scenic_background = false;
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
    RendererDelegator.specular_enabled = this.saved_specular_enabled;
    RendererDelegator.fresnel_enabled = this.saved_fresnel_enabled;
    RendererDelegator.fill_light_enabled = this.saved_fill_light_enabled;
    RendererDelegator.antialiasing = this.saved_antialiasing;
    RendererDelegator.scenic_background = this.saved_scenic_background;
  }

  private int[] renderWithRing() {
    final Primitive[] primitives = {
        new RTSphere(CX, CY, 0.0, RADIUS, 0x336699) };
    final RTRing[] rings = { new RTRing(CX, CY, 0.0, 0, 0, 1,
        RADIUS + 5.0 * WORLD_PER_PIXEL, RADIUS + 7.0 * WORLD_PER_PIXEL,
        0xFF4040) };
    final BVH bvh = new BVH(primitives);
    final RayCamera camera = new RayCamera();
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, rings, pixels, null);
    return pixels;
  }

  @Test
  public void ringPixelIsFlatRed() {
    // Unlit: the ring renders in its flat red at full strength, with
    // no diffuse shading -- exactly 0xFFFF4040 (fog is a no-op here).
    assertEquals(RING_RED, renderWithRing()[100 * 200 + 108],
        "the annulus pixel must be the flat selection red");
  }

  @Test
  public void nodeBodyKeepsItsOwnColour() {
    // Like the default renderer, the selected node keeps its class
    // colour; only the ring marks the selection.
    final int centre = renderWithRing()[100 * 200 + 100];
    assertNotEquals(RING_RED, centre,
        "the node body must not be painted selection red");
    assertNotEquals(0xFF000000, centre,
        "the centre pixel must hit the node, not the background");
  }

  @Test
  public void ringIsARingNotADisc() {
    // Past the outer edge the ray misses the ring: background, not red.
    assertNotEquals(RING_RED, renderWithRing()[100 * 200 + 120],
        "outside the annulus there must be no red");
  }
}
