package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.springie.elements.nodes.NodeManager;
import com.springie.render.modules.ModularRendererBase;

/**
 * While the ray-traced renderer holds the model for an in-flight frame,
 * repaint() has nothing new to show until the frame stages -- so the
 * delegator must skip the call instead of redrawing at the animation
 * tick rate. (Before the fix, every animation tick ran the full repaint
 * path while a frame rendered, so one completed frame saw dozens of
 * empty redraws.)
 *
 * <p>Headless-safe: with the model held, passOnToUpdateMethods touches
 * nothing but the renderer stub.
 */
class RendererDelegatorHoldTest {

  private static final class StubRenderer implements ModularRendererBase {
    final boolean hold;
    final boolean staged;
    int repaint_calls;

    StubRenderer(boolean hold, boolean staged) {
      this.hold = hold;
      this.staged = staged;
    }

    @Override
    public void repaint(Graphics graphics, NodeManager manager) {
      this.repaint_calls++;
    }

    @Override
    public void resize(int x, int y) {
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean holdModelForFrame() {
      return this.hold;
    }

    @Override
    public boolean hasStagedFrame() {
      return this.staged;
    }
  }

  private ModularRendererBase saved_renderer;

  private int repaintCallsWhile(boolean hold, boolean staged) {
    this.saved_renderer = RendererDelegator.renderer;
    final StubRenderer stub = new StubRenderer(hold, staged);
    RendererDelegator.renderer = stub;
    try {
      RendererDelegator.passOnToUpdateMethods(null);
    } finally {
      RendererDelegator.renderer = this.saved_renderer;
    }
    return stub.repaint_calls;
  }

  @AfterEach
  void restoreRenderer() {
    if (this.saved_renderer != null) {
      RendererDelegator.renderer = this.saved_renderer;
      this.saved_renderer = null;
    }
  }

  @Test
  void holdingWithNothingStagedSkipsRepaint() {
    assertEquals(0, repaintCallsWhile(true, false),
        "a held renderer with no staged frame must not be repainted");
  }

  @Test
  void holdingWithStagedFrameRepaints() {
    assertEquals(1, repaintCallsWhile(true, true),
        "a held renderer with a staged frame must be repainted");
  }
}
