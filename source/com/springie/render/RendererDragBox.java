// This code has been placed into the public domain by its author
package com.springie.render;

import java.awt.Graphics;
import java.awt.Point;

import com.springie.gui.gestures.DragBoxManager;

public class RendererDragBox {
  public Point min = new Point(0, 0);

  public Point max = new Point(0, 0);

  /**
   * The previous frame's rectangle. The tiled renderer skips empty
   * bins, so it must be told to repaint the bins under both the old
   * and the new rectangle -- otherwise the old one leaves a trail.
   */
  public Point last_min = new Point(0, 0);

  public Point last_max = new Point(0, 0);

  /**
   * Whether min/max/last_min/last_max hold a drawn rectangle. The
   * coordinates are only cached on draw, so on the very first frame --
   * and for a zero-area box, which the "max &lt;= min" test cannot tell
   * apart from "never cached" -- callers must fall back to the
   * gesture's live points instead.
   */
  public boolean cache_valid = false;

  public void draw(Graphics g, DragBoxManager drag_box_manager) {
    drawDragBox(g, drag_box_manager);
  }

  private void drawDragBox(Graphics g, DragBoxManager drag_box_manager) {
    g.setClip(0, 0, 9999, 9999);
    if (drag_box_manager.drag_box_end != null) {
      if (drag_box_manager.drag_box_start != null) {
        cacheDragBoxCoordinates(drag_box_manager);

        // No erase step: every frame painted during a drag repaints the
        // damaged region (the bins are forced dirty, the ray-traced
        // frame includes the box's old and new rectangles in its dirty
        // rectangles), which already covers the previous rectangle.
        // Erasing with the background colour here just blinked the
        // rectangle off and on again every paint.
        g.setColor(RendererDelegator.colour_selected);
        drawTheCurrentDragBox(g);
      } else {
        drag_box_manager.drag_box_end = null;
        resetMaxAndMin();
      }
    }
  }

  private void resetMaxAndMin() {
    this.min = new Point(0, 0);
    this.max = new Point(0, 0);
    this.cache_valid = false;
  }

  private void cacheDragBoxCoordinates(DragBoxManager drag_box_manager) {
    final Point one = drag_box_manager.drag_box_start;
    final Point two = drag_box_manager.drag_box_end;

    if (this.cache_valid) {
      this.last_min.x = this.min.x;
      this.last_min.y = this.min.y;
      this.last_max.x = this.max.x;
      this.last_max.y = this.max.y;
    }

    this.min.x = Math.min(one.x, two.x);
    this.max.x = Math.max(one.x, two.x);
    this.min.y = Math.min(one.y, two.y);
    this.max.y = Math.max(one.y, two.y);

    if (!this.cache_valid) {
      // First draw: there is no previous rectangle, so the "last"
      // coordinates are the current ones (not the (0, 0) initial
      // values, which would needlessly widen the damage region).
      this.last_min.x = this.min.x;
      this.last_min.y = this.min.y;
      this.last_max.x = this.max.x;
      this.last_max.y = this.max.y;
      this.cache_valid = true;
    }
  }

  private void drawTheCurrentDragBox(Graphics g) {
    final int min_x_s = Coords.getPixelFromInternalCoords(this.min.x);
    final int max_x_s = Coords.getPixelFromInternalCoords(this.max.x);
    
    final int min_y_s = Coords.getPixelFromInternalCoords(this.min.y);
    final int max_y_s = Coords.getPixelFromInternalCoords(this.max.y);

    drawThickLine(g, min_x_s, min_y_s, min_x_s, max_y_s);
    drawThickLine(g, min_x_s, min_y_s, max_x_s, min_y_s);
    drawThickLine(g, min_x_s, max_y_s, max_x_s, max_y_s);
    drawThickLine(g, max_x_s, min_y_s, max_x_s, max_y_s);
  }

  private void drawThickLine(Graphics g, int min_x, int min_y, int max_x, int max_y) {
    final int x = 3;
    g.fillRect(min_x - x, min_y - x, max_x - min_x + x + x, max_y - min_y + x + x);
  }
}
