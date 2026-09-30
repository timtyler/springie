package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.springie.FrEnd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the pause+step bug (Tim, 2026-09-30): clicking Step
 * while paused only advanced the simulation less than half the time.
 *
 * <p>Root cause: the stepping counter was decremented in FrEnd.run()
 * (animation thread), but the physics runs in RendererDelegator (AWT thread,
 * via async repaint). The animation thread would decrement the counter to
 * zero and set paused=true before the AWT thread ran the physics -- the
 * step was lost. The race was introduced by f9abb82 (2026-09-15), which moved
 * message processing to the animation thread while physics stayed on AWT.
 *
 * <p>Fix: the counter is now decremented in RendererDelegator.stepIfNeeded(),
 * synchronously with the physics frame (right after nodeAndLinkUpdate()).
 * Each stepIfNeeded() call corresponds to exactly one physics frame.
 */
class SteppingTest {

  private boolean old_paused;
  private int old_stepping;

  @BeforeEach
  void saveState() {
    old_paused = FrEnd.paused;
    old_stepping = FrEnd.stepping;
  }

  @AfterEach
  void restoreState() {
    FrEnd.paused = old_paused;
    FrEnd.stepping = old_stepping;
  }

  @Test
  void stepDecrementsCounterSynchronously() {
    // Simulate the step button: paused=false, stepping=2.
    FrEnd.paused = false;
    FrEnd.stepping = 2;

    // First physics frame: counter 2 -> 1, still running.
    RendererDelegator.stepIfNeeded();
    assertEquals(1, FrEnd.stepping, "stepping must decrement by one per frame");
    assertFalse(FrEnd.paused, "must not pause until counter reaches zero");

    // Second physics frame: counter 1 -> 0, endStepping() pauses.
    RendererDelegator.stepIfNeeded();
    assertEquals(0, FrEnd.stepping, "stepping must reach zero");
    assertTrue(FrEnd.paused, "must pause when stepping completes");
  }

  @Test
  void stepDoesNothingWhenCounterIsZero() {
    FrEnd.paused = false;
    FrEnd.stepping = 0;

    RendererDelegator.stepIfNeeded();

    assertEquals(0, FrEnd.stepping, "zero counter must stay zero");
    assertFalse(FrEnd.paused, "must not pause when not stepping");
  }

  @Test
  void singleStepPausesAfterOneFrame() {
    // The common case: user clicks Step once (stepping=1).
    FrEnd.paused = false;
    FrEnd.stepping = 1;

    RendererDelegator.stepIfNeeded();

    assertEquals(0, FrEnd.stepping);
    assertTrue(FrEnd.paused, "single step must pause after exactly one frame");
  }
}
