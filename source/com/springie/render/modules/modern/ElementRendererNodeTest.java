package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;

/**
 * The "Node polyhedron" dropdown offers Dodecahedron, Octahedron, Cube,
 * Icosahedron, Square and Hexagon. Square and Hexagon are flat shapes in
 * the z = 0 plane, so they only render if their winding survives the
 * backface test in ElementRendererNode.isVisible. These tests pin that
 * every offered polyhedron actually produces polygons.
 */
class ElementRendererNodeTest {

  private boolean saved_depth_is_relative;

  @BeforeEach
  void isolateStatics() {
    // getColourOfDeepObject would otherwise go through ContextManager and
    // need a booted app; with the absolute path the depth factor is exactly
    // 1024, so colours pass through unchanged.
    this.saved_depth_is_relative = DeepObjectColourCalculator.depth_is_relative;
    DeepObjectColourCalculator.depth_is_relative = false;
  }

  @AfterEach
  void restoreStatics() {
    DeepObjectColourCalculator.depth_is_relative = this.saved_depth_is_relative;
  }

  private static Node testNode() {
    final Node node = new Node(new Point3D(0, 0, 1 << 20), 42,
        new NodeTypeFactory());
    node.type.setSize(1 << 16);
    node.clazz = new Clazz(0xFFFF0000);
    return node;
  }

  private static void assertRenders(String name, ObjectBase shape) {
    final PolygonComposite composite = ElementRendererNode.get(shape,
        testNode());
    assertTrue(composite.array.length > 0,
        name + " must survive the backface test and render polygons");
  }

  @Test
  void squareSurvivesTheBackfaceTest() {
    assertRenders("Square", new SimpleSquare());
  }

  @Test
  void hexagonSurvivesTheBackfaceTest() {
    assertRenders("Hexagon", new SimpleHexagon());
  }

  @Test
  void everyDropdownPolyhedronRenders() {
    assertRenders("Dodecahedron", new SimpleDodecahedron());
    assertRenders("Octahedron", new SimpleOctahedron());
    assertRenders("Cube", new SimpleCube());
    assertRenders("Icosahedron", new SimpleIcosahedron());
    assertRenders("Square", new SimpleSquare());
    assertRenders("Hexagon", new SimpleHexagon());
  }
}
