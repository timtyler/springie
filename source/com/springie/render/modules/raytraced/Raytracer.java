// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.geometry.Vector3D;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.LightSource;

/**
 * Renders one bin tile, pixel by pixel. One primary ray per pixel; no
 * shadow or reflection rays yet -- the shade() method is where those will
 * slot in later.
 *
 * <p>Lighting shares the default renderer's configuration
 * (LightSource.source_1) and its feel: brightness runs from half to full
 * with |normal . light|, so nothing ever goes fully black.
 */
final class Raytracer {
  // Background, in 0xAARRGGBB for BufferedImage.setRGB.
  static final int BACKGROUND_RGB;

  private static final double LIGHT_X;

  private static final double LIGHT_Y;

  private static final double LIGHT_Z;

  static {
    final Vector3D source = LightSource.source_1;
    final double length = Math.sqrt(source.x * source.x + source.y * source.y
        + source.z * source.z);
    LIGHT_X = source.x / length;
    LIGHT_Y = source.y / length;
    LIGHT_Z = source.z / length;

    final int bg = RendererDelegator.color_background_number;
    // The background number is already in 0xRRGGBB packing.
    BACKGROUND_RGB = 0xFF000000 | bg;
  }

  private Raytracer() {
    // ...
  }

  static void renderTile(int x0, int y0, int width, int height,
      RayCamera camera, BVH bvh, int[] pixels) {
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

    void add(int x, int y) {
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

  static void renderTile(int x0, int y0, int width, int height,
      RayCamera camera, BVH bvh, int[] pixels, HitStats stats) {
    final Ray ray = new Ray();
    final Hit hit = new Hit();
    final int[] stack = new int[64];
    int i = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        camera.makeRay(x0 + x, y0 + y, ray);
        hit.reset();
        final boolean struck = bvh.intersect(ray, hit, stack);
        final int rgb = struck ? shade(ray, hit) : BACKGROUND_RGB;
        pixels[i++] = rgb;
        if (struck && stats != null) {
          stats.add(x, y);
        }
      }
    }
  }

  /**
   * Direct diffuse shading only. Shadow rays (towards LIGHT_*) and
   * reflection rays (recursing into shade) will be added here.
   *
   * <p>Matches the default renderer: its [128, 255] brightness range, its
   * depth fog, and its packed-colour convention (0xRRGGBB -- red in the
   * high byte, despite some misleading local variable names in the
   * default renderer's colour code; byte positions are preserved all the
   * way to new Color(packed), so they are preserved here too).
   */
  private static int shade(Ray ray, Hit hit) {
    double dot = hit.nx * LIGHT_X + hit.ny * LIGHT_Y + hit.nz * LIGHT_Z;
    if (dot < 0.0) {
      dot = -dot;
    }
    if (dot > 1.0) {
      dot = 1.0;
    }
    final int scaled = 128 + (int) (127.0 * dot);

    final double pz = ray.oz + ray.dz * hit.t;
    final int fogged = Fog.applyFog(hit.primitive.getColour(), (int) pz);

    final int r = (fogged >> 16) & 0xFF;
    final int g = (fogged >> 8) & 0xFF;
    final int b = fogged & 0xFF;
    final int or = (r * scaled) >> 8;
    final int og = (g * scaled) >> 8;
    final int ob = (b * scaled) >> 8;
    return 0xFF000000 | (or << 16) | (og << 8) | ob;
  }
}
