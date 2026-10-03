// This code has been placed into the public domain by its author

package com.springie.render.modules.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.faces.Face;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.scene.ModelScene;
import com.springie.render.scene.SceneExtractor;

/**
 * The GPU scene-graph builder maps the extracted scene 1:1: one sphere
 * per node, one cylinder per link segment, one mesh per face triangle,
 * at the scene's world-unit positions, with the selection colour shown
 * for selected elements.
 */
public class GpuSceneBuilderTest {
  @BeforeAll
  public static void initFx() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    // Starts the FX toolkit; shape construction needs it.
    new JFXPanel();
  }

  private static NodeManager buildModel() {
    final NodeManager manager = new NodeManager();
    final ArrayList<Node> nodes = new ArrayList<>();
    final int[][] positions = {{0, 0, 0}, {256000, 0, 0}, {0, 256000, 0}};
    for (int i = 0; i < 3; i++) {
      final Node node = manager.addNewAgent();
      node.pos = new Point3D(positions[i][0], positions[i][1],
          positions[i][2]);
      node.type.radius = 10 << Coords.shift;
      node.clazz = new Clazz(0xFF123456);
      nodes.add(node);
    }
    nodes.get(2).type.selected = true;

    final LinkTypeFactory types = new LinkTypeFactory();
    final LinkType strut_type = types.getNew(100 << Coords.shift, 50);
    strut_type.radius = 8 << Coords.shift;
    final Link strut = new Link(nodes.get(0), nodes.get(1), strut_type,
        new Clazz(0xFF654321));
    final LinkType cable_type = types.getNew(100 << Coords.shift, 50);
    cable_type.compression = false;
    cable_type.radius = 6 << Coords.shift;
    final Link cable = new Link(nodes.get(1), nodes.get(2), cable_type,
        new Clazz(0xFFABCDEF));
    manager.getLinkManager().element.add(strut);
    manager.getLinkManager().element.add(cable);

    manager.getFaceManager().element.add(new Face(new ArrayList<>(nodes),
        manager.getFaceManager().face_type_factory.getNew(),
        new Clazz(0xFFFEDCBA)));
    return manager;
  }

  private static int count(Group group, Class<?> clazz) {
    int n = 0;
    for (final javafx.scene.Node child : group.getChildren()) {
      if (clazz.isInstance(child)) {
        n++;
      }
    }
    return n;
  }

  @Test
  public void mapsSceneOneToOne() {
    final ModelScene scene = SceneExtractor.extract(buildModel(),
        true, true, true);
    final Group group = new GpuSceneBuilder().build(scene);

    assertEquals(3, count(group, Sphere.class), "one sphere per node");
    assertEquals(2, count(group, Cylinder.class), "one cylinder per link");
    assertEquals(1, count(group, MeshView.class), "one mesh per triangle");

    // Node positions land in world units (pixels).
    boolean found = false;
    for (final javafx.scene.Node child : group.getChildren()) {
      if (child instanceof Sphere
          && child.getTranslateX() == 1000.0
          && child.getTranslateY() == 0.0
          && ((Sphere) child).getRadius() == 10.0) {
        found = true;
      }
    }
    assertTrue(found, "sphere at the node's world position and radius");
  }

  @Test
  public void selectedNodeShowsSelectionColour() {
    final ModelScene scene = SceneExtractor.extract(buildModel(),
        true, true, true);
    final Group group = new GpuSceneBuilder().build(scene);

    final Color expected = Color.rgb(
        (RendererDelegator.colour_selected_number >> 16) & 255,
        (RendererDelegator.colour_selected_number >> 8) & 255,
        RendererDelegator.colour_selected_number & 255);
    boolean found = false;
    for (final javafx.scene.Node child : group.getChildren()) {
      if (child instanceof Sphere && child.getTranslateX() == 0.0
          && child.getTranslateY() == 1000.0) {
        final PhongMaterial material =
            (PhongMaterial) ((Sphere) child).getMaterial();
        if (material.getDiffuseColor().equals(expected)) {
          found = true;
        }
      }
    }
    assertTrue(found, "selected node uses the selection colour");
  }

  @Test
  public void linkCylinderSpansItsEndpoints() {
    final ModelScene scene = SceneExtractor.extract(buildModel(),
        true, true, true);
    final Group group = new GpuSceneBuilder().build(scene);

    // The a-b link runs (0,0,0)-(1000,0,0): midpoint (500,0,0),
    // length 1000.
    boolean found = false;
    for (final javafx.scene.Node child : group.getChildren()) {
      if (child instanceof Cylinder
          && child.getTranslateX() == 500.0
          && child.getTranslateY() == 0.0
          && child.getTranslateZ() == 0.0
          && ((Cylinder) child).getHeight() == 1000.0) {
        found = true;
      }
    }
    assertTrue(found, "cylinder spans the link endpoints");
  }

  @Test
  public void viewShowsExtractedScene() throws Exception {
    final ModelScene scene = SceneExtractor.extract(buildModel(),
        true, true, true);
    final GpuView view = new GpuView();
    final FutureTask<Scene> create = new FutureTask<>(
        () -> view.createScene(800, 600));
    Platform.runLater(create);
    final Scene fx_scene = create.get(10, TimeUnit.SECONDS);
    assertTrue(fx_scene.getCamera() instanceof PerspectiveCamera,
        "3D camera installed");
    assertTrue(fx_scene.isDepthBuffer(), "depth buffer on for 3D");

    final FutureTask<?> update = new FutureTask<>(() -> {
      view.update(scene);
      return null;
    });
    Platform.runLater(update);
    update.get(10, TimeUnit.SECONDS);
    assertEquals(6, view.getModelGroup().getChildren().size(),
        "3 spheres + 2 cylinders + 1 mesh");
  }
}
