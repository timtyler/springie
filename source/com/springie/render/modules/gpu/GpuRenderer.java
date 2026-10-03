// This code has been placed into the public domain by its author

package com.springie.render.modules.gpu;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Panel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import com.springie.FrEnd;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.scene.ModelScene;
import com.springie.render.scene.SceneExtractor;

/**
 * The "GPU" renderer: the extracted model scene rendered with JavaFX 3D,
 * hosted in a JFXPanel inside the main canvas panel. Each repaint
 * extracts a ModelScene under the usual lock and hands it to the FX
 * Application Thread; the 3D scene graph is rebuilt there. Construct on
 * the AWT event thread (it creates Swing components).
 */
public class GpuRenderer implements ModularRendererBase {
  private final JFXPanel jfx_panel;
  private final GpuView view;
  private final LayoutManager previous_layout;

  public GpuRenderer() {
    Platform.setImplicitExit(false);
    this.view = new GpuView();
    this.jfx_panel = new JFXPanel();
    final Panel canvas_panel = FrEnd.main_canvas.panel;
    this.previous_layout = canvas_panel.getLayout();
    canvas_panel.setLayout(new BorderLayout());
    canvas_panel.add(this.jfx_panel, BorderLayout.CENTER);
    canvas_panel.validate();
    forwardMouseEvents();
    final GpuView view = this.view;
    final JFXPanel panel = this.jfx_panel;
    Platform.runLater(() -> panel.setScene(view.createScene(
        Math.max(1.0, panel.getWidth()),
        Math.max(1.0, panel.getHeight()))));
  }

  /** Removes the JFXPanel when switching to another renderer. */
  public void uninstall() {
    final Panel canvas_panel = FrEnd.main_canvas.panel;
    canvas_panel.remove(this.jfx_panel);
    canvas_panel.setLayout(this.previous_layout);
    canvas_panel.validate();
  }

  @Override
  public void repaint(Graphics graphics, NodeManager manager) {
    // The AWT graphics are unused: the JFXPanel paints itself. The
    // extract runs under the caller's ContextManager lock (reentrant).
    final ModelScene scene = SceneExtractor.extract(manager,
        FrEnd.render_nodes, FrEnd.render_links, FrEnd.render_faces);
    final GpuView view = this.view;
    Platform.runLater(() -> view.update(scene));
  }

  @Override
  public void resize(int x, int y) {
    final GpuView view = this.view;
    Platform.runLater(() -> view.resize(x, y));
  }

  @Override
  public void reset() {
    final GpuView view = this.view;
    Platform.runLater(view::clear);
  }

  /**
   * The JFXPanel covers the canvas, so the panel's own mouse listeners
   * (selection, gestures, drag box) would go deaf. Re-dispatch every
   * mouse event to the canvas panel with translated coordinates, so all
   * existing interaction keeps working unchanged.
   */
  private void forwardMouseEvents() {
    final Panel target = FrEnd.main_canvas.panel;
    this.jfx_panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        forward(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        forward(e);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        forward(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        forward(e);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        forward(e);
      }

      private void forward(MouseEvent e) {
        target.dispatchEvent(new MouseEvent(target, e.getID(),
            e.getWhen(), e.getModifiers(), e.getX(), e.getY(),
            e.getClickCount(), e.isPopupTrigger(), e.getButton()));
      }
    });
    this.jfx_panel.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        target.dispatchEvent(new MouseEvent(target, e.getID(),
            e.getWhen(), e.getModifiers(), e.getX(), e.getY(),
            e.getClickCount(), e.isPopupTrigger(), e.getButton()));
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        target.dispatchEvent(new MouseEvent(target, e.getID(),
            e.getWhen(), e.getModifiers(), e.getX(), e.getY(),
            e.getClickCount(), e.isPopupTrigger(), e.getButton()));
      }
    });
  }
}
