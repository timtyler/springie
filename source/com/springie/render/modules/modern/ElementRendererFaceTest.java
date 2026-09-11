package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceType;
import com.springie.elements.faces.FaceTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.render.RendererDelegator;

/**
 * The modern renderer draws faces as filled quads fanning from each edge
 * into the face centre (ElementRendererFace). At full opacity there is one
 * quad per edge; a translucent face class is drawn as concentric bands,
 * one per render division. These tests pin that contract, plus the colour
 * flow (face class colour, selection colour) into the quads.
 */
class ElementRendererFaceTest {

  private boolean saved_depth_is_relative;
  private int saved_render_divisions;

  @BeforeEach
  void isolateStatics() {
    // getColourOfDeepObject would otherwise go through ContextManager and
    // need a booted app; with the absolute path and z = 0 the depth factor
    // is exactly 1024, so colours pass through unchanged.
    this.saved_depth_is_relative = DeepObjectColourCalculator.depth_is_relative;
    DeepObjectColourCalculator.depth_is_relative = false;
    this.saved_render_divisions = Face.number_of_render_divisions;
    Face.number_of_render_divisions = 4;
  }

  @AfterEach
  void restoreStatics() {
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;
    Face.number_of_render_divisions = this.saved_render_divisions;
  }

  private static Face squareFace(int argb) {
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

  private static Face triangleFace(int argb) {
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
      assertEquals(0xFF000000, quad.colour);
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
      assertEquals(0, quad.colour & 0xFF);
      assertEquals(0, (quad.colour >> 8) & 0xFF);
    }
  }
}
