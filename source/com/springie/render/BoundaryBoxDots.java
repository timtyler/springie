// This program has been placed into the public domain by its author.

package com.springie.render;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

import com.springie.FrEnd;

/**
 * Draws the physics boundary box as a dotted wireframe, one dot per frame.
 *
 * <p>The models run inside the box described by Coords.x_pixels,
 * Coords.y_pixels and Coords.z_pixels. Plotting the whole outline every
 * frame would cost a fillRect per dot; instead a single dot is plotted per
 * frame, cycling through precomputed points spaced along the 8 edges, so
 * the outline builds up over a few hundred frames at negligible per-frame
 * cost (one projection plus one fillRect). Dots are plotted with no depth
 * test: the model's dynamics may overdraw them, which is fine.
 *
 * <p>The draw order is a fixed pseudo-random shuffle (fixed seed), so the
 * outline appears to sparkle on all over rather than tracing the edges in
 * turn, and it is the same every run.
 *
 * <p>Off by default; see FrEnd.show_boundary_box.
 */
public final class BoundaryBoxDots {
  /** Dots per box edge: 8 edges, so this many frames per full outline. */
  private static final int DOTS_PER_EDGE = 16;

  private static final int DOT_COUNT = DOTS_PER_EDGE * 8;

  /** Fixed seed: the draw order shuffles the same way every run. */
  private static final long SHUFFLE_SEED = 0xB0B0L;

  private static final int[] xs = new int[DOT_COUNT];

  private static final int[] ys = new int[DOT_COUNT];

  private static final int[] zs = new int[DOT_COUNT];

  private static int cached_x_pixels = -1;

  private static int cached_y_pixels = -1;

  private static int cached_z_pixels = -1;

  private static int next_dot;

  private BoundaryBoxDots() {
  }

  /**
   * Plots a single white dot of the boundary-box outline. Call once per
   * frame; a no-op unless FrEnd.show_boundary_box is on.
   */
  public static void drawOneDot(Graphics g) {
    if (!FrEnd.show_boundary_box) {
      return;
    }
    rebuildIfResized();
    final int i = next_dot;
    next_dot = (next_dot + 1) % DOT_COUNT;
    final int sx = Coords.getXCoords(xs[i], zs[i]);
    final int sy = Coords.getYCoords(ys[i], zs[i]);
    g.setColor(Color.white);
    g.fillRect(sx, sy, 2, 2);
  }

  /**
   * Recomputes the dot positions (in internal coordinates) when the box
   * dimensions change. Each edge contributes
   * DOTS_PER_EDGE points, excluding its far corner (the next edge starts
   * there).
   */
  private static void rebuildIfResized() {
    if (Coords.x_pixels == cached_x_pixels
        && Coords.y_pixels == cached_y_pixels
        && Coords.z_pixels == cached_z_pixels) {
      return;
    }
    cached_x_pixels = Coords.x_pixels;
    cached_y_pixels = Coords.y_pixels;
    cached_z_pixels = Coords.z_pixels;

    final int x0 = 0;
    final int y0 = 0;
    final int z0 = 0;
    final int x1 = Coords.x_pixels << Coords.shift;
    final int y1 = Coords.y_pixels << Coords.shift;
    final int z1 = Coords.z_pixels << Coords.shift;

    final int[] cx = {x0, x1, x1, x0, x0, x1, x1, x0};
    final int[] cy = {y0, y0, y1, y1, y0, y0, y1, y1};
    final int[] cz = {z0, z0, z0, z0, z1, z1, z1, z1};
    // 8 edges: the 4 back-face edges and the 4 depth edges. The 4
    // front-face edges are implied by the window border itself.
    final int[][] edges = {
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}};

    int n = 0;
    for (final int[] edge : edges) {
      final int ax = cx[edge[0]];
      final int ay = cy[edge[0]];
      final int az = cz[edge[0]];
      final int bx = cx[edge[1]];
      final int by = cy[edge[1]];
      final int bz = cz[edge[1]];
      for (int k = 0; k < DOTS_PER_EDGE; k++) {
        xs[n] = ax + (bx - ax) * k / DOTS_PER_EDGE;
        ys[n] = ay + (by - ay) * k / DOTS_PER_EDGE;
        zs[n] = az + (bz - az) * k / DOTS_PER_EDGE;
        n++;
      }
    }

    // Shuffle the draw order with a fixed seed: the dots appear in a
    // pseudo-random sequence that is identical on every run.
    final Random random = new Random(SHUFFLE_SEED);
    for (int i = DOT_COUNT - 1; i > 0; i--) {
      final int j = random.nextInt(i + 1);
      final int tx = xs[i];
      xs[i] = xs[j];
      xs[j] = tx;
      final int ty = ys[i];
      ys[i] = ys[j];
      ys[j] = ty;
      final int tz = zs[i];
      zs[i] = zs[j];
      zs[j] = tz;
    }
  }
}
