// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.geometry.Vector3D;

public class ObjectBase {
  Double3D[] points;

  int[][] faces;

  Vector3D[] normals;

//  PolygonObject2D[] get(int x, int y, int z, int radius, int colour) {
//    // int actual_radius = Coords.getInternalFromPixelCoords(radius);
//    final int actual_radius = radius;
//    final int size = this.faces.length;
//    final PolygonObject2D[] array_of_polygons = new PolygonObject2D[size];
//
//    int poly_index = 0;
//    for (int poly_count = 0; poly_count < size; poly_count++) {
//      final int[] face_data = this.faces[poly_count];
//      final int n_points = face_data.length;
//
//      final int[] array_x = new int[n_points];
//      final int[] array_y = new int[n_points];
//      for (int point_count = 0; point_count < n_points; point_count++) {
//        final Double3D d3d = this.points[face_data[point_count]];
//        final int x_3d = x + (int) (d3d.x * actual_radius);
//        final int y_3d = y + (int) (d3d.y * actual_radius);
//        final int z_3d = z + (int) (d3d.z * actual_radius);
//
//        int ix = Coords.getXCoords(x_3d, z_3d);
//        int iy = Coords.getYCoords(y_3d, z_3d);
//
//        array_x[point_count] = ix;
//        array_y[point_count] = iy;
//      }
//      if (isVisible(array_x, array_y)) {
//        final Vector3D normal = getNormal(poly_count);
//        final Vector3D light_source = LightSource.source_1;
//        final int dot_product = normal.dot(light_source);
//        int scaled = (dot_product + 65536) >> 9;
//        // Log.log("scaled:" + scaled);
//
//        if (scaled > 255) {
//          scaled = 255;
//        }
//        if (scaled < 0) {
//          scaled = 0;
//        }
//        final int act_colour = getColour(colour, scaled);
//
//        PolygonObject2D polygon = new PolygonObject2D(array_x, array_y, act_colour,
//            z);
//        array_of_polygons[poly_index++] = polygon;
//      }
//    }
//
//    return array_of_polygons;
//  }
//
//  public static int getColour(int colour, int scaled) {
//    final int r = colour & 0xFF;
//    final int g = (colour >> 8) & 0xFF;
//    final int b = (colour >> 16) & 0xFF;
//
//    final int or = (r * scaled) >> 8;
//    final int og = (g * scaled) >> 8;
//    final int ob = (b * scaled) >> 8;
//
//    return 0xFF000000 | or | (og << 8) | (ob << 16);
//  }
//
//  private boolean isVisible(int[] array_x, int[] array_y) {
//    final Point p1 = new Point(array_x[1] - array_x[0], array_y[1] - array_y[0]);
//    final Point p2 = new Point(array_x[1] - array_x[2], array_y[1] - array_y[2]);
//
//    return p1.x * p2.y < p1.y * p2.x;
//  }
//
//  Vector3D getNormal(int poly_count) {
//    if (this.normals == null) {
//      final int size = this.faces.length;
//      this.normals = new Vector3D[size];
//    }
//
//    Vector3D normal = this.normals[poly_count];
//    if (normal == null) {
//      final int[] face_data = this.faces[poly_count];
//
//      final Double3D point0 = this.points[face_data[0]];
//      final Double3D point1 = this.points[face_data[1]];
//      final Double3D point2 = this.points[face_data[2]];
//
//      normal = getNormal(point0, point1, point2);
//
//      this.normals[poly_count] = normal;
//
//      //Log.log(">" + ix + " " + iy + " " + iz);
//    }
//
//    return normal;
//  }
//
//  // given three points, find a normal vector...
//  public static Vector3D getNormal(final Double3D point0, final Double3D point1, final Double3D point2) {
//    Vector3D normal;
//    final Double3D v1 = point0.subtract(point1);
//    final Double3D v2 = point2.subtract(point1);
//    final Double3D cross = v1.crossProduct(v2);
//    cross.normalize();
//
//    final int ix = (int) (cross.x * 256);
//    final int iy = (int) (cross.y * 256);
//    final int iz = (int) (cross.z * 256);
//
//    normal = new Vector3D(ix, iy, iz);
//    return normal;
//  }
}
