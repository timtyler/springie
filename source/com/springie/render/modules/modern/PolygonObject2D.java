// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Color;
import java.awt.Graphics;

import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.Coords;

public class PolygonObject2D {
  int[] x;

  int[] y;

  int colour;

  RectangleInt bounding_box;

  public PolygonObject2D(int[] x, int[] y, int colour) {
    super();
    this.x = x;
    this.y = y;
    this.colour = colour;
  }

  public PolygonObject2D(Point3D[] points, int colour) {
    final int size = points.length;
    this.x = new int[size];
    this.y = new int[size];
    for (int i = 0; i < size; i++) {
      final Point3D point = points[i];

      final int x = Coords.getXCoords(point.x, point.z);
      final int y = Coords.getYCoords(point.y, point.z);
      this.x[i] = x;
      this.y[i] = y;
    }

    final Double3D point_d0 = new Double3D(points[0]);
    final Double3D point_d1 = new Double3D(points[1]);
    final Double3D point_d2 = new Double3D(points[2]);

    final Vector3D normal = ElementRendererNode.getNormal(point_d0, point_d1,
        point_d2);

    final Vector3D light_source = LightSource.source_1;
    final int dot_product = normal.dot(light_source);
    int scaled = dot_product >> (Coords.shift + 1);

    if (scaled < 0) {
      scaled = -scaled;
    }

    if (scaled > 127) {
      scaled = 127;
    }

    scaled += 128;

    final int act_colour = ElementRendererNode.getColour(colour, scaled);

    this.colour = act_colour;
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

  public void fill(Graphics graphics, int colour) {
    graphics.setColor(new Color(colour));
    graphics.fillPolygon(this.x, this.y, this.x.length);
  }
  
  public void draw(Graphics graphics, int colour) {
    graphics.setColor(new Color(colour));
    graphics.drawPolygon(this.x, this.y, this.x.length);
  }
}
