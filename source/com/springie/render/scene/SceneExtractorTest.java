// This code has been placed into the public domain by its author

package com.springie.render.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * The extractor must report exactly what the renderers would draw:
 * node/link/face counts, fixed-point positions converted to world units,
 * strut-vs-cable, colours, and the selection flag -- with hidden types
 * and zero-radius elements skipped, like RayScene.build.
 */
public class SceneExtractorTest {
  private static final double DELTA = 1e-9;

  private NodeManager threeNodes() {
    final NodeManager manager = new NodeManager();
    final Node a = manager.addNewAgent();
    final Node b = manager.addNewAgent();
    final Node c = manager.addNewAgent();
    // Fixed-point: 256000 == 1000 world units (pixels).
    a.pos = new Point3D(0, 0, 0);
    b.pos = new Point3D(256000, 0, 0);
    c.pos = new Point3D(256000, 256000, 0);
    final int radius = 10 << Coords.shift;
    a.type.radius = radius;
    b.type.radius = radius;
    c.type.radius = radius;
    a.clazz = new Clazz(0xFF112233);
    b.clazz = new Clazz(0xFF112233);
    c.clazz = new Clazz(0xFF112233);
    return manager;
  }

  private void addStrutAndCable(final NodeManager manager) {
    final ArrayList<Node> nodes = new ArrayList<>();
    for (final Object o : manager.element) {
      nodes.add((Node) o);
    }
    final LinkTypeFactory types = new LinkTypeFactory();
    final Link strut = new Link(nodes.get(0), nodes.get(1),
        types.getNew(100 << Coords.shift, 50), new Clazz(0xFF445566));
    final LinkType cable_type = types.getNew(100 << Coords.shift, 50);
    cable_type.compression = false;
    cable_type.radius = 5 << Coords.shift;
    final Link cable = new Link(nodes.get(1), nodes.get(2), cable_type,
        new Clazz(0xFF778899));
    manager.getLinkManager().element.add(strut);
    manager.getLinkManager().element.add(cable);
  }

  @Test
  public void extractsNodesLinksAndFaces() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final NodeManager manager = threeNodes();
    addStrutAndCable(manager);
    // A fourth node makes the face a real quad.
    final Node d = manager.addNewAgent();
    d.pos = new Point3D(0, 256000, 0);
    d.type.radius = 10 << Coords.shift;
    d.clazz = new Clazz(0xFF112233);
    final ArrayList<Node> face_nodes = new ArrayList<>();
    for (final Object o : manager.element) {
      face_nodes.add((Node) o);
    }
    final Face face = new Face(face_nodes,
        manager.getFaceManager().face_type_factory.getNew(),
        new Clazz(0xFFAABBCC));
    manager.getFaceManager().element.add(face);

    final ModelScene scene = SceneExtractor.extract(manager, true, true,
        true);

    assertEquals(4, scene.nodes.size(), "all four nodes");
    final SceneNode b = scene.nodes.get(1);
    assertEquals(1000.0, b.x, DELTA, "fixed-point -> world units");
    assertEquals(0.0, b.y, DELTA);
    assertEquals(10.0, b.radius, DELTA, "radius in world units");
    assertEquals(0xFF112233, b.colour);
    assertFalse(b.selected);

    assertEquals(2, scene.links.size(), "strut + cable");
    final SceneLink strut = scene.links.get(0);
    assertTrue(strut.strut, "compression link is a strut");
    assertEquals(0.0, strut.x1, DELTA);
    assertEquals(1000.0, strut.x2, DELTA);
    assertEquals(0xFF445566, strut.colour);
    final SceneLink cable = scene.links.get(1);
    assertFalse(cable.strut, "non-compression link is a cable");
    assertEquals(5.0, cable.radius, DELTA);

    // A quad face fan-triangulates into two triangles.
    assertEquals(2, scene.faces.size(), "quad -> two triangles");
    assertEquals(0xFFAABBCC, scene.faces.get(0).colour, "opaque ARGB");
  }

  @Test
  public void filtersHiddenAndZeroRadius() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final NodeManager manager = threeNodes();
    addStrutAndCable(manager);
    final Node hidden = (Node) manager.element.get(0);
    hidden.type.hidden = true;
    final Link cable =
        (Link) manager.getLinkManager().element.get(1);
    cable.type.radius = 0;

    final ModelScene scene = SceneExtractor.extract(manager, true, true,
        false);

    assertEquals(2, scene.nodes.size(), "hidden node skipped");
    assertEquals(1, scene.links.size(), "zero-radius link skipped");
    assertTrue(scene.faces.isEmpty());
  }

  @Test
  public void renderFlagsGateSections() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final NodeManager manager = threeNodes();
    addStrutAndCable(manager);

    final ModelScene scene = SceneExtractor.extract(manager, false, true,
        false);
    assertTrue(scene.nodes.isEmpty(), "nodes gated off");
    assertEquals(2, scene.links.size(), "links still extracted");
  }

  @Test
  public void selectionFlagKeepsBaseColour() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    final NodeManager manager = threeNodes();
    addStrutAndCable(manager);
    final Node selected = (Node) manager.element.get(0);
    selected.type.selected = true;

    final ModelScene scene = SceneExtractor.extract(manager, true, false,
        false);

    assertTrue(scene.nodes.get(0).selected, "selection reported as flag");
    assertEquals(0xFF112233, scene.nodes.get(0).colour,
        "base colour kept, not baked");
  }
}
