// This program has been placed into the public domain by its author.

package com.springie.render;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Shades the screen area outside the viewpoint grey.
 *
 * <p>The "viewpoint" is the boundary box -- the universe the model runs
 * inside (drawn as the dotted "Viewport overlay"). Panning the view (the
 * Viewpoint tab's Translate X/Y/Z scrollbars) moves the box's projection
 * on the screen; the canvas area outside it -- outside the physics
 * universe -- is shaded grey. With a centred view the box covers the
 * whole canvas, so nothing is shaded.
 *
 * <p>The box's screen rectangle is recomputed only when the viewpoint
 * changes, and the grey is painted on every paint, after the renderers'
 * blits -- a screen-space overlay, like the boundary-box dots. Painting
 * it only on full clears does not survive the ray-traced renderer's
 * whole-frame blits, which repaint the entire canvas on each completed
 * frame. When the box covers the canvas the call is a no-op, and
 * otherwise it is a few fillRects, so animating the model costs nothing
 * extra.
 */
public final class ViewportShade {
  // The viewpoint the cached box rectangle was computed for.
  private static int cached_shift_x = Integer.MIN_VALUE;

  private static int cached_shift_y;

  private static int cached_shift_z;

  private static int cached_w = -1;

  private static int cached_h = -1;

  // Bounding rectangle of the projected box: (box_x0, box_y0) inclusive,
  // (box_x1, box_y1) exclusive.
  private static int box_x0;

  private static int box_y0;

  private static int box_x1;

  private static int box_y1;

  private ViewportShade() {
    // ...
  }

  private static void recomputeIfViewChanged() {
    if (Coords.shift_constant_x == cached_shift_x
        && Coords.shift_constant_y == cached_shift_y
        && Coords.shift_constant_z == cached_shift_z
        && Coords.x_pixels == cached_w
        && Coords.y_pixels == cached_h) {
      return;
    }
    cached_shift_x = Coords.shift_constant_x;
    cached_shift_y = Coords.shift_constant_y;
    cached_shift_z = Coords.shift_constant_z;
    cached_w = Coords.x_pixels;
    cached_h = Coords.y_pixels;

    // The 8 box corners, in internal coordinates -- the same box the
    // boundary-box dots draw.
    final int x1 = Coords.x_pixels << Coords.shift;
    final int y1 = Coords.y_pixels << Coords.shift;
    final int z1 = Coords.z_pixels << Coords.shift;
    final int[] xs = {0, x1, x1, 0, 0, x1, x1, 0};
    final int[] ys = {0, 0, y1, y1, 0, 0, y1, y1};
    final int[] zs = {0, 0, 0, 0, z1, z1, z1, z1};

    int x0 = Integer.MAX_VALUE;
    int y0 = Integer.MAX_VALUE;
    int x2 = Integer.MIN_VALUE;
    int y2 = Integer.MIN_VALUE;
    for (int i = 0; i < 8; i++) {
      // A zero projection divisor (the corner sitting exactly on the eye
      // plane) has nothing to plot -- skip it, like BoundaryBoxDots does.
      if (Coords.shift_constant_z + (zs[i] >> Coords.shift_z) == 0) {
        continue;
      }
      final int sx = Coords.getXCoords(xs[i], zs[i]);
      final int sy = Coords.getYCoords(ys[i], zs[i]);
      if (sx < x0) {
        x0 = sx;
      }
      if (sy < y0) {
        y0 = sy;
      }
      if (sx > x2) {
        x2 = sx;
      }
      if (sy > y2) {
        y2 = sy;
      }
    }
    box_x0 = x0;
    box_y0 = y0;
    // getXCoords returns a pixel; the far edge is exclusive.
    box_x1 = x2 + 1;
    box_y1 = y2 + 1;
  }

  /**
   * Fills the canvas area outside the viewpoint box with grey. Does
   * nothing when the box covers the whole canvas (the normal centred
   * view), so the common case costs nothing.
   */
  public static void shadeOutsideBox(Graphics g) {
    recomputeIfViewChanged();
    final int w = Coords.x_pixels;
    final int h = Coords.y_pixels;
    if (box_x0 <= 0 && box_y0 <= 0 && box_x1 >= w && box_y1 >= h) {
      return;
    }
    // The tile rendering may have left a tight clip behind; paint the
    // whole canvas, then restore the convention the clear path uses.
    g.setClip(0, 0, w, h);
    g.setColor(Color.gray);
    final int ix0 = Math.max(box_x0, 0);
    final int iy0 = Math.max(box_y0, 0);
    final int ix1 = Math.min(box_x1, w);
    final int iy1 = Math.min(box_y1, h);
    if (iy0 > 0) {
      g.fillRect(0, 0, w, iy0);
    }
    if (iy1 < h) {
      g.fillRect(0, iy1, w, h - iy1);
    }
    if (ix0 > 0 && iy1 > iy0) {
      g.fillRect(0, iy0, ix0, iy1 - iy0);
    }
    if (ix1 < w && iy1 > iy0) {
      g.fillRect(ix1, iy0, w - ix1, iy1 - iy0);
    }
    g.setClip(0, 0, 19999, 19999);
  }
}
