// This code has been placed into the public domain by its author

package com.springie.render.modules.gpu;

import java.awt.Graphics;
import java.awt.Panel;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;

import com.springie.FrEnd;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.scene.ModelScene;
import com.springie.render.scene.SceneExtractor;

/**
 * The "GPU" renderer: the extracted model scene rendered with JavaFX 3D
 * into an offscreen scene, then blitted to the AWT canvas. Each repaint
 * draws the latest rendered frame and queues a scene-graph rebuild on
 * the FX Application Thread (at most one queued; stale frames are
 * skipped, not queued). Construct on the AWT event thread.
 *
 * JFXPanel is deliberately not used: on some Windows setups its
 * presentation path shows nothing while the scene itself renders fine,
 * so the pixels are copied to AWT directly instead.
 */
public class GpuRenderer implements ModularRendererBase {
  private final GpuView view = new GpuView();
  private volatile javafx.scene.Scene fx_scene;
  private volatile BufferedImage latest_frame;
  private int scene_width;
  private int scene_height;

  public GpuRenderer() {
    // Keep the toolkit alive: with no Stage or JFXPanel, implicit exit
    // would shut it down.
    Platform.setImplicitExit(false);
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException e) {
      // Toolkit already running.
    }
    final Panel canvas_panel = FrEnd.main_canvas.panel;
    this.scene_width = Math.max(1, canvas_panel.getWidth());
    this.scene_height = Math.max(1, canvas_panel.getHeight());
    final GpuView view = this.view;
    final int width = this.scene_width;
    final int height = this.scene_height;
    // Wait for the FX pipeline to be ready before 3D content is built;
    // creating meshes too early NPEs in GraphicsPipeline.getPipeline().
    final java.util.concurrent.CountDownLatch ready =
        new java.util.concurrent.CountDownLatch(1);
    Platform.runLater(() -> {
      this.fx_scene = view.createScene(width, height);
      // One pulse ensures the pipeline is live.
      final javafx.animation.AnimationTimer waiter =
          new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
              ready.countDown();
              stop();
            }
          };
      waiter.start();
    });
    try {
      ready.await(10, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Clears the offscreen scene when switching to another renderer. */
  public void uninstall() {
    final GpuView view = this.view;
    Platform.runLater(view::clear);
  }

  @Override
  public void repaint(Graphics graphics, NodeManager manager) {
    // Blit the latest rendered frame; the FX thread refreshes it
    // asynchronously below.
    final BufferedImage frame = this.latest_frame;
    if (frame != null) {
      graphics.drawImage(frame, 0, 0, null);
    }
    final javafx.scene.Scene fx_scene = this.fx_scene;
    if (fx_scene == null) {
      return;
    }
    // The extract runs under the caller's ContextManager lock (reentrant).
    final ModelScene scene = SceneExtractor.extract(manager,
        FrEnd.render_nodes, FrEnd.render_links, FrEnd.render_faces);
    final GpuView view = this.view;
    // No throttling: every frame is queued so the true renderer
    // throughput is visible.
    Platform.runLater(() -> {
      view.update(scene);
      final javafx.scene.image.WritableImage snapshot =
          fx_scene.snapshot(null);
      this.latest_frame = SwingFXUtils.fromFXImage(snapshot, null);
      // One completed 3D frame for the Statistics tab FPS readout.
      RendererDelegator.countRenderedFrame();
    });
  }

  @Override
  public void resize(int x, int y) {
    if (x == this.scene_width && y == this.scene_height) {
      return;
    }
    this.scene_width = x;
    this.scene_height = y;
    final GpuView view = this.view;
    Platform.runLater(() -> {
      // Recreate at the new size so the blit stays 1:1.
      this.fx_scene = view.createScene(x, y);
      this.latest_frame = null;
    });
  }

  @Override
  public void reset() {
    final GpuView view = this.view;
    Platform.runLater(view::clear);
  }
}
