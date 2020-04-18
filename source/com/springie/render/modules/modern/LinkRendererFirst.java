// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.Coords;

public final class LinkRendererFirst {
  private LinkRendererFirst() {
    // ...
  }

  public static PolygonObject2D[] getPolygon(Node node_1, Node node_2,
      int thicknesss, int colour) {
    final Point3D point0 = (Point3D) node_1.pos.clone();
    final Point3D point1 = (Point3D) node_2.pos.clone();

    final Vector3D delta = new Vector3D(point0, point1);

    final double length = delta.length();

    final double r1 = node_1.type.radius / length;
    final double r2 = node_2.type.radius / length;

    final Vector3D delta_1 = (Vector3D) delta.clone();
    delta_1.multiplyBy(r1);

    final Vector3D delta_2 = (Vector3D) delta.clone();
    delta_2.multiplyBy(r2);

    point0.subtractTuple3D(delta_1);
    point1.addTuple3D(delta_2);

    // now we have the end points...

    // first cross product: delta

    //final Vector3D z_vector = new Vector3D(0, 0, 1);
    //final Vector3D cross_1 = z_vector.crossProduct(delta);

    //final Vector3D cross_1 = new Vector3D(-delta.y, delta.x, 0);

    //final Vector3D cross_2 = ;

    final int actual_thicknesss = thicknesss << Coords.shift;

    final double dd_x = point0.y - point1.y;
    final double dd_y = point1.x - point0.x;

    int start_length = (int) length - node_1.type.radius - node_2.type.radius;

    if (start_length < 1) {
      start_length = Integer.MAX_VALUE;
    }

    final int d_x = (int) (dd_x * actual_thicknesss) / start_length;
    final int d_y = (int) (dd_y * actual_thicknesss) / start_length;

    final int z0 = point0.z;
    final int z1 = point1.z;

    final int x1 = Coords.getXCoords(point0.x + d_x, z0);
    final int x2 = Coords.getXCoords(point1.x + d_x, z1);
    final int x3 = Coords.getXCoords(point1.x - d_x, z1);
    final int x4 = Coords.getXCoords(point0.x - d_x, z0);

    final int y1 = Coords.getYCoords(point0.y + d_y, z0);
    final int y2 = Coords.getYCoords(point1.y + d_y, z1);
    final int y3 = Coords.getYCoords(point1.y - d_y, z1);
    final int y4 = Coords.getYCoords(point0.y - d_y, z0);

    final int[] iax = {x1, x2, x3, x4, };
    final int[] iay = {y1, y2, y3, y4, };

    final int z = (z0 + z1) >> 1;

    final int new_colour = DeepObjectColourCalculator.getColourOfDeepObject(
        colour, z);

    final PolygonObject2D polygon = new PolygonObject2D(iax, iay, new_colour);

    final PolygonObject2D[] array = new PolygonObject2D[1];

    array[0] = polygon;

    return array;
  }
}
