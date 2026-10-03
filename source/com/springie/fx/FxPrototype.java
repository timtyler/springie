// This code has been placed into the public domain by its author
// JavaFX prototype: proves the JavaFX 3D pipeline runs in this
// environment (Maven + Xvfb). A spoked wheel, Springie-style.

package com.springie.fx;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

/**
 * Minimal JavaFX 3D prototype: a spoked wheel turning under a point
 * light, to prove the JavaFX runtime, 3D scene graph and render loop
 * work here before any Springie integration.
 */
public class FxPrototype extends Application {
  private static final int RIM_RADIUS = 120;
  private static final int SPOKES = 6;

  @Override
  public void start(final Stage stage) {
    final Group wheel = buildWheel();

    // Ground plane for depth.
    final Box ground = new Box(1200, 10, 1200);
    ground.setTranslateY(RIM_RADIUS + 30);
    ground.setMaterial(new PhongMaterial(Color.rgb(40, 60, 40)));

    final Group root = new Group(wheel, ground);

    final AmbientLight ambient = new AmbientLight(Color.rgb(80, 80, 80));
    final PointLight lamp = new PointLight(Color.WHITE);
    lamp.setTranslateX(-300);
    lamp.setTranslateY(-400);
    lamp.setTranslateZ(-300);
    root.getChildren().addAll(ambient, lamp);

    final PerspectiveCamera camera = new PerspectiveCamera(true);
    camera.setTranslateZ(-700);
    camera.setNearClip(1);
    camera.setFarClip(5000);

    final Scene scene = new Scene(root, 800, 600, true);
    scene.setFill(Color.rgb(20, 24, 40));
    scene.setCamera(camera);

    stage.setTitle("Springie JavaFX prototype");
    stage.setScene(scene);
    stage.show();

    final Rotate spin = new Rotate(0, Rotate.Z_AXIS);
    wheel.getTransforms().add(spin);
    new AnimationTimer() {
      @Override
      public void handle(final long now) {
        spin.setAngle((now / 20_000_000.0) % 360);
      }
    }.start();
  }

  private Group buildWheel() {
    final Group wheel = new Group();

    // Rim: a flat cylinder standing up like a coin.
    final Cylinder rim = new Cylinder(RIM_RADIUS, 14, 48);
    rim.setRotationAxis(Rotate.X_AXIS);
    rim.setRotate(90);
    rim.setMaterial(new PhongMaterial(Color.rgb(180, 60, 40)));
    wheel.getChildren().add(rim);

    // Spokes: thin cylinders from hub to rim.
    final PhongMaterial spoke_mat =
        new PhongMaterial(Color.rgb(220, 200, 160));
    for (int i = 0; i < SPOKES; i++) {
      final Cylinder spoke = new Cylinder(5, RIM_RADIUS - 10, 8);
      spoke.setMaterial(spoke_mat);
      final double angle = i * 360.0 / SPOKES;
      // Point the spoke outward, then swing it into the wheel plane.
      spoke.setRotationAxis(Rotate.X_AXIS);
      spoke.setRotate(90);
      final Group holder = new Group(spoke);
      spoke.setTranslateY(-(RIM_RADIUS - 10) / 2.0 + 5);
      holder.setRotationAxis(Rotate.Z_AXIS);
      holder.setRotate(angle);
      wheel.getChildren().add(holder);
    }

    // Hub.
    final Sphere hub = new Sphere(22, 24);
    hub.setMaterial(new PhongMaterial(Color.rgb(60, 60, 70)));
    wheel.getChildren().add(hub);

    return wheel;
  }

  public static void main(final String[] args) {
    launch(args);
  }
}
