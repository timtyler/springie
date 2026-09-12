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

/**
 * Raytracer.renderTile's HitStats must report which tile-local pixels hit
 * geometry, for the "show active bins" overlay. Coordinates are tile-local:
 * the caller adds the tile origin for screen coordinates.
 */
public class RaytracerHitStatsTest {
  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private NodeManager saved_manager;

  private boolean saved_depth_is_relative;

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
    ContextManager.setNodeManager(new NodeManager());
    this.saved_depth_is_relative =
        DeepObjectColourCalculator.depth_is_relative;
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
  }

  private BVH sphereOnCentreRay() {
    final RayCamera camera = new RayCamera();
    final Ray ray = new Ray();
    camera.makeRay(100, 100, ray);
    final double t = 300000.0;
    final Primitive[] primitives = new Primitive[] {
        new RTSphere(ray.ox + ray.dx * t, ray.oy + ray.dy * t,
            ray.oz + ray.dz * t, 8000.0, 0xFFFFFF) };
    return new BVH(primitives);
  }

  @Test
  public void emptySceneHasNoHits() {
    final BVH bvh = new BVH(new Primitive[0]);
    final int[] pixels = new int[200 * 200];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(0, 0, 200, 200, new RayCamera(), bvh, pixels,
        stats);
    assertEquals(0, stats.hits);
  }

  @Test
  public void sphereHitBoxContainsCentrePixel() {
    final int[] pixels = new int[200 * 200];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(0, 0, 200, 200, new RayCamera(),
        sphereOnCentreRay(), pixels, stats);
    assertTrue(stats.hits > 0, "expected the sphere to be hit");
    assertTrue(stats.min_x <= 100 && 100 <= stats.max_x,
        "bbox should span the centre pixel horizontally: [" + stats.min_x
            + ", " + stats.max_x + "]");
    assertTrue(stats.min_y <= 100 && 100 <= stats.max_y,
        "bbox should span the centre pixel vertically: [" + stats.min_y
            + ", " + stats.max_y + "]");
    assertTrue(stats.min_x >= 0 && stats.max_x < 200);
    assertTrue(stats.min_y >= 0 && stats.max_y < 200);
  }

  @Test
  public void statsAreTileLocal() {
    // The sphere projects to screen pixel (100, 100); the tile starts at
    // (60, 60), so the hit box must be reported around (40, 40).
    final int[] pixels = new int[80 * 80];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(60, 60, 80, 80, new RayCamera(),
        sphereOnCentreRay(), pixels, stats);
    assertTrue(stats.hits > 0, "expected the sphere to be hit");
    assertTrue(stats.min_x <= 40 && 40 <= stats.max_x,
        "bbox should be tile-local: [" + stats.min_x + ", " + stats.max_x
            + "]");
    assertTrue(stats.min_y <= 40 && 40 <= stats.max_y,
        "bbox should be tile-local: [" + stats.min_y + ", " + stats.max_y
            + "]");
    assertTrue(stats.max_x < 80 && stats.max_y < 80);
  }

  @Test
  public void nullStatsDisablesTracking() {
    // The old signature must keep working and render identically to the
    // tracking overload: no tracking, no crash, same pixels.
    final int[] pixels_old = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, new RayCamera(),
        sphereOnCentreRay(), pixels_old);
    final int[] pixels_new = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, new RayCamera(),
        sphereOnCentreRay(), pixels_new, new Raytracer.HitStats());
    for (int i = 0; i < pixels_old.length; i++) {
      assertEquals(pixels_old[i], pixels_new[i], "pixel " + i);
    }
  }
}
