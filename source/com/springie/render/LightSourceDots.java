// This program has been placed into the public domain by its author.

package com.springie.render;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Draws the four point light sources (red, green, blue, white) as small
 * colored circles. (Tim, 2026-10-03)
 *
 * <p>Keeps it cheap: one fillOval per light per frame, no depth test.
 * Positions are clamped inside the viewport so the lights stay visible.
 */
public final class LightSourceDots {
  /** Radius of the light circles in pixels. */
  private static final int RADIUS = 6;

  /** Draw every Nth frame (Tim, 2026-10-03). */
  private static final int FRAME_SKIP = 16;

  private static int frame_count = 0;

  private LightSourceDots() {
    // Static only.
  }

  /**
   * Draws all four lights at fixed viewport positions (Tim, 2026-10-04):
   * the world-coordinate projection was fragile and lights overlapped or
   * vanished. Dots are UI indicators; the 3D positions in LightSource
   * (used for shading) are untouched.
   * Only draws 1 frame in 16 to keep it cheap.
   */
  public static void draw(final Graphics g) {
    frame_count = (frame_count + 1) % FRAME_SKIP;
    if (frame_count != 0) {
      return;
    }
    final int w = Coords.x_pixels;
    final int h = Coords.y_pixels;
    if (w <= 0 || h <= 0) {
      return;
    }
    // Skip lights at 0% intensity (Tim, 2026-10-03).
    if (RendererDelegator.red_light_pct > 0) {
      drawAt(g, (int) (w * 0.08), (int) (h * 0.08), Color.red);
    }
    if (RendererDelegator.green_light_pct > 0) {
      drawAt(g, (int) (w * 0.08), (int) (h * 0.92), Color.green);
    }
    if (RendererDelegator.blue_light_pct > 0) {
      drawAt(g, (int) (w * 0.92), (int) (h * 0.08), Color.blue);
    }
    if (RendererDelegator.white_light_pct > 0) {
      drawAt(g, (int) (w * 0.92), (int) (h * 0.92), Color.white);
    }
  }

  private static void drawAt(final Graphics g, final int sx, final int sy,
      final Color color) {
    g.setColor(color);
    g.fillOval(sx - RADIUS, sy - RADIUS, RADIUS * 2, RADIUS * 2);
    g.setColor(Color.black);
    g.drawOval(sx - RADIUS, sy - RADIUS, RADIUS * 2, RADIUS * 2);
  }

}
