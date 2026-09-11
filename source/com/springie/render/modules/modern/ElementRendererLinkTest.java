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
 * renders as an open tube with link_sides sides, i.e. one quad per side
 * per length division, for both struts and cables. The tube ends are
 * left open (no caps).
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
  void strutRendersOneQuadPerSide(int sides) {
    RendererDelegator.link_sides = sides;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);

    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, true), a, b);

    // One segment, no text label (the link is not selected).
    assertEquals(1, composites.size());
    assertEquals(sides, quadCount(composites));
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 4, 6, 8})
  void cableRendersOneQuadPerSide(int sides) {
    RendererDelegator.link_sides = sides;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);

    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, false), a, b);

    assertEquals(1, composites.size());
    assertEquals(sides, quadCount(composites));
  }

  @Test
  void tubeQuadsAreNonDegenerate() {
    RendererDelegator.link_sides = 6;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 256 << Coords.shift, 0);

    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, true), a, b);

    assertEquals(6, quadCount(composites));
    for (final PolygonComposite composite : composites) {
      for (final PolygonObject2D quad : composite.array) {
        final RectangleInt box = quad.getBoundingBox();
        assertTrue(box.max_x > box.min_x && box.max_y > box.min_y,
            "every tube side must be a real quad, not a degenerate sliver");
      }
    }
  }

  @Test
  void sidesMultiplyAcrossLengthDivisions() {
    RendererDelegator.link_sides = 4;
    ElementRendererLink.strut_divisions = 3;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);

    final ArrayList<PolygonComposite> composites = render(
        linkBetween(a, b, true), a, b);

    assertEquals(3, composites.size());
    assertEquals(12, quadCount(composites));
  }
}
