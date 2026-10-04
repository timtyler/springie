// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.FrEnd;
import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import java.awt.Point;
import java.util.ArrayList;

public final class ElementRendererNode {
  private ElementRendererNode() {
    // ...
  }

  static PolygonComposite get(final ObjectBase base, final Node node) {
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
        // RGB light shading (Tim, 2026-10-03): three colored lights.
        // Update positions for current viewport.
        LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
        final double nx = normal.x / (double) (1 << Coords.shift);
        final double ny = normal.y / (double) (1 << Coords.shift);
        final double nz = normal.z / (double) (1 << Coords.shift);
        final double nlen = Math.sqrt(nx * nx + ny * ny + nz * nz);
        // Ambient light (Tim, 2026-10-03).
        final double ambient_n = 0.5 * RendererDelegator.ambient_light_pct / 100.0;
        double r_factor = ambient_n;
        double g_factor = ambient_n;
        double b_factor = ambient_n;
        double r_spec = 0.0;
        double g_spec = 0.0;
        double b_spec = 0.0;
        if (nlen > 1e-12) {
          final double nnx = nx / nlen;
          final double nny = ny / nlen;
          final double nnz = nz / nlen;
          // Red light.
          final double rlx = LightSource.red_px - node.pos.x;
          final double rly = LightSource.red_py - node.pos.y;
          final double rlz = LightSource.red_pz - node.pos.z;
          final double rd = Math.sqrt(rlx * rlx + rly * rly + rlz * rlz);
          if (rd > 1e-12) {
            final double r_dot = Math.abs((nnx * rlx + nny * rly + nnz * rlz) / rd);
            r_factor = Math.min(1.0, ambient_n + 0.75 * r_dot * RendererDelegator.red_light_pct / 50.0);
          }
          // Green light.
          final double glx = LightSource.green_px - node.pos.x;
          final double gly = LightSource.green_py - node.pos.y;
          final double glz = LightSource.green_pz - node.pos.z;
          final double gd = Math.sqrt(glx * glx + gly * gly + glz * glz);
          if (gd > 1e-12) {
            final double g_dot = Math.abs((nnx * glx + nny * gly + nnz * glz) / gd);
            g_factor = Math.min(1.0, ambient_n + 0.75 * g_dot * RendererDelegator.green_light_pct / 50.0);
          }
          // Blue light.
          final double blx = LightSource.blue_px - node.pos.x;
          final double bly = LightSource.blue_py - node.pos.y;
          final double blz = LightSource.blue_pz - node.pos.z;
          final double bd = Math.sqrt(blx * blx + bly * bly + blz * blz);
          if (bd > 1e-12) {
            final double b_dot = Math.abs((nnx * blx + nny * bly + nnz * blz) / bd);
            b_factor = Math.min(1.0, ambient_n + 0.75 * b_dot * RendererDelegator.blue_light_pct / 50.0);
          }
          // White point light (Tim, 2026-10-03).
          final double wlx_n = LightSource.white_px - node.pos.x;
          final double wly_n = LightSource.white_py - node.pos.y;
          final double wlz_n = LightSource.white_pz - node.pos.z;
          final double w_len_n = Math.sqrt(wlx_n * wlx_n + wly_n * wly_n + wlz_n * wlz_n);
          if (w_len_n > 1e-12) {
            final double w_dot = Math.abs((nnx * wlx_n + nny * wly_n
                + nnz * wlz_n) / w_len_n);
            final double w_factor = 0.75 * w_dot
                * RendererDelegator.white_light_pct / 50.0;
            r_factor += w_factor;
            g_factor += w_factor;
            b_factor += w_factor;
          }
          // Specular highlights from the 3 lights (Tim, 2026-10-03).
          // View vector V = (0, 0, -1).
          if (rd > 1e-12) {
            final double lx = rlx / rd;
            final double ly = rly / rd;
            final double lz = rlz / rd;
            final double ndotl = nnx * lx + nny * ly + nnz * lz;
            final double rx = 2.0 * ndotl * nnx - lx;
            final double ry = 2.0 * ndotl * nny - ly;
            final double rz = 2.0 * ndotl * nnz - lz;
            final double rdotv = -rz;
            if (rdotv > 0.0) {
              r_spec = Math.pow(rdotv, 16.0) * RendererDelegator.red_light_pct / 50.0;
            }
          }
          if (gd > 1e-12) {
            final double lx = glx / gd;
            final double ly = gly / gd;
            final double lz = glz / gd;
            final double ndotl = nnx * lx + nny * ly + nnz * lz;
            final double rx = 2.0 * ndotl * nnx - lx;
            final double ry = 2.0 * ndotl * nny - ly;
            final double rz = 2.0 * ndotl * nnz - lz;
            final double rdotv = -rz;
            if (rdotv > 0.0) {
              g_spec = Math.pow(rdotv, 16.0) * RendererDelegator.green_light_pct / 50.0;
            }
          }
          if (bd > 1e-12) {
            final double lx = blx / bd;
            final double ly = bly / bd;
            final double lz = blz / bd;
            final double ndotl = nnx * lx + nny * ly + nnz * lz;
            final double rx = 2.0 * ndotl * nnx - lx;
            final double ry = 2.0 * ndotl * nny - ly;
            final double rz = 2.0 * ndotl * nnz - lz;
            final double rdotv = -rz;
            if (rdotv > 0.0) {
              b_spec = Math.pow(rdotv, 16.0) * RendererDelegator.blue_light_pct / 50.0;
            }
          }
        }
        final int r = (colour >> 16) & 0xFF;
        final int g = (colour >> 8) & 0xFF;
        final int b = colour & 0xFF;
        final int or = Math.min(255, (int) (r * r_factor + 255.0 * r_spec));
        final int og = Math.min(255, (int) (g * g_factor + 255.0 * g_spec));
        final int ob = Math.min(255, (int) (b * b_factor + 255.0 * b_spec));
        final int act_colour = (colour & 0xFF000000) | (or << 16) | (og << 8) | ob;

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

  private static PolygonObject2D[] unNull(final PolygonObject2D[] array) {
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

  public static int getColour(final int colour, final int scaled) {
    final int r = colour & 0xFF;
    final int g = (colour >> 8) & 0xFF;
    final int b = (colour >> 16) & 0xFF;

    final int or = (r * scaled) >> 8;
    final int og = (g * scaled) >> 8;
    final int ob = (b * scaled) >> 8;

    return 0xFF000000 | or | (og << 8) | (ob << 16);
  }

  /**
   * Backface test on projected screen coordinates: true when the polygon
   * winds the way a front-facing (camera-facing) surface does. Also used
   * by ElementRendererLink to cull the far side of link tubes.
   */
  static boolean isVisible(final int[] array_x, final int[] array_y) {
    final Point p1 = new Point(array_x[1] - array_x[0], array_y[1] - array_y[0]);
    final Point p2 = new Point(array_x[1] - array_x[2], array_y[1] - array_y[2]);

    return p1.x * p2.y < p1.y * p2.x;
  }

  static Vector3D getNormal(final ObjectBase base, final int poly_count) {
    // NOT TRUE
    if (base.normals == null) {
      final int size = base.faces.length;
      base.normals = new Vector3D[size];
    }

    Vector3D normal = base.normals[poly_count];
    if (normal == null) {
      final int[] face_data = base.faces[poly_count];
      // NOT TRUE

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

  private static PolygonComposite addSelection(final Node node,
      PolygonComposite composite) {
    final int width = 1200;
    final int colour = RendererDelegator.colour_selected_number;
    final int actual_colour = DeepObjectColourCalculator.getColourOfDeepObject(colour,
        node.pos.z);

    final double radius_in = node.type.radius * 4 / 3;
    final double radius_mid = radius_in + width;
    final double radius_out = radius_in + width + width;

    final int sides = 9;

    // Two polygons per side.
    final ArrayList<PolygonObject2D> polygon_vector = new ArrayList<>(
        sides * 2);
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
      polygon_vector.add(polygon1);

      final Point3D[] point2 = new Point3D[4];
      point2[0] = new Point3D(x6, y6, z);
      point2[1] = new Point3D(x5, y5, z);
      point2[2] = new Point3D(x4, y4, z - width);
      point2[3] = new Point3D(x3, y3, z - width);

      final PolygonObject2D polygon2 = new PolygonObject2D(point2, actual_colour);
      polygon_vector.add(polygon2);
    }

    return combine(polygon_vector, composite);
  }

  private static PolygonComposite addPositiveCharge(final Node node,
      PolygonComposite composite) {
    final int size = node.type.radius / 7;

    final int colour = RendererDelegator.color_charge_number;

    final ArrayList<PolygonObject2D> polygon_vector = new ArrayList<>(1);

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
    polygon_vector.add(polygon1);

    return combine(polygon_vector, composite);
  }

  private static PolygonComposite addNegativeCharge(final Node node,
      PolygonComposite composite) {
    final int size = node.type.radius / 7;

    final int colour = RendererDelegator.color_charge_number;

    final ArrayList<PolygonObject2D> polygon_vector = new ArrayList<>(1);

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
    polygon_vector.add(polygon1);

    return combine(polygon_vector, composite);
  }

  private static PolygonComposite combine(final ArrayList<PolygonObject2D> polygon_vector,
      PolygonComposite composite) {
    final int size_1 = polygon_vector.size();
    final int size_2 = composite.count;

    final PolygonObject2D[] out = new PolygonObject2D[size_1 + size_2];

    for (int i = 0; i < size_1; i++) {
      out[i] = polygon_vector.get(i);
    }

    for (int i = 0; i < size_2; i++) {
      out[i + size_1] = composite.array[i];
    }

    return new PolygonComposite(out, composite.z);
  }
}
