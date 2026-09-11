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
  }

  @AfterEach
  void restoreStatics() {
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;
    RendererDelegator.link_sides = this.saved_sides;
    ElementRendererLink.strut_divisions = this.saved_strut_divisions;
    ElementRendererLink.cable_divisions = this.saved_cable_divisions;
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
}
