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
 * frame, cycling through precomputed points spaced along the box's 12
 * edges, so the outline builds up over a few hundred frames at negligible
 * per-frame cost (one projection plus one fillRect). Dots are plotted with
 * no depth test: the model's dynamics may overdraw them, which is fine.
 *
 * <p>The front face's edges are only drawn when they would show on
 * screen: with a centred viewport the front face projects off-screen,
 * so the window border implies it and its edges are skipped.
 *
 * <p>The draw order is a fixed pseudo-random shuffle (fixed seed), so the
 * outline appears to sparkle on all over rather than tracing the edges in
 * turn, and it is the same every run.
 *
 * <p>On by default; see FrEnd.show_boundary_box.
 */
public final class BoundaryBoxDots {
  /** Total dots in the outline, shared across however many edges draw. */
  private static final int DOT_COUNT = 128;

  /** Fixed seed: the draw order shuffles the same way every run. */
  private static final long SHUFFLE_SEED = 0xB0B0L;

  private static final int[] xs = new int[DOT_COUNT];

  private static final int[] ys = new int[DOT_COUNT];

  private static final int[] zs = new int[DOT_COUNT];

  private static int cached_x_pixels = -1;

  private static int cached_y_pixels = -1;

  private static int cached_z_pixels = -1;

  private static int cached_view_x = -1;

  private static int cached_view_y = -1;

  private static int cached_view_z = -1;

  private static int next_dot;

  private BoundaryBoxDots() {
  }

  /**
   * Plots a single gray dot of the boundary-box outline. Call once per
   * frame; a no-op unless FrEnd.show_boundary_box is on.
   */
  public static void drawOneDot(Graphics g) {
    if (!FrEnd.show_boundary_box) {
      return;
    }
    rebuildIfViewChanged();
    final int i = next_dot;
    next_dot = (next_dot + 1) % DOT_COUNT;
    // At Z-viewport 0 the front-face dots sit exactly on the eye plane:
    // the projection divides by zero, so there is nothing to plot.
    if (Coords.shift_constant_z + (zs[i] >> Coords.shift_z) == 0) {
      return;
    }
    final int sx = Coords.getXCoords(xs[i], zs[i]);
    final int sy = Coords.getYCoords(ys[i], zs[i]);
    g.setColor(Color.gray);
    g.fillRect(sx, sy, 2, 2);
  }

  /**
   * Whether a box edge would show on screen: any part of its projected
   * segment falls inside the window. The front-face edges project to
   * screen-axis-aligned segments, so the endpoint bounding-box test is
   * exact for them. A zero projection divisor (the face sitting on the
   * eye plane) means there is nothing to plot.
   */
  private static boolean edgeVisibleOnScreen(int ax, int ay, int az,
      int bx, int by, int bz) {
    if (Coords.shift_constant_z + (az >> Coords.shift_z) == 0
        || Coords.shift_constant_z + (bz >> Coords.shift_z) == 0) {
      return false;
    }
    final int sx0 = Coords.getXCoords(ax, az);
    final int sy0 = Coords.getYCoords(ay, az);
    final int sx1 = Coords.getXCoords(bx, bz);
    final int sy1 = Coords.getYCoords(by, bz);
    return Math.max(sx0, sx1) >= 0 && Math.min(sx0, sx1) < Coords.x_pixels
        && Math.max(sy0, sy1) >= 0 && Math.min(sy0, sy1) < Coords.y_pixels;
  }

  /**
   * Recomputes the dot positions (in internal coordinates) when the box
   * dimensions or the viewport offsets change. A viewport change moves the
   * old dots to stale positions, so the screen is cleared and the outline
   * restarts from the first dot.
   */
  private static void rebuildIfViewChanged() {
    if (Coords.x_pixels == cached_x_pixels
        && Coords.y_pixels == cached_y_pixels
        && Coords.z_pixels == cached_z_pixels
        && Coords.shift_constant_x == cached_view_x
        && Coords.shift_constant_y == cached_view_y
        && Coords.shift_constant_z == cached_view_z) {
      return;
    }
    cached_x_pixels = Coords.x_pixels;
    cached_y_pixels = Coords.y_pixels;
    cached_z_pixels = Coords.z_pixels;
    cached_view_x = Coords.shift_constant_x;
    cached_view_y = Coords.shift_constant_y;
    cached_view_z = Coords.shift_constant_z;

    final int x0 = 0;
    final int y0 = 0;
    final int z0 = 0;
    final int x1 = Coords.x_pixels << Coords.shift;
    final int y1 = Coords.y_pixels << Coords.shift;
    final int z1 = Coords.z_pixels << Coords.shift;

    final int[] cx = {x0, x1, x1, x0, x0, x1, x1, x0};
    final int[] cy = {y0, y0, y1, y1, y0, y0, y1, y1};
    final int[] cz = {z0, z0, z0, z0, z1, z1, z1, z1};

    // All 12 box edges: the 4 back-face edges, the 4 depth edges, and the
    // 4 front-face edges. A front-face edge is drawn only when it would
    // show on screen.
    final int[][] edges = new int[12][];
    edges[0] = new int[] {4, 5};
    edges[1] = new int[] {5, 6};
    edges[2] = new int[] {6, 7};
    edges[3] = new int[] {7, 4};
    edges[4] = new int[] {0, 4};
    edges[5] = new int[] {1, 5};
    edges[6] = new int[] {2, 6};
    edges[7] = new int[] {3, 7};
    int edge_count = 8;
    final int[][] front_edges = {
        {0, 3}, {1, 2}, {0, 1}, {2, 3},
    };
    for (final int[] edge : front_edges) {
      if (edgeVisibleOnScreen(cx[edge[0]], cy[edge[0]], cz[edge[0]],
          cx[edge[1]], cy[edge[1]], cz[edge[1]])) {
        edges[edge_count++] = edge;
      }
    }

    // Share the dots across the drawn edges as evenly as possible.
    final int per_edge = DOT_COUNT / edge_count;
    int extra = DOT_COUNT % edge_count;
    int n = 0;
    for (int e = 0; e < edge_count; e++) {
      final int dots_this_edge = per_edge + (extra > 0 ? 1 : 0);
      if (extra > 0) {
        extra--;
      }
      final int ax = cx[edges[e][0]];
      final int ay = cy[edges[e][0]];
      final int az = cz[edges[e][0]];
      final int bx = cx[edges[e][1]];
      final int by = cy[edges[e][1]];
      final int bz = cz[edges[e][1]];
      for (int k = 0; k < dots_this_edge; k++) {
        xs[n] = ax + (bx - ax) * k / dots_this_edge;
        ys[n] = ay + (by - ay) * k / dots_this_edge;
        zs[n] = az + (bz - az) * k / dots_this_edge;
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

    next_dot = 0;
    // Wipe the stale dots with the next repaint; the outline restarts.
    RendererDelegator.repaint_all_objects = true;
  }
}
