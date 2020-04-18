// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.faces.Face;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.RendererDelegator;

public final class ElementRendererFace {
  private ElementRendererFace() {
    // ...
  }

  public static PolygonComposite getPolygon(Face face) {
    int colour;
    if (face.type.selected) {
      colour = RendererDelegator.colour_selected_number;
    } else {
      colour = face.clazz.colour;
    }
    return getPolygonSimple(face, colour);
  }

  private static PolygonComposite getPolygonSimple(Face face, int colour) {
    final int npolygon = face.nodes.size();

    final Point3D center = getCoordsOfCentre(face);

    final int opacity = face.clazz.colour >>> 24;

    final int n = (opacity == 255) ? 1 : Face.number_of_render_divisions;

    final double opacity_d = opacity / 510.0D;

    final double off_in = 0.5 - opacity_d;
    final double off_out = 0.5 + opacity_d;

    final PolygonObject2D[] polygon_array = new PolygonObject2D[npolygon * n];

    for (int i = npolygon; --i >= 0;) {
      final Node node1 = (Node) face.nodes.elementAt(i);
      final Node node2 = (Node) face.nodes.elementAt((i + 1) % npolygon);

      final Vector3D v1 = new Vector3D(node1.pos, center);
      final Vector3D v2 = new Vector3D(node2.pos, center);

      final int new_colour = DeepObjectColourCalculator.getColourOfDeepObject(
          colour, center.z);
      for (int p = 0; p < n; p++) {
        final Point3D[] points = new Point3D[4];
        final int p1_x1 = center.x + (int) (v1.x * (p + off_in) / n);
        final int p1_y1 = center.y + (int) (v1.y * (p + off_in) / n);
        final int p1_z1 = center.z + (int) (v1.z * (p + off_in) / n);
        final int p1_x2 = center.x + (int) (v1.x * (p + off_out) / n);
        final int p1_y2 = center.y + (int) (v1.y * (p + off_out) / n);
        final int p1_z2 = center.z + (int) (v1.z * (p + off_out) / n);

        final int p2_x1 = center.x + (int) (v2.x * (p + off_in) / n);
        final int p2_y1 = center.y + (int) (v2.y * (p + off_in) / n);
        final int p2_z1 = center.z + (int) (v2.z * (p + off_in) / n);
        final int p2_x2 = center.x + (int) (v2.x * (p + off_out) / n);
        final int p2_y2 = center.y + (int) (v2.y * (p + off_out) / n);
        final int p2_z2 = center.z + (int) (v2.z * (p + off_out) / n);

        points[0] = new Point3D(p1_x1, p1_y1, p1_z1);
        points[1] = new Point3D(p1_x2, p1_y2, p1_z2);
        points[2] = new Point3D(p2_x2, p2_y2, p2_z2);
        points[3] = new Point3D(p2_x1, p2_y1, p2_z1);
        polygon_array[i * n + p] = new PolygonObject2D(points, new_colour);
      }
    }

    return new PolygonComposite(polygon_array, center.z);
  }

  private static Point3D getCoordsOfCentre(Face face) {
    final int npoints = face.nodes.size();
    final Point3D sum = new Point3D(0, 0, 0);

    for (int i = npoints; --i >= 0;) {
      final Node n = (Node) face.nodes.elementAt(i);
      sum.addTuple3D(n.pos);
    }

    sum.divideBy(npoints);

    return sum;
  }
}
