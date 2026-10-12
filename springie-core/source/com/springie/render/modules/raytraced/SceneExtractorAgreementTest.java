// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.FrEnd;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.scene.ModelScene;
import com.springie.render.scene.SceneExtractor;
import com.springie.render.scene.SceneFace;
import com.springie.render.scene.SceneLink;
import com.springie.render.scene.SceneNode;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Agreement test: the extractor must see exactly what the renderers see.
 * RayScene.build is the established snapshot of "what the renderers
 * draw" (same filters, same model walk); this test runs both on one
 * model and asserts they agree on counts, positions (fixed-point vs
 * world units), colours and the strut/cable distinction. Selection is
 * the one deliberate difference: RayScene bakes the selection colour
 * in, the extractor reports it as a flag with the base colour kept.
 */
public class SceneExtractorAgreementTest {
  private static final double DELTA = 1e-6;
  private static final double FIXED_PER_WORLD = 1 << Coords.shift;

  private NodeManager buildModel() {
    final NodeManager manager = new NodeManager();
    // Four nodes: visible, hidden, zero-radius, selected. Distinct
    // colours so primitives can be matched back to scene entries.
    final int[] colours = {0xFF111111, 0xFF222222, 0xFF333333, 0xFF444444};
    final ArrayList<Node> nodes = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final Node node = manager.addNewAgent();
      node.pos = new Point3D(i * 256000, (i % 2) * 256000, 0);
      node.type.radius = 10 << Coords.shift;
      node.clazz = new Clazz(colours[i]);
      nodes.add(node);
    }
    nodes.get(1).type.hidden = true;
    nodes.get(2).type.radius = 0;
    nodes.get(3).type.selected = true;

    // Links: strut, cable, hidden, zero-radius. Node 1 is hidden, so
    // links touching it still extract (links filter on their own type).
    final LinkTypeFactory types = new LinkTypeFactory();
    final Link strut = new Link(nodes.get(0), nodes.get(3),
        types.getNew(100 << Coords.shift, 50), new Clazz(0xFF555555));
    strut.type.radius = 8 << Coords.shift;
    final LinkType cable_type = types.getNew(100 << Coords.shift, 50);
    cable_type.compression = false;
    cable_type.radius = 6 << Coords.shift;
    final Link cable = new Link(nodes.get(0), nodes.get(2), cable_type,
        new Clazz(0xFF666666));
    final LinkType hidden_type = types.getNew(100 << Coords.shift, 50);
    hidden_type.hidden = true;
    final Link hidden = new Link(nodes.get(2), nodes.get(3), hidden_type,
        new Clazz(0xFF777777));
    final LinkType thin_type = types.getNew(100 << Coords.shift, 50);
    thin_type.radius = 0;
    final Link thin = new Link(nodes.get(3), nodes.get(0), thin_type,
        new Clazz(0xFF888888));
    manager.getLinkManager().element.add(strut);
    manager.getLinkManager().element.add(cable);
    manager.getLinkManager().element.add(hidden);
    manager.getLinkManager().element.add(thin);

    // Faces: a triangle and a quad (fan-triangulates to two).
    final ArrayList<Node> tri_nodes = new ArrayList<>(nodes.subList(0, 3));
    final ArrayList<Node> quad_nodes = new ArrayList<>(nodes);
    manager.getFaceManager().element.add(new Face(tri_nodes,
        manager.getFaceManager().face_type_factory.getNew(),
        new Clazz(0xFF999999)));
    manager.getFaceManager().element.add(new Face(quad_nodes,
        manager.getFaceManager().face_type_factory.getNew(),
        new Clazz(0xFFAAAAAA)));

    return manager;
  }

  private static double centreX(final Primitive p) {
    final AABB box = new AABB();
    p.writeBounds(box);
    return (box.min_x + box.max_x) / 2;
  }

  private static double centreY(final Primitive p) {
    final AABB box = new AABB();
    p.writeBounds(box);
    return (box.min_y + box.max_y) / 2;
  }

  @Test
  public void extractorAgreesWithRayScene() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    FrEnd.render_nodes = true;
    FrEnd.render_links = true;
    FrEnd.render_faces = true;

    final NodeManager manager = buildModel();
    final Primitive[] primitives = RayScene.build(manager);
    final ModelScene scene =
        SceneExtractor.extract(manager, true, true, true);

    int spheres = 0;
    int cylinders = 0;
    int ellipsoids = 0;
    int triangles = 0;
    for (final Primitive p : primitives) {
      if (p instanceof RTSphere) {
        spheres++;
      } else if (p instanceof RTCylinder) {
        cylinders++;
      } else if (p instanceof RTEllipsoid) {
        ellipsoids++;
      } else if (p instanceof RTTriangle) {
        triangles++;
      }
    }

    // Nodes: zero-radius filtered by both. Hidden is the deliberate
    // difference: RayScene draws hidden nodes, the extractor follows
    // the default polygon renderer and skips them.
    assertEquals(3, spheres, "RayScene draws the hidden node too");
    assertEquals(2, scene.nodes.size(), "extractor skips hidden nodes");

    // Links: hidden and zero-radius filtered by both; struts become
    // ellipsoids, cables become cylinders.
    assertEquals(scene.links.size(), cylinders + ellipsoids,
        "link segment count");
    int struts = 0;
    int cables = 0;
    for (final SceneLink link : scene.links) {
      if (link.strut) {
        struts++;
      } else {
        cables++;
      }
    }
    assertEquals(struts, ellipsoids, "struts <-> ellipsoids");
    assertEquals(cables, cylinders, "cables <-> cylinders");
    assertEquals(2, scene.links.size(), "strut + cable survive");

    // Faces: triangle + quad (two triangles).
    assertEquals(scene.faces.size(), triangles, "face triangle count");
    assertEquals(3, triangles, "1 + 2 fan triangles");

    // Every extracted node matches a sphere: position (fixed-point)
    // and colour.
    for (final SceneNode node : scene.nodes) {
      boolean matched = false;
      for (final Primitive p : primitives) {
        if (!(p instanceof RTSphere)) {
          continue;
        }
        if (Math.abs(centreX(p) - node.x * FIXED_PER_WORLD) < DELTA
            && Math.abs(centreY(p) - node.y * FIXED_PER_WORLD) < DELTA
            && p.getColour() == node.colour) {
          matched = true;
          break;
        }
      }
      assertTrue(matched, "sphere for node at " + node.x + "," + node.y);
    }

    // The hidden node's sphere exists in RayScene but has no SceneNode:
    // pin the documented difference.
    boolean hidden_sphere_found = false;
    boolean hidden_node_extracted = false;
    for (final Primitive p : primitives) {
      if (p instanceof RTSphere && p.getColour() == 0xFF222222) {
        hidden_sphere_found = true;
      }
    }
    for (final SceneNode node : scene.nodes) {
      if (node.colour == 0xFF222222) {
        hidden_node_extracted = true;
      }
    }
    assertTrue(hidden_sphere_found, "RayScene draws the hidden node");
    assertFalse(hidden_node_extracted, "extractor skips the hidden node");

    // Every extracted link matches a cylinder/ellipsoid: the baked
    // colour (selection -> selection colour) and the strut/cable kind.
    for (final SceneLink link : scene.links) {
      final int baked = link.selected
          ? RendererDelegator.colour_selected_number
          : link.colour;
      boolean matched = false;
      for (final Primitive p : primitives) {
        if (p.getColour() != baked) {
          continue;
        }
        final boolean is_ellipsoid = p instanceof RTEllipsoid;
        if (is_ellipsoid == link.strut) {
          matched = true;
          break;
        }
      }
      assertTrue(matched, "primitive for link colour "
          + Integer.toHexString(link.colour));
    }

    // Every extracted face triangle matches an RTTriangle by colour.
    // (RGB only: RayScene masks to 24 bits, the extractor reports
    // opaque ARGB.)
    for (final SceneFace face : scene.faces) {
      boolean matched = false;
      for (final Primitive p : primitives) {
        if (p instanceof RTTriangle
            && (p.getColour() & 0xFFFFFF) == (face.colour & 0xFFFFFF)) {
          matched = true;
          break;
        }
      }
      assertTrue(matched, "triangle for face colour "
          + Integer.toHexString(face.colour));
    }
  }
}
