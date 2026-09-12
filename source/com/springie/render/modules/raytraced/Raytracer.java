// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.geometry.Vector3D;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.LightSource;

/**
 * Renders one bin tile, pixel by pixel. One primary ray per pixel, plus
 * optional shadow rays, specular highlights, and mirror-reflection rays
 * per the RendererDelegator settings.
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
        final int rgb = struck ? shade(ray, hit, bvh, stack, 0)
            : BACKGROUND_RGB;
        pixels[i++] = rgb;
        if (struck && stats != null) {
          stats.add(x, y);
        }
      }
    }
  }

  /**
   * Diffuse shading with optional shadows, specular highlights, and
   * mirror reflection. Shadow rays (towards LIGHT_*) are traced when
   * RendererDelegator.shadows is set.
   *
   * <p>Matches the default renderer: its [128, 255] brightness range, its
   * depth fog, and its packed-colour convention (0xRRGGBB -- red in the
   * high byte, despite some misleading local variable names in the
   * default renderer's colour code; byte positions are preserved all the
   * way to new Color(packed), so they are preserved here too).
   */
  private static int shade(Ray ray, Hit hit, BVH bvh, int[] stack,
      int depth) {
    double dot = hit.nx * LIGHT_X + hit.ny * LIGHT_Y + hit.nz * LIGHT_Z;
    if (dot < 0.0) {
      dot = -dot;
    }
    if (dot > 1.0) {
      dot = 1.0;
    }

    final boolean shadowed = RendererDelegator.shadows
        && inShadow(ray, hit, bvh, stack);
    if (shadowed) {
      // Ambient light only: the diffuse boost and the specular
      // highlight both need direct light.
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

    if (!shadowed) {
      final int highlight = specularHighlight(ray, hit);
      if (highlight > 0) {
        or = Math.min(255, or + highlight);
        og = Math.min(255, og + highlight);
        ob = Math.min(255, ob + highlight);
      }
    }

    final int glossiness = RendererDelegator.glossiness;
    if (glossiness > 0 && depth < RendererDelegator.max_bounces) {
      final Ray reflected = new Ray();
      reflect(ray, hit, reflected);
      final Hit reflected_hit = new Hit();
      final int reflected_rgb = bvh.intersect(reflected, reflected_hit,
          stack) ? shade(reflected, reflected_hit, bvh, stack, depth + 1)
          : BACKGROUND_RGB;
      final int rr = (reflected_rgb >> 16) & 0xFF;
      final int rg = (reflected_rgb >> 8) & 0xFF;
      final int rb = reflected_rgb & 0xFF;
      final int matte = 100 - glossiness;
      or = (or * matte + rr * glossiness) / 100;
      og = (og * matte + rg * glossiness) / 100;
      ob = (ob * matte + rb * glossiness) / 100;
    }

    return 0xFF000000 | (or << 16) | (og << 8) | ob;
  }

  /**
   * True when something blocks the light from the hit point. The ray is
   * nudged off the surface towards the light so it does not shadow
   * itself.
   */
  private static boolean inShadow(Ray ray, Hit hit, BVH bvh, int[] stack) {
    final double px = ray.ox + ray.dx * hit.t;
    final double py = ray.oy + ray.dy * hit.t;
    final double pz = ray.oz + ray.dz * hit.t;
    final double toward_light = hit.nx * LIGHT_X + hit.ny * LIGHT_Y
        + hit.nz * LIGHT_Z;
    final double side = toward_light > 0.0 ? 1.0 : -1.0;
    final Ray shadow = new Ray();
    shadow.ox = px + side * hit.nx;
    shadow.oy = py + side * hit.ny;
    shadow.oz = pz + side * hit.nz;
    shadow.dx = LIGHT_X;
    shadow.dy = LIGHT_Y;
    shadow.dz = LIGHT_Z;
    final Hit shadow_hit = new Hit();
    return bvh.intersect(shadow, shadow_hit, stack);
  }

  /**
   * Blinn-Phong highlight: how directly the surface reflects the light
   * into the viewer. Returns the 0-255 white to add, or 0 when specular
   * highlights are off or the geometry faces away.
   */
  private static int specularHighlight(Ray ray, Hit hit) {
    final int specular = RendererDelegator.specular;
    if (specular <= 0) {
      return 0;
    }
    // Halfway between the light direction and the view direction.
    final double hx = LIGHT_X - ray.dx;
    final double hy = LIGHT_Y - ray.dy;
    final double hz = LIGHT_Z - ray.dz;
    final double length = Math.sqrt(hx * hx + hy * hy + hz * hz);
    if (length < 1e-12) {
      return 0;
    }
    final double cosine = (hit.nx * hx + hit.ny * hy + hit.nz * hz)
        / length;
    if (cosine <= 0.0) {
      return 0;
    }
    return (int) (255.0 * Math.pow(cosine, 32.0) * specular / 100.0);
  }

  /**
   * Mirror reflection of the incoming ray about the surface normal. The
   * origin is nudged along the normal so the ray does not re-hit the
   * surface it just left.
   */
  private static void reflect(Ray ray, Hit hit, Ray reflected) {
    final double px = ray.ox + ray.dx * hit.t;
    final double py = ray.oy + ray.dy * hit.t;
    final double pz = ray.oz + ray.dz * hit.t;
    final double cosine = ray.dx * hit.nx + ray.dy * hit.ny + ray.dz
        * hit.nz;
    // Nudge off the surface on the side the ray came from, so the ray
    // does not re-hit the surface it just left.
    final double side = cosine < 0.0 ? 1.0 : -1.0;
    reflected.ox = px + side * hit.nx;
    reflected.oy = py + side * hit.ny;
    reflected.oz = pz + side * hit.nz;
    reflected.dx = ray.dx - 2.0 * cosine * hit.nx;
    reflected.dy = ray.dy - 2.0 * cosine * hit.ny;
    reflected.dz = ray.dz - 2.0 * cosine * hit.nz;
  }
}
