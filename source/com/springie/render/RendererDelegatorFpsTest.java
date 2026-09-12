// This code has been placed into the public domain by its author

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Statistics tab's frames-per-second readout counts completed
 * frames, not AWT paints. (It used to count paints, so in ray-traced
 * mode -- where paints fire far more often than the asynchronous
 * renderer finishes frames -- it showed a wildly inflated number.)
 *
 * <p>Headless-safe: counting never touches AWT; the label is only
 * published on the paint path.
 */
public class RendererDelegatorFpsTest {
  private int saved_frame_count;

  private long saved_time_last_ms;

  private String saved_fps_string;

  private boolean saved_fps_dirty;

  @BeforeEach
  public void setUp() {
    this.saved_frame_count = RendererDelegator.frame_count;
    this.saved_time_last_ms = RendererDelegator.time_last_ms;
    this.saved_fps_string = RendererDelegator.fps_string;
    this.saved_fps_dirty = RendererDelegator.fps_dirty;
    RendererDelegator.frame_count = 0;
    RendererDelegator.fps_dirty = false;
  }

  @AfterEach
  public void tearDown() {
    RendererDelegator.frame_count = this.saved_frame_count;
    RendererDelegator.time_last_ms = this.saved_time_last_ms;
    RendererDelegator.fps_string = this.saved_fps_string;
    RendererDelegator.fps_dirty = this.saved_fps_dirty;
  }

  @Test
  public void fiftyCompletedFramesCloseTheWindow() {
    for (int i = 0; i < 49; i++) {
      RendererDelegator.countRenderedFrame();
    }
    assertEquals(49, RendererDelegator.frame_count);
    assertFalse(RendererDelegator.fps_dirty,
        "no readout yet after 49 frames");
    // Keep the window's duration non-zero so the fps is finite.
    RendererDelegator.time_last_ms -= 100;
    RendererDelegator.countRenderedFrame();
    assertEquals(0, RendererDelegator.frame_count,
        "the 50th completed frame must close the window and reset");
    assertTrue(RendererDelegator.fps_dirty,
        "the 50th completed frame must publish a readout");
    assertNotNull(RendererDelegator.fps_string);
    // Must be a parseable number, not garbage.
    Double.parseDouble(RendererDelegator.fps_string);
  }

  @Test
  public void slowFramesStillRefreshTheReadout() {
    // A window already open for 2.1s with one frame counted.
    RendererDelegator.frame_count = 1;
    RendererDelegator.time_last_ms = System.currentTimeMillis() - 2100;
    RendererDelegator.countRenderedFrame();
    assertEquals(0, RendererDelegator.frame_count,
        "a frame after a 2s stall must close the window");
    assertTrue(RendererDelegator.fps_dirty,
        "slow frames must still refresh the readout");
    Double.parseDouble(RendererDelegator.fps_string);
  }

  @Test
  public void windowStartsAtTheFirstFrame() {
    // A stale time_last_ms (e.g. class load, long ago) must not make
    // the first frame report ~0 fps over an enormous duration.
    RendererDelegator.time_last_ms = 0;
    RendererDelegator.countRenderedFrame();
    assertFalse(RendererDelegator.fps_dirty,
        "the first frame only opens the window");
    assertTrue(RendererDelegator.time_last_ms > 0,
        "the window must start at the first frame");
  }
}
