// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RectangleInt;
import java.awt.Color;
import java.awt.Graphics;

public class PolygonObject2D {
  int[] x;

  int[] y;

  int colour;

  RectangleInt bounding_box;

  /**
   * Frame number (RendererTileManager.render_frame) for which
   * colour_cache_filled / colour_cache_wireframe were computed. The
   * colour modifiers are frame-constant, and the polygon's own colour is
   * rewritten before each frame's render, so one computation per frame
   * is enough however many tiles the polygon lands in.
   */
  int colour_cache_frame = -1;

  int colour_cache_filled;

  int colour_cache_wireframe;

  /**
   * Direct-mapped cache of java.awt.Color by ARGB int. fill/draw used to
   * allocate a Color per polygon per tile per pass -- tens of thousands
   * per frame for a Moscow-sized model. Colours repeat heavily across
   * polygons, tiles and frames, so a small direct-mapped cache hits
   * almost always while staying bounded. Rendering is single-threaded.
   */
  private static final int COLOR_CACHE_MASK = 1023;

  private static final int[] color_cache_keys = new int[1024];

  private static final Color[] color_cache_values = new Color[1024];

  private static final boolean[] color_cache_valid = new boolean[1024];

  static Color colorFor(final int colour) {
    final int slot = (colour ^ (colour >>> 16)) & COLOR_CACHE_MASK;
    if (color_cache_valid[slot] && color_cache_keys[slot] == colour) {
      return color_cache_values[slot];
    }
    final Color color = new Color(colour);
    color_cache_keys[slot] = colour;
    color_cache_values[slot] = color;
    color_cache_valid[slot] = true;
    return color;
  }

  public PolygonObject2D(int[] x, int[] y, int colour) {
    super();
    this.x = x;
    this.y = y;
    this.colour = colour;
  }

  /**
   * Creates an empty polygon with room for size corners. The caller must
   * fill it via set() before use -- for the link render cache, which
   * reuses its polygons across frames instead of allocating per frame.
   */
  PolygonObject2D(final int size) {
    super();
    this.x = new int[size];
    this.y = new int[size];
  }

  public PolygonObject2D(final Point3D[] points, final int colour) {
    super();
    this.x = new int[points.length];
    this.y = new int[points.length];
    set(points, colour);
  }

  /**
   * Rewrites this polygon's projected corners and lit colour in place.
   * The floating-point operations run in exactly the same order as the
   * old constructor (which allocated three Double3Ds and a Vector3D per
   * call), so the result is bit-identical with no allocation.
   */
  void set(final Point3D[] points, int colour) {
    final int size = points.length;
    if (this.x.length != size) {
      this.x = new int[size];
      this.y = new int[size];
    }
    for (int i = 0; i < size; i++) {
      final Point3D point = points[i];

      final int x = Coords.getXCoords(point.x, point.z);
      final int y = Coords.getYCoords(point.y, point.z);
      this.x[i] = x;
      this.y[i] = y;
    }

    // Surface normal, exactly as ElementRendererNode.getNormal over three
    // Double3Ds: v1 = p0 - p1, v2 = p2 - p1, n = normalize(v1 x v2),
    // scaled by 256 into the integer normal.
    final double x0 = points[0].x;
    final double y0 = points[0].y;
    final double z0 = points[0].z;
    final double x1 = points[1].x;
    final double y1 = points[1].y;
    final double z1 = points[1].z;
    final double x2 = points[2].x;
    final double y2 = points[2].y;
    final double z2 = points[2].z;
    final double v1x = x0 - x1;
    final double v1y = y0 - y1;
    final double v1z = z0 - z1;
    final double v2x = x2 - x1;
    final double v2y = y2 - y1;
    final double v2z = z2 - z1;
    double nx = v1y * v2z - v1z * v2y;
    double ny = v1z * v2x - v1x * v2z;
    double nz = v1x * v2y - v1y * v2x;
    final double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
    nx /= len;
    ny /= len;
    nz /= len;

    // N RGB point lights (Tim, 2026-10-05): accumulate diffuse per
    // channel. Face center is the shading point.
    final double cx = (x0 + x1 + x2) / 3.0;
    final double cy = (y0 + y1 + y2) / 3.0;
    final double cz = (z0 + z1 + z2) / 3.0;
    double r_acc = 0.0;
    double g_acc = 0.0;
    double b_acc = 0.0;
    // Ambient base (Tim, 2026-10-05): matches the old +128 offset
    // (128/256 = 50% ambient).
    final double ambient = 0.5;
    synchronized (LightSource.class) {
      for (final com.springie.render.modules.modern.Light light
          : LightSource.lights) {
        final int pct = light.intensity_pct;
        if (pct <= 0) {
          continue;
        }
        final double lx = light.px - cx;
        final double ly = light.py - cy;
        final double lz = light.pz - cz;
        final double llen = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (llen < 1e-9) {
          continue;
        }
        // Diffuse: dot of normal with direction to light. Backfaces
        // (dot < 0) get no contribution (Tim, 2026-10-05).
        double dot = (nx * lx + ny * ly + nz * lz) / llen;
        if (dot < 0.0) {
          dot = 0.0;
        }
        final double contrib = dot * pct / 100.0;
        final int col = light.colour;
        r_acc += contrib * ((col >> 16) & 0xFF) / 255.0;
        g_acc += contrib * ((col >> 8) & 0xFF) / 255.0;
        b_acc += contrib * (col & 0xFF) / 255.0;
      }
    }
    // Scale to 0-255 per channel: ambient + accumulated, clamped.
    int r_scaled = (int) ((ambient + r_acc) * 255.0);
    int g_scaled = (int) ((ambient + g_acc) * 255.0);
    int b_scaled = (int) ((ambient + b_acc) * 255.0);
    if (r_scaled > 255) {
      r_scaled = 255;
    }
    if (g_scaled > 255) {
      g_scaled = 255;
    }
    if (b_scaled > 255) {
      b_scaled = 255;
    }
    // Apply per-channel scaling to the base color.
    final int base_r = colour & 0xFF;
    final int base_g = (colour >> 8) & 0xFF;
    final int base_b = (colour >> 16) & 0xFF;
    final int or = (base_r * r_scaled) >> 8;
    final int og = (base_g * g_scaled) >> 8;
    final int ob = (base_b * b_scaled) >> 8;
    final int act_colour = 0xFF000000 | or | (og << 8) | (ob << 16);

    this.colour = act_colour;
    this.bounding_box = null;
  }

  public RectangleInt getBoundingBox() {
    if (this.bounding_box == null) {
      this.bounding_box = new RectangleInt(Integer.MAX_VALUE,
          Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
      final int size = this.x.length;
      for (int i = 0; i < size; i++) {
        int xx = this.x[i];
        int yy = this.y[i];
        if (xx < this.bounding_box.min_x) {
          this.bounding_box.min_x = xx;
        }
        if (xx + 1 > this.bounding_box.max_x) {
          this.bounding_box.max_x = xx + 1;
        }
        if (yy < this.bounding_box.min_y) {
          this.bounding_box.min_y = yy;
        }
        if (yy + 1 > this.bounding_box.max_y) {
          this.bounding_box.max_y = yy + 1;
        }
      }
    }
    return this.bounding_box;
  }

  public void fill(final Graphics graphics, final int colour) {
    graphics.setColor(colorFor(colour));
    graphics.fillPolygon(this.x, this.y, this.x.length);
  }

  public void draw(final Graphics graphics, final int colour) {
    graphics.setColor(colorFor(colour));
    graphics.drawPolygon(this.x, this.y, this.x.length);
  }
}
