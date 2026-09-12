// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.ModularRendererNew;

/**
 * The ray-traced renderer holds the model while a frame is rendering, so
 * every model state is rendered exactly once, in order, instead of
 * intermediate states being skipped to keep real-time pace. Renderers
 * that keep up with the model never hold.
 */
public class HoldModelForFrameTest {

  private void setFrameDone(ModularRendererRaytraced renderer, boolean done)
      throws Exception {
    final Field field =
        ModularRendererRaytraced.class.getDeclaredField("frame_done");
    field.setAccessible(true);
    field.setBoolean(renderer, done);
  }

  @Test
  public void modernRendererNeverHolds() {
    assertFalse(new ModularRendererNew().holdModelForFrame(),
        "the default renderer keeps up with the model and must not hold it");
  }

  @Test
  public void raytracedRendererDoesNotHoldBeforeFirstFrame()
      throws Exception {
    assertFalse(new ModularRendererRaytraced().holdModelForFrame(),
        "with no frame in progress the model must advance freely");
  }

  @Test
  public void raytracedRendererHoldsWhileFrameRenders() throws Exception {
    final ModularRendererRaytraced renderer = new ModularRendererRaytraced();
    setFrameDone(renderer, false);
    final ModularRendererBase previous = RendererDelegator.renderer;
    RendererDelegator.renderer = renderer;
    try {
      assertTrue(renderer.holdModelForFrame(),
          "a frame in progress must hold the model so no state is skipped");
    } finally {
      RendererDelegator.renderer = previous;
    }
  }

  @Test
  public void holdReleasesWhenRendererIsSwitched() throws Exception {
    final ModularRendererRaytraced renderer = new ModularRendererRaytraced();
    setFrameDone(renderer, false);
    // RendererDelegator.renderer is still the default: a mid-frame
    // renderer switch must not freeze the model.
    assertFalse(renderer.holdModelForFrame(),
        "switching renderers mid-frame must release the hold");
  }

  @Test
  public void holdReleasesWhenFrameCompletes() throws Exception {
    final ModularRendererRaytraced renderer = new ModularRendererRaytraced();
    setFrameDone(renderer, false);
    final ModularRendererBase previous = RendererDelegator.renderer;
    RendererDelegator.renderer = renderer;
    try {
      setFrameDone(renderer, true);
      assertFalse(renderer.holdModelForFrame(),
          "a completed frame must let the model advance to the next state");
    } finally {
      RendererDelegator.renderer = previous;
    }
  }
}
