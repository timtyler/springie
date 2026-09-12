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
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

/**
 * Builds an immutable ray-traceable snapshot of the model: nodes become
 * spheres, struts become stretched spheres (ellipsoids), cables become
 * open cylinders like the default renderer, faces become triangle fans.
 * Link and face selection is baked in as a colour change, exactly like
 * the default renderer. Node selection is a red billboard ring around
 * the node -- the ray-traced version of the default renderer's
 * screen-space selection circle -- built separately by
 * {@link #selectionRings} so it stays out of the BVH and can never cast
 * shadows.
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
      // Selected nodes keep their class colour, like the default
      // renderer; the selection itself is the billboard ring from
      // selectionRings().
      primitives.add(new RTSphere(node.pos.x, node.pos.y, node.pos.z,
          radius, node.clazz.colour));
    }
  }

  /**
   * One billboard selection ring per selected node: a flat annulus in
   * the plane through the node centre, perpendicular to the ray from
   * the camera eye to that centre, so it always faces the viewer --
   * exactly like the default renderer's screen-space selection circle.
   *
   * <p>The ring sits just outside the node: 5 to 7 pixels beyond its
   * silhouette at the node's depth (the default renderer draws its
   * circle 6 pixels out), in the same red (0xFF4040) the default
   * renderer uses, unlit. Kept out of the BVH: selection rings never
   * cast shadows, and when nothing is selected the array is empty, so
   * the per-ray cost is a single length check.
   *
   * @param ex ey ez the camera eye position, in world units
   */
  static RTRing[] selectionRings(NodeManager manager, double ex, double ey,
      double ez) {
    if (!FrEnd.render_nodes) {
      return new RTRing[0];
    }
    final List<RTRing> rings = new ArrayList<RTRing>();
    final List<?> elements = manager.element;
    final int n = elements.size();
    for (int i = 0; i < n; i++) {
      final Node node = (Node) elements.get(i);
      if (!node.type.selected) {
        continue;
      }
      final double radius = node.type.radius;
      if (radius <= 0.0) {
        continue;
      }
      final double nx = node.pos.x - ex;
      final double ny = node.pos.y - ey;
      final double nz = node.pos.z - ez;
      final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (length <= 1e-9) {
        // Degenerate: the node sits on the eye; no well-defined plane.
        continue;
      }
      // World units per pixel at the node's depth, from the same
      // projection the camera inverts (Coords.getRadius divides by
      // exactly this).
      final double world_per_pixel = Coords.shift_constant_z
          + (node.pos.z >> Coords.shift_z);
      final double inner = radius + 5.0 * world_per_pixel;
      final double outer = radius + 7.0 * world_per_pixel;
      rings.add(new RTRing(node.pos.x, node.pos.y, node.pos.z, nx / length,
          ny / length, nz / length, inner, outer, 0xFF4040));
    }
    return rings.toArray(new RTRing[rings.size()]);
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
