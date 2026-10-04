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
    // Combine: ambient + diffuse from N lights (Tim, 2026-10-04).
    // Ambient light (Tim, 2026-10-03): from the slider. 50% = 0.25.
    final double ambient = 0.5 * RendererDelegator.ambient_light_pct / 100.0;
    // Per-output-channel diffuse: sum each light's factor scaled by
    // its color (Tim, 2026-10-04).
    double r_factor = ambient;
    double g_factor = ambient;
    double b_factor = ambient;
    // Specular accumulators (Tim, 2026-10-04).
    double spec_r = 0.0;
    double spec_g = 0.0;
    double spec_b = 0.0;
    synchronized (LightSource.class) {
      for (final com.springie.render.modules.modern.Light light
          : LightSource.lights) {
        if (light.intensity_pct <= 0) {
          continue;
        }
        final double lx = light.px - center.x;
        final double ly = light.py - center.y;
        final double lz = light.pz - center.z;
        final double ld = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (ld < 1e-9) {
          continue;
        }
        final double light_dot = Math.abs((nx * lx + ny * ly + nz * lz) / ld);
        final double diff = 0.75 * view_dot * light_dot
            * light.intensity_pct / 50.0;
        final int lr = (light.colour >> 16) & 0xFF;
        final int lg = (light.colour >> 8) & 0xFF;
        final int lb = light.colour & 0xFF;
        r_factor += diff * lr / 255.0;
        g_factor += diff * lg / 255.0;
        b_factor += diff * lb / 255.0;
        // Specular highlight (Tim, 2026-10-03): R = 2*dot(N,L)*N - L;
        // spec = pow(max(0, dot(R,V)), 32). View vector V = (0, 0, -1).
        // Tints by light color (Tim, 2026-10-04).
        final double nlx = lx / ld;
        final double nly = ly / ld;
        final double nlz = lz / ld;
        final double dot_nl = nx * nlx + ny * nly + nz * nlz;
        final double rx = 2.0 * dot_nl * nx - nlx;
        final double ry = 2.0 * dot_nl * ny - nly;
        final double rz = 2.0 * dot_nl * nz - nlz;
        // V = (0, 0, -1), so dot(R,V) = -rz.
        final double spec_dot = Math.max(0.0, -rz);
        if (spec_dot > 0.0) {
          final double spec = Math.pow(spec_dot, 16.0)
              * light.intensity_pct / 50.0;
          spec_r += spec * lr / 255.0;
          spec_g += spec * lg / 255.0;
          spec_b += spec * lb / 255.0;
        }
      }
    }
    r_factor = Math.min(1.0, r_factor);
    g_factor = Math.min(1.0, g_factor);
    b_factor = Math.min(1.0, b_factor);
    // (Specular is accumulated in the light loop above.)
    final int r = (colour >> 16) & 0xFF;
    final int g = (colour >> 8) & 0xFF;
    final int b = colour & 0xFF;
    final int or = Math.min(255, (int) (r * r_factor + 255.0 * spec_r));
    final int og = Math.min(255, (int) (g * g_factor + 255.0 * spec_g));
    final int ob = Math.min(255, (int) (b * b_factor + 255.0 * spec_b));
    return (colour & 0xFF000000) | (or << 16) | (og << 8) | ob;
  }
}
