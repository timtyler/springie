// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.geometry.Vector3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.ScenicBackground;
import com.springie.render.modules.modern.LightSource;
import com.springie.render.modules.modern.RendererTileManager;
import java.awt.image.BufferedImage;

/**
 * Renders one bin tile, pixel by pixel. One primary ray per pixel, plus
 * optional shadow rays; specular highlights and the glossy sheen are pure
 * shading math, no extra rays.
 *
 * <p>Lighting shares the default renderer's configuration
 * (LightSource.source_1) and its feel: brightness runs from half to full
 * with |normal . light|, so nothing ever goes fully black.
 */
final class Raytracer {
  private static final double LIGHT_X;

  private static final double LIGHT_Y;

  private static final double LIGHT_Z;

  /** RGB lights (Tim, 2026-10-03): normalized directions. */
  private static final double RED_X, RED_Y, RED_Z;
  private static final double GREEN_X, GREEN_Y, GREEN_Z;
  private static final double BLUE_X, BLUE_Y, BLUE_Z;

  /** RGB point light positions (Tim, 2026-10-03): viewport-dependent. */
  static double RED_PX, RED_PY, RED_PZ;
  static double GREEN_PX, GREEN_PY, GREEN_PZ;
  static double BLUE_PX, BLUE_PY, BLUE_PZ;
  static double WHITE_PX, WHITE_PY, WHITE_PZ;

  /**
   * Test hook: when true, updateLightPositions() does not overwrite the
   * cached light positions, letting tests position lights manually.
   */
  static boolean freeze_lights = false;

  /** Distance falloff constant: intensity = 1/(1+(d/K)^2). */
  private static final double LIGHT_FALLOFF_K = 5120000.0;

  /**
   * RGB light brightness boost (Tim, 2026-10-03): the three colored
   * lights together should match the old white light's punch.
   */
  private static final double LIGHT_BRIGHTNESS = 4.0;

  /**
   * Ambient light base (Tim, 2026-10-03): from the Ambient light slider.
   * 50% = 96, matching the old hardcoded ambient.
   */
  private static int ambientBase() {
    return (int) (192.0 * RendererDelegator.ambient_light_pct / 100.0);
  }

  static {
    final Vector3D source = LightSource.source_1;
    final double length = Math.sqrt(source.x * source.x + source.y * source.y
        + source.z * source.z);
    LIGHT_X = source.x / length;
    LIGHT_Y = source.y / length;
    LIGHT_Z = source.z / length;

    // RGB lights: normalize each.
    final Vector3D red = LightSource.source_red;
    final double red_len = Math.sqrt(red.x * red.x + red.y * red.y + red.z * red.z);
    RED_X = red.x / red_len;
    RED_Y = red.y / red_len;
    RED_Z = red.z / red_len;
    final Vector3D green = LightSource.source_green;
    final double green_len = Math.sqrt(green.x * green.x + green.y * green.y + green.z * green.z);
    GREEN_X = green.x / green_len;
    GREEN_Y = green.y / green_len;
    GREEN_Z = green.z / green_len;
    final Vector3D blue = LightSource.source_blue;
    final double blue_len = Math.sqrt(blue.x * blue.x + blue.y * blue.y + blue.z * blue.z);
    BLUE_X = blue.x / blue_len;
    BLUE_Y = blue.y / blue_len;
    BLUE_Z = blue.z / blue_len;

    // Point light positions are viewport-dependent; set by
    // updateLightPositions() at the start of each tile render.
    // (Tim, 2026-10-03)
  }

  /**
   * Positions the RGB point lights based on the viewport dimensions
   * (Tim, 2026-10-03): near the top of the frame, equally spaced
   * across the width. Called at the start of each tile render.
   */
  private static void updateLightPositions() {
    if (freeze_lights) {
      return;
    }
    LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
    RED_PX = LightSource.red_px;
    RED_PY = LightSource.red_py;
    RED_PZ = LightSource.red_pz;
    GREEN_PX = LightSource.green_px;
    GREEN_PY = LightSource.green_py;
    GREEN_PZ = LightSource.green_pz;
    BLUE_PX = LightSource.blue_px;
    BLUE_PY = LightSource.blue_py;
    BLUE_PZ = LightSource.blue_pz;
    WHITE_PX = LightSource.white_px;
    WHITE_PY = LightSource.white_py;
    WHITE_PZ = LightSource.white_pz;
  }

  private Raytracer() {
    // ...
  }

  static void renderTile(final int x0, final int y0, final int width, final int height,
      RayCamera camera, final BVH bvh, final int[] pixels) {
    renderTile(x0, y0, width, height, camera, bvh, pixels, null);
  }

  /**
   * Per-tile hit statistics: which pixels hit geometry, for the
   * "show active bins" overlay. Coordinates are tile-local; the caller
   * adds the tile origin to get screen coordinates. A null stats
   * argument disables tracking.
   */
  static final class HitStats {
    int hits;

    int min_x = Integer.MAX_VALUE;

    int min_y = Integer.MAX_VALUE;

    int max_x = Integer.MIN_VALUE;

    int max_y = Integer.MIN_VALUE;

    void add(final int x, final int y) {
      this.hits++;
      if (x < this.min_x) {
        this.min_x = x;
      }
      if (x > this.max_x) {
        this.max_x = x;
      }
      if (y < this.min_y) {
        this.min_y = y;
      }
      if (y > this.max_y) {
        this.max_y = y;
      }
    }
  }

  static void renderTile(final int x0, final int y0, final int width, final int height,
      RayCamera camera, final BVH bvh, final int[] pixels, final HitStats stats) {
    renderTile(x0, y0, width, height, camera, bvh, NO_RINGS, pixels,
        stats);
  }

  /**
   * The selection rings live outside the BVH (they never cast shadows),
   * so they ride along as a side array; when nothing is selected it is
   * empty and the per-ray cost is a single length check.
   */
  private static final RTRing[] NO_RINGS = new RTRing[0];

  /**
   * The closest hit across the BVH scene and the selection rings. The
   * rings live outside the BVH so they can never cast shadows; the
   * shared Hit keeps whichever is closer.
   */
  private static boolean intersectScene(final Ray ray, final Hit hit, final BVH bvh,
      RTRing[] rings, final int[] stack) {
    boolean struck = bvh.intersect(ray, hit, stack);
    for (int i = 0; i < rings.length; i++) {
      if (rings[i].intersect(ray, hit)) {
        struck = true;
      }
    }
    return struck;
  }

  static void renderTile(final int x0, final int y0, final int width, final int height,
      RayCamera camera, final BVH bvh, final RTRing[] rings, final int[] pixels,
      HitStats stats) {
    // Position the RGB lights for the current viewport (Tim, 2026-10-03).
    updateLightPositions();
    final Ray ray = new Ray();
    final Hit hit = new Hit();
    final int[] stack = new int[64];
    // One reusable jitter source per tile: reseeded per pixel with the
    // same seed the per-pixel Random used, so the sub-pixel rays are
    // bit-identical with none of the allocation.
    final JitterRandom jitter = new JitterRandom();
    // Scratch shadow-query instances, reused across the tile's pixels:
    // one tile is rendered by one worker thread, so these are thread-local
    // by construction.
    final Ray shadow_ray = new Ray();
    final Hit shadow_hit = new Hit();
    // Read live, once per tile: the user can recolour the background
    // mid-session (Colours > General > Background). The background
    // number is already in 0xRRGGBB packing.
    final int background_rgb =
        0xFF000000 | RendererDelegator.color_background_number;
    // The scenic grass/sky texture, or null for the flat colour above.
    // Sized to the full canvas; tile pixels address it directly.
    final BufferedImage scenic = RendererDelegator.scenic_background
        && Coords.x_pixels > 0 && Coords.y_pixels > 0
            ? ScenicBackground.imageFor(Coords.x_pixels, Coords.y_pixels)
            : null;
    // Anti-aliasing: an aa-by-aa grid of sub-pixel rays per pixel,
    // box-filtered. 1x1 is the historical single-ray path, untouched.
    final int aa = RendererDelegator.antialiasing;
    // Pixellation: one shade per px-by-px screen block, replicated
    // across the block. With anti-aliasing off the representative ray
    // goes through the block's top-left pixel, so the pixellated image
    // is exactly the 1x1 image subsampled and replicated; with
    // anti-aliasing on the sub-pixel rays spread across the whole
    // block, resolving each coarse pixel.
    final int px = RendererDelegator.pixellation;
    if (px > 1) {
      if (RendererDelegator.simple_lighting
          && RendererDelegator.coarse_to_fine > 0) {
        // Coarse-to-fine on the pixellated grid (Tim, 2026-10-04).
        renderTileCoarseToFine(x0, y0, width, height, camera, bvh, rings,
            pixels, stats, scenic, background_rgb, ray, hit, stack,
            shadow_ray, shadow_hit, jitter, aa, px);
      } else {
        renderTilePixellated(x0, y0, width, height, camera, bvh, rings,
            pixels, stats, scenic, background_rgb, px, aa);
      }
      return;
    }
    if (aa <= 1) {
      renderTileFloodFill(x0, y0, width, height, camera, bvh, rings,
          pixels, stats, scenic, background_rgb, ray, hit, stack,
          shadow_ray, shadow_hit, jitter, aa);
      return;
    }
    renderTileFloodFill(x0, y0, width, height, camera, bvh, rings,
        pixels, stats, scenic, background_rgb, ray, hit, stack,
        shadow_ray, shadow_hit, jitter, aa);
  }

  /**
   * Flood-fill tile rendering (Tim, 2026-10-03): the tile is blanked
   * with the background first, then 8 random seed pixels are
   * ray-traced. A seed that hits the model pushes its 4-neighbours
   * onto a stack; the fill spreads through hits until it reaches
   * background (the model's edge). Pixels never reached cost no rays.
   * The fill stays inside the tile; 4-connectivity.
   */
  /**
   * Tries a phase of seed points for the flood fill. Each seed is traced;
   * hits push their 4-neighbours. Returns the updated stack top.
   * (Tim, 2026-10-03: multi-phase seeds to avoid missing content.)
   */
  private static int seedPhase(final int x0, final int y0, final int width,
      final int height, final RayCamera camera, final BVH bvh,
      final RTRing[] rings, final int[] pixels, final HitStats stats,
      final BufferedImage scenic, final int background_rgb, final Ray ray,
      final Hit hit, final int[] stack, final Ray shadow_ray,
      final Hit shadow_hit, final JitterRandom jitter, final int aa,
      final boolean[] visited, final int[] flood, int top,
      final int[] grid_x, final int[] grid_y, final int grid_w,
      final int grid_h) {
    for (int s = 0; s < grid_x.length; s++) {
      final int cx = grid_x[s];
      final int cy = grid_y[s];
      final int sx;
      final int sy;
      if (grid_w == 2 && grid_h == 2 && grid_x.length == 5) {
        // Phase 2: corners + center. Map 0->0, 1->width-1, 2->center.
        sx = cx == 2 ? width / 2 : (cx == 0 ? 0 : width - 1);
        sy = cy == 2 ? height / 2 : (cy == 0 ? 0 : height - 1);
      } else {
        // Uniform grid: cell centres.
        sx = Math.min((cx * width + width / 2) / grid_w, width - 1);
        sy = Math.min((cy * height + height / 2) / grid_h, height - 1);
      }
      final int idx = sy * width + sx;
      if (visited[idx]) {
        continue;
      }
      visited[idx] = true;
      final int rgb;
      final boolean struck;
      if (aa <= 1) {
        camera.makeRay(x0 + sx, y0 + sy, ray);
        hit.reset();
        struck = intersectScene(ray, hit, bvh, rings, stack);
        rgb = struck
            ? shade(ray, hit, bvh, stack, shadow_ray, shadow_hit, jitter)
            : pixels[idx];
      } else {
        long r = 0, g = 0, b = 0;
        boolean hit_any = false;
        jitter.setSeed((x0 + sx) * 73856093L ^ (y0 + sy) * 19349663L
            ^ 0x9E3779B9L);
        for (int sy2 = 0; sy2 < aa; sy2++) {
          for (int sx2 = 0; sx2 < aa; sx2++) {
            final double sub_x =
                x0 + sx + (sx2 + jitter.nextDouble()) / aa;
            final double sub_y =
                y0 + sy + (sy2 + jitter.nextDouble()) / aa;
            camera.makeRay(sub_x, sub_y, ray);
            hit.reset();
            if (intersectScene(ray, hit, bvh, rings, stack)) {
              final int sm =
                  shade(ray, hit, bvh, stack, shadow_ray, shadow_hit, jitter);
              r += (sm >> 16) & 0xFF;
              g += (sm >> 8) & 0xFF;
              b += sm & 0xFF;
              hit_any = true;
            } else {
              final int sm = backgroundAt(scenic, background_rgb, ray,
                  (int) Math.round(sub_x), (int) Math.round(sub_y));
              r += (sm >> 16) & 0xFF;
              g += (sm >> 8) & 0xFF;
              b += sm & 0xFF;
            }
          }
        }
        struck = hit_any;
        final int samples = aa * aa;
        rgb = struck
            ? 0xFF000000 | (int) (r / samples) << 16
                | (int) (g / samples) << 8 | (int) (b / samples)
            : pixels[idx];
      }
      if (struck) {
        pixels[idx] = rgb;
        if (stats != null) {
          stats.add(sx, sy);
        }
        if (sx > 0) {
          flood[top++] = idx - 1;
        }
        if (sx + 1 < width) {
          flood[top++] = idx + 1;
        }
        if (sy > 0) {
          flood[top++] = idx - width;
        }
        if (sy + 1 < height) {
          flood[top++] = idx + width;
        }
      }
    }
    return top;
  }

  private static void renderTileFloodFill(final int x0, final int y0, final int width,
      int height, final RayCamera camera, final BVH bvh, final RTRing[] rings, final int[] pixels,
      HitStats stats, final BufferedImage scenic, final int background_rgb, final Ray ray,
      Hit hit, final int[] stack, final Ray shadow_ray, final Hit shadow_hit,
      JitterRandom jitter, final int aa) {
    if (RendererDelegator.simple_lighting
        && RendererDelegator.coarse_to_fine > 0) {
      // Coarse-to-fine: only trace edges, fill interiors. (Tim, 2026-10-03)
      renderTileCoarseToFine(x0, y0, width, height, camera, bvh, rings,
          pixels, stats, scenic, background_rgb, ray, hit, stack,
          shadow_ray, shadow_hit, jitter, aa, 1);
      return;
    }
    // Blank the tile with the background. If there's no scenic texture,
    // a single fill is enough (no per-pixel rays). (Tim, 2026-10-03)
    if (scenic == null) {
      java.util.Arrays.fill(pixels, 0xFF000000 | background_rgb);
    } else {
      int i = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          camera.makeRay(x0 + x, y0 + y, ray);
          pixels[i++] = backgroundAt(scenic, background_rgb, ray,
              x0 + x, y0 + y);
        }
      }
    }
    final boolean[] visited = new boolean[width * height];
    final int[] flood = new int[width * height];
    int top = 0;
    // Multi-phase seeds (Tim, 2026-10-03): if the uniform seeds miss,
    // try harder before giving up. Phase 1: 8 uniform (4x2 grid).
    // Phase 2: 4 corners + center. Phase 3: 4x4 dense grid. Deterministic,
    // so renders are reproducible. If all fail, the tile is empty.
    // Phase 1: 8 uniform seeds (4x2 grid, cell centres).
    top = seedPhase(x0, y0, width, height, camera, bvh, rings, pixels,
        stats, scenic, background_rgb, ray, hit, stack, shadow_ray,
        shadow_hit, jitter, aa, visited, flood, top,
        new int[]{0, 1, 2, 3, 0, 1, 2, 3},
        new int[]{0, 0, 0, 0, 1, 1, 1, 1}, 4, 2);
    if (top == 0) {
      // Phase 2: 4 corners + center.
      top = seedPhase(x0, y0, width, height, camera, bvh, rings, pixels,
          stats, scenic, background_rgb, ray, hit, stack, shadow_ray,
          shadow_hit, jitter, aa, visited, flood, top,
          new int[]{0, 0, 1, 1, 2},
          new int[]{0, 1, 0, 1, 2}, 2, 2);
    }
    if (top == 0) {
      // Phase 3: 4x4 dense grid.
      top = seedPhase(x0, y0, width, height, camera, bvh, rings, pixels,
          stats, scenic, background_rgb, ray, hit, stack, shadow_ray,
          shadow_hit, jitter, aa, visited, flood, top,
          new int[]{0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3},
          new int[]{0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3},
          4, 4);
    }
    if (top == 0) {
      // No seed hit: tile is empty, background stands.
      return;
    }
    final int samples = aa * aa;
    while (top > 0) {
      final int idx = flood[--top];
      if (visited[idx]) {
        continue;
      }
      visited[idx] = true;
      final int x = idx % width;
      final int y = idx / width;
      final int rgb;
      final boolean struck;
      if (aa <= 1) {
        camera.makeRay(x0 + x, y0 + y, ray);
        hit.reset();
        struck = intersectScene(ray, hit, bvh, rings, stack);
        rgb = struck
            ? shade(ray, hit, bvh, stack, shadow_ray, shadow_hit, jitter)
            : pixels[idx]; // background, already filled
      } else {
        long r = 0;
        long g = 0;
        long b = 0;
        boolean hit_any = false;
        jitter.setSeed(
            (x0 + x) * 73856093L ^ (y0 + y) * 19349663L ^ 0x9E3779B9L);
        for (int sy = 0; sy < aa; sy++) {
          for (int sx = 0; sx < aa; sx++) {
            final double sub_x = x0 + x + (sx + jitter.nextDouble()) / aa;
            final double sub_y = y0 + y + (sy + jitter.nextDouble()) / aa;
            camera.makeRay(sub_x, sub_y, ray);
            hit.reset();
            final int sample_rgb;
            if (intersectScene(ray, hit, bvh, rings, stack)) {
              sample_rgb =
                  shade(ray, hit, bvh, stack, shadow_ray, shadow_hit, jitter);
              hit_any = true;
            } else {
              sample_rgb = backgroundAt(scenic, background_rgb, ray,
                  (int) Math.round(sub_x), (int) Math.round(sub_y));
            }
            r += (sample_rgb >> 16) & 0xFF;
            g += (sample_rgb >> 8) & 0xFF;
            b += sample_rgb & 0xFF;
          }
        }
        struck = hit_any;
        rgb = struck
            ? 0xFF000000 | (int) (r / samples) << 16
                | (int) (g / samples) << 8 | (int) (b / samples)
            : pixels[idx]; // background, already filled
      }
      if (struck) {
        pixels[idx] = rgb;
        if (stats != null) {
          stats.add(x, y);
        }
        // 4-neighbours, inside the tile.
        if (x > 0) {
          final int n = idx - 1;
          if (!visited[n]) {
            flood[top++] = n;
          }
        }
        if (x + 1 < width) {
          final int n = idx + 1;
          if (!visited[n]) {
            flood[top++] = n;
          }
        }
        if (y > 0) {
          final int n = idx - width;
          if (!visited[n]) {
            flood[top++] = n;
          }
        }
        if (y + 1 < height) {
          final int n = idx + width;
          if (!visited[n]) {
            flood[top++] = n;
          }
        }
      }
    }
  }

  /**
   * Coarse-to-fine fill (Tim, 2026-10-03/04): only active when Simple
   * lighting is on. Hierarchical from the dropdown-selected max block size
   * (16, 8, or 4): trace corners (+ edge midpoints for larger blocks); if
   * uniform fill, else subdivide down to 4x4 (or 1 cell when px>1), then
   * trace. With pixelation (px>1), operates on the cell grid where each
   * cell is a px-by-px screen block.
   */
  private static void renderTileCoarseToFine(final int x0, final int y0, final int width,
      int height, final RayCamera camera, final BVH bvh, final RTRing[] rings, final int[] pixels,
      HitStats stats, final BufferedImage scenic, final int background_rgb, final Ray ray,
      Hit hit, final int[] stack, final Ray shadow_ray, final Hit shadow_hit,
      JitterRandom jitter, final int aa, final int px) {
    // Blank the tile with the background.
    if (scenic == null) {
      // Flat background: single fill, no per-pixel rays.
      java.util.Arrays.fill(pixels, 0xFF000000 | background_rgb);
    } else {
      int i = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          camera.makeRay(x0 + x, y0 + y, ray);
          pixels[i++] = backgroundAt(scenic, background_rgb, ray,
              x0 + x, y0 + y);
        }
      }
    }
    // Cell grid dimensions (px=1: cells are pixels).
    final int cw = (width + px - 1) / px;
    final int ch = (height + px - 1) / px;
    // Tim's hierarchical 4-corner algorithm (2026-10-03/04): start at the
    // dropdown-selected max block size (16, 8, or 4 cells). Trace the
    // corners (+ edge midpoints for larger blocks); if uniform, fill.
    // Else subdivide into quadrants down to min block, then trace.
    final int max_cs = RendererDelegator.coarse_to_fine;
    // Min block: 4 cells when px=1, else 1 cell (Tim, 2026-10-04).
    final int min_cs = px > 1 ? 1 : 4;
    for (int by = 0; by < ch; by += max_cs) {
      for (int bx = 0; bx < cw; bx += max_cs) {
        renderBlock(x0, y0, bx, by, Math.min(bx + max_cs, cw),
            Math.min(by + max_cs, ch), max_cs, min_cs, px, camera, bvh,
            rings, ray, hit, stack, shadow_ray, shadow_hit, jitter, pixels,
            width, stats, null);
      }
    }
  }

  /**
   * Renders one quadrant child with optimistic sample reuse (Tim,
   * 2026-10-04). Corners matching parent samples exactly are reused;
   * corners adjacent to parent samples are reused optimistically and
   * verified with fresh traces before filling. Only truly-new corners
   * are traced immediately. If the optimistic test fails, or
   * verification fails, falls back to full renderBlock (reusing the
   * fresh traces via inherited samples).
   */
  private static void renderQuadrant(final int x0, final int y0,
      final int qx0, final int qy0, final int qx1, final int qy1,
      final int cs, final int min_cs, final int px, final RayCamera camera,
      final BVH bvh, final RTRing[] rings, final Ray ray, final Hit hit,
      final int[] stack, final Ray shadow_ray, final Hit shadow_hit,
      final JitterRandom jitter, final int[] pixels, final int width,
      final HitStats stats, final BlockSamples parent) {
    final BlockSamples child = new BlockSamples();
    final boolean[] optimistic = new boolean[4];
    final int[] cx = {qx0, qx1 - 1, qx0, qx1 - 1};
    final int[] cy = {qy0, qy0, qy1 - 1, qy1 - 1};
    for (int i = 0; i < 4; i++) {
      child.sx[child.n] = cx[i];
      child.sy[child.n] = cy[i];
      final int pi = parent.find(cx[i], cy[i]);
      if (pi >= 0) {
        child.prim[child.n] = parent.prim[pi];
        child.rgb[child.n] = parent.rgb[pi];
      } else {
        final int qi = parent.findAdjacent(cx[i], cy[i]);
        if (qi >= 0) {
          child.prim[child.n] = parent.prim[qi];
          child.rgb[child.n] = parent.rgb[qi];
          optimistic[child.n] = true;
        } else {
          child.prim[child.n] = samplePrimitive(x0 + cx[i] * px,
              y0 + cy[i] * px, camera, bvh, rings, ray, hit, stack);
          child.rgb[child.n] = child.prim[child.n] == null ? 0 : shade(
              ray, hit, bvh, stack, shadow_ray, shadow_hit, jitter);
        }
      }
      child.n++;
    }
    if (samplesUniform(child)) {
      // Verify optimistic samples with fresh traces before filling.
      boolean verified = true;
      for (int i = 0; i < 4; i++) {
        if (optimistic[i]) {
          final Primitive p = samplePrimitive(x0 + child.sx[i] * px,
              y0 + child.sy[i] * px, camera, bvh, rings, ray, hit, stack);
          final int rgb = p == null ? 0 : shade(ray, hit, bvh, stack,
              shadow_ray, shadow_hit, jitter);
          // Must match the optimistically reused sample.
          if (p != child.prim[i] || !colorsMatch(rgb, child.rgb[i])) {
            verified = false;
            child.prim[i] = p;
            child.rgb[i] = rgb;
          }
        }
      }
      if (verified && samplesUniform(child)) {
        if (samplesSeenHit(child)) {
          fillBlock(pixels, width, qx0, qy0, qx1, qy1, px, child, stats);
        }
        return;
      }
    }
    // Not uniform, or verification failed: full render, reusing traces.
    renderBlock(x0, y0, qx0, qy0, qx1, qy1, cs, min_cs, px, camera, bvh,
        rings, ray, hit, stack, shadow_ray, shadow_hit, jitter, pixels,
        width, stats, child);
  }

  /**
   * Renders one block (in cell coordinates): try blockUniform; if not
   * uniform and bigger than min_cs, split into quadrants and recurse;
   * else trace every cell.
   */
  private static void renderBlock(final int x0, final int y0, final int bx,
      final int by, final int x_end, final int y_end, final int cs,
      final int min_cs, final int px, final RayCamera camera,
      final BVH bvh, final RTRing[] rings, final Ray ray, final Hit hit,
      final int[] stack, final Ray shadow_ray, final Hit shadow_hit,
      final JitterRandom jitter, final int[] pixels, final int width,
      final HitStats stats, final BlockSamples inherited) {
    final BlockSamples samples = new BlockSamples();
    if (cs > min_cs && blockUniform(x0, y0, bx, by, x_end, y_end, px,
        camera, bvh, rings, ray, hit, stack, shadow_ray, shadow_hit,
        jitter, pixels, width, stats, samples, inherited)) {
      return;
    }
    if ((cs == 16 || cs == 8) && x_end - bx == cs && y_end - by == cs) {
      // Tim, 2026-10-04: intermediate cs x (cs/2) split before quadrants.
      // Reuses the parent samples; only the cut edge's new corners traced.
      if (tryIntermediateSplit(x0, y0, bx, by, cs, px, camera, bvh, rings,
          ray, hit, stack, shadow_ray, shadow_hit, jitter, pixels, width,
          stats, samples, min_cs)) {
        return;
      }
    }
    if (cs > min_cs) {
      // Not uniform: split into quadrants with optimistic reuse (Tim,
      // 2026-10-04). Child corners reusing parent samples exactly or
      // adjacently (verified before filling) avoid re-tracing points
      // right next to already-traced ones.
      final int half = cs / 2;
      final int mx = bx + half;
      final int my = by + half;
      renderQuadrant(x0, y0, bx, by, Math.min(mx, x_end),
          Math.min(my, y_end), half, min_cs, px, camera, bvh, rings, ray,
          hit, stack, shadow_ray, shadow_hit, jitter, pixels, width,
          stats, samples);
      if (mx < x_end) {
        renderQuadrant(x0, y0, mx, by, x_end, Math.min(my, y_end), half,
            min_cs, px, camera, bvh, rings, ray, hit, stack, shadow_ray,
            shadow_hit, jitter, pixels, width, stats, samples);
      }
      if (my < y_end) {
        renderQuadrant(x0, y0, bx, my, Math.min(mx, x_end), y_end, half,
            min_cs, px, camera, bvh, rings, ray, hit, stack, shadow_ray,
            shadow_hit, jitter, pixels, width, stats, samples);
      }
      if (mx < x_end && my < y_end) {
        renderQuadrant(x0, y0, mx, my, x_end, y_end, half, min_cs, px,
            camera, bvh, rings, ray, hit, stack, shadow_ray, shadow_hit,
            jitter, pixels, width, stats, samples);
      }
      return;
    }
    // Min block (or smaller edge block): try fill, else trace all cells.
    final BlockSamples min_samples = new BlockSamples();
    if (blockUniform(x0, y0, bx, by, x_end, y_end, px, camera, bvh, rings,
        ray, hit, stack, shadow_ray, shadow_hit, jitter, pixels, width,
        stats, min_samples, inherited)) {
      return;
    }
    for (int cy = by; cy < y_end; cy++) {
      for (int cx = bx; cx < x_end; cx++) {
        // Cell (cx, cy) -> screen top-left for the ray, tile-relative for
        // the pixels[] index. Then replicate px-by-px.
        final int scr_x = x0 + cx * px;
        final int scr_y = y0 + cy * px;
        camera.makeRay(scr_x, scr_y, ray);
        hit.reset();
        if (intersectScene(ray, hit, bvh, rings, stack)) {
          final int rgb = shade(ray, hit, bvh, stack, shadow_ray,
              shadow_hit, jitter);
          final int px_x = cx * px;
          final int px_y = cy * px;
          for (int dy = 0; dy < px; dy++) {
            for (int dx = 0; dx < px; dx++) {
              pixels[(px_y + dy) * width + (px_x + dx)] = rgb;
            }
          }
        }
        // Else: background, already filled.
      }
    }
  }

  /**
   * Traces a single ray, returning the primitive hit (or null for
   * background). Leaves the hit in the reusable Hit object.
   */
  private static Primitive samplePrimitive(final int x, final int y,
      final RayCamera camera, final BVH bvh, final RTRing[] rings,
      final Ray ray, final Hit hit, final int[] stack) {
    camera.makeRay(x, y, ray);
    hit.reset();
    if (!intersectScene(ray, hit, bvh, rings, stack)) {
      return null;
    }
    return hit.primitive;
  }

  /**
   * Traces a single interior sample; returns true if it matches the
   * corner verdict (same primitive, or background if all corners missed).
   */
  /**
   * Returns true if two shaded colors are close enough to fill a block
   * with one of them. Fog and shadows can vary the shade across a large
   * primitive; if the variation is visible, subdivide instead of filling.
   * (Tim, 2026-10-04)
   */
  private static boolean colorsMatch(final int rgb1, final int rgb2) {
    final int r1 = (rgb1 >> 16) & 0xFF;
    final int g1 = (rgb1 >> 8) & 0xFF;
    final int b1 = rgb1 & 0xFF;
    final int r2 = (rgb2 >> 16) & 0xFF;
    final int g2 = (rgb2 >> 8) & 0xFF;
    final int b2 = rgb2 & 0xFF;
    // Threshold: 3/255 per channel (just-noticeable).
    return Math.abs(r1 - r2) <= 3 && Math.abs(g1 - g2) <= 3
        && Math.abs(b1 - b2) <= 3;
  }

  /**
   * Holds traced samples for a block, so a parent block's samples can be
   * reused when testing subdivisions (Tim, 2026-10-04: intermediate
   * 16x8/8x16 split). Positions are in cell coordinates, tile-relative.
   */
  private static final class BlockSamples {
    int n;
    final int[] sx = new int[8];
    final int[] sy = new int[8];
    final Primitive[] prim = new Primitive[8];
    final int[] rgb = new int[8];

    /** Finds a sample at (x, y), or -1. */
    int find(final int x, final int y) {
      for (int i = 0; i < n; i++) {
        if (sx[i] == x && sy[i] == y) {
          return i;
        }
      }
      return -1;
    }

    /**
     * Finds a sample within 1 cell (Chebyshev distance) of (x, y), or -1.
     * Used for optimistic reuse (Tim, 2026-10-04): child corners landing
     * next to parent samples reuse them, verified later before filling.
     */
    int findAdjacent(final int x, final int y) {
      for (int i = 0; i < n; i++) {
        if (Math.abs(sx[i] - x) <= 1 && Math.abs(sy[i] - y) <= 1) {
          return i;
        }
      }
      return -1;
    }
  }

  /**
   * Samples a block (corners + interior for larger blocks). If all samples
   * hit the same primitive (or all miss) AND produce the same shaded color
   * (fog/shadows can vary it), fills the block and returns true. Otherwise
   * returns false (caller subdivides or traces fully).
   */
  private static boolean blockUniform(final int x0, final int y0,
      final int bx, final int by, final int x_end, final int y_end,
      final int px, final RayCamera camera, final BVH bvh,
      final RTRing[] rings, final Ray ray, final Hit hit, final int[] stack,
      final Ray shadow_ray, final Hit shadow_hit, final JitterRandom jitter,
      final int[] pixels, final int width, final HitStats stats,
      final BlockSamples samples, final BlockSamples inherited) {
    // Collect all sample points (in cell coordinates): 4 corners, plus 4
    // edge midpoints for 16x16. (Tim, 2026-10-04: extra samples go on the
    // edge, not center.)
    final int bw = x_end - bx;
    final int bh = y_end - by;
    final int mcx = bx + bw / 2;
    final int mcy = by + bh / 2;
    // Max 8 samples: 4 corners + 4 edge midpoints (16x16 only).
    samples.n = 0;
    samples.sx[samples.n] = bx; samples.sy[samples.n] = by; samples.n++;
    samples.sx[samples.n] = x_end - 1; samples.sy[samples.n] = by; samples.n++;
    samples.sx[samples.n] = bx; samples.sy[samples.n] = y_end - 1; samples.n++;
    samples.sx[samples.n] = x_end - 1; samples.sy[samples.n] = y_end - 1; samples.n++;
    if (bw >= 16) {
      samples.sx[samples.n] = mcx; samples.sy[samples.n] = by; samples.n++;
      samples.sx[samples.n] = mcx; samples.sy[samples.n] = y_end - 1; samples.n++;
      samples.sx[samples.n] = bx; samples.sy[samples.n] = mcy; samples.n++;
      samples.sx[samples.n] = x_end - 1; samples.sy[samples.n] = mcy; samples.n++;
    }
    // Trace and shade all samples, reusing inherited ones exactly.
    for (int i = 0; i < samples.n; i++) {
      final int pi = inherited == null ? -1
          : inherited.find(samples.sx[i], samples.sy[i]);
      if (pi >= 0) {
        samples.prim[i] = inherited.prim[pi];
        samples.rgb[i] = inherited.rgb[pi];
      } else {
        // Cell -> screen: top-left of the px-by-px block.
        samples.prim[i] = samplePrimitive(x0 + samples.sx[i] * px,
            y0 + samples.sy[i] * px, camera, bvh, rings, ray, hit, stack);
        samples.rgb[i] = samples.prim[i] == null ? 0 : shade(ray, hit, bvh,
            stack, shadow_ray, shadow_hit, jitter);
      }
    }
    if (!samplesUniform(samples)) {
      return false;  // Subdivide.
    }
    if (!samplesSeenHit(samples)) {
      // Entire block is background (all missed), already pre-filled.
      return true;
    }
    // Uniform: fill the block.
    fillBlock(pixels, width, bx, by, x_end, y_end, px, samples, stats);
    return true;
  }

  /**
   * Uniformity test: all samples hit the same primitive (or all miss) AND
   * produce the same shaded color (fog/shadows can vary it). (Tim, 2026-10-04)
   */
  /**
   * Intermediate split (Tim, 2026-10-04): a non-uniform cs×cs block tries
   * cs×(cs/2) or (cs/2)×cs halves before quadrants. Reuses the parent's
   * samples; only the cut edge's new corners are traced (2 for 16x16,
   * which has edge midpoints; 4 for 8x8). The split direction is picked
   * heuristically: whichever axis has lower within-half color variation
   * among the existing samples is "more promising".
   * Returns true if both halves were resolved (filled or subdivided).
   */
  private static boolean tryIntermediateSplit(final int x0, final int y0,
      final int bx, final int by, final int cs, final int px,
      final RayCamera camera, final BVH bvh, final RTRing[] rings,
      final Ray ray, final Hit hit, final int[] stack, final Ray shadow_ray,
      final Hit shadow_hit, final JitterRandom jitter, final int[] pixels,
      final int width, final HitStats stats, final BlockSamples parent,
      final int min_cs) {
    // Parent sample indices: 0=TL, 1=TR, 2=BL, 3=BR, 4=TM, 5=BM, 6=ML,
    // 7=MR (midpoints only for 16x16).
    final int half = cs / 2;
    final int h_score;
    final int v_score;
    if (cs >= 16) {
      h_score = variation(parent, new int[]{0, 1, 4})
          + variation(parent, new int[]{2, 3, 5, 6, 7});
      v_score = variation(parent, new int[]{0, 2, 6})
          + variation(parent, new int[]{1, 3, 4, 5, 7});
    } else {
      h_score = variation(parent, new int[]{0, 1})
          + variation(parent, new int[]{2, 3});
      v_score = variation(parent, new int[]{0, 2})
          + variation(parent, new int[]{1, 3});
    }
    final boolean horizontal = h_score <= v_score;
    // Halves as {x0, y0, x1, y1} in cell coords.
    final int[][] halves;
    if (horizontal) {
      halves = new int[][]{
          {bx, by, bx + cs, by + half},
          {bx, by + half, bx + cs, by + cs}};
    } else {
      halves = new int[][]{
          {bx, by, bx + half, by + cs},
          {bx + half, by, bx + cs, by + cs}};
    }
    for (int h = 0; h < 2; h++) {
      final int hx0 = halves[h][0];
      final int hy0 = halves[h][1];
      final int hx1 = halves[h][2];
      final int hy1 = halves[h][3];
      final BlockSamples half_samples = new BlockSamples();
      // 4 corners: (hx0,hy0), (hx1-1,hy0), (hx0,hy1-1), (hx1-1,hy1-1).
      // Reuse parent samples where available; trace the rest.
      final int[] cx = {hx0, hx1 - 1, hx0, hx1 - 1};
      final int[] cy = {hy0, hy0, hy1 - 1, hy1 - 1};
      for (int i = 0; i < 4; i++) {
        final int pi = parent.find(cx[i], cy[i]);
        half_samples.sx[half_samples.n] = cx[i];
        half_samples.sy[half_samples.n] = cy[i];
        if (pi >= 0) {
          half_samples.prim[half_samples.n] = parent.prim[pi];
          half_samples.rgb[half_samples.n] = parent.rgb[pi];
        } else {
          half_samples.prim[half_samples.n] = samplePrimitive(
              x0 + cx[i] * px, y0 + cy[i] * px, camera, bvh, rings, ray,
              hit, stack);
          half_samples.rgb[half_samples.n] =
              half_samples.prim[half_samples.n] == null ? 0 : shade(ray,
                  hit, bvh, stack, shadow_ray, shadow_hit, jitter);
        }
        half_samples.n++;
      }
      if (samplesUniform(half_samples)) {
        if (samplesSeenHit(half_samples)) {
          fillBlock(pixels, width, hx0, hy0, hx1, hy1, px, half_samples,
              stats);
        }
        // Else: all background, already pre-filled.
      } else {
        // Not uniform: split the half into two (cs/2)x(cs/2) blocks,
        // reusing the half's samples.
        final int child = cs / 2;
        if (horizontal) {
          final int mx = hx0 + half;
          renderBlock(x0, y0, hx0, hy0, mx, hy1, child, min_cs, px, camera,
              bvh, rings, ray, hit, stack, shadow_ray, shadow_hit,
              jitter, pixels, width, stats, half_samples);
          renderBlock(x0, y0, mx, hy0, hx1, hy1, child, min_cs, px, camera,
              bvh, rings, ray, hit, stack, shadow_ray, shadow_hit,
              jitter, pixels, width, stats, half_samples);
        } else {
          final int my = hy0 + half;
          renderBlock(x0, y0, hx0, hy0, hx1, my, child, min_cs, px, camera,
              bvh, rings, ray, hit, stack, shadow_ray, shadow_hit,
              jitter, pixels, width, stats, half_samples);
          renderBlock(x0, y0, hx0, my, hx1, hy1, child, min_cs, px, camera,
              bvh, rings, ray, hit, stack, shadow_ray, shadow_hit,
              jitter, pixels, width, stats, half_samples);
        }
      }
    }
    return true;
  }

  /**
   * Heuristic variation of a sample subset: count of samples not matching
   * the first (different primitive or non-matching color). Lower means the
   * subset is more internally uniform. (Tim, 2026-10-04)
   */
  private static int variation(final BlockSamples s, final int[] indices) {
    int v = 0;
    final Primitive p0 = s.prim[indices[0]];
    final int rgb0 = s.rgb[indices[0]];
    for (int k = 1; k < indices.length; k++) {
      final int i = indices[k];
      if (s.prim[i] != p0 || !colorsMatch(s.rgb[i], rgb0)) {
        v++;
      }
    }
    return v;
  }

  private static boolean samplesUniform(final BlockSamples s) {    Primitive first_prim = null;
    int first_rgb = 0;
    boolean seen_hit = false;
    boolean seen_miss = false;
    boolean first = true;
    for (int i = 0; i < s.n; i++) {
      final Primitive p = s.prim[i];
      if (p == null) {
        seen_miss = true;
        if (seen_hit) {
          return false;  // Mixed hit/miss.
        }
        continue;
      }
      seen_hit = true;
      if (seen_miss) {
        return false;  // Mixed miss/hit.
      }
      if (first) {
        first_prim = p;
        first_rgb = s.rgb[i];
        first = false;
      } else if (p != first_prim || !colorsMatch(s.rgb[i], first_rgb)) {
        return false;  // Different primitive or shade.
      }
    }
    return true;
  }

  /** True if any sample hit a primitive. */
  private static boolean samplesSeenHit(final BlockSamples s) {
    for (int i = 0; i < s.n; i++) {
      if (s.prim[i] != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Fills a block's cells, replicating px-by-px (tile-relative). In debug
   * mode ("Show active tiles") sampled positions keep their real color and
   * the rest go red; otherwise all go the uniform shade. (Tim, 2026-10-03/04)
   */
  private static void fillBlock(final int[] pixels, final int width,
      final int bx, final int by, final int x_end, final int y_end,
      final int px, final BlockSamples samples, final HitStats stats) {
    final boolean debug = RendererTileManager.show_active_tiles;
    // All samples share the shade when uniform; take the first hit's.
    int fill_rgb = 0;
    for (int i = 0; i < samples.n; i++) {
      if (samples.prim[i] != null) {
        fill_rgb = samples.rgb[i];
        break;
      }
    }
    final int red_rgb = 0xFFFF0000;
    for (int cy = by; cy < y_end; cy++) {
      for (int cx = bx; cx < x_end; cx++) {
        final boolean sampled = samples.find(cx, cy) >= 0;
        final int rgb;
        if (debug) {
          rgb = sampled ? fill_rgb : red_rgb;
        } else {
          rgb = fill_rgb;
        }
        // Replicate across the px-by-px screen block (tile-relative).
        final int px_x = cx * px;
        final int px_y = cy * px;
        for (int dy = 0; dy < px; dy++) {
          for (int dx = 0; dx < px; dx++) {
            pixels[(px_y + dy) * width + (px_x + dx)] = rgb;
          }
        }
      }
    }
    // Record the filled pixels as hits, so "Show active tiles" still
    // draws the tile's red outline when coarse-to-fine is on. (Tim, 2026-10-03)
    stats.add(bx * px, by * px);
    stats.add((x_end - 1) * px, (y_end - 1) * px);
  }

  /**
   * Pixellated tile rendering: one shade per px-by-px block, replicated
   * across the block. Blocks align to screen coordinates, so neighbouring
   * tiles agree on block boundaries and no seams appear. With
   * anti-aliasing off the block's top-left pixel is shaded; with it on,
   * the aa-by-aa stratified samples spread across the whole block.
   */
  private static void renderTilePixellated(final int x0, final int y0, final int width,
      int height, final RayCamera camera, final BVH bvh, final RTRing[] rings, final int[] pixels,
      HitStats stats, final BufferedImage scenic, final int background_rgb, final int px,
      int aa) {
    final Ray ray = new Ray();
    final Hit hit = new Hit();
    final int[] stack = new int[64];
    // One reusable jitter source per tile: reseeded per block with the
    // same seed the per-block Random used, so the sub-pixel rays are
    // bit-identical with none of the allocation.
    final JitterRandom jitter = new JitterRandom();
    // Scratch shadow-query instances, reused across the tile's pixels:
    // one tile is rendered by one worker thread, so these are thread-local
    // by construction.
    final Ray shadow_ray = new Ray();
    final Hit shadow_hit = new Hit();
    final int x1 = x0 + width;
    final int y1 = y0 + height;
    // Screen-aligned blocks: the first block may start before the tile.
    final int bx0 = (x0 / px) * px;
    final int by0 = (y0 / px) * px;
    final int samples = aa * aa;
    for (int by = by0; by < y1; by += px) {
      for (int bx = bx0; bx < x1; bx += px) {
        final int rgb;
        final boolean struck;
        if (aa <= 1) {
          camera.makeRay(bx, by, ray);
          hit.reset();
          if (intersectScene(ray, hit, bvh, rings, stack)) {
            rgb = shade(ray, hit, bvh, stack, shadow_ray, shadow_hit, jitter);
            struck = true;
          } else {
            rgb = backgroundAt(scenic, background_rgb, ray, bx, by);
            struck = false;
          }
        } else {
          long r = 0;
          long g = 0;
          long b = 0;
          boolean hit_any = false;
          // Seeded per block, like the per-pixel path, so a render is
          // deterministic run to run and independent of tile boundaries.
          jitter.setSeed(bx * 73856093L ^ by * 19349663L ^ 0x9E3779B9L);
          for (int sy = 0; sy < aa; sy++) {
            for (int sx = 0; sx < aa; sx++) {
              final double sub_x = bx + (sx + jitter.nextDouble()) * px / aa;
              final double sub_y = by + (sy + jitter.nextDouble()) * px / aa;
              camera.makeRay(sub_x, sub_y, ray);
              hit.reset();
              final int sample_rgb;
              if (intersectScene(ray, hit, bvh, rings, stack)) {
                sample_rgb = shade(ray, hit, bvh, stack, shadow_ray, shadow_hit, jitter);
                hit_any = true;
              } else {
                sample_rgb = backgroundAt(scenic, background_rgb, ray,
                    (int) Math.round(sub_x), (int) Math.round(sub_y));
              }
              r += (sample_rgb >> 16) & 0xFF;
              g += (sample_rgb >> 8) & 0xFF;
              b += sample_rgb & 0xFF;
            }
          }
          rgb = 0xFF000000 | (int) (r / samples) << 16
              | (int) (g / samples) << 8 | (int) (b / samples);
          struck = hit_any;
        }
        // Replicate the block colour across the tile.
        final int xs = Math.max(bx, x0);
        final int ys = Math.max(by, y0);
        final int xe = Math.min(bx + px, x1);
        final int ye = Math.min(by + px, y1);
        for (int y = ys; y < ye; y++) {
          final int row = (y - y0) * width;
          for (int x = xs; x < xe; x++) {
            pixels[row + (x - x0)] = rgb;
          }
        }
        if (struck && stats != null) {
          // The block hit: extend the hit box over the whole block.
          stats.add(xs - x0, ys - y0);
          stats.add(xe - 1 - x0, ye - 1 - y0);
        }
      }
    }
  }

  /**
   * The background colour for a missed ray: the flat background colour,
   * or the scenic grass/sky texture when it is enabled. The texture is
   * sampled pan-aware, so the background behaves like a world-fixed
   * backdrop: panning the view slides the grass blades across the
   * screen. Rays through the upper half of the screen (dy &lt; 0) take
   * the distant-sky parallax rate; the rest take the near-grass rate.
   */
  private static int backgroundAt(final BufferedImage scenic, final int background_rgb,
      Ray ray, final int sx, final int sy) {
    if (scenic == null) {
      return background_rgb;
    }
    return ScenicBackground.sampleWithPan(scenic, sx, sy, ray.dy < 0.0);
  }

  /**
   * Diffuse shading with optional shadows, a fill light, a glossy
   * sheen, specular highlights, and an optional Fresnel rim. Shadow
   * rays (towards LIGHT_*) are traced when RendererDelegator.shadows
   * is set; the fill, the sheen, the highlight and the rim are smooth
   * functions of the surface normal, so they can never speckle. Added
   * light rolls off softly towards 255 instead of clipping, so hot
   * spots keep their detail -- except the specular highlight, which
   * keeps its original hard clip and punches through to white.
   *
   * <p>Matches the default renderer: its [128, 255] brightness range, its
   * depth fog, and its packed-colour convention (0xRRGGBB -- red in the
   * high byte, despite some misleading local variable names in the
   * default renderer's colour code; byte positions are preserved all the
   * way to new Color(packed), so they are preserved here too).
   */
  private static int shade(final Ray ray, final Hit hit, final BVH bvh, final int[] stack,
      Ray shadow_ray, final Hit shadow_hit, final JitterRandom jitter) {
    if (hit.primitive.isUnlit()) {
      // Overlay indicators like the selection ring: flat colour at
      // full strength from any angle, fogged for depth like the
      // default renderer's depth-shaded selection circle.
      final double pz = ray.oz + ray.dz * hit.t;
      return 0xFF000000 | Fog.applyFog(hit.primitive.getColour(), (int) pz);
    }
    if (RendererDelegator.simple_lighting) {
      // Simple lighting (Tim, 2026-10-03): RGB point lights, viewport-
      // dependent. Nodes are flat; links and faces get one flat RGB
      // shade per primitive, computed at the primitive center
      // (deterministic, no flicker).
      final double pz = ray.oz + ray.dz * hit.t;
      final int fogged =
          Fog.applyFog(hit.primitive.getColour(), (int) pz);
      if (hit.primitive instanceof RTSphere) {
        // Node: RGB flat shading by distance to lights (Tim, 2026-10-03).
        // Use the sphere center for the light distance.
        final double ncx = ((RTSphere) hit.primitive).cx;
        final double ncy = ((RTSphere) hit.primitive).cy;
        final double ncz = ((RTSphere) hit.primitive).cz;
        final double nrdx = RED_PX - ncx;
        final double nrdy = RED_PY - ncy;
        final double nrdz = RED_PZ - ncz;
        final double nrd = Math.sqrt(nrdx * nrdx + nrdy * nrdy + nrdz * nrdz);
        final double nr_fall = 1.0 / (1.0 + (nrd / LIGHT_FALLOFF_K) * (nrd / LIGHT_FALLOFF_K));
        final double ngdx = GREEN_PX - ncx;
        final double ngdy = GREEN_PY - ncy;
        final double ngdz = GREEN_PZ - ncz;
        final double ngd = Math.sqrt(ngdx * ngdx + ngdy * ngdy + ngdz * ngdz);
        final double ng_fall = 1.0 / (1.0 + (ngd / LIGHT_FALLOFF_K) * (ngd / LIGHT_FALLOFF_K));
        final double nbdx = BLUE_PX - ncx;
        final double nbdy = BLUE_PY - ncy;
        final double nbdz = BLUE_PZ - ncz;
        final double nbd = Math.sqrt(nbdx * nbdx + nbdy * nbdy + nbdz * nbdz);
        final double nb_fall = 1.0 / (1.0 + (nbd / LIGHT_FALLOFF_K) * (nbd / LIGHT_FALLOFF_K));
        // White point light in fast mode (Tim, 2026-10-03): flat falloff,
        // like RGB. Adds equally to all channels.
        final double nwdx = WHITE_PX - ncx;
        final double nwdy = WHITE_PY - ncy;
        final double nwdz = WHITE_PZ - ncz;
        final double nwd = Math.sqrt(nwdx * nwdx + nwdy * nwdy + nwdz * nwdz);
        final double nw_fall = 1.0 / (1.0 + (nwd / LIGHT_FALLOFF_K) * (nwd / LIGHT_FALLOFF_K));
        final int w_add_fast = (int) (200.0 * Math.min(1.0, nw_fall
            * LIGHT_BRIGHTNESS * RendererDelegator.white_light_pct / 100.0
            * RendererDelegator.white_light_pct / 100.0));
        final int amb = ambientBase();
        final int nr_scaled = Math.min(255, amb + w_add_fast + (int) (159.0 * Math.min(1.0, nr_fall * LIGHT_BRIGHTNESS * RendererDelegator.red_light_pct / 100.0 * RendererDelegator.red_light_pct / 100.0)));
        final int ng_scaled = Math.min(255, amb + w_add_fast + (int) (159.0 * Math.min(1.0, ng_fall * LIGHT_BRIGHTNESS * RendererDelegator.green_light_pct / 100.0 * RendererDelegator.green_light_pct / 100.0)));
        final int nb_scaled = Math.min(255, amb + w_add_fast + (int) (159.0 * Math.min(1.0, nb_fall * LIGHT_BRIGHTNESS * RendererDelegator.blue_light_pct / 100.0 * RendererDelegator.blue_light_pct / 100.0)));
        final int nr = (fogged >> 16) & 0xFF;
        final int ng = (fogged >> 8) & 0xFF;
        final int nb = fogged & 0xFF;
        final int nor = (nr * nr_scaled) >> 8;
        final int nog = (ng * ng_scaled) >> 8;
        final int nob = (nb * nb_scaled) >> 8;
        return 0xFF000000 | (nor << 16) | (nog << 8) | nob;
      }
      // Primitive center for the light distance/direction.
      final double pcx;
      final double pcy;
      final double pcz;
      final double ax;
      final double ay;
      final double az;
      final boolean is_cylinder;
      if (hit.primitive instanceof RTCylinder) {
        final RTCylinder cyl = (RTCylinder) hit.primitive;
        pcx = cyl.getCenterX();
        pcy = cyl.getCenterY();
        pcz = cyl.getCenterZ();
        ax = cyl.getAxisX();
        ay = cyl.getAxisY();
        az = cyl.getAxisZ();
        is_cylinder = true;
      } else if (hit.primitive instanceof RTEllipsoid) {
        final RTEllipsoid ell = (RTEllipsoid) hit.primitive;
        pcx = ell.getCenterX();
        pcy = ell.getCenterY();
        pcz = ell.getCenterZ();
        ax = ell.getAxisX();
        ay = ell.getAxisY();
        az = ell.getAxisZ();
        is_cylinder = true;
      } else {
        // Face: use hit position as center; normal from hit.
        pcx = ray.ox + ray.dx * hit.t;
        pcy = ray.oy + ray.dy * hit.t;
        pcz = ray.oz + ray.dz * hit.t;
        ax = hit.nx;
        ay = hit.ny;
        az = hit.nz;
        is_cylinder = false;
      }
      final double r_factor;
      final double g_factor;
      final double b_factor;
      if (is_cylinder) {
        // Cable/strut: per-light brightness from axis angle to the
        // light direction, with distance falloff.
        double rlx = RED_PX - pcx;
        double rly = RED_PY - pcy;
        double rlz = RED_PZ - pcz;
        double rd = Math.sqrt(rlx * rlx + rly * rly + rlz * rlz);
        double r_dot = (rlx * ax + rly * ay + rlz * az) / rd;
        double r_fall = 1.0 / (1.0 + (rd / LIGHT_FALLOFF_K) * (rd / LIGHT_FALLOFF_K));
        r_factor = Math.sqrt(Math.max(0.0, 1.0 - r_dot * r_dot))
            * Math.min(1.0, r_fall * LIGHT_BRIGHTNESS * RendererDelegator.red_light_pct / 100.0);
        double glx = GREEN_PX - pcx;
        double gly = GREEN_PY - pcy;
        double glz = GREEN_PZ - pcz;
        double gd = Math.sqrt(glx * glx + gly * gly + glz * glz);
        double g_dot = (glx * ax + gly * ay + glz * az) / gd;
        double g_fall = 1.0 / (1.0 + (gd / LIGHT_FALLOFF_K) * (gd / LIGHT_FALLOFF_K));
        g_factor = Math.sqrt(Math.max(0.0, 1.0 - g_dot * g_dot))
            * Math.min(1.0, g_fall * LIGHT_BRIGHTNESS * RendererDelegator.green_light_pct / 100.0);
        double blx = BLUE_PX - pcx;
        double bly = BLUE_PY - pcy;
        double blz = BLUE_PZ - pcz;
        double bd = Math.sqrt(blx * blx + bly * bly + blz * blz);
        double b_dot = (blx * ax + bly * ay + blz * az) / bd;
        double b_fall = 1.0 / (1.0 + (bd / LIGHT_FALLOFF_K) * (bd / LIGHT_FALLOFF_K));
        b_factor = Math.sqrt(Math.max(0.0, 1.0 - b_dot * b_dot))
            * Math.min(1.0, b_fall * LIGHT_BRIGHTNESS * RendererDelegator.blue_light_pct / 100.0);
      } else {
        // Face (triangle): geometric normal is constant; dot with each
        // light direction from the face center.
        double rlx = RED_PX - pcx;
        double rly = RED_PY - pcy;
        double rlz = RED_PZ - pcz;
        double rd = Math.sqrt(rlx * rlx + rly * rly + rlz * rlz);
        double r_fall = 1.0 / (1.0 + (rd / LIGHT_FALLOFF_K) * (rd / LIGHT_FALLOFF_K));
        r_factor = Math.max(0.0, Math.min(1.0,
            (ax * rlx + ay * rly + az * rlz) / rd))
            * Math.min(1.0, r_fall * LIGHT_BRIGHTNESS * RendererDelegator.red_light_pct / 100.0);
        double glx = GREEN_PX - pcx;
        double gly = GREEN_PY - pcy;
        double glz = GREEN_PZ - pcz;
        double gd = Math.sqrt(glx * glx + gly * gly + glz * glz);
        double g_fall = 1.0 / (1.0 + (gd / LIGHT_FALLOFF_K) * (gd / LIGHT_FALLOFF_K));
        g_factor = Math.max(0.0, Math.min(1.0,
            (ax * glx + ay * gly + az * glz) / gd))
            * Math.min(1.0, g_fall * LIGHT_BRIGHTNESS * RendererDelegator.green_light_pct / 100.0);
        double blx = BLUE_PX - pcx;
        double bly = BLUE_PY - pcy;
        double blz = BLUE_PZ - pcz;
        double bd = Math.sqrt(blx * blx + bly * bly + blz * blz);
        double b_fall = 1.0 / (1.0 + (bd / LIGHT_FALLOFF_K) * (bd / LIGHT_FALLOFF_K));
        b_factor = Math.max(0.0, Math.min(1.0,
            (ax * blx + ay * bly + az * blz) / bd))
            * Math.min(1.0, b_fall * LIGHT_BRIGHTNESS * RendererDelegator.blue_light_pct / 100.0);
      }
      // Half-to-full brightness per channel.
      // White point light in fast mode (Tim, 2026-10-03): flat falloff
      // from primitive center, adds equally to all channels.
      final double wdx2 = WHITE_PX - pcx;
      final double wdy2 = WHITE_PY - pcy;
      final double wdz2 = WHITE_PZ - pcz;
      final double wd2 = Math.sqrt(wdx2 * wdx2 + wdy2 * wdy2 + wdz2 * wdz2);
      final double w_fall2 = 1.0 / (1.0 + (wd2 / LIGHT_FALLOFF_K) * (wd2 / LIGHT_FALLOFF_K));
      final int w_add2 = (int) (200.0 * Math.min(1.0, w_fall2
          * LIGHT_BRIGHTNESS * RendererDelegator.white_light_pct / 100.0));
      final int amb2 = ambientBase();
      final int r_scaled = Math.min(255, amb2 + w_add2 + (int) (159.0 * r_factor));
      final int g_scaled = Math.min(255, amb2 + w_add2 + (int) (159.0 * g_factor));
      final int b_scaled = Math.min(255, amb2 + w_add2 + (int) (159.0 * b_factor));
      final int r = (fogged >> 16) & 0xFF;
      final int g = (fogged >> 8) & 0xFF;
      final int b = fogged & 0xFF;
      final int or = (r * r_scaled) >> 8;
      final int og = (g * g_scaled) >> 8;
      final int ob = (b * b_scaled) >> 8;
      return 0xFF000000 | (or << 16) | (og << 8) | ob;
    }

    // RGB point-light diffuse (Tim, 2026-10-03): three colored lights
    // near the top, intensity falls off with distance.
    final double px = ray.ox + ray.dx * hit.t;
    final double py = ray.oy + ray.dy * hit.t;
    final double pz_light = ray.oz + ray.dz * hit.t;
    // Red light.
    final double rlx = RED_PX - px;
    final double rly = RED_PY - py;
    final double rlz = RED_PZ - pz_light;
    final double rd = Math.sqrt(rlx * rlx + rly * rly + rlz * rlz);
    double r_dot = (hit.nx * rlx + hit.ny * rly + hit.nz * rlz) / rd;
    if (r_dot < 0.0) {
      r_dot = -r_dot;
    }
    if (r_dot > 1.0) {
      r_dot = 1.0;
    }
    final double r_fall = 1.0 / (1.0 + (rd / LIGHT_FALLOFF_K) * (rd / LIGHT_FALLOFF_K));
    // Green light.
    final double glx = GREEN_PX - px;
    final double gly = GREEN_PY - py;
    final double glz = GREEN_PZ - pz_light;
    final double gd = Math.sqrt(glx * glx + gly * gly + glz * glz);
    double g_dot = (hit.nx * glx + hit.ny * gly + hit.nz * glz) / gd;
    if (g_dot < 0.0) {
      g_dot = -g_dot;
    }
    if (g_dot > 1.0) {
      g_dot = 1.0;
    }
    final double g_fall = 1.0 / (1.0 + (gd / LIGHT_FALLOFF_K) * (gd / LIGHT_FALLOFF_K));
    // Blue light.
    final double blx = BLUE_PX - px;
    final double bly = BLUE_PY - py;
    final double blz = BLUE_PZ - pz_light;
    final double bd = Math.sqrt(blx * blx + bly * bly + blz * blz);
    double b_dot = (hit.nx * blx + hit.ny * bly + hit.nz * blz) / bd;
    if (b_dot < 0.0) {
      b_dot = -b_dot;
    }
    if (b_dot > 1.0) {
      b_dot = 1.0;
    }
    final double b_fall = 1.0 / (1.0 + (bd / LIGHT_FALLOFF_K) * (bd / LIGHT_FALLOFF_K));
    // White point light (Tim, 2026-10-03): fourth light, like RGB.
    final double wlx = WHITE_PX - px;
    final double wly = WHITE_PY - py;
    final double wlz = WHITE_PZ - pz_light;
    final double wd = Math.sqrt(wlx * wlx + wly * wly + wlz * wlz);
    double w_dot = (hit.nx * wlx + hit.ny * wly + hit.nz * wlz) / wd;
    if (w_dot < 0.0) {
      w_dot = -w_dot;
    }
    if (w_dot > 1.0) {
      w_dot = 1.0;
    }
    final double w_fall = 1.0 / (1.0 + (wd / LIGHT_FALLOFF_K) * (wd / LIGHT_FALLOFF_K));

    // Per-light shadows from the RGB point lights (Tim, 2026-10-03).
    // Each light gets its own shadow factor (1.0 = lit, 0.0 = shadowed);
    // soft shadows give fractional penumbras.
    double r_shadow = 1.0;
    double g_shadow = 1.0;
    double b_shadow = 1.0;
    if (RendererDelegator.shadows) {
      r_shadow = shadowFactor(ray, hit, bvh, stack, shadow_ray, shadow_hit,
          RED_PX, RED_PY, RED_PZ, jitter, px, py, pz_light);
      g_shadow = shadowFactor(ray, hit, bvh, stack, shadow_ray, shadow_hit,
          GREEN_PX, GREEN_PY, GREEN_PZ, jitter, px, py, pz_light);
      b_shadow = shadowFactor(ray, hit, bvh, stack, shadow_ray, shadow_hit,
          BLUE_PX, BLUE_PY, BLUE_PZ, jitter, px, py, pz_light);
      final double w_shadow = shadowFactor(ray, hit, bvh, stack, shadow_ray,
          shadow_hit, WHITE_PX, WHITE_PY, WHITE_PZ, jitter, px, py, pz_light);
      r_dot *= r_shadow;
      g_dot *= g_shadow;
      b_dot *= b_shadow;
      w_dot *= w_shadow;
    }
    // Ambient occlusion (Tim, 2026-10-03): short hemisphere rays darken
    // crevices. Scales the diffuse; specular and fill are unaffected.
    if (RendererDelegator.ambient_occlusion) {
      final int ao_rays = 8;
      int ao_hits = 0;
      for (int i = 0; i < ao_rays; i++) {
        double dx = jitter.nextDouble() * 2.0 - 1.0;
        double dy = jitter.nextDouble() * 2.0 - 1.0;
        double dz = jitter.nextDouble() * 2.0 - 1.0;
        final double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-12) {
          continue;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        // Flip into the hemisphere around the normal.
        if (dx * hit.nx + dy * hit.ny + dz * hit.nz < 0.0) {
          dx = -dx;
          dy = -dy;
          dz = -dz;
        }
        shadow_ray.ox = px + hit.nx;
        shadow_ray.oy = py + hit.ny;
        shadow_ray.oz = pz_light + hit.nz;
        shadow_ray.dx = dx;
        shadow_ray.dy = dy;
        shadow_ray.dz = dz;
        shadow_hit.reset();
        // Only nearby geometry occludes (50000 fixed-point units).
        if (bvh.intersect(shadow_ray, shadow_hit, stack)
            && shadow_hit.t < 50000.0) {
          ao_hits++;
        }
      }
      final double ao = 1.0 - 0.5 * ((double) ao_hits / ao_rays);
      r_dot *= ao;
      g_dot *= ao;
      b_dot *= ao;
    }
    final int amb3 = ambientBase();
    final int r_scaled = Math.min(255, amb3
        + (int) (159.0 * Math.min(1.0, r_dot * r_fall * LIGHT_BRIGHTNESS * RendererDelegator.red_light_pct / 100.0)));
    final int g_scaled = Math.min(255, amb3
        + (int) (159.0 * Math.min(1.0, g_dot * g_fall * LIGHT_BRIGHTNESS * RendererDelegator.green_light_pct / 100.0)));
    final int b_scaled = Math.min(255, amb3
        + (int) (159.0 * Math.min(1.0, b_dot * b_fall * LIGHT_BRIGHTNESS * RendererDelegator.blue_light_pct / 100.0)));
    // White point light (Tim, 2026-10-03): fourth light, adds equally
    // to all channels. Boosted 25% vs RGB (Tim, 2026-10-03) -- the white
    // felt weak at 100%.
    final int w_add = (int) (200.0 * Math.min(1.0, w_dot * w_fall
        * LIGHT_BRIGHTNESS * RendererDelegator.white_light_pct / 100.0));
    final int r_white = Math.min(255, r_scaled + w_add);
    final int g_white = Math.min(255, g_scaled + w_add);
    final int b_white = Math.min(255, b_scaled + w_add);

    final double pz = ray.oz + ray.dz * hit.t;
    final int fogged = Fog.applyFog(hit.primitive.getColour(), (int) pz);

    final int r = (fogged >> 16) & 0xFF;
    final int g = (fogged >> 8) & 0xFF;
    final int b = fogged & 0xFF;
    int or = (r * r_white) >> 8;
    int og = (g * g_white) >> 8;
    int ob = (b * b_white) >> 8;

    // RGB specular reflections (Tim, 2026-10-03): the glossy sheen
    // and specular highlight are computed per light, so they show
    // the light's color. Each is scaled by its light's shadow factor.
    // Quality mode only.
    {
      final int r_sheen;
      final int g_sheen;
      final int b_sheen;
      final int r_highlight;
      final int g_highlight;
      final int b_highlight;
      if (RendererDelegator.glossiness_enabled
          || RendererDelegator.specular_enabled) {
        // Normalized directions toward each light (from diffuse above).
        final double r_lobe = lobeCosineFor(ray, hit,
            rlx / rd, rly / rd, rlz / rd);
        final double g_lobe = lobeCosineFor(ray, hit,
            glx / gd, gly / gd, glz / gd);
        final double b_lobe = lobeCosineFor(ray, hit,
            blx / bd, bly / bd, blz / bd);
        r_sheen = RendererDelegator.glossiness_enabled
            ? (int) (lobeValue(r_lobe, RendererDelegator.glossiness, 8.0)
                * r_fall * LIGHT_BRIGHTNESS * RendererDelegator.red_light_pct / 100.0
                * r_shadow)
            : 0;
        g_sheen = RendererDelegator.glossiness_enabled
            ? (int) (lobeValue(g_lobe, RendererDelegator.glossiness, 8.0)
                * g_fall * LIGHT_BRIGHTNESS * RendererDelegator.green_light_pct / 100.0
                * g_shadow)
            : 0;
        b_sheen = RendererDelegator.glossiness_enabled
            ? (int) (lobeValue(b_lobe, RendererDelegator.glossiness, 8.0)
                * b_fall * LIGHT_BRIGHTNESS * RendererDelegator.blue_light_pct / 100.0
                * b_shadow)
            : 0;
        r_highlight = RendererDelegator.specular_enabled
            ? (int) (lobeValue(r_lobe, RendererDelegator.specular, 32.0)
                * r_fall * LIGHT_BRIGHTNESS * RendererDelegator.red_light_pct / 100.0
                * r_shadow)
            : 0;
        g_highlight = RendererDelegator.specular_enabled
            ? (int) (lobeValue(g_lobe, RendererDelegator.specular, 32.0)
                * g_fall * LIGHT_BRIGHTNESS * RendererDelegator.green_light_pct / 100.0
                * g_shadow)
            : 0;
        b_highlight = RendererDelegator.specular_enabled
            ? (int) (lobeValue(b_lobe, RendererDelegator.specular, 32.0)
                * b_fall * LIGHT_BRIGHTNESS * RendererDelegator.blue_light_pct / 100.0
                * b_shadow)
            : 0;
      } else {
        r_sheen = 0;
        g_sheen = 0;
        b_sheen = 0;
        r_highlight = 0;
        g_highlight = 0;
        b_highlight = 0;
      }
      final int rim = fresnelRim(ray, hit);
      or = softAdd(softAdd(or, r_sheen), rim);
      og = softAdd(softAdd(og, g_sheen), rim);
      ob = softAdd(softAdd(ob, b_sheen), rim);
      // The specular highlight keeps its original hard clip: at full
      // strength it punches through instead of rolling off softly.
      or = Math.min(255, or + r_highlight);
      og = Math.min(255, og + g_highlight);
      ob = Math.min(255, ob + b_highlight);
    }

    // Single-bounce reflections on nodes only (Tim, 2026-10-03).
    if (RendererDelegator.reflections_enabled
        && hit.primitive instanceof RTSphere) {
      // Reflection direction: R = D - 2*(D·N)*N.
      final double d_dot_n = ray.dx * hit.nx + ray.dy * hit.ny + ray.dz * hit.nz;
      final double rx = ray.dx - 2.0 * d_dot_n * hit.nx;
      final double ry = ray.dy - 2.0 * d_dot_n * hit.ny;
      final double rz = ray.dz - 2.0 * d_dot_n * hit.nz;
      // Reuse the shadow scratch ray (shadows are done by now).
      shadow_ray.ox = px + hit.nx;
      shadow_ray.oy = py + hit.ny;
      shadow_ray.oz = pz_light + hit.nz;
      shadow_ray.dx = rx;
      shadow_ray.dy = ry;
      shadow_ray.dz = rz;
      shadow_hit.reset();
      if (bvh.intersect(shadow_ray, shadow_hit, stack) && shadow_hit.primitive != null) {
        // Single bounce: flat base color of what we hit, fogged.
        final double rpx = shadow_ray.ox + shadow_ray.dx * shadow_hit.t;
        final double rpy = shadow_ray.oy + shadow_ray.dy * shadow_hit.t;
        final double rpz = shadow_ray.oz + shadow_ray.dz * shadow_hit.t;
        final int refl_fogged = Fog.applyFog(shadow_hit.primitive.getColour(), (int) rpz);
        final int rr = (refl_fogged >> 16) & 0xFF;
        final int rg = (refl_fogged >> 8) & 0xFF;
        final int rb = refl_fogged & 0xFF;
        // 30% reflection, 70% base.
        or = (int) (or * 0.7 + rr * 0.3);
        og = (int) (og * 0.7 + rg * 0.3);
        ob = (int) (ob * 0.7 + rb * 0.3);
      }
    }

    return 0xFF000000 | (or << 16) | (og << 8) | ob;
  }

  /**
   * Adds white light with a soft shoulder instead of a hard clip at
   * 255: small adds behave linearly, large ones asymptote to 255, so
   * hot spots keep their detail instead of blowing out to flat white.
   */
  private static int softAdd(final int base, final int add) {
    if (add <= 0) {
      return base;
    }
    if (base >= 255) {
      return 255;
    }
    return 255 - (255 - base) * 255 / (255 + add);
  }

  /**
   * True when something blocks the light from the hit point. The ray is
   * nudged off the surface towards the light so it does not shadow
   * itself.
   */
  /**
   * Shadow factor for one point light (Tim, 2026-10-03): 1.0 = fully lit,
   * 0.0 = fully shadowed. For hard shadows a single ray gives a binary
   * result; for soft shadows multiple jittered rays give a fractional
   * penumbra. The ray is only blocked by occluders closer than the light.
   */
  private static double shadowFactor(final Ray ray, final Hit hit, final BVH bvh,
      final int[] stack, final Ray shadow_ray, final Hit shadow_hit,
      final double lightX, final double lightY, final double lightZ,
      final JitterRandom jitter, final double px, final double py, final double pz) {
    final int rays = RendererDelegator.soft_shadows ? 4 : 1;
    int unblocked = 0;
    for (int i = 0; i < rays; i++) {
      // Jitter the light position for soft shadows (area light approx).
      double jx = lightX;
      double jy = lightY;
      double jz = lightZ;
      if (RendererDelegator.soft_shadows) {
        // Radius in fixed-point units (~78 pixels): the penumbra size.
        final double radius = 20000.0;
        jx += (jitter.nextDouble() * 2.0 - 1.0) * radius;
        jy += (jitter.nextDouble() * 2.0 - 1.0) * radius;
        jz += (jitter.nextDouble() * 2.0 - 1.0) * radius;
      }
      double dx = jx - px;
      double dy = jy - py;
      double dz = jz - pz;
      final double dist_to_light = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist_to_light < 1e-12) {
        unblocked++;
        continue;
      }
      dx /= dist_to_light;
      dy /= dist_to_light;
      dz /= dist_to_light;
      // Offset along the normal to avoid self-intersection.
      final double toward_light = hit.nx * dx + hit.ny * dy + hit.nz * dz;
      final double side = toward_light > 0.0 ? 1.0 : -1.0;
      shadow_ray.ox = px + side * hit.nx;
      shadow_ray.oy = py + side * hit.ny;
      shadow_ray.oz = pz + side * hit.nz;
      shadow_ray.dx = dx;
      shadow_ray.dy = dy;
      shadow_ray.dz = dz;
      shadow_hit.reset();
      if (bvh.intersect(shadow_ray, shadow_hit, stack)) {
        // Blocked only if the occluder is closer than the light.
        if (shadow_hit.t >= dist_to_light) {
          unblocked++;
        }
      } else {
        unblocked++;
      }
    }
    return (double) unblocked / rays;
  }

  private static boolean inShadow(final Ray ray, final Hit hit, final BVH bvh,
      int[] stack, final Ray shadow_ray, final Hit shadow_hit) {
    final double px = ray.ox + ray.dx * hit.t;
    final double py = ray.oy + ray.dy * hit.t;
    final double pz = ray.oz + ray.dz * hit.t;
    final double toward_light = hit.nx * LIGHT_X + hit.ny * LIGHT_Y
        + hit.nz * LIGHT_Z;
    final double side = toward_light > 0.0 ? 1.0 : -1.0;
    // Scratch instances owned by the calling tile (one tile per worker
    // thread), so the shadow query allocates nothing per pixel.
    shadow_ray.ox = px + side * hit.nx;
    shadow_ray.oy = py + side * hit.ny;
    shadow_ray.oz = pz + side * hit.nz;
    shadow_ray.dx = LIGHT_X;
    shadow_ray.dy = LIGHT_Y;
    shadow_ray.dz = LIGHT_Z;
    shadow_hit.reset();
    return bvh.intersect(shadow_ray, shadow_hit, stack);
  }

  /**
   * Halfway-vector cosine for a given light direction (not the fixed
   * white light), shared by the glossy sheen and the specular highlight.
   */
  private static double lobeCosineFor(final Ray ray, final Hit hit,
      final double lx, final double ly, final double lz) {
    // Halfway between the light direction and the view direction.
    final double hx = lx - ray.dx;
    final double hy = ly - ray.dy;
    final double hz = lz - ray.dz;
    final double length = Math.sqrt(hx * hx + hy * hy + hz * hz);
    if (length < 1e-12) {
      return 0.0;
    }
    // Double-sided like the diffuse (Tim, 2026-10-04): take the abs so
    // front faces get highlights even if the light is behind.
    final double cosine = Math.abs(hit.nx * hx + hit.ny * hy + hit.nz * hz)
        / length;
    return cosine > 0.0 ? cosine : 0.0;
  }

  private static double lobeCosine(final Ray ray, final Hit hit) {
    // Halfway between the light direction and the view direction.
    final double hx = LIGHT_X - ray.dx;
    final double hy = LIGHT_Y - ray.dy;
    final double hz = LIGHT_Z - ray.dz;
    final double length = Math.sqrt(hx * hx + hy * hy + hz * hz);
    if (length < 1e-12) {
      return 0.0;
    }
    // Double-sided like the diffuse (Tim, 2026-10-04).
    final double cosine = Math.abs(hit.nx * hx + hit.ny * hy + hit.nz * hz)
        / length;
    return cosine > 0.0 ? cosine : 0.0;
  }

  /**
   * Shared lobe math: the halfway-vector cosine raised to the given
   * exponent and scaled by the 0-100 strength. A small exponent gives a
   * broad satin sheen, a large one a tight sparkle. Pure shading -- no
   * rays, so the result is always smooth.
   */
  private static int lobeValue(final double cosine, final int strength,
      double exponent) {
    if (cosine <= 0.0 || strength <= 0) {
      return 0;
    }
    return (int) (255.0 * Math.pow(cosine, exponent) * strength / 100.0);
  }

  /**
   * The Fresnel rim: a view-dependent brightness that follows Schlick's
   * approximation of real reflectivity -- nothing when the surface faces
   * the viewer head-on, rising with the fifth power of the grazing angle
   * to the full configured strength at the silhouette. Returns the 0-255
   * white to add, or 0 when fresnel is off. Pure shading -- no rays, so the
   * result is always smooth.
   */
  private static int fresnelRim(final Ray ray, final Hit hit) {
    if (!RendererDelegator.fresnel_enabled) {
      return 0;
    }
    final int strength = RendererDelegator.fresnel;
    if (strength <= 0) {
      return 0;
    }
    // Cosine between the surface normal and the view direction (the ray
    // points into the scene, so the view direction is its negation).
    // The normal faces the camera on every hit, so this is in [0, 1].
    double cosine = -(hit.nx * ray.dx + hit.ny * ray.dy + hit.nz * ray.dz);
    if (cosine < 0.0) {
      cosine = 0.0;
    } else if (cosine > 1.0) {
      cosine = 1.0;
    }
    final double facing = 1.0 - cosine;
    final double schlick = facing * facing * facing * facing * facing;
    return (int) (strength * 2.55 * schlick);
  }
}
