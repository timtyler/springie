// This code has been placed into the public domain by its author

package com.springie.render.modules;

import java.awt.Graphics;

import com.springie.elements.nodes.NodeManager;

public interface ModularRendererBase {
  void repaint(Graphics graphics, NodeManager manager);
  void resize(int x, int y);
  void reset();

  /**
   * True while the renderer is still producing the current frame and the
   * model must not advance: the generation counter, the dynamics update
   * and the world update are all skipped until the frame completes. The
   * animation then runs at the renderer's pace, and every model state is
   * rendered exactly once, in order, instead of intermediate states being
   * skipped to keep real-time pace. Renderers that keep up with the model
   * return false.
   */
  default boolean holdModelForFrame() {
    return false;
  }
}
