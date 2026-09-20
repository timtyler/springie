package com.springie.render.modules.modern;

import com.springie.render.RectangleInt;

import com.springie.render.RectangleInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

/**
 * Pins the link tessellation contract of the modern renderer: a link
 * generates an open tube with link_sides sides (one quad per side per
 * length division), but only the front-facing quads are emitted. The
 * far side is culled by the 3D facing of each side: each composite
 * carries a single depth, so the painter's algorithm cannot sort quads
 * within it, and unculled far-side quads would paint over the near
 * side, making struts look transparent. (The projected 2D winding test
 * the node polyhedra use flips almost at random once a side projects
 * to a sub-pixel sliver, so it is not used for tubes.) link_sides = 2
 * is the billboard special case: the two coplanar quads have opposite
 * facings, so the culling keeps the one facing the viewer and every
 * strut renders as a quad that always points at the user.
 */
class ElementRendererLinkTest {

  private int saved_sides;
  private boolean saved_depth_is_relative;
  private int saved_strut_divisions;
  private int saved_cable_divisions;
  private int saved_x_pixels;
  private int saved_y_pixels;
  private int saved_x_pixelso2;
  private int saved_y_pixelso2;
  private int saved_shift_constant_x;
  private int saved_shift_constant_y;
  private int saved_shift_constant_z;

  @BeforeEach
  void isolateStatics() {
    // getColourOfDeepObject would otherwise go through ContextManager and
    // need a booted app; with the absolute path colours pass through.
    this.saved_depth_is_relative = DeepObjectColourCalculator.depth_is_relative;
    DeepObjectColourCalculator.depth_is_relative = false;
    this.saved_sides = RendererDelegator.link_sides;
    this.saved_strut_divisions = ElementRendererLink.strut_divisions;
    this.saved_cable_divisions = ElementRendererLink.cable_divisions;
    ElementRendererLink.strut_divisions = 1;
    ElementRendererLink.cable_divisions = 1;
    // The winding test projects 3D corners to pixels; GUI tests resize
    // the canvas (changing Coords), and integer rounding can collapse a
    // thin quad to a degenerate sliver for some window sizes. Pin the
    // projection so the test is hermetic.
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_x_pixelso2 = Coords.x_pixelso2;
    this.saved_y_pixelso2 = Coords.y_pixelso2;
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;
    Coords.x_pixels = 800;
    Coords.y_pixels = 600;
    Coords.x_pixelso2 = 400;
    Coords.y_pixelso2 = 300;
    Coords.shift_constant_x = 0;
    Coords.shift_constant_y = 0;
    Coords.shift_constant_z = Coords.shift_shifted
        - (Coords.shift_shifted >> 2);
  }

  @AfterEach
  void restoreStatics() {
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;
    RendererDelegator.link_sides = this.saved_sides;
    ElementRendererLink.strut_divisions = this.saved_strut_divisions;
    ElementRendererLink.cable_divisions = this.saved_cable_divisions;
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = this.saved_x_pixelso2;
    Coords.y_pixelso2 = this.saved_y_pixelso2;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
  }

  private static Node nodeAt(int x, int y, int z) {
    return new Node(new Point3D(x, y, z), 42, new NodeTypeFactory());
  }

  private static Link linkBetween(Node a, Node b, boolean compression) {
    // LinkType's constructor is protected (same-package only), so go
    // through the public factory, as production code does.
    final LinkType type = new LinkTypeFactory().getNew(100 << Coords.shift, 50);
    type.compression = compression;
    return new Link(a, b, type, new Clazz(0));
  }

  private static ArrayList<PolygonComposite> render(Link link, Node a, Node b) {
    return ElementRendererLink.getPolygon(link, a, b, link.getThicknesss(),
        0xFF0000FF);
  }

  private static int quadCount(ArrayList<PolygonComposite> composites) {
    int quads = 0;
    for (final PolygonComposite composite : composites) {
      quads += composite.count;
    }
    return quads;
  }

  /**
   * The live quads of a composite: the leading count entries of its
   * reused backing array. Backface-culled quads are compacted out of
   * this prefix; the tail holds stale quads from previous frames.
   */
  private static PolygonObject2D[] liveQuads(PolygonComposite composite) {
    final PolygonObject2D[] live = new PolygonObject2D[composite.count];
    System.arraycopy(composite.array, 0, live, 0, composite.count);
    return live;
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4, 6, 8})
  void onlyFrontFacingQuadsAreEmitted(int sides) {
    RendererDelegator.link_sides = sides;
    for (final boolean compression : new boolean[] {true, false}) {
      final Node a = nodeAt(0, 0, 0);
      final Node b = nodeAt(512 << Coords.shift, 0, 0);
      final ArrayList<PolygonComposite> composites = render(
          linkBetween(a, b, compression), a, b);

      // One segment, no text label (the link is not selected).
      assertEquals(1, composites.size());
      // The axis-aligned link's frame puts cross_2 at -z, so side s
      // faces the viewer iff its outward normal's z (=-sin(mid-angle))
      // is negative. (For sides = 3 the middle side is edge-on up to
      // floating point -- sin(pi) is a positive 1.2e-16 -- so it is
      // kept: no hole in the tube wall.)
      int expected = 0;
      for (int s = 0; s < sides; s++) {
        if (Math.sin(2.0 * Math.PI * (s + 0.5) / sides) > 0.0) {
          expected++;
        }
      }
      assertEquals(expected, quadCount(composites),
          "culling must keep exactly the viewer-facing half of the tube: "
              + "leaked far-side quads paint over the near side, dropped "
              + "near-side quads leave holes");
    }
  }

  @Test
  void axisAlignedFourSidedTubeKeepsTwoQuads() {
    // Symmetry: of the 4 sides, 2 face the camera and 2 face away.
    RendererDelegator.link_sides = 4;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);

    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, true), a, b);

    assertEquals(1, composites.size());
    assertEquals(2, quadCount(composites));
  }

  @Test
  void tubeQuadsAreNonDegenerate() {
    RendererDelegator.link_sides = 6;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 256 << Coords.shift, 0);

    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, true), a, b);

    assertTrue(quadCount(composites) >= 1);
    for (final PolygonComposite composite : composites) {
      for (final PolygonObject2D quad : liveQuads(composite)) {
        final RectangleInt box = quad.getBoundingBox();
        assertTrue(box.max_x > box.min_x && box.max_y > box.min_y,
            "every tube side must be a real quad, not a degenerate sliver");
      }
    }
  }

  @Test
  void degenerateOrientationsNeverEmitNullsOrEmptyComposites() {
    // A link pointing at the camera culls to (almost) nothing; the
    // fallback must keep the whole tube, never nulls or an empty array.
    RendererDelegator.link_sides = 8;
    final int[][] dirs = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 1, 1},
        {3, 1, 2}};
    for (final int[] d : dirs) {
      final Node a = nodeAt(0, 0, 0);
      final Node b = nodeAt(d[0] << Coords.shift, d[1] << Coords.shift,
          d[2] << Coords.shift);
      final ArrayList<PolygonComposite> composites = render(
          linkBetween(a, b, true), a, b);
      assertTrue(composites.size() >= 1);
      for (final PolygonComposite composite : composites) {
        assertTrue(composite.count >= 1,
            "a culled-to-nothing tube must fall back to the whole tube");
        for (final PolygonObject2D quad : liveQuads(composite)) {
          assertTrue(quad != null, "culled slots must not leak nulls");
        }
      }
    }
  }

  @Test
  void cullingAppliesPerLengthDivision() {    RendererDelegator.link_sides = 4;
    ElementRendererLink.strut_divisions = 3;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);

    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, true), a, b);

    // 3 segments, 2 front-facing quads each.
    assertEquals(3, composites.size());
    assertEquals(6, quadCount(composites));
  }

  /**
   * Independent geometric oracle for which tube sides face the viewer.
   * Replicates only the frame construction from getPolygon (delta,
   * cross_1, cross_2); the corner winding under test is not replicated.
   * The camera looks down +z (established: the depth sort draws the
   * largest z first), so a side faces the viewer iff its outward normal
   * has negative z.
   *
   * @return per-side outward-normal z, from which facing is derived.
   */
  private static double[] sideNormalZ(Point3D p0, Point3D p1, int sides) {
    final double dx = p0.x - p1.x;
    final double dy = p0.y - p1.y;
    final double dz = p0.z - p1.z;
    double c1x = -dy;
    double c1y = dx;
    double c1z = 0.0;
    double c2x;
    double c2y;
    double c2z;
    if (c1x == 0.0 && c1y == 0.0) {
      c1x = 1.0;
      c2x = 0.0;
      c2y = 0.0;
      c2z = 1.0;
    } else {
      final double l1 = Math.sqrt(c1x * c1x + c1y * c1y);
      c1x /= l1;
      c1y /= l1;
      c2x = c1y * dz - c1z * dy;
      c2y = c1z * dx - c1x * dz;
      c2z = c1x * dy - c1y * dx;
      final double l2 = Math.sqrt(c2x * c2x + c2y * c2y + c2z * c2z);
      c2x /= l2;
      c2y /= l2;
      c2z /= l2;
    }
    final double[] nz = new double[sides];
    for (int s = 0; s < sides; s++) {
      final double mid = 2.0 * Math.PI * (s + 0.5) / sides;
      nz[s] = Math.cos(mid) * c1z + Math.sin(mid) * c2z;
    }
    return nz;
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 4, 6, 8})
  void tubeWindingMatchesNodeConvention(int sides) {
    // Pins the winding DIRECTION of the tube quads. The other tests
    // reuse isVisible to check isVisible-selected quads, so they pass
    // either way; this one fails if the quads are wound inside-out
    // (which rendered every strut showing its far wall). For each side,
    // tubeQuad's corners must wind so that the winding test agrees with
    // the geometric facing: front-facing sides pass, far sides fail --
    // exactly like the node polyhedra.
    //
    // Only the most face-on sides are asserted: a side straddling the
    // edge-on angles projects to a thin sliver whose 2D winding is not a
    // reliable indicator of its 3D facing. The inversion under test is
    // global (every side flips), so two sides suffice to catch it.
    // (For 3 sides, side 1's mid-angle is at 180 degrees, i.e. its
    // outward normal is -cross_1, which always has z = 0, so that side
    // is viewed exactly edge-on and is skipped.)
    final boolean[] assertSide;
    if (sides == 3) {
      assertSide = new boolean[] {true, false, true};
    } else if (sides == 4) {
      assertSide = new boolean[] {false, true, true, false};
    } else if (sides == 6) {
      assertSide = new boolean[] {false, true, false, false, true, false};
    } else {
      assertSide = new boolean[] {false, true, true, false, false, true,
          true, false};
    }
    final int[][] dirs;
    if (sides == 6) {
      dirs = new int[][] {{1, 0, 0}, {0, 1, 0}, {1, 1, 1}};
    } else {
      dirs = new int[][] {{1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {2, 1, 1}};
    }
    // A thick tube: the asserted quads must project to comfortably
    // non-degenerate quads, so integer rounding in the projection cannot
    // collapse them to slivers.
    final double thickness = 32 << Coords.shift;
    for (final int[] d : dirs) {
      final Point3D p0 = new Point3D(0, 0, 0);
      final Point3D p1 = new Point3D((512 * d[0]) << Coords.shift,
          (512 * d[1]) << Coords.shift, (512 * d[2]) << Coords.shift);
      final double[] nz = sideNormalZ(p0, p1, sides);

      // Frame vectors, scaled as getPolygon scales them.
      final double dx = p0.x - p1.x;
      final double dy = p0.y - p1.y;
      final double dz = p0.z - p1.z;
      double c1x = -dy;
      double c1y = dx;
      double c1z = 0.0;
      final com.springie.geometry.Vector3D cross_1_int;
      final com.springie.geometry.Vector3D cross_2_int;
      if (c1x == 0.0 && c1y == 0.0) {
        cross_1_int = new com.springie.geometry.Vector3D((int) thickness, 0, 0);
        cross_2_int = new com.springie.geometry.Vector3D(0, 0, (int) thickness);
      } else {
        final double l1 = Math.sqrt(c1x * c1x + c1y * c1y);
        c1x /= l1;
        c1y /= l1;
        double c2x = c1y * dz - c1z * dy;
        double c2y = c1z * dx - c1x * dz;
        double c2z = c1x * dy - c1y * dx;
        final double l2 = Math.sqrt(c2x * c2x + c2y * c2y + c2z * c2z);
        c2x /= l2;
        c2y /= l2;
        c2z /= l2;
        cross_1_int = new com.springie.geometry.Vector3D((int) (c1x * thickness),
            (int) (c1y * thickness), (int) (c1z * thickness));
        cross_2_int = new com.springie.geometry.Vector3D((int) (c2x * thickness),
            (int) (c2y * thickness), (int) (c2z * thickness));
      }

      for (int s = 0; s < sides; s++) {
        if (!assertSide[s]) {
          continue;
        }
        assertTrue(Math.abs(nz[s]) > 0.15,
            "test orientation must not be edge-on: side " + s + " of "
                + sides + " for direction (" + d[0] + "," + d[1] + "," + d[2]
                + ")");
        final boolean front = nz[s] < 0.0;
        final double a0 = 2.0 * Math.PI * s / sides;
        final double a1 = 2.0 * Math.PI * (s + 1) / sides;
        final PolygonObject2D quad = ElementRendererLink.tubeQuad(p0, p1,
            cross_1_int, cross_2_int, Math.cos(a0), Math.sin(a0),
            Math.cos(a1), Math.sin(a1), 1.0, 1.0, 0xFF0000FF);
        assertEquals(front, ElementRendererNode.isVisible(quad.x, quad.y),
            "side " + s + " of " + sides + " for direction (" + d[0] + ","
                + d[1] + "," + d[2] + "): winding test must agree with the "
                + "geometric facing (inside-out quads render the far wall)");
      }
    }
  }

  @Test
  void thinTubeCullingKeepsExactlyTheViewerFacingSides() {
    // Regression test: 8-sided struts rendered inside-out (back faces
    // leaking through, front faces dropping out, leaving see-through
    // gaps). The old projected-winding cull flipped almost at random on
    // thin tubes -- every side projects to a sub-pixel sliver, so integer
    // rounding in the projection dominated the winding sign: a Monte
    // Carlo over random links leaked a back face 41% of the time and
    // dropped a front face 99% of the time. The 3D-facing cull must keep
    // exactly the sides whose outward normal points at the viewer: no
    // leaks, no drops, at every supported side count. Seeded for
    // determinism.
    final java.util.Random random = new java.util.Random(20260919);
    final int[] sideset = {2, 3, 4, 6, 8};
    // A thin strut, as rendered: 2 px.
    final double thickness = 2 << Coords.shift;
    for (int trial = 0; trial < 150; trial++) {
      // Random link, 100..500 px long, midpoint near the screen centre,
      // depth kept well in front of the camera (never behind it).
      final double theta = random.nextDouble() * 2 * Math.PI;
      final double phi = Math.acos(2 * random.nextDouble() - 1);
      final double len = 100 + random.nextDouble() * 400;
      final double ux = Math.sin(phi) * Math.cos(theta);
      final double uy = Math.sin(phi) * Math.sin(theta);
      final double uz = Math.cos(phi) * 0.6;
      final int mx = (int) (400 + (random.nextDouble() - 0.5) * 300);
      final int my = (int) (300 + (random.nextDouble() - 0.5) * 200);
      final int mz = (int) ((random.nextDouble() - 0.5) * 200);
      final Point3D p0 = new Point3D(
          (int) (mx - ux * len / 2) << Coords.shift,
          (int) (my - uy * len / 2) << Coords.shift,
          (int) (mz - uz * len / 2) << Coords.shift);
      final Point3D p1 = new Point3D(
          (int) (mx + ux * len / 2) << Coords.shift,
          (int) (my + uy * len / 2) << Coords.shift,
          (int) (mz + uz * len / 2) << Coords.shift);

      // Frame vectors, exactly as getPolygon builds them.
      final double dx = p0.x - p1.x;
      final double dy = p0.y - p1.y;
      final double dz = p0.z - p1.z;
      double c1x = -dy;
      double c1y = dx;
      final double c1z = 0.0;
      double c2x;
      double c2y;
      double c2z;
      if (c1x == 0.0 && c1y == 0.0) {
        c1x = 1.0;
        c1y = 0.0;
        c2x = 0.0;
        c2y = 0.0;
        c2z = 1.0;
      } else {
        final double l1 = Math.sqrt(c1x * c1x + c1y * c1y);
        c1x /= l1;
        c1y /= l1;
        c2x = c1y * dz - c1z * dy;
        c2y = c1z * dx - c1x * dz;
        c2z = c1x * dy - c1y * dx;
        final double l2 = Math.sqrt(c2x * c2x + c2y * c2y + c2z * c2z);
        if (l2 == 0.0) {
          continue;
        }
        c2x /= l2;
        c2y /= l2;
        c2z /= l2;
      }
      final Double3D cross_1 = new Double3D(c1x, c1y, c1z);
      final Double3D cross_2 = new Double3D(c2x, c2y, c2z);
      final com.springie.geometry.Vector3D cross_1_int =
          new com.springie.geometry.Vector3D((int) (c1x * thickness),
              (int) (c1y * thickness), (int) (c1z * thickness));
      final com.springie.geometry.Vector3D cross_2_int =
          new com.springie.geometry.Vector3D((int) (c2x * thickness),
              (int) (c2y * thickness), (int) (c2z * thickness));

      for (final int sides : sideset) {
        final PolygonObject2D[] quads = new PolygonObject2D[sides];
        final PolygonObject2D[] original = new PolygonObject2D[sides];
        for (int s = 0; s < sides; s++) {
          final double a0 = 2.0 * Math.PI * s / sides;
          final double a1 = 2.0 * Math.PI * (s + 1) / sides;
          quads[s] = ElementRendererLink.tubeQuad(p0, p1, cross_1_int,
              cross_2_int, Math.cos(a0), Math.sin(a0), Math.cos(a1),
              Math.sin(a1), 1.0, 1.0, 0xFF0000FF);
          original[s] = quads[s];
        }
        final int kept = ElementRendererLink.cullTubeBackFaces(quads,
            cross_1, cross_2, sides);
        // Oracle: the geometric facing, as sideNormalZ computes it --
        // independent of the culling under test.
        int expected = 0;
        for (int s = 0; s < sides; s++) {
          final double mid = 2.0 * Math.PI * (s + 0.5) / sides;
          if (Math.cos(mid) * c1z + Math.sin(mid) * c2z < 0.0) {
            assertTrue(quads[expected] == original[s],
                "trial " + trial + ": culling must keep sides in order; "
                    + "side " + s + " of " + sides + " faces the viewer");
            expected++;
          }
        }
        assertEquals(expected, kept,
            "trial " + trial + ": culling must keep exactly the "
                + expected + " viewer-facing sides of " + sides
                + " (a leaked far-side quad paints over the near side; "
                + "a dropped near-side quad leaves a hole)");
      }
    }
  }

  @Test
  void twoSidedLinkIsAViewerFacingBillboard() {
    // link_sides = 2 is the billboard special case: one quad per length
    // division, always facing the viewer. cross_1 = (-delta.y, delta.x, 0)
    // is exactly the cylindrical-billboard width direction (perpendicular
    // to the link axis, facing the camera), so the two coplanar quads the
    // tube loop builds have opposite windings and the culling keeps the
    // one facing the viewer.
    RendererDelegator.link_sides = 2;
    final int[][] dirs =
        {{512, 0, 0}, {0, 512, 0}, {512, 512, 0}, {1024, 512, 512}};
    for (final int[] d : dirs) {
      final Node a = nodeAt(0, 0, 0);
      final Node b = nodeAt(d[0] << Coords.shift, d[1] << Coords.shift,
          d[2] << Coords.shift);
      final ArrayList<PolygonComposite> composites = render(
          linkBetween(a, b, true), a, b);

      // One segment: exactly one quad survives (the coplanar pair has
      // opposite windings, so exactly one passes the winding test).
      assertEquals(1, composites.size());
      assertEquals(1, quadCount(composites),
          "the billboard must emit exactly one quad for direction ("
              + d[0] + "," + d[1] + "," + d[2] + ")");
      final PolygonObject2D quad = composites.get(0).array[0];
      assertTrue(ElementRendererNode.isVisible(quad.x, quad.y),
          "the emitted billboard quad must face the camera");

      // "Always points at the user" means the quad's width runs
      // perpendicular to the link's screen projection. Corners are
      // (p0+c1, p1+c1, p1-c1, p0-c1) up to the winding the culling kept:
      // width = corner0-corner3, axis = corner1-corner0 either way.
      final int wx = quad.x[0] - quad.x[3];
      final int wy = quad.y[0] - quad.y[3];
      final int ax = quad.x[1] - quad.x[0];
      final int ay = quad.y[1] - quad.y[0];
      assertTrue(wx * wx + wy * wy > 0,
          "the billboard must have non-zero width");
      final double dot = wx * (double) ax + wy * (double) ay;
      final double cross = wx * (double) ay - wy * (double) ax;
      assertTrue(Math.abs(dot) < 0.25 * Math.abs(cross),
          "billboard width must be perpendicular to the link on screen");
    }
  }

  @Test
  void twoSidedEndOnLinkRendersWithoutNaN() {
    // A link pointing straight at the viewer zeroes cross_1, and
    // normalizing the zero vector used to poison every corner with NaN
    // ((int) NaN collapses every corner to the origin).
    RendererDelegator.link_sides = 2;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(0, 0, 512 << Coords.shift);
    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, true), a, b);
    assertTrue(composites.size() >= 1);
    for (final PolygonComposite composite : composites) {
      assertTrue(composite.count >= 1);
      for (final PolygonObject2D quad : liveQuads(composite)) {
        assertTrue(quad != null);
        final RectangleInt box = quad.getBoundingBox();
        assertTrue(box.max_x > box.min_x || box.max_y > box.min_y,
            "an end-on billboard must not collapse to a point");
      }
    }
  }
}
