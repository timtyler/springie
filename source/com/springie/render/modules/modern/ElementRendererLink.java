// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.util.Vector;

import com.springie.context.ContextMananger;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.gui.panels.controls.PanelControlsStatistics;
import com.springie.gui.panels.preferences.PanelPreferencesRendererModern;
import com.springie.io.out.WriteFloatingPoint;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

public final class ElementRendererLink {
  public static int strut_divisions = 1;

  public static int cable_divisions = 1;

  public static int colour_bg = 0xFFFFFF00;
  public static int colour_fg = 0xFF0000FF;

  static final int margin_x = 4;
  static final int margin_y = 0;

  private ElementRendererLink() {
    // ...
  }

  public static Vector getPolygon(Link link, Node node_1, Node node_2,
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

    // final Vector3D z_vector = new Vector3D(0, 0, 1);
    // final Vector3D cross_1 = z_vector.crossProduct(delta);

    // Find two normalized vectors.
    final Double3D cross_1 = new Double3D(-delta.y, delta.x, 0);
    cross_1.normalize();

    final Double3D orignial = new Double3D(delta.x, delta.y, delta.z);

    final Double3D cross_2 = cross_1.crossProduct(orignial);
    cross_2.normalize();

    // switch them to integer vectors

    final int actual_thicknesss = thicknesss << Coords.shift;

    final int c1_x = (int) (cross_1.x * actual_thicknesss);
    final int c1_y = (int) (cross_1.y * actual_thicknesss);
    final int c1_z = (int) (cross_1.z * actual_thicknesss);

    final Vector3D cross_1_int = new Vector3D(c1_x, c1_y, c1_z);

    final int c2_x = (int) (cross_2.x * actual_thicknesss);
    final int c2_y = (int) (cross_2.y * actual_thicknesss);
    final int c2_z = (int) (cross_2.z * actual_thicknesss);

    final Vector3D cross_2_int = new Vector3D(c2_x, c2_y, c2_z);

    final Vector return_vector = new Vector();

    int strut_divisions_actual = strut_divisions;
    if (RendererDelegator.fat_struts) {
      if (ContextMananger.getNodeManager().is_tensegrity) {
        if (strut_divisions == 1) {
          strut_divisions_actual = 2;
        }
      }
    }

    final int divisions = link.type.compression ? strut_divisions_actual
        : cable_divisions;
    final boolean simple = divisions == 1;
    final double iv = simple ? 1 : 0.4d;
    final double mult = link.type.compression ? 0.6d : -0.1d;
    int min_z = Integer.MAX_VALUE;

    for (int segment = 0; segment < divisions; segment++) {
      int pologon_count = 0;
      final PolygonObject2D[] array = new PolygonObject2D[2];

      // calculate coordinates of the first rectangle...

      final Point3D[] r1_points = new Point3D[4];

      final int sp0 = segment + 0;
      final int sp1 = segment + 1;

      final double sf1 = iv + mult * Math.sin(Math.PI * sp0 / divisions);
      final double sf2 = iv + mult * Math.sin(Math.PI * sp1 / divisions);

      final Vector3D cross_1_int_sf1 = new Vector3D(cross_1_int);
      cross_1_int_sf1.multiplyBy(sf1);
      final Vector3D cross_1_int_sf2 = new Vector3D(cross_1_int);
      cross_1_int_sf2.multiplyBy(sf2);

      final Vector3D cross_2_int_sf1 = new Vector3D(cross_2_int);
      cross_2_int_sf1.multiplyBy(sf1);
      final Vector3D cross_2_int_sf2 = new Vector3D(cross_2_int);
      cross_2_int_sf2.multiplyBy(sf2);

      final Vector3D partial_start = new Vector3D(point1, point0);
      final Vector3D partial_end = new Vector3D(partial_start);
      partial_start.multiplyBy(sp0);
      partial_start.divideBy(divisions);
      partial_end.multiplyBy(sp1);
      partial_end.divideBy(divisions);

      final Point3D point0n = new Point3D(point0);
      final Point3D point1n = new Point3D(point0);
      point0n.addTuple3D(partial_start);
      point1n.addTuple3D(partial_end);

      r1_points[0] = new Point3D(point0n);
      r1_points[0].addTuple3D(cross_2_int_sf1);
      r1_points[1] = new Point3D(point0n);
      r1_points[1].addTuple3D(cross_1_int_sf1);

      r1_points[2] = new Point3D(point1n);
      r1_points[2].addTuple3D(cross_1_int_sf2);
      r1_points[3] = new Point3D(point1n);
      r1_points[3].addTuple3D(cross_2_int_sf2);

      // calculate coordinates of the second rectangle...

      final Point3D[] r2_points = new Point3D[4];

      r2_points[3] = r1_points[0];
      r2_points[2] = new Point3D(point0n);
      r2_points[2].subtractTuple3D(cross_1_int_sf1);

      r2_points[1] = new Point3D(point1n);
      r2_points[1].subtractTuple3D(cross_1_int_sf2);
      r2_points[0] = r1_points[3];

      final int z0 = point0n.z;
      final int z1 = point1n.z;
      final int z = (z0 + z1) >> 1;
      if (z < min_z) {
        min_z = z;
      }

      final int new_colour = DeepObjectColourCalculator.getColourOfDeepObject(
          colour, z);

      final PolygonObject2D polygon1 = new PolygonObject2D(r1_points,
          new_colour);
      array[pologon_count++] = polygon1;
      final PolygonObject2D polygon2 = new PolygonObject2D(r2_points,
          new_colour);
      array[pologon_count++] = polygon2;

      return_vector.addElement(new PolygonComposite(array, z));
    }

    final int render_label_when = PanelPreferencesRendererModern.render_label_when;

    if ((render_label_when == 1) || ((render_label_when == 3) && link.isSelected())) {
      return_vector.addElement(addRelevantText(link, min_z));
    }

    return return_vector;
  }

  private static PolygonComposite addRelevantText(Link link, int min_z) {
    final int distance_fowards = 6000;
    final Point3D p_c = link.getCoordinatesOfCentrePoint();

    p_c.x -= margin_x << Coords.shift;
    p_c.y -= margin_y << Coords.shift;
    p_c.z -= distance_fowards;

    final int length = link.type.length;

    final double fraction = length
        / (double) PanelControlsStatistics.length_of_shortest_link;

    final String text = emitFloat(fraction);
    final int length_of_text = text.length();

    int d_x = (10 * length_of_text + margin_x + margin_x) << Coords.shift;
    int d_y = (19 + margin_y + margin_y) << Coords.shift;

    final Point3D[] text_points = new Point3D[4];
    text_points[0] = new Point3D(p_c);
    text_points[1] = new Point3D(p_c);
    text_points[2] = new Point3D(p_c);
    text_points[3] = new Point3D(p_c);

    text_points[1].x += d_x;
    text_points[2].x += d_x;

    text_points[0].y += d_y;
    text_points[1].y += d_y;

    final int point_size = Coords.getRadius(4200, p_c.z);
    
    final int new_colour_bg = DeepObjectColourCalculator.getColourOfDeepObject(
        colour_bg, min_z - 1);
    final int new_colour_fg = DeepObjectColourCalculator.getColourOfDeepObject(
        colour_fg, min_z - 1);

    final RenderableText2D polygon_text = new RenderableText2D(text_points,
        new_colour_bg, new_colour_fg, text, point_size);
    final PolygonObject2D[] array = new PolygonObject2D[1];
    array[0] = polygon_text;

    final PolygonComposite pc_text = new PolygonComposite(array, min_z - 1);

    return pc_text;
  }

  private static String emitFloat(final double fraction) {
    final String probable = WriteFloatingPoint.emit((float) fraction, 5, false);
    if (probable.indexOf('.') < 0) {
      return probable + ".0";
    }
    return probable;
  }
}
