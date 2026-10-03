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

  /**
   * The fill light: front-right, mirroring the key light's front-left
   * azimuth, so surfaces turned away from the key still model instead
   * of sitting at the flat diffuse floor. Weaker by convention -- the
   * Fill light percentage scales it -- and shadow-independent, the way
   * a photographer's fill lifts the shadows.
   */
  private static final double FILL_X;

  private static final double FILL_Y;

  private static final double FILL_Z;

  static {
    final Vector3D source = LightSource.source_1;
    final double length = Math.sqrt(source.x * source.x + source.y * source.y
        + source.z * source.z);
    LIGHT_X = source.x / length;
    LIGHT_Y = source.y / length;
    LIGHT_Z = source.z / length;

    final double fx = -source.x;
    final double fy = -source.y;
    final double fz = source.z;
    final double fill_length = Math.sqrt(fx * fx + fy * fy + fz * fz);
    FILL_X = fx / fill_length;
    FILL_Y = fy / fill_length;
    FILL_Z = fz / fill_length;
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
      renderTilePixellated(x0, y0, width, height, camera, bvh, rings,
          pixels, stats, scenic, background_rgb, px, aa);
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
  private static void renderTileFloodFill(final int x0, final int y0, final int width,
      int height, final RayCamera camera, final BVH bvh, final RTRing[] rings, final int[] pixels,
      HitStats stats, final BufferedImage scenic, final int background_rgb, final Ray ray,
      Hit hit, final int[] stack, final Ray shadow_ray, final Hit shadow_hit,
      JitterRandom jitter, final int aa) {
    if (RendererDelegator.simple_lighting) {
      // Coarse-to-fine: only trace edges, fill interiors. (Tim, 2026-10-03)
      renderTileCoarseToFine(x0, y0, width, height, camera, bvh, rings,
          pixels, stats, scenic, background_rgb, ray, hit, stack,
          shadow_ray, shadow_hit, jitter, aa);
      return;
    }
    // Blank the tile with the background: no rays, just the colour
    // (or scenic texture) lookup.
    int i = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        camera.makeRay(x0 + x, y0 + y, ray);
        pixels[i++] = backgroundAt(scenic, background_rgb, ray,
            x0 + x, y0 + y);
      }
    }
    final boolean[] visited = new boolean[width * height];
    final int[] flood = new int[width * height];
    int top = 0;
    // 8 uniform seeds per tile (4x2 grid, cell centres). Deterministic,
    // so renders are reproducible. (Tim, 2026-10-03: the small chance
    // of missing a tiny isolated component is acceptable.)
    for (int cy = 0; cy < 2; cy++) {
      for (int cx = 0; cx < 4; cx++) {
        final int sx = Math.min((cx * width + width / 2) / 4, width - 1);
        final int sy = Math.min((cy * height + height / 2) / 2, height - 1);
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
              ? shade(ray, hit, bvh, stack, shadow_ray, shadow_hit)
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
                final int s =
                    shade(ray, hit, bvh, stack, shadow_ray, shadow_hit);
                r += (s >> 16) & 0xFF;
                g += (s >> 8) & 0xFF;
                b += s & 0xFF;
                hit_any = true;
              } else {
                final int s = backgroundAt(scenic, background_rgb, ray,
                    (int) Math.round(sub_x), (int) Math.round(sub_y));
                r += (s >> 16) & 0xFF;
                g += (s >> 8) & 0xFF;
                b += s & 0xFF;
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
          // Seed hit: push its 4-neighbours for the flood fill.
          // (The seed itself is already traced and marked visited.)
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
            ? shade(ray, hit, bvh, stack, shadow_ray, shadow_hit)
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
                  shade(ray, hit, bvh, stack, shadow_ray, shadow_hit);
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
   * Coarse-to-fine tile rendering (Tim, 2026-10-03): only active when
   * Simple lighting is on. Traces a coarse 4x4-block grid via flood
   * fill, then for each hit block decides edge vs interior. Edge blocks
   * (a neighbour is background or a different primitive) are traced at
   * full resolution; interior blocks are filled with the block's colour.
   * For Simple lighting nodes are flat, so the fill is exact; links and
   * faces get the centre colour (a 4px-step approximation, acceptable
   * for the fast low-quality path).
   */
  private static void renderTileCoarseToFine(final int x0, final int y0, final int width,
      int height, final RayCamera camera, final BVH bvh, final RTRing[] rings, final int[] pixels,
      HitStats stats, final BufferedImage scenic, final int background_rgb, final Ray ray,
      Hit hit, final int[] stack, final Ray shadow_ray, final Hit shadow_hit,
      JitterRandom jitter, final int aa) {
    // Blank the tile with the background. When "Show rendering details"
    // is on, blank with red instead: red marks pixels where no ray was
    // traced (the savings). (Tim, 2026-10-03)
    final boolean debug = RendererTileManager.show_active_tiles;
    final int blank_rgb = debug ? 0xFFFF0000 : 0;
    int i = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (debug) {
          pixels[i++] = blank_rgb;
        } else {
          camera.makeRay(x0 + x, y0 + y, ray);
          pixels[i++] = backgroundAt(scenic, background_rgb, ray,
              x0 + x, y0 + y);
        }
      }
    }
    final int cs = 4; // coarse block size
    final int cw = (width + cs - 1) / cs;
    final int ch = (height + cs - 1) / cs;
    final boolean[] c_hit = new boolean[cw * ch];
    final Primitive[] c_prim = new Primitive[cw * ch];
    final int[] c_rgb = new int[cw * ch];
    final boolean[] c_visited = new boolean[cw * ch];
    final int[] c_flood = new int[cw * ch];
    int c_top = 0;
    // 8 uniform seeds at block resolution (4x2 grid, cell centres).
    for (int cy = 0; cy < 2; cy++) {
      for (int cx = 0; cx < 4; cx++) {
        final int bx = Math.min((cx * cw + cw / 2) / 4, cw - 1);
        final int by = Math.min((cy * ch + ch / 2) / 2, ch - 1);
        final int bidx = by * cw + bx;
        if (c_visited[bidx]) {
          continue;
        }
        c_visited[bidx] = true;
        // Trace the block centre.
        final int px = Math.min(bx * cs + cs / 2, width - 1);
        final int py = Math.min(by * cs + cs / 2, height - 1);
        camera.makeRay(x0 + px, y0 + py, ray);
        hit.reset();
        if (intersectScene(ray, hit, bvh, rings, stack)) {
          c_hit[bidx] = true;
          c_prim[bidx] = hit.primitive;
          c_rgb[bidx] =
              shade(ray, hit, bvh, stack, shadow_ray, shadow_hit);
          // Push 4-neighbour blocks (mark visited at push time so the
          // stack can't overflow with duplicates).
          if (bx > 0 && !c_visited[bidx - 1]) {
            c_visited[bidx - 1] = true;
            c_flood[c_top++] = bidx - 1;
          }
          if (bx + 1 < cw && !c_visited[bidx + 1]) {
            c_visited[bidx + 1] = true;
            c_flood[c_top++] = bidx + 1;
          }
          if (by > 0 && !c_visited[bidx - cw]) {
            c_visited[bidx - cw] = true;
            c_flood[c_top++] = bidx - cw;
          }
          if (by + 1 < ch && !c_visited[bidx + cw]) {
            c_visited[bidx + cw] = true;
            c_flood[c_top++] = bidx + cw;
          }
        }
      }
    }
    // Coarse flood fill.
    while (c_top > 0) {
      final int bidx = c_flood[--c_top];
      // (Visited was marked at push time, so no check needed here.)
      final int bx = bidx % cw;
      final int by = bidx / cw;
      final int px = Math.min(bx * cs + cs / 2, width - 1);
      final int py = Math.min(by * cs + cs / 2, height - 1);
      camera.makeRay(x0 + px, y0 + py, ray);
      hit.reset();
      if (intersectScene(ray, hit, bvh, rings, stack)) {
        c_hit[bidx] = true;
        c_prim[bidx] = hit.primitive;
        c_rgb[bidx] = shade(ray, hit, bvh, stack, shadow_ray, shadow_hit);
        if (bx > 0 && !c_visited[bidx - 1]) {
          c_visited[bidx - 1] = true;
          c_flood[c_top++] = bidx - 1;
        }
        if (bx + 1 < cw && !c_visited[bidx + 1]) {
          c_visited[bidx + 1] = true;
          c_flood[c_top++] = bidx + 1;
        }
        if (by > 0 && !c_visited[bidx - cw]) {
          c_visited[bidx - cw] = true;
          c_flood[c_top++] = bidx - cw;
        }
        if (by + 1 < ch && !c_visited[bidx + cw]) {
          c_visited[bidx + cw] = true;
          c_flood[c_top++] = bidx + cw;
        }
      }
    }
    // Refine: edge blocks get full-res traces, interiors get filled.
    for (int by = 0; by < ch; by++) {
      for (int bx = 0; bx < cw; bx++) {
        final int bidx = by * cw + bx;
        if (!c_hit[bidx]) {
          continue;
        }
        // Edge if any 8-neighbour is a miss or a different primitive.
        boolean edge = false;
        for (int dy = -1; dy <= 1 && !edge; dy++) {
          for (int dx = -1; dx <= 1 && !edge; dx++) {
            if (dx == 0 && dy == 0) {
              continue;
            }
            final int nx = bx + dx;
            final int ny = by + dy;
            if (nx < 0 || nx >= cw || ny < 0 || ny >= ch) {
              edge = true; // tile boundary counts as edge
              continue;
            }
            final int nidx = ny * cw + nx;
            if (!c_hit[nidx] || c_prim[nidx] != c_prim[bidx]) {
              edge = true;
            }
          }
        }
        final int x_start = bx * cs;
        final int y_start = by * cs;
        final int x_end = Math.min(x_start + cs, width);
        final int y_end = Math.min(y_start + cs, height);
        if (!edge) {
          // Interior: fill with the block's colour (no rays). When
          // "Show active tiles" is on, paint these ray-saving fills red
          // so the savings are visible. (Tim, 2026-10-03)
          final int rgb = RendererTileManager.show_active_tiles
              ? 0xFFFF0000
              : c_rgb[bidx];
          for (int y = y_start; y < y_end; y++) {
            for (int x = x_start; x < x_end; x++) {
              pixels[y * width + x] = rgb;
            }
          }
          continue;
        }
        // Edge: trace each pixel at full resolution.
        for (int y = y_start; y < y_end; y++) {
          for (int x = x_start; x < x_end; x++) {
            final int idx = y * width + x;
            final int rgb;
            if (aa <= 1) {
              camera.makeRay(x0 + x, y0 + y, ray);
              hit.reset();
              if (intersectScene(ray, hit, bvh, rings, stack)) {
                rgb = shade(ray, hit, bvh, stack, shadow_ray, shadow_hit);
              } else if (debug) {
                // A ray was traced but missed: show the real background,
                // not the red "no ray" marker.
                rgb = backgroundAt(scenic, background_rgb, ray,
                    x0 + x, y0 + y);
              } else {
                rgb = pixels[idx]; // background, already filled
              }
            } else {
              long r = 0, g = 0, b = 0;
              boolean hit_any = false;
              jitter.setSeed((x0 + x) * 73856093L
                  ^ (y0 + y) * 19349663L ^ 0x9E3779B9L);
              for (int sy = 0; sy < aa; sy++) {
                for (int sx = 0; sx < aa; sx++) {
                  final double sub_x =
                      x0 + x + (sx + jitter.nextDouble()) / aa;
                  final double sub_y =
                      y0 + y + (sy + jitter.nextDouble()) / aa;
                  camera.makeRay(sub_x, sub_y, ray);
                  hit.reset();
                  if (intersectScene(ray, hit, bvh, rings, stack)) {
                    final int s = shade(ray, hit, bvh, stack,
                        shadow_ray, shadow_hit);
                    r += (s >> 16) & 0xFF;
                    g += (s >> 8) & 0xFF;
                    b += s & 0xFF;
                    hit_any = true;
                  } else {
                    final int s = backgroundAt(scenic, background_rgb,
                        ray, (int) Math.round(sub_x),
                        (int) Math.round(sub_y));
                    r += (s >> 16) & 0xFF;
                    g += (s >> 8) & 0xFF;
                    b += s & 0xFF;
                  }
                }
              }
              final int samples = aa * aa;
              if (hit_any) {
                rgb = 0xFF000000 | (int) (r / samples) << 16
                    | (int) (g / samples) << 8 | (int) (b / samples);
              } else if (debug) {
                // Rays were traced but all missed: show the real
                // background, not the red "no ray" marker.
                camera.makeRay(x0 + x, y0 + y, ray);
                rgb = backgroundAt(scenic, background_rgb, ray,
                    x0 + x, y0 + y);
              } else {
                rgb = pixels[idx];
              }
            }
            pixels[idx] = rgb;
            if (stats != null) {
              stats.add(x, y);
            }
          }
        }
      }
    }
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
            rgb = shade(ray, hit, bvh, stack, shadow_ray, shadow_hit);
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
                sample_rgb = shade(ray, hit, bvh, stack, shadow_ray, shadow_hit);
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
      Ray shadow_ray, final Hit shadow_hit) {
    if (hit.primitive.isUnlit()) {
      // Overlay indicators like the selection ring: flat colour at
      // full strength from any angle, fogged for depth like the
      // default renderer's depth-shaded selection circle.
      final double pz = ray.oz + ray.dz * hit.t;
      return 0xFF000000 | Fog.applyFog(hit.primitive.getColour(), (int) pz);
    }
    if (RendererDelegator.simple_lighting) {
      // Simple lighting (Tim, 2026-10-03): front-lit, as if the light
      // is at the viewer. Nodes are flat; links and faces get one flat
      // shade level per primitive, from a single dot product. The shade
      // is computed from the primitive's geometry (deterministic), not
      // from the hit order (which flickers as the model animates).
      final double pz = ray.oz + ray.dz * hit.t;
      final int fogged =
          Fog.applyFog(hit.primitive.getColour(), (int) pz);
      if (hit.primitive instanceof RTSphere) {
        // Node: flat base colour with depth fog.
        return 0xFF000000 | fogged;
      }
      final double factor;
      if (hit.primitive instanceof RTCylinder) {
        // Cable: brightness from the axis angle to the fixed view
        // direction (0,0,-1). Side-on (axis in XY plane) is brightest;
        // end-on (axis along Z) is darkest.
        final double nz = ((RTCylinder) hit.primitive).getAxisZ();
        factor = Math.sqrt(Math.max(0.0, 1.0 - nz * nz));
      } else if (hit.primitive instanceof RTEllipsoid) {
        // Strut: same as cable.
        final double nz = ((RTEllipsoid) hit.primitive).getAxisZ();
        factor = Math.sqrt(Math.max(0.0, 1.0 - nz * nz));
      } else {
        // Face (triangle): the geometric normal is constant across the
        // face, so the dot with the fixed view direction is stable.
        double dot = -hit.nz;
        if (dot < 0.0) {
          dot = 0.0;
        }
        if (dot > 1.0) {
          dot = 1.0;
        }
        factor = dot;
      }
      // Same half-to-full brightness range as the default renderer.
      final int scaled = 128 + (int) (127.0 * factor);
      final int r = (fogged >> 16) & 0xFF;
      final int g = (fogged >> 8) & 0xFF;
      final int b = fogged & 0xFF;
      final int or = (r * scaled) >> 8;
      final int og = (g * scaled) >> 8;
      final int ob = (b * scaled) >> 8;
      return 0xFF000000 | (or << 16) | (og << 8) | ob;
    }

    double dot = hit.nx * LIGHT_X + hit.ny * LIGHT_Y + hit.nz * LIGHT_Z;
    if (dot < 0.0) {
      dot = -dot;
    }
    if (dot > 1.0) {
      dot = 1.0;
    }

    final boolean shadowed = RendererDelegator.shadows
        && inShadow(ray, hit, bvh, stack, shadow_ray, shadow_hit);
    if (shadowed) {
      // Ambient light only: the diffuse boost, the glossy sheen and
      // the specular highlight all need direct light.
      dot = 0.0;
    }
    final int scaled = 128 + (int) (127.0 * dot);

    final double pz = ray.oz + ray.dz * hit.t;
    final int fogged = Fog.applyFog(hit.primitive.getColour(), (int) pz);

    final int r = (fogged >> 16) & 0xFF;
    final int g = (fogged >> 8) & 0xFF;
    final int b = fogged & 0xFF;
    int or = (r * scaled) >> 8;
    int og = (g * scaled) >> 8;
    int ob = (b * scaled) >> 8;

    // The fill light is shadow-independent: it lifts the shadowed
    // areas too, the way a photographer's fill does.
    final int fill = fillLight(hit);
    or = softAdd(or, fill);
    og = softAdd(og, fill);
    ob = softAdd(ob, fill);

    if (!shadowed) {
      // The glossy sheen and the specular highlight share the same
      // half-vector: one cosine (and one square root) serves both.
      final int sheen;
      final int highlight;
      if (RendererDelegator.glossiness_enabled
          || RendererDelegator.specular_enabled) {
        final double lobe_cosine = lobeCosine(ray, hit);
        sheen = RendererDelegator.glossiness_enabled
            ? lobeValue(lobe_cosine, RendererDelegator.glossiness, 8.0)
            : 0;
        highlight = RendererDelegator.specular_enabled
            ? lobeValue(lobe_cosine, RendererDelegator.specular, 32.0)
            : 0;
      } else {
        sheen = 0;
        highlight = 0;
      }
      final int rim = fresnelRim(ray, hit);
      or = softAdd(softAdd(or, sheen), rim);
      og = softAdd(softAdd(og, sheen), rim);
      ob = softAdd(softAdd(ob, sheen), rim);
      // The specular highlight keeps its original hard clip: at full
      // strength it punches through to white instead of rolling off
      // softly like the sheen and the rim do.
      or = Math.min(255, or + highlight);
      og = Math.min(255, og + highlight);
      ob = Math.min(255, ob + highlight);
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
   * The fill light: |normal . fill| scaled by the Fill light
   * percentage. Returns the 0-255 white to add, or 0 when the setting
   * is 0. Shadow-independent, so it lifts shadowed areas too.
   *
   * The fill runs at half the key light's strength: 100% fill adds at
   * most ~127, so it models the dark side without flattening it.
   */
  private static int fillLight(final Hit hit) {
    if (!RendererDelegator.fill_light_enabled) {
      return 0;
    }
    final int strength = RendererDelegator.fill_light;
    if (strength <= 0) {
      return 0;
    }
    double dot = hit.nx * FILL_X + hit.ny * FILL_Y + hit.nz * FILL_Z;
    if (dot < 0.0) {
      dot = -dot;
    }
    if (dot > 1.0) {
      dot = 1.0;
    }
    return (int) (strength * 1.275 * dot);
  }

  /**
   * True when something blocks the light from the hit point. The ray is
   * nudged off the surface towards the light so it does not shadow
   * itself.
   */
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
   * The cosine between the surface normal and the Blinn-Phong
   * half-vector (halfway between the light direction and the view
   * direction), shared by the glossy sheen and the specular highlight.
   * Returns 0 when the surface faces away or the vector degenerates,
   * exactly the cases the old per-effect code returned 0 for.
   */
  private static double lobeCosine(final Ray ray, final Hit hit) {
    // Halfway between the light direction and the view direction.
    final double hx = LIGHT_X - ray.dx;
    final double hy = LIGHT_Y - ray.dy;
    final double hz = LIGHT_Z - ray.dz;
    final double length = Math.sqrt(hx * hx + hy * hy + hz * hz);
    if (length < 1e-12) {
      return 0.0;
    }
    final double cosine = (hit.nx * hx + hit.ny * hy + hit.nz * hz)
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
