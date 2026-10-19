// This code has been placed into the public domain by its author.

package com.springie;

import com.springie.gui.state.UniverseState;
import com.springie.messages.MessagePump;

/**
 * The animation thread: processes messages and triggers repaints.
 * Extracted from FrEnd (Tim, 2026-10-18) as the first step in
 * splitting up the GUI module.
 *
 * <p>Physics runs on the AWT thread via the repaint path
 * (RendererDelegator.passOnToUpdateMethods); this thread only pumps
 * messages and schedules the repaints.
 */
public class AnimationLoop implements Runnable {
  public static volatile boolean thread_terminated;

  private static volatile Thread runner;

  @Override
  public final void run() {
    thread_terminated = false;

    while (!thread_terminated) {
      // Stepping is decremented in RendererDelegator, synchronously with
      // the physics (nodeAndLinkUpdate). It used to be here, but repaint()
      // is async -- the counter would hit zero and set World.paused=true before
      // the AWT thread ran the physics, so steps were lost. (f9abb82 moved
      // message processing to this thread, introducing the race.)

      // Process messages on the animation thread (not the AWT thread):
      // model-building messages must not run concurrently with physics.
      MessagePump.processAll();

      // (Tim, 2026-10-17): no separate animation thread. Physics always
      // runs on the AWT thread via the repaint path. Headless mode
      // (World.headless) skips the pixel rendering in RendererDelegator,
      // but the physics path is identical.
      FrEnd.main_canvas.panel.repaint();

      if (UniverseState.delay > 0) {
        takeFortyWinks();
      }

      Thread.yield();
    }

    thread_terminated = true;

    Thread.yield();
  }

  private static void takeFortyWinks() {
    try {
      Thread.sleep(UniverseState.delay);
    } catch (InterruptedException e) {
    }
  }

  /** Starts the animation thread. */
  public static void startThread(final FrEnd frEnd) {
    FrEnd.main_canvas.forceResize();
    thread_terminated = false;

    runner = new Thread(new AnimationLoop());
    runner.start();

    frEnd.validate();
    frEnd.repaint();
  }

  /** Stops the animation thread. */
  public static void stopThread() {
    thread_terminated = true;
  }
}
