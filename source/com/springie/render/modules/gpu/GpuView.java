// This code has been placed into the public domain by its author

package com.springie.render.modules.gpu;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;

import com.springie.render.Coords;
import com.springie.render.scene.ModelScene;

/**
 * The JavaFX side of the GPU renderer. Owns the 3D scene: a model group
 * rebuilt from each extracted ModelScene, a camera matching the AWT
 * projection (eye from Coords, perspective strength 1/1024 like the
 * other renderers), and a simple two-light rig. All methods run on the
 * FX Application Thread.
 */
final class GpuView {
  private Group model_group;
  private PerspectiveCamera camera;
  private PointLight point_light;
  private final GpuSceneBuilder builder = new GpuSceneBuilder();

  Scene createScene(double width, double height) {
    this.model_group = new Group();
    final Group root = new Group();
    // The FX scene is display-only: mouse interaction stays on the AWT
    // canvas. Mouse-transparent skips JavaFX's 3D ray-picking on every
    // mouse event, which would otherwise pick against hundreds of
    // shapes per event and stall the FX thread during drags.
    root.setMouseTransparent(true);
    final AmbientLight ambient = new AmbientLight(Color.rgb(110, 110, 130));
    this.point_light = new PointLight(Color.WHITE);
    root.getChildren().addAll(this.model_group, ambient, this.point_light);
    this.camera = new PerspectiveCamera(true);
    positionCamera(width, height);
    final Scene scene = new Scene(root, width, height, true,
        SceneAntialiasing.DISABLED);
    scene.setCamera(this.camera);
    scene.setFill(Color.rgb(8, 10, 20));
    return scene;
  }

  /** Shows the latest extracted scene. */
  void update(ModelScene scene) {
    this.model_group.getChildren().setAll(this.builder.build(scene)
        .getChildren());
  }

  /** Clears the model, keeping lights and camera. */
  void clear() {
    this.model_group.getChildren().clear();
  }

  /** Test hook: the live model content. */
  Group getModelGroup() {
    return this.model_group;
  }

  void resize(double width, double height) {
    positionCamera(width, height);
  }

  /**
   * Matches the AWT projection: the eye sits at the Coords eye position
   * (canvas centre in x/y, 1024 * D in front in fixed-point z) and looks
   * along +z; the vertical field of view gives the same perspective
   * strength (1/1024 per pixel). JavaFX is +x right, +y down, +z into
   * the screen -- the same handedness as the model -- so no axis flips.
   */
  private void positionCamera(double width, double height) {
    final double ex = ((Coords.x_pixelso2 << Coords.shift)
        - Coords.shift_constant_x) / 256.0;
    final double ey = ((Coords.y_pixelso2 << Coords.shift)
        - Coords.shift_constant_y) / 256.0;
    final double ez = -1024.0 * Coords.shift_constant_z / 256.0;
    this.camera.setTranslateX(ex);
    this.camera.setTranslateY(ey);
    this.camera.setTranslateZ(ez);
    this.camera.setFieldOfView(
        Math.toDegrees(2.0 * Math.atan((height / 2.0) / 1024.0)));
    this.camera.setNearClip(1.0);
    this.camera.setFarClip(200000.0);
    // Key light follows the camera, like a headlamp.
    this.point_light.setTranslateX(ex);
    this.point_light.setTranslateY(ey);
    this.point_light.setTranslateZ(ez);
  }
}
