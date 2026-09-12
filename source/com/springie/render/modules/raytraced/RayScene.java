// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import java.util.ArrayList;
import java.util.List;

import com.springie.FrEnd;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.RendererDelegator;

/**
 * Builds an immutable ray-traceable snapshot of the model: nodes become
 * spheres, struts become stretched spheres (ellipsoids), cables become
 * open cylinders like the default renderer, faces become triangle fans.
 * Selection is baked in as a colour change, exactly like the default
 * renderer; there are no selection boxes or rings.
 */
final class RayScene {
  private RayScene() {
    // ...
  }

  static Primitive[] build(NodeManager manager) {
    final List<Primitive> primitives = new ArrayList<Primitive>();
    addNodes(manager, primitives);
    addLinks(manager, primitives);
    addFaces(manager, primitives);
    return primitives.toArray(new Primitive[primitives.size()]);
  }

  private static void addNodes(NodeManager manager,
      List<Primitive> primitives) {
    if (!FrEnd.render_nodes) {
      return;
    }
    final List<?> elements = manager.element;
    final int n = elements.size();
    for (int i = 0; i < n; i++) {
      final Node node = (Node) elements.get(i);
      final double radius = node.type.radius;
      if (radius <= 0.0) {
        continue;
      }
      primitives.add(new RTSphere(node.pos.x, node.pos.y, node.pos.z,
          radius, colourOf(node.type.selected, node.clazz.colour)));
    }
  }

  private static void addLinks(NodeManager manager,
      List<Primitive> primitives) {
    if (!FrEnd.render_links) {
      return;
    }
    final LinkManager link_manager = manager.getLinkManager();
    final List<?> elements = link_manager.element;
    final int n = elements.size();
    for (int i = 0; i < n; i++) {
      final Link link = (Link) elements.get(i);
      if (link.type.hidden) {
        continue;
      }
      final double radius = link.type.radius;
      if (radius <= 0.0) {
        continue;
      }
      final int colour = colourOf(link.type.selected, link.clazz.colour);
      final Node[] nodes = link.nodes;
      for (int s = 0; s < nodes.length - 1; s++) {
        final Node n1 = nodes[s];
        final Node n2 = nodes[s + 1];
        if (link.type.compression) {
          // A strut: stretched sphere, bulging mid-span.
          primitives.add(new RTEllipsoid(n1.pos.x, n1.pos.y, n1.pos.z,
              n2.pos.x, n2.pos.y, n2.pos.z, radius, colour));
        } else {
          // A cable: a plain cylinder, like the default renderer.
          primitives.add(new RTCylinder(n1.pos.x, n1.pos.y, n1.pos.z,
              n2.pos.x, n2.pos.y, n2.pos.z, radius, colour));
        }
      }
    }
  }

  private static void addFaces(NodeManager manager,
      List<Primitive> primitives) {
    if (!FrEnd.render_faces) {
      return;
    }
    final FaceManager face_manager = manager.getFaceManager();
    final List<?> elements = face_manager.element;
    final int n = elements.size();
    for (int i = 0; i < n; i++) {
      final Face face = (Face) elements.get(i);
      final ArrayList<Node> nodes = face.nodes;
      final int points = nodes.size();
      if (points < 3) {
        continue;
      }
      // Faces are opaque: the translucency bits are ignored.
      final int colour = colourOf(face.type.selected,
          face.clazz.colour & 0xFFFFFF);
      final Node n0 = nodes.get(0);
      for (int s = 1; s < points - 1; s++) {
        final Node n1 = nodes.get(s);
        final Node n2 = nodes.get(s + 1);
        primitives.add(new RTTriangle(n0.pos.x, n0.pos.y, n0.pos.z,
            n1.pos.x, n1.pos.y, n1.pos.z, n2.pos.x, n2.pos.y, n2.pos.z,
            colour));
      }
    }
  }

  private static int colourOf(boolean selected, int colour) {
    return selected ? RendererDelegator.colour_selected_number : colour;
  }
}
