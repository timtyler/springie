package com.springie.render.modules.modern;

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
 * far side is culled with the same winding test the node polyhedra use:
 * each composite carries a single depth, so the painter's algorithm
 * cannot sort quads within it, and unculled far-side quads would paint
 * over the near side, making struts look transparent.
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
      quads += composite.array.length;
    }
    return quads;
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 4, 6, 8})
  void onlyFrontFacingQuadsAreEmitted(int sides) {
    RendererDelegator.link_sides = sides;
    for (final boolean compression : new boolean[] {true, false}) {
      final Node a = nodeAt(0, 0, 0);
      final Node b = nodeAt(512 << Coords.shift, 0, 0);
      final ArrayList<PolygonComposite> composites = render(
          linkBetween(a, b, compression), a, b);

      // One segment, no text label (the link is not selected).
      assertEquals(1, composites.size());
      final int quads = quadCount(composites);
      assertTrue(quads >= 1 && quads <= sides,
          "culling must keep the front of the tube: some quads, not all, not none");
      for (final PolygonComposite composite : composites) {
        for (final PolygonObject2D quad : composite.array) {
          assertTrue(ElementRendererNode.isVisible(quad.x, quad.y),
              "every emitted quad must face the camera; an unculled "
                  + "far-side quad paints over the near side");
        }
      }
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
      for (final PolygonObject2D quad : composite.array) {
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
        assertTrue(composite.array.length >= 1,
            "a culled-to-nothing tube must fall back to the whole tube");
        for (final PolygonObject2D quad : composite.array) {
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
}
