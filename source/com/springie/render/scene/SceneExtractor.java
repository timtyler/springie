// This code has been placed into the public domain by its author

package com.springie.render.scene;

import java.util.ArrayList;
import java.util.List;

import com.springie.context.ContextManager;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Builds an immutable {@link ModelScene} snapshot of the model: nodes,
 * links and faces as plain data in world units (pixels), with no AWT
 * types. The walk mirrors the default polygon renderer's visibility:
 * hidden types and zero-radius elements are skipped, faces are
 * fan-triangulated, and selection is reported as a flag with the base
 * colour kept (rather than baked in, so consumers can highlight their
 * own way). Note RayScene (ray tracer) differs: it draws hidden nodes
 * and faces, checking hidden only for links.
 *
 * <p>Takes synchronized (ContextManager.class) internally -- the same
 * lock the AWT render path holds while walking the model -- so it is
 * safe to call from any thread. The render_* flags are parameters rather
 * than FrEnd statics so the extractor stays free of UI classes.
 */
public final class SceneExtractor {
  /** Fixed-point model units per world unit (pixel). */
  private static final double FIXED_PER_WORLD = 1 << Coords.shift;

  private SceneExtractor() {
    // Static utility.
  }

  public static ModelScene extract(final NodeManager manager,
      final boolean render_nodes, final boolean render_links,
      final boolean render_faces) {
    synchronized (ContextManager.class) {
      final List<SceneNode> nodes = new ArrayList<>();
      final List<SceneLink> links = new ArrayList<>();
      final List<SceneFace> faces = new ArrayList<>();
      if (render_nodes) {
        addNodes(manager, nodes);
      }
      if (render_links) {
        addLinks(manager, links);
      }
      if (render_faces) {
        addFaces(manager, faces);
      }
      return new ModelScene(nodes, links, faces);
    }
  }

  private static double world(final int fixed) {
    return fixed / FIXED_PER_WORLD;
  }

  private static void addNodes(final NodeManager manager,
      final List<SceneNode> out) {
    final List<?> elements = manager.element;
    final int size = elements.size();
    for (int i = 0; i < size; i++) {
      final Node node = (Node) elements.get(i);
      if (node.type.hidden) {
        continue;
      }
      final double radius = world(node.type.radius);
      if (radius <= 0.0) {
        continue;
      }
      final Point3D pos = node.pos;
      out.add(new SceneNode(world(pos.x), world(pos.y), world(pos.z),
          radius, node.clazz.colour, node.type.selected));
    }
  }

  private static void addLinks(final NodeManager manager,
      final List<SceneLink> out) {
    final LinkManager link_manager = manager.getLinkManager();
    final List<?> elements = link_manager.element;
    final int size = elements.size();
    for (int i = 0; i < size; i++) {
      final Link link = (Link) elements.get(i);
      if (link.type.hidden) {
        continue;
      }
      final double radius = world(link.type.radius);
      if (radius <= 0.0) {
        continue;
      }
      final Node[] link_nodes = link.nodes;
      for (int s = 0; s < link_nodes.length - 1; s++) {
        final Point3D p1 = link_nodes[s].pos;
        final Point3D p2 = link_nodes[s + 1].pos;
        out.add(new SceneLink(world(p1.x), world(p1.y), world(p1.z),
            world(p2.x), world(p2.y), world(p2.z), radius,
            link.clazz.colour, link.type.compression,
            link.type.selected));
      }
    }
  }

  private static void addFaces(final NodeManager manager,
      final List<SceneFace> out) {
    final FaceManager face_manager = manager.getFaceManager();
    final List<?> elements = face_manager.element;
    final int size = elements.size();
    for (int i = 0; i < size; i++) {
      final Face face = (Face) elements.get(i);
      if (face.type.hidden) {
        continue;
      }
      final List<Node> face_nodes = face.nodes;
      final int points = face_nodes.size();
      if (points < 3) {
        continue;
      }
      // Faces are opaque: the translucency bits are ignored, like the
      // ray tracer.
      final int colour = 0xFF000000 | (face.clazz.colour & 0xFFFFFF);
      final Node n0 = face_nodes.get(0);
      for (int s = 1; s < points - 1; s++) {
        final Node n1 = face_nodes.get(s);
        final Node n2 = face_nodes.get(s + 1);
        final Point3D p0 = n0.pos;
        final Point3D p1 = n1.pos;
        final Point3D p2 = n2.pos;
        out.add(new SceneFace(world(p0.x), world(p0.y), world(p0.z),
            world(p1.x), world(p1.y), world(p1.z),
            world(p2.x), world(p2.y), world(p2.z), colour));
      }
    }
  }
}
