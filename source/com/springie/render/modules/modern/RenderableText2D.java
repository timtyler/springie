// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import com.springie.geometry.Point3D;

public class RenderableText2D extends PolygonObject2D {
  String text;

  private int point_size;

  private int colour_fg;

  public RenderableText2D(Point3D[] points, int colour_bg, int colour_fg, String text,
      int point_size) {
    super(points, colour_bg);
    this.text = text;
    this.colour_fg = colour_fg;
    this.point_size = point_size;
  }

  public void fill(Graphics graphics, int colour) {
    graphics.setColor(new Color(colour));
    graphics.fillPolygon(this.x, this.y, this.x.length);

    final Font font = new Font("monospaced", Font.BOLD, this.point_size);

    graphics.setFont(font);
    graphics.setColor(new Color(this.colour_fg));

    final FontMetrics fm = graphics.getFontMetrics();
    final int dy = fm.getDescent();
    final int margin_x = ElementRendererLink.margin_x;
    final int margin_y = ElementRendererLink.margin_y;
    graphics.drawString(this.text, this.x[0] + margin_x, this.y[0] - dy - margin_y);
  }

  public void draw(Graphics graphics, int colour) {
    graphics.setColor(new Color(colour));
    graphics.drawPolygon(this.x, this.y, this.x.length);
  }
}
