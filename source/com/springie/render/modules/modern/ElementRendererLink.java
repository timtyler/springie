// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

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
import java.util.ArrayList;
import java.util.WeakHashMap;

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

  // Scratch corners for the in-place tube-quad path, reused across quads,
  // segments and frames. PolygonObject2D.set consumes them immediately
  // (projecting into the polygon's own arrays), so reuse is safe.
  private static final Point3D[] scratch_corners = new Point3D[] {
      new Point3D(0, 0, 0), new Point3D(0, 0, 0),
      new Point3D(0, 0, 0), new Point3D(0, 0, 0) };

  /**
   * Per-link render cache. A link's tube tessellation (divisions x sides
   * quads per node pair) keeps its shape unless the tessellation
   * signature changes, so the polygon objects are built once and their
   * corners recomputed in place every frame instead of allocating tens
   * of thousands of objects per frame. Entries vanish when their link
   * is deleted (WeakHashMap); rendering is single-threaded, so no
   * synchronization is needed.
   */
  private static final WeakHashMap<Link, LinkCache> link_caches =
      new WeakHashMap<>();

  private static final class LinkCache {
    final ArrayList<PairCache> pairs = new ArrayList<>(2);
  }

  private static final class PairCache {
    final Node node_1;

    final Node node_2;

    int divisions;

    int sides;

    ArrayList<PolygonComposite> list;

    PolygonComposite[] composites;

    PolygonObject2D[][] quads;

    PairCache(Node node_1, Node node_2) {
      this.node_1 = node_1;
      this.node_2 = node_2;
    }
  }

  /**
   * Returns the reusable polygon structures for one node pair of a link,
   * rebuilding them when the tessellation signature (divisions, sides)
   * changed since the last frame. Package-visible for the tests.
   */
  static PairCache getPairCache(final Link link, final Node node_1, final Node node_2,
      int divisions, final int sides) {
    LinkCache link_cache = link_caches.get(link);
    if (link_cache == null) {
      link_cache = new LinkCache();
      link_caches.put(link, link_cache);
    }
    final ArrayList<PairCache> pairs = link_cache.pairs;
    final int n = pairs.size();
    for (int i = 0; i < n; i++) {
      final PairCache pair = pairs.get(i);
      if (pair.node_1 == node_1 && pair.node_2 == node_2) {
        if (pair.divisions != divisions || pair.sides != sides) {
          buildPairCache(pair, divisions, sides);
        }
        return pair;
      }
    }
    final PairCache pair = new PairCache(node_1, node_2);
    buildPairCache(pair, divisions, sides);
    pairs.add(pair);
    return pair;
  }

  private static void buildPairCache(final PairCache pair, int divisions,
      int sides) {
    pair.divisions = divisions;
    pair.sides = sides;
    pair.list = new ArrayList<>(divisions + 1);
    pair.composites = new PolygonComposite[divisions];
    pair.quads = new PolygonObject2D[divisions][sides];
    for (int segment = 0; segment < divisions; segment++) {
      for (int side = 0; side < sides; side++) {
        pair.quads[segment][side] = new PolygonObject2D(4);
      }
      pair.composites[segment] = new PolygonComposite(pair.quads[segment],
          0);
    }
  }

  public static int strut_divisions = 5;

  public static int cable_divisions = 1;

  public static int colour_bg = 0xFFFFFF00;
  public static int colour_fg = 0xFF0000FF;

  static final int margin_x = 4;
  static final int margin_y = 0;

  private ElementRendererLink() {
    // ...
  }

  public static ArrayList<PolygonComposite> getPolygon(final Link link, final Node node_1, final Node node_2,
      int thicknesss, final int colour) {
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
    if (delta.x == 0 && delta.y == 0) {
      // The link points straight at the viewer: every screen-plane
      // direction is equally good (and normalizing the zero vector
      // would poison every corner with NaN).
      cross_1.set(1, 0, 0);
    } else {
      cross_1.set(-delta.y, delta.x, 0);
    }
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
    // The polygon structures are cached per link and rewritten in place
    // each frame; only the tessellation signature (divisions, sides)
    // triggers a rebuild. The cached objects are also referenced by
    // last frame's bins for damage repair, which reads only their count
    // and cached rectangles -- never the rewritten geometry -- so
    // in-place updates are safe.
    final PairCache pair_cache = getPairCache(link, node_1, node_2,
        divisions, sides);
    final ArrayList<PolygonComposite> return_vector = pair_cache.list;
    return_vector.clear();
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

      final int new_colour_base = DeepObjectColourCalculator.getColourOfDeepObject(
          colour, z);

      // RGB light shading (Tim, 2026-10-03): three colored lights affect
      // the whole model, including links. Use the segment midpoint.
      LightSource.updateForViewport(Coords.x_pixelso2, Coords.y_pixelso2);
      final double midx = (point0n.x + point1n.x) / 2.0;
      final double midy = (point0n.y + point1n.y) / 2.0;
      final double midz = (point0n.z + point1n.z) / 2.0;
      // Link axis (normalized).
      double ax = point1n.x - point0n.x;
      double ay = point1n.y - point0n.y;
      double az = point1n.z - point0n.z;
      final double alen = Math.sqrt(ax * ax + ay * ay + az * az);
      double r_factor = 0.25;
      double g_factor = 0.25;
      double b_factor = 0.25;
      double r_spec = 0.0;
      double g_spec = 0.0;
      double b_spec = 0.0;
      if (alen > 1e-12) {
        ax /= alen;
        ay /= alen;
        az /= alen;
        // Red light.
        final double rlx = LightSource.red_px - midx;
        final double rly = LightSource.red_py - midy;
        final double rlz = LightSource.red_pz - midz;
        final double rd = Math.sqrt(rlx * rlx + rly * rly + rlz * rlz);
        if (rd > 1e-12) {
          final double r_dot = (rlx * ax + rly * ay + rlz * az) / rd;
          r_factor = 0.25 + 0.75 * Math.sqrt(Math.max(0.0, 1.0 - r_dot * r_dot)) * RendererDelegator.red_light_pct / 50.0;
        }
        // Green light.
        final double glx = LightSource.green_px - midx;
        final double gly = LightSource.green_py - midy;
        final double glz = LightSource.green_pz - midz;
        final double gd = Math.sqrt(glx * glx + gly * gly + glz * glz);
        if (gd > 1e-12) {
          final double g_dot = (glx * ax + gly * ay + glz * az) / gd;
          g_factor = 0.25 + 0.75 * Math.sqrt(Math.max(0.0, 1.0 - g_dot * g_dot)) * RendererDelegator.green_light_pct / 50.0;
        }
        // Blue light.
        final double blx = LightSource.blue_px - midx;
        final double bly = LightSource.blue_py - midy;
        final double blz = LightSource.blue_pz - midz;
        final double bd = Math.sqrt(blx * blx + bly * bly + blz * blz);
        if (bd > 1e-12) {
          final double b_dot = (blx * ax + bly * ay + blz * az) / bd;
          b_factor = 0.25 + 0.75 * Math.sqrt(Math.max(0.0, 1.0 - b_dot * b_dot)) * RendererDelegator.blue_light_pct / 50.0;
        }
        // White directional light (Tim, 2026-10-03).
        final com.springie.geometry.Vector3D white_dir = LightSource.source_1;
        final double w_len = Math.sqrt(white_dir.x * white_dir.x
            + white_dir.y * white_dir.y + white_dir.z * white_dir.z);
        if (w_len > 1e-12) {
          final double w_dot = (white_dir.x * ax + white_dir.y * ay
              + white_dir.z * az) / w_len;
          final double w_factor = 0.75
              * Math.sqrt(Math.max(0.0, 1.0 - w_dot * w_dot))
              * RendererDelegator.white_light_pct / 50.0;
          r_factor += w_factor;
          g_factor += w_factor;
          b_factor += w_factor;
        }
        // Specular highlights from the 3 lights (Tim, 2026-10-03).
        // For a cylinder, use the normal at the brightest point:
        // N = normalize(L - (L·A)*A). View V = (0, 0, -1).
        if (rd > 1e-12) {
          final double lx = rlx / rd;
          final double ly = rly / rd;
          final double lz = rlz / rd;
          final double ldotA = lx * ax + ly * ay + lz * az;
          double nnx = lx - ldotA * ax;
          double nny = ly - ldotA * ay;
          double nnz = lz - ldotA * az;
          final double nlen = Math.sqrt(nnx * nnx + nny * nny + nnz * nnz);
          if (nlen > 1e-12) {
            nnx /= nlen;
            nny /= nlen;
            nnz /= nlen;
            final double ndotl = nnx * lx + nny * ly + nnz * lz;
            final double rx = 2.0 * ndotl * nnx - lx;
            final double ry = 2.0 * ndotl * nny - ly;
            final double rz = 2.0 * ndotl * nnz - lz;
            final double rdotv = -rz;
            if (rdotv > 0.0) {
              r_spec = Math.pow(rdotv, 16.0) * RendererDelegator.red_light_pct / 50.0;
            }
          }
        }
        if (gd > 1e-12) {
          final double lx = glx / gd;
          final double ly = gly / gd;
          final double lz = glz / gd;
          final double ldotA = lx * ax + ly * ay + lz * az;
          double nnx = lx - ldotA * ax;
          double nny = ly - ldotA * ay;
          double nnz = lz - ldotA * az;
          final double nlen = Math.sqrt(nnx * nnx + nny * nny + nnz * nnz);
          if (nlen > 1e-12) {
            nnx /= nlen;
            nny /= nlen;
            nnz /= nlen;
            final double ndotl = nnx * lx + nny * ly + nnz * lz;
            final double rx = 2.0 * ndotl * nnx - lx;
            final double ry = 2.0 * ndotl * nny - ly;
            final double rz = 2.0 * ndotl * nnz - lz;
            final double rdotv = -rz;
            if (rdotv > 0.0) {
              g_spec = Math.pow(rdotv, 16.0) * RendererDelegator.green_light_pct / 50.0;
            }
          }
        }
        if (bd > 1e-12) {
          final double lx = blx / bd;
          final double ly = bly / bd;
          final double lz = blz / bd;
          final double ldotA = lx * ax + ly * ay + lz * az;
          double nnx = lx - ldotA * ax;
          double nny = ly - ldotA * ay;
          double nnz = lz - ldotA * az;
          final double nlen = Math.sqrt(nnx * nnx + nny * nny + nnz * nnz);
          if (nlen > 1e-12) {
            nnx /= nlen;
            nny /= nlen;
            nnz /= nlen;
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
      }
      final int cr = (new_colour_base >> 16) & 0xFF;
      final int cg = (new_colour_base >> 8) & 0xFF;
      final int cb = new_colour_base & 0xFF;
      final int or = Math.min(255, (int) (cr * r_factor + 255.0 * r_spec));
      final int og = Math.min(255, (int) (cg * g_factor + 255.0 * g_spec));
      final int ob = Math.min(255, (int) (cb * b_factor + 255.0 * b_spec));
      final int new_colour = (new_colour_base & 0xFF000000) | (or << 16) | (og << 8) | ob;

      final PolygonObject2D[] quads = pair_cache.quads[segment];
      for (int side = 0; side < sides; side++) {
        final double a0 = 2.0 * Math.PI * side / sides;
        final double a1 = 2.0 * Math.PI * (side + 1) / sides;
        writeTubeQuad(quads[side], point0n, point1n, cross_1_int,
            cross_2_int, Math.cos(a0), Math.sin(a0), Math.cos(a1),
            Math.sin(a1), sf1, sf2, new_colour);
      }
      // Backface culling: a closed tube only shows its near side. The
      // far-side quads would otherwise paint over the near side -- each
      // composite carries a single depth, so the painter's algorithm
      // cannot sort quads within it -- making the strut look transparent.
      // Culled by the 3D facing of each side, not by the projected 2D
      // winding: on a thin tube every side projects to a sub-pixel sliver,
      // and integer rounding in the projection flips the winding test
      // almost at random -- back faces leak through and front faces drop
      // out, which is why 8-sided struts looked inside-out. The mid-angle
      // normal is exact in double precision.
      final int front_count = cullTubeBackFaces(quads, cross_1, cross_2,
          sides);
      final PolygonComposite composite = pair_cache.composites[segment];
      composite.z = z;
      // The culling already compacted the survivors to the front of the
      // reused array in place; record how many are live instead of
      // allocating a trimmed copy. The degenerate end-on view keeps the
      // whole tube, exactly as before.
      composite.count =
          (front_count == 0 || front_count == sides) ? sides : front_count;
      composite.bounding_box = null;

      return_vector.add(composite);
    }

    final int render_label_when = PanelPreferencesRendererModern.render_label_when;

    if (render_label_when == 1 || render_label_when == 3 && link.isSelected()) {
      return_vector.add(addRelevantText(link, min_z));
    }

    return return_vector;
  }

  /**
   * Whether a tube side faces the viewer, from the 3D frame vectors. The
   * side's outward normal at its mid-angle is cos(mid) * cross_1 +
   * sin(mid) * cross_2; it faces the viewer when its z-component is
   * negative -- the node-polyhedra convention pinned by
   * tubeWindingMatchesNodeConvention. Exact in double precision, unlike
   * the projected 2D winding, which integer rounding flips almost at
   * random once a side projects to a sub-pixel sliver.
   *
   * Package-visible for the culling test.
   */
  static boolean tubeSideFacesViewer(final Double3D cross_1, final Double3D cross_2,
      int side, final int sides) {
    final double mid = 2.0 * Math.PI * (side + 0.5) / sides;
    return Math.cos(mid) * cross_1.z + Math.sin(mid) * cross_2.z < 0.0;
  }

  /**
   * Compacts the viewer-facing tube quads to the front of the array, in
   * place, and returns how many are live. The survivors keep their
   * original order; entries past the returned count are stale and must
   * not be drawn.
   *
   * Package-visible for the culling test.
   */
  static int cullTubeBackFaces(final PolygonObject2D[] quads, final Double3D cross_1,
      Double3D cross_2, final int sides) {
    int front_count = 0;
    for (int side = 0; side < sides; side++) {
      if (tubeSideFacesViewer(cross_1, cross_2, side, sides)) {
        quads[front_count++] = quads[side];
      }
    }
    return front_count;
  }

  /**
   * Rewrites a tube quad's corners into an existing polygon, with no
   * allocation. The corners land in the shared scratch array and are
   * consumed immediately by PolygonObject2D.set.
   */
  static void writeTubeQuad(final PolygonObject2D out, final Point3D point0n,
      Point3D point1n, final Vector3D cross_1_int, final Vector3D cross_2_int,
      double cos_a, final double sin_a, final double cos_b, final double sin_b,
      double sf1, final double sf2, final int new_colour) {
    writeTubeCorners(scratch_corners, point0n, point1n, cross_1_int,
        cross_2_int, cos_a, sin_a, cos_b, sin_b, sf1, sf2);
    out.set(scratch_corners, new_colour);
  }

  private static void writeTubeCorners(final Point3D[] corners, final Point3D point0n,
      Point3D point1n, final Vector3D cross_1_int, final Vector3D cross_2_int,
      double cos_a, final double sin_a, final double cos_b, final double sin_b,
      double sf1, final double sf2) {
    tubeCornerInto(corners[0], point0n, cross_1_int, cross_2_int,
        cos_a, sin_a, sf1);
    tubeCornerInto(corners[1], point1n, cross_1_int, cross_2_int,
        cos_a, sin_a, sf2);
    tubeCornerInto(corners[2], point1n, cross_1_int, cross_2_int,
        cos_b, sin_b, sf2);
    tubeCornerInto(corners[3], point0n, cross_1_int, cross_2_int,
        cos_b, sin_b, sf1);
  }

  /**
   * One side of the open tube: the quad between two adjacent cross-section
   * directions. The tube ends are left open (no caps).
   *
   * The corners are wound so the quad's facing matches the node
   * polyhedra convention: a quad whose outward normal points at the
   * viewer passes ElementRendererNode.isVisible. (The link culling
   * itself now tests the 3D facing directly; the winding convention is
   * still pinned by tubeWindingMatchesNodeConvention.)
   *
   * Package-visible for the winding-direction test.
   */
  static PolygonObject2D tubeQuad(final Point3D point0n, final Point3D point1n,
      Vector3D cross_1_int, final Vector3D cross_2_int,
      double cos_a, final double sin_a, final double cos_b, final double sin_b,
      double sf1, final double sf2, final int new_colour) {
    writeTubeCorners(scratch_corners, point0n, point1n, cross_1_int,
        cross_2_int, cos_a, sin_a, cos_b, sin_b, sf1, sf2);
    final Point3D[] quad_points = new Point3D[4];
    for (int i = 0; i < 4; i++) {
      quad_points[i] = new Point3D(scratch_corners[i]);
    }
    return new PolygonObject2D(quad_points, new_colour);
  }

  /**
   * In-place tube corner: out = base + sf * (cos_a * cross_1 + sin_a *
   * cross_2). Same arithmetic as the old allocating tubeCorner.
   */
  private static void tubeCornerInto(final Point3D out, final Point3D base,
      Vector3D cross_1_int, final Vector3D cross_2_int, final double cos_a,
      double sin_a, final double sf) {
    final Vector3D dir = scratch_tube_dir;
    dir.set(cross_1_int);
    dir.multiplyBy(cos_a);
    final Vector3D tmp = scratch_tube_tmp;
    tmp.set(cross_2_int);
    tmp.multiplyBy(sin_a);
    dir.addTuple3D(tmp);
    dir.multiplyBy(sf);
    out.set(base);
    out.addTuple3D(dir);
  }

  private static PolygonComposite addRelevantText(final Link link, final int min_z) {
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
