// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.util.ArrayList;

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
  // Scratch objects reused across calls to avoid per-frame allocation.
  // Rendering is single-threaded, and these are never held across calls.
  private static final Point3D scratch_point0 = new Point3D(0, 0, 0);
  private static final Point3D scratch_point1 = new Point3D(0, 0, 0);
  private static final Point3D scratch_point0n = new Point3D(0, 0, 0);
  private static final Point3D scratch_point1n = new Point3D(0, 0, 0);
  private static final Vector3D scratch_delta = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_delta_1 = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_delta_2 = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_cross_1_int = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_cross_2_int = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_partial_start = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_partial_end = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_tube_dir = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_tube_tmp = new Vector3D(0, 0, 0);
  private static final Double3D scratch_cross_1 = new Double3D(0, 0, 0);
  private static final Double3D scratch_original = new Double3D(0, 0, 0);
  private static final Double3D scratch_cross_2 = new Double3D(0, 0, 0);

  public static int strut_divisions = 1;

  public static int cable_divisions = 1;

  public static int colour_bg = 0xFFFFFF00;
  public static int colour_fg = 0xFF0000FF;

  static final int margin_x = 4;
  static final int margin_y = 0;

  private ElementRendererLink() {
    // ...
  }

  public static ArrayList<PolygonComposite> getPolygon(Link link, Node node_1, Node node_2,
      int thicknesss, int colour) {
    final Point3D point0 = scratch_point0;
    point0.set(node_1.pos);
    final Point3D point1 = scratch_point1;
    point1.set(node_2.pos);

    final Vector3D delta = scratch_delta;
    delta.set(point0);
    delta.subtractTuple3D(point1);

    final double length = delta.length();

    final double r1 = node_1.type.radius / length;
    final double r2 = node_2.type.radius / length;

    final Vector3D delta_1 = scratch_delta_1;
    delta_1.set(delta);
    delta_1.multiplyBy(r1);

    final Vector3D delta_2 = scratch_delta_2;
    delta_2.set(delta);
    delta_2.multiplyBy(r2);

    point0.subtractTuple3D(delta_1);
    point1.addTuple3D(delta_2);

    // now we have the end points...

    // first cross product: delta

    // final Vector3D z_vector = new Vector3D(0, 0, 1);
    // final Vector3D cross_1 = z_vector.crossProduct(delta);

    // Find two normalized vectors.
    final Double3D cross_1 = scratch_cross_1;
    cross_1.set(-delta.y, delta.x, 0);
    cross_1.normalize();

    final Double3D orignial = scratch_original;
    orignial.set(delta.x, delta.y, delta.z);

    final Double3D cross_2 = scratch_cross_2;
    cross_2.setCrossProduct(cross_1, orignial);
    cross_2.normalize();

    // switch them to integer vectors

    final int actual_thicknesss = thicknesss << Coords.shift;

    final int c1_x = (int) (cross_1.x * actual_thicknesss);
    final int c1_y = (int) (cross_1.y * actual_thicknesss);
    final int c1_z = (int) (cross_1.z * actual_thicknesss);

    final Vector3D cross_1_int = scratch_cross_1_int;
    cross_1_int.set(c1_x, c1_y, c1_z);

    final int c2_x = (int) (cross_2.x * actual_thicknesss);
    final int c2_y = (int) (cross_2.y * actual_thicknesss);
    final int c2_z = (int) (cross_2.z * actual_thicknesss);

    final Vector3D cross_2_int = scratch_cross_2_int;
    cross_2_int.set(c2_x, c2_y, c2_z);

    // Struts and cables render as an open tube with link_sides sides:
    // link_sides quads around the axis. The ends are left open; capping
    // them costs polygons without helping the picture.
    final int sides = RendererDelegator.link_sides;

    final int divisions = link.type.compression ? strut_divisions
        : cable_divisions;

    // One composite per segment, plus room for the optional text label.
    final ArrayList<PolygonComposite> return_vector = new ArrayList<>(
        divisions + 1);
    final boolean simple = divisions == 1;
    final double iv = simple ? 1 : 0.4d;
    final double mult = link.type.compression ? 0.6d : -0.1d;
    int min_z = Integer.MAX_VALUE;

    for (int segment = 0; segment < divisions; segment++) {
      final int sp0 = segment + 0;
      final int sp1 = segment + 1;

      final double sf1 = iv + mult * Math.sin(Math.PI * sp0 / divisions);
      final double sf2 = iv + mult * Math.sin(Math.PI * sp1 / divisions);

      final Vector3D partial_start = scratch_partial_start;
      partial_start.set(point1);
      partial_start.subtractTuple3D(point0);
      final Vector3D partial_end = scratch_partial_end;
      partial_end.set(partial_start);
      partial_start.multiplyBy(sp0);
      partial_start.divideBy(divisions);
      partial_end.multiplyBy(sp1);
      partial_end.divideBy(divisions);

      final Point3D point0n = scratch_point0n;
      point0n.set(point0);
      final Point3D point1n = scratch_point1n;
      point1n.set(point0);
      point0n.addTuple3D(partial_start);
      point1n.addTuple3D(partial_end);

      final int z0 = point0n.z;
      final int z1 = point1n.z;
      final int z = (z0 + z1) >> 1;
      if (z < min_z) {
        min_z = z;
      }

      final int new_colour = DeepObjectColourCalculator.getColourOfDeepObject(
          colour, z);

      final PolygonObject2D[] array = new PolygonObject2D[sides];
      for (int side = 0; side < sides; side++) {
        final double a0 = 2.0 * Math.PI * side / sides;
        final double a1 = 2.0 * Math.PI * (side + 1) / sides;
        array[side] = tubeQuad(point0n, point1n, cross_1_int, cross_2_int,
            Math.cos(a0), Math.sin(a0), Math.cos(a1), Math.sin(a1), sf1, sf2,
            new_colour);
      }

      return_vector.add(new PolygonComposite(array, z));
    }

    final int render_label_when = PanelPreferencesRendererModern.render_label_when;

    if ((render_label_when == 1) || ((render_label_when == 3) && link.isSelected())) {
      return_vector.add(addRelevantText(link, min_z));
    }

    return return_vector;
  }

  /**
   * One side of the open tube: the quad between two adjacent cross-section
   * directions. The tube ends are left open (no caps).
   */
  private static PolygonObject2D tubeQuad(Point3D point0n, Point3D point1n,
      Vector3D cross_1_int, Vector3D cross_2_int,
      double cos_a, double sin_a, double cos_b, double sin_b,
      double sf1, double sf2, int new_colour) {
    final Point3D[] quad_points = new Point3D[4];
    quad_points[0] = tubeCorner(point0n, cross_1_int, cross_2_int,
        cos_a, sin_a, sf1);
    quad_points[1] = tubeCorner(point0n, cross_1_int, cross_2_int,
        cos_b, sin_b, sf1);
    quad_points[2] = tubeCorner(point1n, cross_1_int, cross_2_int,
        cos_b, sin_b, sf2);
    quad_points[3] = tubeCorner(point1n, cross_1_int, cross_2_int,
        cos_a, sin_a, sf2);
    return new PolygonObject2D(quad_points, new_colour);
  }

  /**
   * One tube corner: base + sf * (cos_a * cross_1 + sin_a * cross_2).
   */
  private static Point3D tubeCorner(Point3D base, Vector3D cross_1_int,
      Vector3D cross_2_int, double cos_a, double sin_a, double sf) {
    final Vector3D dir = scratch_tube_dir;
    dir.set(cross_1_int);
    dir.multiplyBy(cos_a);
    final Vector3D tmp = scratch_tube_tmp;
    tmp.set(cross_2_int);
    tmp.multiplyBy(sin_a);
    dir.addTuple3D(tmp);
    dir.multiplyBy(sf);
    final Point3D corner = new Point3D(base);
    corner.addTuple3D(dir);
    return corner;
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
