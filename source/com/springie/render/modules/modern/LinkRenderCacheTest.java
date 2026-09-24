package com.springie.render.modules.modern;

import com.springie.render.RectangleInt;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
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
import com.springie.geometry.Vector3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

/**
 * Regression tests for the link render cache (allocation reduction).
 *
 * The modern renderer used to allocate, per link per frame: a return
 * list, a PolygonObject2D per tube side per length division, a
 * PolygonComposite per division, a trimmed copy after backface culling,
 * four Point3Ds per quad, plus three Double3Ds and a Vector3D per quad
 * for the lighting normal -- and a java.awt.Color per polygon per tile
 * per pass at draw time. The link's polygon structures are now built
 * once per tessellation signature and rewritten in place each frame,
 * and colours go through a cache.
 *
 * The subtle contract under test: the tile manager keeps last frame's
 * composites in tiles_last for damage repair while the next frame's
 * build rewrites the very same objects in place. That is safe only
 * because damage repair reads nothing but the tile's composite count
 * and its value-copied damage rectangle -- never the rewritten
 * geometry. These tests pin that contract.
 */
class LinkRenderCacheTest {

  private int saved_sides;
  private int saved_strut_divisions;
  private int saved_cable_divisions;
  private boolean saved_depth_is_relative;
  private int saved_x_pixels;
  private int saved_y_pixels;
  private int saved_x_pixelso2;
  private int saved_y_pixelso2;
  private int saved_shift_constant_x;
  private int saved_shift_constant_y;
  private int saved_shift_constant_z;

  @BeforeEach
  void isolateStatics() {
    this.saved_depth_is_relative = DeepObjectColourCalculator.depth_is_relative;
    DeepObjectColourCalculator.depth_is_relative = false;
    this.saved_sides = RendererDelegator.link_sides;
    this.saved_strut_divisions = ElementRendererLink.strut_divisions;
    this.saved_cable_divisions = ElementRendererLink.cable_divisions;
    ElementRendererLink.strut_divisions = 1;
    ElementRendererLink.cable_divisions = 1;
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
    final LinkType type = new LinkTypeFactory().getNew(100 << Coords.shift, 50);
    type.compression = compression;
    return new Link(a, b, type, new Clazz(0));
  }

  private static ArrayList<PolygonComposite> render(Link link, Node a, Node b) {
    return ElementRendererLink.getPolygon(link, a, b, link.getThicknesss(),
        0xFF0000FF);
  }

  /**
   * The in-place quad writer must be bit-identical to the old allocating
   * path (kept as tubeQuad): same projected corners, same lit colour.
   */
  @Test
  void writeTubeQuadIsBitIdenticalToTubeQuad() {
    final Point3D p0 = new Point3D(100 << Coords.shift, 0, 300 << Coords.shift);
    final Point3D p1 = new Point3D(0, 100 << Coords.shift, 300 << Coords.shift);
    final Vector3D cross_1 = new Vector3D(100, 0, 0);
    final Vector3D cross_2 = new Vector3D(0, 100, 0);

    final double[][] angles = { { 0.0, 0.5 }, { 1.0, 2.0 }, { 3.0, 4.0 } };
    for (final double[] angle : angles) {
      final double cos_a = Math.cos(angle[0]);
      final double sin_a = Math.sin(angle[0]);
      final double cos_b = Math.cos(angle[1]);
      final double sin_b = Math.sin(angle[1]);

      final PolygonObject2D expected = ElementRendererLink.tubeQuad(p0, p1,
          cross_1, cross_2, cos_a, sin_a, cos_b, sin_b, 1.0, 0.9,
          0xFF00FF00);

      final PolygonObject2D actual = new PolygonObject2D(4);
      ElementRendererLink.writeTubeQuad(actual, p0, p1, cross_1, cross_2,
          cos_a, sin_a, cos_b, sin_b, 1.0, 0.9, 0xFF00FF00);

      assertEquals(expected.colour, actual.colour,
          "lit colour must match for angles " + angle[0] + "," + angle[1]);
      for (int i = 0; i < 4; i++) {
        assertEquals(expected.x[i], actual.x[i], "x[" + i + "] must match");
        assertEquals(expected.y[i], actual.y[i], "y[" + i + "] must match");
      }
    }
  }

  /**
   * Two frames of the same link must return the very same composite and
   * polygon objects (no per-frame allocation), with rewritten geometry.
   */
  @Test
  void polygonObjectsAreReusedAcrossFrames() {
    final Node a = nodeAt(0, 0, 400 << Coords.shift);
    final Node b = nodeAt(200 << Coords.shift, 0, 400 << Coords.shift);
    final Link link = linkBetween(a, b, true);

    final ArrayList<PolygonComposite> frame_1 = render(link, a, b);
    final ArrayList<PolygonComposite> frame_2 = render(link, a, b);

    assertSame(frame_1, frame_2, "the return list must be reused");
    assertEquals(frame_1.size(), frame_2.size());
    for (int i = 0; i < frame_1.size(); i++) {
      final PolygonComposite c1 = frame_1.get(i);
      final PolygonComposite c2 = frame_2.get(i);
      assertSame(c1, c2, "composite " + i + " must be reused");
      assertEquals(c1.count, c2.count);
      for (int q = 0; q < c1.count; q++) {
        assertSame(c1.array[q], c2.array[q],
            "quad " + q + " of composite " + i + " must be reused");
      }
    }

    // ... and the reuse must carry fresh geometry, not stale corners.
    final int x_before = frame_1.get(0).array[0].x[0];
    a.pos.x += 50 << Coords.shift;
    final ArrayList<PolygonComposite> frame_3 = render(link, a, b);
    assertSame(frame_1.get(0), frame_3.get(0),
        "same objects after the move");
    assertTrue(frame_3.get(0).array[0].x[0] != x_before,
        "moved node must rewrite the cached corners");
  }

  /**
   * Changing the tessellation signature must rebuild the cache (new
   * objects, new counts); changing it back must rebuild again rather
   * than serve stale structures.
   */
  @Test
  void cacheRebuildsWhenTessellationChanges() {
    final Node a = nodeAt(0, 0, 400 << Coords.shift);
    final Node b = nodeAt(0, 200 << Coords.shift, 400 << Coords.shift);
    final Link link = linkBetween(a, b, true);

    RendererDelegator.link_sides = 4;
    final ArrayList<PolygonComposite> four = render(link, a, b);
    final PolygonComposite composite_4 = four.get(0);
    assertEquals(4, composite_4.array.length);

    RendererDelegator.link_sides = 8;
    final ArrayList<PolygonComposite> eight = render(link, a, b);
    final PolygonComposite composite_8 = eight.get(0);
    assertNotSame(composite_4, composite_8,
        "signature change must rebuild, not resize in place");
    assertEquals(8, composite_8.array.length);
    assertTrue(composite_8.count <= 8 && composite_8.count >= 1,
        "live count must fit the new tessellation");

    RendererDelegator.link_sides = 4;
    final ArrayList<PolygonComposite> four_again = render(link, a, b);
    assertEquals(4, four_again.get(0).array.length,
        "restoring the signature must rebuild the 4-sided tube");
    assertEquals(composite_4.count, four_again.get(0).count,
        "restored tessellation must cull exactly as before");
  }

  /**
   * The two-frame lifetime contract: the tile manager keeps last frame's
   * composites in tiles_last for damage repair while the next frame's
   * build rewrites the same objects in place. Damage repair reads only
   * the tile's value-copied damage rectangle (RendererTile.setUpActual
   * copies the ints out of the composite's bounding box), so mutating
   * the cached objects for frame 2 must leave frame 1's recorded
   * rectangle untouched.
   */
  @Test
  void previousFrameDamageRectSurvivesInPlaceUpdate() {
    final Node a = nodeAt(0, 0, 400 << Coords.shift);
    final Node b = nodeAt(200 << Coords.shift, 0, 400 << Coords.shift);
    final Link link = linkBetween(a, b, true);

    // Frame 1: the tile snapshots its damage rectangle, exactly as
    // renderTiled/renderDirect do via setUpActual.
    final RendererTile last_tile = new RendererTile();
    for (final PolygonComposite composite : render(link, a, b)) {
      last_tile.vector.add(composite);
    }
    assertTrue(last_tile.vector.size() > 0, "the test link must be rendered");
    last_tile.setUpActual(new RectangleInt(-100000, -100000, 100000, 100000));
    final int min_x = last_tile.actual.min_x;
    final int min_y = last_tile.actual.min_y;
    final int max_x = last_tile.actual.max_x;
    final int max_y = last_tile.actual.max_y;
    final int size = last_tile.vector.size();

    // Frame 2: move the link and rebuild -- this mutates the very same
    // cached objects that last_tile still references.
    a.pos.x += 300 << Coords.shift;
    b.pos.x += 300 << Coords.shift;
    render(link, a, b);

    assertEquals(min_x, last_tile.actual.min_x, "damage min_x");
    assertEquals(min_y, last_tile.actual.min_y, "damage min_y");
    assertEquals(max_x, last_tile.actual.max_x, "damage max_x");
    assertEquals(max_y, last_tile.actual.max_y, "damage max_y");
    assertEquals(size, last_tile.vector.size(), "damage composite count");
  }

  /**
   * The Color cache must return the same instance for the same ARGB --
   * fill/draw allocate no Color per polygon per tile per pass.
   */
  @Test
  void colorForCachesByArgb() {
    final Color first = PolygonObject2D.colorFor(0xFF123456);
    final Color second = PolygonObject2D.colorFor(0xFF123456);
    assertSame(first, second, "same ARGB must return the cached Color");
    assertEquals(0xFF123456, first.getRGB());

    final Color other = PolygonObject2D.colorFor(0xFF654321);
    assertEquals(0xFF654321, other.getRGB(), "cached colour must be exact");
    assertSame(other, PolygonObject2D.colorFor(0xFF654321));

    // A sweep of distinct colours must each resolve correctly; the
    // direct-mapped cache may evict under pressure, but must never
    // corrupt: every lookup returns the exact ARGB it was asked for.
    for (int i = 0; i < 4096; i++) {
      final int argb = 0xFF000000 | (i * 7919);
      assertEquals(argb, PolygonObject2D.colorFor(argb).getRGB(),
          "colour " + i + " must survive the cache");
    }
    assertEquals(0xFF123456, PolygonObject2D.colorFor(0xFF123456).getRGB(),
        "an evicted colour must still resolve correctly on re-fetch");
    assertSame(PolygonObject2D.colorFor(0xFF123456),
        PolygonObject2D.colorFor(0xFF123456),
        "a re-fetched colour must be served from the cache");
  }
}
