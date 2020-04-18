// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Point;
import java.util.Vector;

import com.springie.FrEnd;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

public final class ElementRendererNode {
  private ElementRendererNode() {
    // ...
  }

  static PolygonComposite get(ObjectBase base, Node node) {
    final int actual_radius = node.type.radius;
    final int size = base.faces.length;
    final PolygonObject2D[] array_of_polygons = new PolygonObject2D[size];

    int poly_index = 0;
    for (int poly_count = 0; poly_count < size; poly_count++) {
      final int[] face_data = base.faces[poly_count];
      final int n_points = face_data.length;

      final int[] array_x = new int[n_points];
      final int[] array_y = new int[n_points];
      for (int point_count = 0; point_count < n_points; point_count++) {
        final Double3D d3d = base.points[face_data[point_count]];
        final int x_3d = node.pos.x + (int) (d3d.x * actual_radius);
        final int y_3d = node.pos.y + (int) (d3d.y * actual_radius);
        final int z_3d = node.pos.z + (int) (d3d.z * actual_radius);

        int ix = Coords.getXCoords(x_3d, z_3d);
        int iy = Coords.getYCoords(y_3d, z_3d);

        array_x[point_count] = ix;
        array_y[point_count] = iy;
      }
      if (isVisible(array_x, array_y)) {
        int colour = node.clazz.colour;

        colour = DeepObjectColourCalculator.getColourOfDeepObject(colour,
            node.pos.z);

        final Vector3D normal = getNormal(base, poly_count);
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

        final int act_colour = getColour(colour, scaled);

        PolygonObject2D polygon = new PolygonObject2D(array_x, array_y,
            act_colour);
        array_of_polygons[poly_index++] = polygon;
      }
    }

    // make new array with no nulls!;

    final PolygonObject2D[] no_nulls = unNull(array_of_polygons);

    PolygonComposite composite = new PolygonComposite(no_nulls, node.pos.z - 500);

    if (node.type.selected) {
      composite = addSelection(node, composite);
    }

    if (FrEnd.render_charges) {
      if (node.type.charge > 0) {
        composite = addPositiveCharge(node, composite);
      } else if (node.type.charge < 0) {
        composite = addNegativeCharge(node, composite);
      }
    }

    return composite;
  }

  private static PolygonObject2D[] unNull(PolygonObject2D[] array) {
    // count nulls
    int nc = 0;
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) {
        nc++;
      }
    }

    final PolygonObject2D[] unnulled = new PolygonObject2D[nc];

    int idx = 0;
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) {
        unnulled[idx++] = array[i];
      }
    }

    return unnulled;
  }

  public static int getColour(int colour, int scaled) {
    final int r = colour & 0xFF;
    final int g = (colour >> 8) & 0xFF;
    final int b = (colour >> 16) & 0xFF;

    final int or = (r * scaled) >> 8;
    final int og = (g * scaled) >> 8;
    final int ob = (b * scaled) >> 8;

    return 0xFF000000 | or | (og << 8) | (ob << 16);
  }

  private static boolean isVisible(int[] array_x, int[] array_y) {
    final Point p1 = new Point(array_x[1] - array_x[0], array_y[1] - array_y[0]);
    final Point p2 = new Point(array_x[1] - array_x[2], array_y[1] - array_y[2]);

    return p1.x * p2.y < p1.y * p2.x;
  }

  static Vector3D getNormal(ObjectBase base, int poly_count) {
    // NOT TRUE
    Forget.about(poly_count);
    if (base.normals == null) {
      final int size = base.faces.length;
      base.normals = new Vector3D[size];
    }

    Vector3D normal = base.normals[poly_count];
    if (normal == null) {
      final int[] face_data = base.faces[poly_count];
      // NOT TRUE
      Forget.about(face_data);

      final Double3D point0 = base.points[face_data[0]];
      final Double3D point1 = base.points[face_data[1]];
      final Double3D point2 = base.points[face_data[2]];

      normal = getNormal(point0, point1, point2);

      base.normals[poly_count] = normal;

      // Log.log(">" + ix + " " + iy + " " + iz);
    }

    return normal;
  }

  // given three points, find a normal vector...
  public static Vector3D getNormal(final Double3D point0,
      final Double3D point1, final Double3D point2) {
    Vector3D normal;
    final Double3D v1 = point0.subtract(point1);
    final Double3D v2 = point2.subtract(point1);
    final Double3D cross = v1.crossProduct(v2);
    cross.normalize();

    final int ix = (int) (cross.x * 256);
    final int iy = (int) (cross.y * 256);
    final int iz = (int) (cross.z * 256);

    normal = new Vector3D(ix, iy, iz);
    return normal;
  }

  private static PolygonComposite addSelection(Node node,
      PolygonComposite composite) {
    final int width = 1200;
    final int colour = RendererDelegator.colour_selected_number;
    final int actual_colour = DeepObjectColourCalculator.getColourOfDeepObject(colour,
        node.pos.z);

    final Vector polygon_vector = new Vector();
    final double radius_in = node.type.radius * 4 / 3;
    final double radius_mid = radius_in + width;
    final double radius_out = radius_in + width + width;

    final int sides = 9;
    final double increment = 2 * Math.PI / sides;
    final int x = node.pos.x;
    final int y = node.pos.y;
    final int z = node.pos.z - 2600;
    for (int i = 0; i < sides; i++) {
      final double theta_1 = i * increment;
      final double theta_2 = theta_1 + increment;
      final int x1 = (int) (x + radius_out * Math.cos(theta_1));
      final int x2 = (int) (x + radius_out * Math.cos(theta_2));
      final int x3 = (int) (x + radius_mid * Math.cos(theta_2));
      final int x4 = (int) (x + radius_mid * Math.cos(theta_1));
      final int x5 = (int) (x + radius_in * Math.cos(theta_1));
      final int x6 = (int) (x + radius_in * Math.cos(theta_2));

      final int y1 = (int) (y + radius_out * Math.sin(theta_1));
      final int y2 = (int) (y + radius_out * Math.sin(theta_2));
      final int y3 = (int) (y + radius_mid * Math.sin(theta_2));
      final int y4 = (int) (y + radius_mid * Math.sin(theta_1));
      final int y5 = (int) (y + radius_in * Math.sin(theta_1));
      final int y6 = (int) (y + radius_in * Math.sin(theta_2));

      final Point3D[] point1 = new Point3D[4];
      point1[0] = new Point3D(x1, y1, z);
      point1[1] = new Point3D(x2, y2, z);
      point1[2] = new Point3D(x3, y3, z - width);
      point1[3] = new Point3D(x4, y4, z - width);

      final PolygonObject2D polygon1 = new PolygonObject2D(point1, actual_colour);
      polygon_vector.addElement(polygon1);

      final Point3D[] point2 = new Point3D[4];
      point2[0] = new Point3D(x6, y6, z);
      point2[1] = new Point3D(x5, y5, z);
      point2[2] = new Point3D(x4, y4, z - width);
      point2[3] = new Point3D(x3, y3, z - width);

      final PolygonObject2D polygon2 = new PolygonObject2D(point2, actual_colour);
      polygon_vector.addElement(polygon2);
    }

    return combine(polygon_vector, composite);
  }

  private static PolygonComposite addPositiveCharge(Node node,
      PolygonComposite composite) {
    final int size = node.type.radius / 7;

    final int colour = RendererDelegator.colour_charge_number;

    final Vector polygon_vector = new Vector();

    final int x = node.pos.x;
    final int y = node.pos.y;
    final int z = node.pos.z - node.type.radius - 2000;

    final int x1 = x - size * 3;
    final int x2 = x - size * 1;
    final int x3 = x + size * 1;
    final int x4 = x + size * 3;

    final int y1 = y - size * 3;
    final int y2 = y - size * 1;
    final int y3 = y + size * 1;
    final int y4 = y + size * 3;

    final Point3D[] point1 = new Point3D[12];
    point1[0] = new Point3D(x1, y3, z);
    point1[1] = new Point3D(x1, y2, z);
    point1[2] = new Point3D(x2, y2, z);
    point1[3] = new Point3D(x2, y1, z);
    point1[4] = new Point3D(x3, y1, z);
    point1[5] = new Point3D(x3, y2, z);
    point1[6] = new Point3D(x4, y2, z);
    point1[7] = new Point3D(x4, y3, z);
    point1[8] = new Point3D(x3, y3, z);
    point1[9] = new Point3D(x3, y4, z);
    point1[10] = new Point3D(x2, y4, z);
    point1[11] = new Point3D(x2, y3, z);

    final PolygonObject2D polygon1 = new PolygonObject2D(point1, colour);
    polygon_vector.addElement(polygon1);

    return combine(polygon_vector, composite);
  }

  private static PolygonComposite addNegativeCharge(Node node,
      PolygonComposite composite) {
    final int size = node.type.radius / 7;

    final int colour = RendererDelegator.colour_charge_number;

    final Vector polygon_vector = new Vector();

    final int x = node.pos.x;
    final int y = node.pos.y;
    final int z = node.pos.z - node.type.radius - 2000;

    final int x1 = x - size * 3;
    final int x4 = x + size * 3;

    final int y2 = y - size * 1;
    final int y3 = y + size * 1;

    final Point3D[] point1 = new Point3D[4];
    point1[3] = new Point3D(x1, y3, z);
    point1[2] = new Point3D(x1, y2, z);
    point1[1] = new Point3D(x4, y2, z);
    point1[0] = new Point3D(x4, y3, z);

    final PolygonObject2D polygon1 = new PolygonObject2D(point1, colour);
    polygon_vector.addElement(polygon1);

    return combine(polygon_vector, composite);
  }

  private static PolygonComposite combine(Vector polygon_vector,
      PolygonComposite composite) {
    final int size_1 = polygon_vector.size();
    final int size_2 = composite.array.length;

    final PolygonObject2D[] out = new PolygonObject2D[size_1 + size_2];

    for (int i = 0; i < size_1; i++) {
      out[i] = (PolygonObject2D) polygon_vector.elementAt(i);
    }

    for (int i = 0; i < size_2; i++) {
      out[i + size_1] = composite.array[i];
    }

    return new PolygonComposite(out, composite.z);
  }
}
