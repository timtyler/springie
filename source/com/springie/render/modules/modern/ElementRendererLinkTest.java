package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 * Pins the link/strut polygon contract of ElementRendererLink.
 *
 * A standard link segment is a two-quad ribbon; a triangular strut is a
 * 3-sided prism (three quads, one length division); cables are unaffected
 * by the triangular option (two quads is already the minimum).
 */
class ElementRendererLinkTest {

  private boolean saved_depth_is_relative;
  private boolean saved_fat_struts;
  private boolean saved_triangular_struts;
  private int saved_strut_divisions;

  @BeforeEach
  void isolateStatics() {
    // getColourOfDeepObject would otherwise go through ContextManager and
    // need a booted app; with the absolute path colours pass through.
    this.saved_depth_is_relative = DeepObjectColourCalculator.depth_is_relative;
    DeepObjectColourCalculator.depth_is_relative = false;
    // The fat-strut path reads ContextManager.getNodeManager(), so keep it
    // off here; the multi-division ribbon path is covered via
    // strut_divisions directly instead.
    this.saved_fat_struts = RendererDelegator.fat_struts;
    RendererDelegator.fat_struts = false;
    this.saved_triangular_struts = RendererDelegator.triangular_struts;
    this.saved_strut_divisions = ElementRendererLink.strut_divisions;
    ElementRendererLink.strut_divisions = 1;
  }

  @AfterEach
  void restoreStatics() {
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;
    RendererDelegator.fat_struts = this.saved_fat_struts;
    RendererDelegator.triangular_struts = this.saved_triangular_struts;
    ElementRendererLink.strut_divisions = this.saved_strut_divisions;
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

  @Test
  void standardStrutIsTwoQuads() {
    RendererDelegator.triangular_struts = false;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);
    final Link link = linkBetween(a, b, true);

    final ArrayList<PolygonComposite> composites = render(link, a, b);

    // One segment, no text label (the link is not selected).
    assertEquals(1, composites.size());
    assertEquals(2, composites.get(0).array.length);
  }

  @Test
  void triangularStrutIsThreeQuads() {
    RendererDelegator.triangular_struts = true;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);
    final Link link = linkBetween(a, b, true);

    final ArrayList<PolygonComposite> composites = render(link, a, b);

    assertEquals(1, composites.size());
    assertEquals(3, composites.get(0).array.length);
    for (final PolygonObject2D quad : composites.get(0).array) {
      final RectangleInt box = quad.getBoundingBox();
      assertTrue(box.max_x > box.min_x && box.max_y > box.min_y,
          "each side of the prism must be non-degenerate");
    }
  }

  @Test
  void triangularLeavesCablesAlone() {
    RendererDelegator.triangular_struts = true;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);
    final Link cable = linkBetween(a, b, false);

    final ArrayList<PolygonComposite> composites = render(cable, a, b);

    // Two quads is already the minimum; the triangular option must not
    // make cables more expensive.
    assertEquals(1, composites.size());
    assertEquals(2, composites.get(0).array.length);
  }

  @Test
  void multiDivisionRibbonStillTwoQuadsPerSegment() {
    RendererDelegator.triangular_struts = false;
    ElementRendererLink.strut_divisions = 2;
    final Node a = nodeAt(0, 0, 0);
    final Node b = nodeAt(512 << Coords.shift, 0, 0);
    final Link link = linkBetween(a, b, true);

    final ArrayList<PolygonComposite> composites = render(link, a, b);

    assertEquals(2, composites.size());
    assertEquals(2, composites.get(0).array.length);
    assertEquals(2, composites.get(1).array.length);
  }
}
