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

  /**
   * True when calling repaint() would put something new on the screen.
   * The ray-traced renderer finishes frames asynchronously: while it is
   * still tracing, repaint() has nothing new to show, so the delegator
   * skips the call instead of spinning empty repaints at the animation
   * tick rate. Renderers that paint synchronously return true.
   */
  default boolean hasStagedFrame() {
    return true;
  }
}
