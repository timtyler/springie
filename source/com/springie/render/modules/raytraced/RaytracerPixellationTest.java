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
 * Pixellation (RendererDelegator.pixellation): one shade per n-by-n
 * screen block, replicated across the block. With anti-aliasing off the
 * pixellated image is exactly the 1x1 image subsampled and replicated;
 * with it on, the sub-pixel rays spread across the whole block.
 */
public class RaytracerPixellationTest {
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

  private int saved_pixellation;

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
    this.saved_pixellation = RendererDelegator.pixellation;
    RendererDelegator.antialiasing = 1;
    RendererDelegator.pixellation = 1;
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
    RendererDelegator.pixellation = this.saved_pixellation;
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

  private int[] render(int px, int aa) {
    RendererDelegator.pixellation = px;
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
  public void twoByTwoIsExactlySubsampledAndReplicated() {
    final int[] plain = render(1, 1);
    final int[] pix = render(2, 1);

    for (int y = 0; y < SIZE; y++) {
      for (int x = 0; x < SIZE; x++) {
        final int bx = (x / 2) * 2;
        final int by = (y / 2) * 2;
        assertEquals(plain[by * SIZE + bx], pix[y * SIZE + x],
            "pixel differed at " + x + "," + y);
      }
    }
  }

  @Test
  public void fourByFourBlocksAreUniform() {
    final int[] pix = render(4, 1);

    for (int by = 0; by < SIZE; by += 4) {
      for (int bx = 0; bx < SIZE; bx += 4) {
        final int colour = pix[by * SIZE + bx];
        for (int y = by; y < by + 4; y++) {
          for (int x = bx; x < bx + 4; x++) {
            assertEquals(colour, pix[y * SIZE + x],
                "block not uniform at " + x + "," + y);
          }
        }
      }
    }
  }

  @Test
  public void blocksAlignToScreenCoordinates() {
    final int[] full = render(2, 1);

    // Tile origin not a multiple of the block size: the blocks must
    // still line up with the full-frame render, with no seams.
    RendererDelegator.pixellation = 2;
    RendererDelegator.antialiasing = 1;
    final int tile_w = 60;
    final int tile_h = 60;
    final int[] tile = new int[tile_w * tile_h];
    Raytracer.renderTile(7, 0, tile_w, tile_h, new RayCamera(),
        sphereScene(), tile);

    for (int y = 0; y < tile_h; y++) {
      for (int x = 0; x < tile_w; x++) {
        assertEquals(full[y * SIZE + 7 + x], tile[y * tile_w + x],
            "tile pixel differed at " + x + "," + y);
      }
    }
  }

  @Test
  public void hitStatsCoverWholeBlocks() {
    // The pixellated path shades the same ray as the 1x1 path's
    // top-left block pixel, so the 1x1 render pins exactly which
    // blocks hit.
    final int[] plain = render(1, 1);

    RendererDelegator.pixellation = 4;
    RendererDelegator.antialiasing = 1;
    final int[] pixels = new int[SIZE * SIZE];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(0, 0, SIZE, SIZE, new RayCamera(), sphereScene(),
        pixels, stats);

    assertTrue(stats.hits > 0, "sphere scene should hit some blocks");

    final int bg = backgroundRgb();
    int exp_min_x = SIZE;
    int exp_min_y = SIZE;
    int exp_max_x = -1;
    int exp_max_y = -1;
    for (int by = 0; by < SIZE; by += 4) {
      for (int bx = 0; bx < SIZE; bx += 4) {
        // The pixellated block shades exactly the ray the 1x1 path
        // traces through the block's top-left pixel.
        final int shade = plain[by * SIZE + bx];
        for (int y = by; y < by + 4; y++) {
          for (int x = bx; x < bx + 4; x++) {
            assertEquals(shade, pixels[y * SIZE + x],
                "block not replicated at " + x + "," + y);
          }
        }
        if (shade != bg) {
          exp_min_x = Math.min(exp_min_x, bx);
          exp_min_y = Math.min(exp_min_y, by);
          exp_max_x = Math.max(exp_max_x, bx + 3);
          exp_max_y = Math.max(exp_max_y, by + 3);
        }
      }
    }
    assertEquals(exp_min_x, stats.min_x);
    assertEquals(exp_min_y, stats.min_y);
    assertEquals(exp_max_x, stats.max_x);
    assertEquals(exp_max_y, stats.max_y);
  }

  @Test
  public void antialiasingResolvesTheCoarsePixel() {
    final int[] aa1 = render(2, 1);
    final int[] aa2 = render(2, 2);

    // Interior reference: fully covered, smoothly lit.
    final int interior = render(1, 1)[CENTRE_Y * SIZE + CENTRE_X];
    assertNotEquals(backgroundRgb(), interior);

    // Find a block whose single top-left ray missed the sphere, but
    // whose 2x2-AA block is a strict blend: some sub-ray inside the
    // block hit, proving the samples spread across the whole block
    // rather than clustering at the top-left pixel.
    boolean found = false;
    for (int by = 0; by < SIZE && !found; by += 2) {
      for (int bx = 0; bx < SIZE; bx += 2) {
        if (aa1[by * SIZE + bx] != backgroundRgb()) {
          continue;
        }
        if (isBetween(aa2[by * SIZE + bx], backgroundRgb(), interior)) {
          found = true;
          break;
        }
      }
    }
    assertTrue(found,
        "2x2 pixellated + 2x2 AA: no block blended past its top-left ray");
  }

  private static boolean isBetween(int rgb, int lo, int hi) {
    for (final int shift : new int[] { 16, 8, 0 }) {
      final int c = channel(rgb, shift);
      if (c <= channel(lo, shift) || c >= channel(hi, shift)) {
        return false;
      }
    }
    return true;
  }
}
