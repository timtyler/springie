// This code has been placed into the public domain by its author

package com.springie.render;

/**
 * An integer screen rectangle, min/max in each axis, inclusive. Empty
 * when a min exceeds its max. Shared by the polygon renderer (per-bin
 * content rectangles) and the ray-traced renderer (per-tile dirty
 * rectangles): both union element boxes into one rect per bin/tile and
 * repaint only the union with last frame's rect.
 */
public class RectangleInt {
  public int min_x;

  public int min_y;

  public int max_x;

  public int max_y;

  public RectangleInt(int min_x, int min_y, int max_x, int max_y) {
    super();
    this.min_x = min_x;
    this.min_y = min_y;
    this.max_x = max_x;
    this.max_y = max_y;
  }

  public void setTo(RectangleInt other) {
    this.min_x = other.min_x;
    this.min_y = other.min_y;
    this.max_x = other.max_x;
    this.max_y = other.max_y;
  }

  public void setToUnion(RectangleInt r1, RectangleInt r2) {
    this.min_x = Math.min(r1.min_x, r2.min_x);
    this.max_x = Math.max(r1.max_x, r2.max_x);
    this.min_y = Math.min(r1.min_y, r2.min_y);
    this.max_y = Math.max(r1.max_y, r2.max_y);
  }

  /**
   * Unions the box (x0, y0)..(x1, y1) into this rectangle.
   */
  public void unionBox(long x0, long y0, long x1, long y1) {
    if (x0 < this.min_x) {
      this.min_x = (int) x0;
    }
    if (y0 < this.min_y) {
      this.min_y = (int) y0;
    }
    if (x1 > this.max_x) {
      this.max_x = (int) x1;
    }
    if (y1 > this.max_y) {
      this.max_y = (int) y1;
    }
  }

  /**
   * Whether the rectangle holds no pixels.
   */
  public boolean isEmpty() {
    return this.min_x > this.max_x || this.min_y > this.max_y;
  }
}
