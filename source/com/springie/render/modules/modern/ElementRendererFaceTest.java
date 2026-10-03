package com.springie.render.modules.modern;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceType;
import com.springie.elements.faces.FaceTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The modern renderer draws faces as filled quads fanning from each edge
 * into the face centre (ElementRendererFace). At full opacity there is one
 * quad per edge; a translucent face class is drawn as concentric bands,
 * one per render division; zero render divisions ("Face lines = 0") fills
 * the face in a solid colour. These tests pin that contract, plus the colour
 * flow (face class colour, selection colour) into the quads.
 */
class ElementRendererFaceTest {

  private boolean saved_depth_is_relative;
  private int saved_render_divisions;
  private int saved_red_pct;
  private int saved_green_pct;
  private int saved_blue_pct;
  private int saved_x_pixels;
  private int saved_y_pixels;

  @BeforeEach
  void isolateStatics() {
    // getColourOfDeepObject would otherwise go through ContextManager and
    // need a booted app; with the absolute path and z = 0 the depth factor
    // is exactly 1024, so colours pass through unchanged.
    this.saved_depth_is_relative = DeepObjectColourCalculator.depth_is_relative;
    DeepObjectColourCalculator.depth_is_relative = false;
    this.saved_render_divisions = Face.number_of_render_divisions;
    Face.number_of_render_divisions = 4;
    // RGB light percentages affect the face shading (Tim, 2026-10-03).
    this.saved_red_pct = RendererDelegator.red_light_pct;
    this.saved_green_pct = RendererDelegator.green_light_pct;
    this.saved_blue_pct = RendererDelegator.blue_light_pct;
    RendererDelegator.red_light_pct = 50;
    RendererDelegator.green_light_pct = 50;
    RendererDelegator.blue_light_pct = 50;
    // The face renderer positions lights from Coords (Tim, 2026-10-03):
    // fix the viewport for deterministic results.
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    Coords.x_pixels = 800;
    Coords.y_pixels = 600;
    Coords.x_pixelso2 = 400;
    Coords.y_pixelso2 = 300;
  }

  @AfterEach
  void restoreStatics() {
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;
    Face.number_of_render_divisions = this.saved_render_divisions;
    RendererDelegator.red_light_pct = this.saved_red_pct;
    RendererDelegator.green_light_pct = this.saved_green_pct;
    RendererDelegator.blue_light_pct = this.saved_blue_pct;
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = this.saved_x_pixels >> 1;
    Coords.y_pixelso2 = this.saved_y_pixels >> 1;
  }

  private static Face squareFace(final int argb) {
    final int s = 1 << 16;
    final ArrayList<Node> nodes = new ArrayList<>();
    final int[][] corners = {{-s, -s}, {s, -s}, {s, s}, {-s, s}};
    for (final int[] c : corners) {
      final Node node = new Node();
      node.pos = new Point3D(c[0], c[1], 0);
      nodes.add(node);
    }
    final FaceType type = new FaceTypeFactory().getNew();
    return new Face(nodes, type, new Clazz(argb));
  }

  private static Face triangleFace(final int argb) {
    final int s = 1 << 16;
    final ArrayList<Node> nodes = new ArrayList<>();
    final int[][] corners = {{-s, -s}, {s, -s}, {0, s}};
    for (final int[] c : corners) {
      final Node node = new Node();
      node.pos = new Point3D(c[0], c[1], 0);
      nodes.add(node);
    }
    final FaceType type = new FaceTypeFactory().getNew();
    return new Face(nodes, type, new Clazz(argb));
  }

  @Test
  void fullOpacitySquareYieldsOneQuadPerEdge() {
    final PolygonComposite composite =
        ElementRendererFace.getPolygon(squareFace(0xFF000000));
    assertEquals(4, composite.array.length);
  }

  @Test
  void fullOpacityTriangleYieldsOneQuadPerEdge() {
    final PolygonComposite composite =
        ElementRendererFace.getPolygon(triangleFace(0xFFFFFFFF));
    assertEquals(3, composite.array.length);
  }

  @Test
  void translucentFaceYieldsOneBandPerRenderDivisionPerEdge() {
    final PolygonComposite composite =
        ElementRendererFace.getPolygon(squareFace(0x80000000));
    assertEquals(4 * Face.number_of_render_divisions, composite.array.length);
  }

  @Test
  void zeroDivisionsFillsTranslucentFaceSolid() {
    Face.number_of_render_divisions = 0;
    final PolygonComposite composite =
        ElementRendererFace.getPolygon(squareFace(0x80000000));
    // One full-coverage quad per edge, as in the opaque case.
    // (On the old code this was an empty array: the face vanished.)
    assertEquals(4, composite.array.length);

    // Each solid quad spans from the face centre out to its edge, so
    // every quad has a corner at the projected centre...
    final int centre_x = Coords.getXCoords(0, 0);
    final int centre_y = Coords.getYCoords(0, 0);
    final Set<String> corners = new HashSet<>();
    for (final PolygonObject2D quad : composite.array) {
      boolean has_centre = false;
      for (int i = 0; i < quad.x.length; i++) {
        corners.add(quad.x[i] + "," + quad.y[i]);
        if (quad.x[i] == centre_x && quad.y[i] == centre_y) {
          has_centre = true;
        }
      }
      assertTrue(has_centre, "each solid quad starts at the face centre");
    }

    // ...and together the quads reach every rim node (full coverage).
    final int s = 1 << 16;
    final int[][] nodes = {{-s, -s}, {s, -s}, {s, s}, {-s, s}};
    for (final int[] node : nodes) {
      final String rim = Coords.getXCoords(node[0], 0) + ","
          + Coords.getYCoords(node[1], 0);
      assertTrue(corners.contains(rim), "rim corner " + rim + " is covered");
    }
  }

  @Test
  void zeroDivisionsLeavesOpaqueFaceSolid() {
    Face.number_of_render_divisions = 0;
    final PolygonComposite composite =
        ElementRendererFace.getPolygon(squareFace(0xFF000000));
    assertEquals(4, composite.array.length);
  }

  @Test
  void everyQuadHasFourCorners() {
    final PolygonComposite composite =
        ElementRendererFace.getPolygon(squareFace(0xFF000000));
    for (final PolygonObject2D quad : composite.array) {
      assertEquals(4, quad.x.length);
      assertEquals(4, quad.y.length);
    }
  }

  @Test
  void faceClassColourFlowsThroughToEveryQuad() {
    // Solid black, at z = 0 with the absolute depth path, is untouched by
    // the depth-fog adjustment.
    final PolygonComposite composite =
        ElementRendererFace.getPolygon(squareFace(0xFF000000));
    assertTrue(composite.array.length > 0);
    for (final PolygonObject2D quad : composite.array) {
      // RGB lights add diffuse + specular (Tim, 2026-10-03).
      assertEquals(-12749507, quad.colour);
    }
  }

  @Test
  void selectedFaceUsesTheSelectionColour() {
    final Face face = squareFace(0xFF000000);
    face.type.selected = true;
    final PolygonComposite composite = ElementRendererFace.getPolygon(face);
    assertTrue(composite.array.length > 0);
    // The quads carry the selection colour through the directional-light
    // shading in PolygonObject2D: hue preserved, uniformly dimmed.
    // ElementRendererNode.getColour scales by [128, 255], so the red
    // channel lands in [(255*128)>>8, (255*255)>>8] = [127, 254].
    final int first = composite.array[0].colour;
    final int red = (first >> 16) & 0xFF;
    assertTrue(red >= 127 && red <= 254, "red channel: " + red);
    for (final PolygonObject2D quad : composite.array) {
      assertEquals(first, quad.colour);
      // RGB lights may add specular highlights (Tim, 2026-10-03), so
      // green/blue need not be zero, but red must dominate.
      final int qred = (quad.colour >> 16) & 0xFF;
      final int qgreen = (quad.colour >> 8) & 0xFF;
      final int qblue = quad.colour & 0xFF;
      assertTrue(qred >= qgreen && qred >= qblue,
          "red must dominate for selection colour");
    }
  }
}
