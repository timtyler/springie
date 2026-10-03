// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.faces.Face;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.RendererDelegator;

public final class ElementRendererFace {
  // Scratch objects reused across calls to avoid per-frame allocation.
  // Rendering is single-threaded, and these are never held across calls.
  private static final Point3D scratch_center = new Point3D(0, 0, 0);
  private static final Vector3D scratch_v1 = new Vector3D(0, 0, 0);
  private static final Vector3D scratch_v2 = new Vector3D(0, 0, 0);

  private ElementRendererFace() {
    // ...
  }

  public static PolygonComposite getPolygon(final Face face) {
    int colour;
    if (face.type.selected) {
      colour = RendererDelegator.colour_selected_number;
    } else {
      colour = face.clazz.colour;
    }
    return getPolygonSimple(face, colour);
  }

  private static PolygonComposite getPolygonSimple(final Face face, final int colour) {
    final int npolygon = face.nodes.size();

    final Point3D center = scratch_center;
    getCoordsOfCentre(face, center);

    // RGB light shading (Tim, 2026-10-03): based on angle between
    // polygon and viewer, and polygon and light source.
    final int rgb_shaded_colour = applyRgbLights(face, colour, center);

    final int opacity = face.clazz.colour >>> 24;

    // "Face lines = 0" fills the face in a solid colour: a single
    // full-coverage quad per edge, whatever the face's opacity.
    final boolean solid_fill = opacity == 255
        || Face.number_of_render_divisions == 0;

    final int n = solid_fill ? 1 : Face.number_of_render_divisions;

    final double opacity_d = solid_fill ? 0.5 : opacity / 510.0D;

    final double off_in = 0.5 - opacity_d;
    final double off_out = 0.5 + opacity_d;

    final PolygonObject2D[] polygon_array = new PolygonObject2D[npolygon * n];

    for (int i = npolygon; --i >= 0;) {
      final Node node1 = (Node) face.nodes.get(i);
      final Node node2 = (Node) face.nodes.get((i + 1) % npolygon);

      final Vector3D v1 = scratch_v1;
      v1.set(node1.pos);
      v1.subtractTuple3D(center);
      final Vector3D v2 = scratch_v2;
      v2.set(node2.pos);
      v2.subtractTuple3D(center);

      final int new_colour = DeepObjectColourCalculator.getColourOfDeepObject(
          rgb_shaded_colour, center.z);
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

  private static void getCoordsOfCentre(final Face face, final Point3D sum) {
    final int npoints = face.nodes.size();
    sum.set(0, 0, 0);

    for (int i = npoints; --i >= 0;) {
      final Node n = (Node) face.nodes.get(i);
      sum.addTuple3D(n.pos);
    }

    sum.divideBy(npoints);
  }

  /**
   * RGB light shading for polygon faces (Tim, 2026-10-03): modulates
   * the colour based on the angle between the polygon normal and the
   * viewer, and between the normal and each RGB light source.
   */
  private static int applyRgbLights(final Face face, final int colour,
      final Point3D center) {
    final int npolygon = face.nodes.size();
    if (npolygon < 3) {
      return colour;
    }
    // Compute face normal from first three nodes.
    final Node n0 = (Node) face.nodes.get(0);
    final Node n1 = (Node) face.nodes.get(1);
    final Node n2 = (Node) face.nodes.get(2);
    final double e1x = n1.pos.x - n0.pos.x;
    final double e1y = n1.pos.y - n0.pos.y;
    final double e1z = n1.pos.z - n0.pos.z;
    final double e2x = n2.pos.x - n0.pos.x;
    final double e2y = n2.pos.y - n0.pos.y;
    final double e2z = n2.pos.z - n0.pos.z;
    double nx = e1y * e2z - e1z * e2y;
    double ny = e1z * e2x - e1x * e2z;
    double nz = e1x * e2y - e1y * e2x;
    final double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
    if (len < 1e-12) {
      return colour;
    }
    nx /= len;
    ny /= len;
    nz /= len;
    // View direction (orthographic): viewer looks down +Z, so the
    // direction to the viewer is (0, 0, -1).
    final double view_dot = Math.abs(nz);
    // Update light positions for current viewport.
    LightSource.updateForViewport(com.springie.render.Coords.x_pixelso2,
        com.springie.render.Coords.y_pixelso2);
    // Red light.
    final double rlx = LightSource.red_px - center.x;
    final double rly = LightSource.red_py - center.y;
    final double rlz = LightSource.red_pz - center.z;
    final double rd = Math.sqrt(rlx * rlx + rly * rly + rlz * rlz);
    final double r_dot = Math.abs((nx * rlx + ny * rly + nz * rlz) / rd);
    // Green light.
    final double glx = LightSource.green_px - center.x;
    final double gly = LightSource.green_py - center.y;
    final double glz = LightSource.green_pz - center.z;
    final double gd = Math.sqrt(glx * glx + gly * gly + glz * glz);
    final double g_dot = Math.abs((nx * glx + ny * gly + nz * glz) / gd);
    // Blue light.
    final double blx = LightSource.blue_px - center.x;
    final double bly = LightSource.blue_py - center.y;
    final double blz = LightSource.blue_pz - center.z;
    final double bd = Math.sqrt(blx * blx + bly * bly + blz * blz);
    final double b_dot = Math.abs((nx * blx + ny * bly + nz * blz) / bd);
    // Combine: 25% ambient + 75% diffuse (view * light). RGB is
    // prominent; the white ambient is dimmer. (Tim, 2026-10-03)
    final double r_factor = 0.25 + 0.75 * view_dot * r_dot * RendererDelegator.red_light_pct / 50.0;
    final double g_factor = 0.25 + 0.75 * view_dot * g_dot * RendererDelegator.green_light_pct / 50.0;
    final double b_factor = 0.25 + 0.75 * view_dot * b_dot * RendererDelegator.blue_light_pct / 50.0;
    // White directional light (Tim, 2026-10-03): the old white light,
    // restored. Adds equally to all channels.
    final com.springie.geometry.Vector3D white_dir = LightSource.source_1;
    final double w_len = Math.sqrt(white_dir.x * white_dir.x
        + white_dir.y * white_dir.y + white_dir.z * white_dir.z);
    final double w_dot = Math.abs((nx * white_dir.x + ny * white_dir.y
        + nz * white_dir.z) / w_len);
    final double w_factor = 0.75 * view_dot * w_dot
        * RendererDelegator.white_light_pct / 50.0;
    // Specular highlight (Tim, 2026-10-03, extra credit): where the
    // polygon reflects the light directly at the viewer, add extra
    // highlighting. R = 2*dot(N,L)*N - L; spec = pow(max(0, dot(R,V)), 32).
    // View vector V = (0, 0, -1).
    double r_spec = 0.0;
    double g_spec = 0.0;
    double b_spec = 0.0;
    {
      // Red light specular.
      final double lx = rlx / rd;
      final double ly = rly / rd;
      final double lz = rlz / rd;
      final double ndotl = nx * lx + ny * ly + nz * lz;
      final double rx = 2.0 * ndotl * nx - lx;
      final double ry = 2.0 * ndotl * ny - ly;
      final double rz = 2.0 * ndotl * nz - lz;
      final double rdotv = -(rz); // dot(R, (0,0,-1)) = -Rz
      if (rdotv > 0.0) {
        r_spec = Math.pow(rdotv, 16.0);
      }
      // Green light specular.
      final double glxn = glx / gd;
      final double glyn = gly / gd;
      final double glzn = glz / gd;
      final double gndotl = nx * glxn + ny * glyn + nz * glzn;
      final double grx = 2.0 * gndotl * nx - glxn;
      final double gry = 2.0 * gndotl * ny - glyn;
      final double grz = 2.0 * gndotl * nz - glzn;
      final double grdotv = -(grz);
      if (grdotv > 0.0) {
        g_spec = Math.pow(grdotv, 16.0);
      }
      // Blue light specular.
      final double blxn = blx / bd;
      final double blyn = bly / bd;
      final double blzn = blz / bd;
      final double bndotl = nx * blxn + ny * blyn + nz * blzn;
      final double brx = 2.0 * bndotl * nx - blxn;
      final double bry = 2.0 * bndotl * ny - blyn;
      final double brz = 2.0 * bndotl * nz - blzn;
      final double brdotv = -(brz);
      if (brdotv > 0.0) {
        b_spec = Math.pow(brdotv, 16.0);
      }
    }
    final int r = (colour >> 16) & 0xFF;
    final int g = (colour >> 8) & 0xFF;
    final int b = colour & 0xFF;
    // White light adds equally to all channels (Tim, 2026-10-03).
    final int or = Math.min(255, (int) (r * (r_factor + w_factor) + 255.0 * r_spec));
    final int og = Math.min(255, (int) (g * (g_factor + w_factor) + 255.0 * g_spec));
    final int ob = Math.min(255, (int) (b * (b_factor + w_factor) + 255.0 * b_spec));
    return (colour & 0xFF000000) | (or << 16) | (og << 8) | ob;
  }
}
