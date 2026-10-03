// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import java.util.ArrayList;
import java.util.List;

import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.scene.ModelScene;
import com.springie.render.scene.SceneFace;
import com.springie.render.scene.SceneLink;
import com.springie.render.scene.SceneNode;

/**
 * Converts a {@link ModelScene} snapshot into ray-traceable primitives:
 * nodes become spheres, struts become stretched spheres (ellipsoids),
 * cables become open cylinders like the default renderer, faces are
 * already triangulated by the extractor. The model walk (and its
 * filters) lives in the extractor; this class only maps scene entries
 * to primitives, converting world units back to the fixed-point units
 * the ray tracer works in (an exact round-trip). Link and face
 * selection is baked in as a colour change, exactly like the default
 * renderer. Node selection is a red billboard ring around the node --
 * the ray-traced version of the default renderer's screen-space
 * selection circle -- built separately by {@link #selectionRings} so it
 * stays out of the BVH and can never cast shadows.
 */
final class RayScene {
  /** Fixed-point model units per world unit: inverts the extractor. */
  private static final double FIXED_PER_WORLD = 1 << Coords.shift;

  private RayScene() {
    // ...
  }

  static Primitive[] build(ModelScene scene) {
    final List<Primitive> primitives = new ArrayList<Primitive>();
    addNodes(scene, primitives);
    addLinks(scene, primitives);
    addFaces(scene, primitives);
    return primitives.toArray(new Primitive[primitives.size()]);
  }

  /** World units back to the fixed-point units the tracer works in. */
  private static double fixed(final double world) {
    return world * FIXED_PER_WORLD;
  }

  private static void addNodes(ModelScene scene,
      List<Primitive> primitives) {
    for (final SceneNode node : scene.nodes) {
      // The extractor already filtered hidden types and zero radii.
      primitives.add(new RTSphere(fixed(node.x), fixed(node.y),
          fixed(node.z), fixed(node.radius), node.colour));
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
  static RTRing[] selectionRings(List<SceneNode> nodes, double ex,
      double ey, double ez) {
    final List<RTRing> rings = new ArrayList<RTRing>();
    for (final SceneNode node : nodes) {
      if (!node.selected) {
        continue;
      }
      // The extractor already filtered zero radii.
      final double x = fixed(node.x);
      final double y = fixed(node.y);
      final double z = fixed(node.z);
      final double radius = fixed(node.radius);
      final double nx = x - ex;
      final double ny = y - ey;
      final double nz = z - ez;
      final double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (length <= 1e-9) {
        // Degenerate: the node sits on the eye; no well-defined plane.
        continue;
      }
      // World units per pixel at the node's depth, from the same
      // projection the camera inverts (Coords.getRadius divides by
      // exactly this).
      final double world_per_pixel = Coords.shift_constant_z
          + ((int) z >> Coords.shift_z);
      // Like the default renderer's selection ring: a prominent annulus
      // starting at 4/3 the node radius, several pixels thick, so the
      // selection reads as red at a glance.
      final double inner = radius * 4.0 / 3.0;
      final double outer = inner + 8.0 * world_per_pixel;
      rings.add(new RTRing(x, y, z, nx / length, ny / length, nz / length,
          inner, outer, 0xFF4040));
    }
    return rings.toArray(new RTRing[rings.size()]);
  }

  private static void addLinks(ModelScene scene,
      List<Primitive> primitives) {
    for (final SceneLink link : scene.links) {
      // The extractor already filtered hidden types and zero radii, and
      // split multi-node links into segments.
      final int colour = colourOf(link.selected, link.colour);
      final double x1 = fixed(link.x1);
      final double y1 = fixed(link.y1);
      final double z1 = fixed(link.z1);
      final double x2 = fixed(link.x2);
      final double y2 = fixed(link.y2);
      final double z2 = fixed(link.z2);
      final double radius = fixed(link.radius);
      if (link.strut) {
        // A strut: stretched sphere, bulging mid-span.
        primitives.add(
            new RTEllipsoid(x1, y1, z1, x2, y2, z2, radius, colour));
      } else {
        // A cable: a plain cylinder, like the default renderer.
        primitives.add(
            new RTCylinder(x1, y1, z1, x2, y2, z2, radius, colour));
      }
    }
  }

  private static void addFaces(ModelScene scene,
      List<Primitive> primitives) {
    for (final SceneFace face : scene.faces) {
      // The extractor already fan-triangulated and filtered hidden
      // types. Faces are opaque: the translucency bits are ignored.
      final int colour = colourOf(face.selected,
          face.colour & 0xFFFFFF);
      primitives.add(new RTTriangle(fixed(face.x1), fixed(face.y1),
          fixed(face.z1), fixed(face.x2), fixed(face.y2), fixed(face.z2),
          fixed(face.x3), fixed(face.y3), fixed(face.z3), colour));
    }
  }

  private static int colourOf(boolean selected, int colour) {
    return selected ? RendererDelegator.colour_selected_number : colour;
  }
}
