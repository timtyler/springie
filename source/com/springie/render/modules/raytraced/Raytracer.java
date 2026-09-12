// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import java.util.Random;
import java.awt.image.BufferedImage;

import com.springie.geometry.Vector3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.ScenicBackground;
import com.springie.render.modules.modern.LightSource;

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
    if (aa <= 1) {
      int i = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          camera.makeRay(x0 + x, y0 + y, ray);
          hit.reset();
          final boolean struck = bvh.intersect(ray, hit, stack);
          final int rgb = struck ? shade(ray, hit, bvh, stack)
              : backgroundAt(scenic, background_rgb, ray, x0 + x, y0 + y);
          pixels[i++] = rgb;
          if (struck && stats != null) {
            stats.add(x, y);
          }
        }
      }
      return;
    }
    final int samples = aa * aa;
    int i = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        long r = 0;
        long g = 0;
        long b = 0;
        boolean struck = false;
        // Stratified jitter: one sample per stratum, randomly placed
        // inside it. Same ray count as the regular grid, but edges near
        // the pixel axes no longer alias in lockstep. Seeded per pixel
        // so a render is deterministic run to run and independent of
        // tile boundaries.
        final Random jitter = new Random(
            (x0 + x) * 73856093L ^ (y0 + y) * 19349663L ^ 0x9E3779B9L);
        for (int sy = 0; sy < aa; sy++) {
          for (int sx = 0; sx < aa; sx++) {
            final double sub_x = x0 + x + (sx + jitter.nextDouble()) / aa;
            final double sub_y = y0 + y + (sy + jitter.nextDouble()) / aa;
            camera.makeRay(sub_x, sub_y, ray);
            hit.reset();
            final int rgb;
            if (bvh.intersect(ray, hit, stack)) {
              rgb = shade(ray, hit, bvh, stack);
              struck = true;
            } else {
              rgb = backgroundAt(scenic, background_rgb, ray,
                  (int) Math.round(sub_x), (int) Math.round(sub_y));
            }
            r += (rgb >> 16) & 0xFF;
            g += (rgb >> 8) & 0xFF;
            b += rgb & 0xFF;
          }
        }
        pixels[i++] = 0xFF000000 | (int) (r / samples) << 16
            | (int) (g / samples) << 8 | (int) (b / samples);
        if (struck && stats != null) {
          stats.add(x, y);
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
  private static int backgroundAt(BufferedImage scenic, int background_rgb,
      Ray ray, int sx, int sy) {
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
  private static int shade(Ray ray, Hit hit, BVH bvh, int[] stack) {
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
      final int sheen = glossySheen(ray, hit);
      final int highlight = specularHighlight(ray, hit);
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
  private static int softAdd(int base, int add) {
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
  private static int fillLight(Hit hit) {
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
    if (!RendererDelegator.specular_enabled) {
      return 0;
    }
    return lobeHighlight(ray, hit, RendererDelegator.specular, 32.0);
  }

  /**
   * The glossy sheen: a broad Blinn-Phong lobe around the perfect mirror
   * direction, so surfaces look satiny rather than speckled. Returns the
   * 0-255 white to add, or 0 when glossiness is off or the geometry
   * faces away.
   */
  private static int glossySheen(Ray ray, Hit hit) {
    if (!RendererDelegator.glossiness_enabled) {
      return 0;
    }
    return lobeHighlight(ray, hit, RendererDelegator.glossiness, 8.0);
  }

  /**
   * The Fresnel rim: a view-dependent brightness that follows Schlick's
   * approximation of real reflectivity -- nothing when the surface faces
   * the viewer head-on, rising with the fifth power of the grazing angle
   * to the full configured strength at the silhouette. Returns the 0-255
   * white to add, or 0 when fresnel is off. Pure shading -- no rays, so the
   * result is always smooth.
   */
  private static int fresnelRim(Ray ray, Hit hit) {
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

  /**
   * Shared lobe math: the halfway vector between the light direction
   * and the view direction, raised to the given exponent and scaled by
   * the 0-100 strength. A small exponent gives a broad satin sheen, a
   * large one a tight sparkle. Pure shading -- no rays, so the result is
   * always smooth.
   */
  private static int lobeHighlight(Ray ray, Hit hit, int strength,
      double exponent) {
    if (strength <= 0) {
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
    return (int) (255.0 * Math.pow(cosine, exponent) * strength / 100.0);
  }
}
