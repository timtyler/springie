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
 * Renders a tile containing a single sphere through the full pipeline and
 * checks the sphere appears where the default renderer's projection says
 * it should.
 */
public class RaytracerPipelineTest {
  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private NodeManager saved_manager;

  private boolean saved_depth_is_relative;

  private int saved_glossiness;

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

    // These tests pin the diffuse pipeline; reflections are covered by
    // RaytracerReflectionTest.
    this.saved_glossiness = RendererDelegator.glossiness;
    RendererDelegator.glossiness = 0;
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
  }

  /**
   * Depth fog must be graded by world z (the depth axis), not by screen
   * height. Two identical white spheres on the same pixel ray, one twice
   * as far down the ray: same diffuse term, so any brightness difference
   * is fog. Catches passing ray.oy + ray.dy * t as the fog coordinate.
   */
  @Test
  public void fogDarkensWithDepthNotHeight() {
    ContextManager.setNodeManager(new NodeManager());
    // Absolute fog: depth = z_pixels - ((z >> shift) * factor >> 10),
    // a pure function of world z.
    DeepObjectColourCalculator.depth_is_relative = false;

    final int near_brightness = centreBrightnessAtDistance(300000.0);
    final int far_brightness = centreBrightnessAtDistance(600000.0);

    assertTrue(far_brightness < near_brightness,
        "far sphere should be fogged darker: near=" + near_brightness
            + " far=" + far_brightness);
  }

  private int centreBrightnessAtDistance(double t) {
    final RayCamera camera = new RayCamera();
    final Ray ray = new Ray();
    camera.makeRay(100, 100, ray);
    final double cx = ray.ox + ray.dx * t;
    final double cy = ray.oy + ray.dy * t;
    final double cz = ray.oz + ray.dz * t;
    final Primitive[] primitives = new Primitive[] {
        new RTSphere(cx, cy, cz, 8000.0, 0xFFFFFF) };
    final BVH bvh = new BVH(primitives);
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);
    final int centre = pixels[100 * 200 + 100];
    assertTrue(centre != Raytracer.BACKGROUND_RGB,
        "sphere at t=" + t + " missed the centre pixel");
    return centre & 0xFFFFFF;
  }

  @Test
  public void sphereRendersAtProjectedPosition() {
    // World point projecting exactly onto pixel (100, 100).
    final double wx = 100 << Coords.shift;
    final double wy = 100 << Coords.shift;
    final Primitive[] primitives = new Primitive[] {
        new RTSphere(wx, wy, 0.0, 3840.0, 0xFFFFFF) };
    final BVH bvh = new BVH(primitives);
    final RayCamera camera = new RayCamera();

    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);

    // Sphere radius 3840 at depth divisor 192 covers ~20 pixels.
    final int centre = pixels[100 * 200 + 100];
    assertNotEquals(Raytracer.BACKGROUND_RGB, centre);
    // Lit from the front-ish: should be bright, not a dark smudge.
    // (Unsigned comparison: the pixel carries an opaque alpha.)
    assertTrue((centre & 0xFFFFFF) > 0x808080, "centre pixel too dark: "
        + Integer.toHexString(centre));

    // Well outside the sphere: background.
    assertEquals(Raytracer.BACKGROUND_RGB, pixels[0]);
    assertEquals(Raytracer.BACKGROUND_RGB, pixels[199 * 200 + 199]);

    // Roughly circular: symmetric pixels around the centre are shaded.
    assertNotEquals(Raytracer.BACKGROUND_RGB, pixels[100 * 200 + 110]);
    assertNotEquals(Raytracer.BACKGROUND_RGB, pixels[110 * 200 + 100]);
  }

  @Test
  public void emptySceneRendersBackground() {
    final BVH bvh = new BVH(new Primitive[0]);
    final int[] pixels = new int[64 * 64];
    Raytracer.renderTile(10, 10, 64, 64, new RayCamera(), bvh, pixels);
    for (final int pixel : pixels) {
      assertEquals(Raytracer.BACKGROUND_RGB, pixel);
    }
  }
}
