// This code has been placed into the public domain by its author
package com.springie.render;

import java.awt.Graphics;
import java.awt.Point;

import com.springie.gui.gestures.DragBoxManager;

public class RendererDragBox {
  public Point min = new Point(0, 0);

  public Point max = new Point(0, 0);

  public void draw(Graphics g, DragBoxManager drag_box_manager) {
    drawDragBox(g, drag_box_manager);
  }

  private void drawDragBox(Graphics g, DragBoxManager drag_box_manager) {
    g.setClip(0, 0, 9999, 9999);
    if (drag_box_manager.drag_box_end != null) {
      g.setColor(RendererDelegator.color_background);
      drawTheCurrentDragBox(g);

      if (drag_box_manager.drag_box_start != null) {
        cacheDragBoxCoordinates(drag_box_manager);

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
  }

  private void cacheDragBoxCoordinates(DragBoxManager drag_box_manager) {
    final Point one = drag_box_manager.drag_box_start;
    final Point two = drag_box_manager.drag_box_end;

    this.min.x = Math.min(one.x, two.x);
    this.max.x = Math.max(one.x, two.x);
    this.min.y = Math.min(one.y, two.y);
    this.max.y = Math.max(one.y, two.y);
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
