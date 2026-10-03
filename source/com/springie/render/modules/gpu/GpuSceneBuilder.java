// This code has been placed into the public domain by its author

package com.springie.render.modules.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

import com.springie.render.RendererDelegator;
import com.springie.render.scene.ModelScene;
import com.springie.render.scene.SceneFace;
import com.springie.render.scene.SceneLink;
import com.springie.render.scene.SceneNode;

/**
 * Builds a JavaFX 3D scene graph from an extracted ModelScene: nodes
 * become spheres, links become cylinders, faces become triangle meshes.
 * A pure function of the scene -- no camera, no lights, no toolkit
 * state beyond the shapes themselves -- so it is unit-testable.
 * Selection is shown with the selection colour, like the other
 * renderers. (Pixellation and anti-aliasing are deliberately not a
 * concern here.)
 */
final class GpuSceneBuilder {
  private final Map<Integer, PhongMaterial> materials = new HashMap<>();

  /** The model content of one frame, without camera or lights. */
  Group build(ModelScene scene) {
    final List<javafx.scene.Node> children = new ArrayList<>();
    for (final SceneNode node : scene.nodes) {
      children.add(makeNode(node));
    }
    for (final SceneLink link : scene.links) {
      final Cylinder cylinder = makeLink(link);
      if (cylinder != null) {
        children.add(cylinder);
      }
    }
    for (final SceneFace face : scene.faces) {
      children.add(makeFace(face));
    }
    final Group group = new Group();
    group.getChildren().addAll(children);
    return group;
  }

  private Sphere makeNode(SceneNode node) {
    final Sphere sphere = new Sphere(node.radius, 12);
    sphere.setTranslateX(node.x);
    sphere.setTranslateY(node.y);
    sphere.setTranslateZ(node.z);
    sphere.setMaterial(material(node.colour, node.selected));
    return sphere;
  }

  private Cylinder makeLink(SceneLink link) {
    final Point3D p1 = new Point3D(link.x1, link.y1, link.z1);
    final Point3D p2 = new Point3D(link.x2, link.y2, link.z2);
    final Point3D dir = p2.subtract(p1);
    final double length = dir.magnitude();
    if (length <= 1e-9 || link.radius <= 0.0) {
      return null;
    }
    final Cylinder cylinder = new Cylinder(link.radius, length, 6);
    cylinder.setMaterial(material(link.colour, link.selected));
    final Point3D mid = p1.add(p2).multiply(0.5);
    cylinder.setTranslateX(mid.getX());
    cylinder.setTranslateY(mid.getY());
    cylinder.setTranslateZ(mid.getZ());
    // A cylinder's axis is +Y; rotate it onto the link direction.
    final Point3D y_axis = new Point3D(0, 1, 0);
    final Point3D axis = y_axis.crossProduct(dir);
    if (axis.magnitude() > 1e-9) {
      cylinder.setRotationAxis(axis.normalize());
      cylinder.setRotate(Math.toDegrees(Math.acos(
          y_axis.dotProduct(dir) / length)));
    } else if (y_axis.dotProduct(dir) < 0.0) {
      // Antiparallel: 180 degrees about any perpendicular axis.
      cylinder.setRotationAxis(new Point3D(1, 0, 0));
      cylinder.setRotate(180.0);
    }
    return cylinder;
  }

  private MeshView makeFace(SceneFace face) {
    final TriangleMesh mesh = new TriangleMesh();
    mesh.getPoints().addAll((float) face.x1, (float) face.y1,
        (float) face.z1, (float) face.x2, (float) face.y2,
        (float) face.z2, (float) face.x3, (float) face.y3,
        (float) face.z3);
    mesh.getTexCoords().addAll(0, 0);
    mesh.getFaces().addAll(0, 0, 2, 0, 1, 0);
    final MeshView view = new MeshView(mesh);
    view.setMaterial(material(face.colour, face.selected));
    // Faces are single-sided in the model; render both sides rather
    // than depending on winding order.
    view.setCullFace(CullFace.NONE);
    return view;
  }

  private PhongMaterial material(int argb, boolean selected) {
    final int rgb = selected ? RendererDelegator.colour_selected_number
        : argb;
    PhongMaterial found = this.materials.get(rgb);
    if (found == null) {
      found = new PhongMaterial();
      found.setDiffuseColor(fxColor(rgb));
      found.setSpecularColor(Color.rgb(70, 70, 70));
      this.materials.put(rgb, found);
    }
    return found;
  }

  private static Color fxColor(int argb) {
    return Color.rgb((argb >> 16) & 255, (argb >> 8) & 255, argb & 255);
  }
}
