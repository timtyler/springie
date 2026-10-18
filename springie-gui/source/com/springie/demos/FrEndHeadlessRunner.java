// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;
import com.springie.world.World;

/**
 * Runs demos through the real FrEnd with the renderer off
 * (Tim, 2026-10-17: no separate animation thread; World.headless means
 * "renderer off", physics via the normal AWT path).
 *
 * <p>Replaces the old HeadlessRunner direct-NodeManager approach, which
 * bypassed FrEnd, the message path, and the real World settings.
 */
public class FrEndHeadlessRunner {

  /**
   * Boots FrEnd, sets World.headless (renderer off), loads the demo via
   * the message path, runs the given number of ticks, and returns the
   * NodeManager for measurement.
   */
  public static NodeManager runDemo(
      final Object demo_message, final int ticks) throws Exception {
    final NodeManager nm = startHeadlessRun(demo_message);
    try {
      waitForTicks(ticks);
      return nm;
    } finally {
      finishHeadlessRun();
    }
  }

  /**
   * Boots FrEnd headless and loads the demo, returning the NodeManager.
   * The caller drives ticks via {@link #waitForOneTick()} for per-tick
   * observation. Call {@link #finishHeadlessRun()} when done.
   */
  public static NodeManager startHeadlessRun(final Object demo_message)
      throws Exception {
    GuiTestSupport.bootApp();
    final boolean old_headless = World.headless;
    World.headless = true;
    try {
      World.new_message_manager.add(demo_message);
      final NodeManager nm = waitForLoad();
      World.paused = false;
      return nm;
    } catch (Exception e) {
      World.headless = old_headless;
      throw e;
    }
  }

  /** Waits for exactly one physics tick to complete. */
  public static void waitForOneTick() throws Exception {
    final int start = RendererDelegator.generation;
    final long deadline = System.currentTimeMillis() + 10000;
    while (RendererDelegator.generation <= start) {
      if (System.currentTimeMillis() > deadline) {
        throw new RuntimeException("Timed out waiting for one tick");
      }
      Thread.sleep(10);
    }
  }

  /** Restores World state after a headless run. */
  public static void finishHeadlessRun() {
    World.paused = true;
    World.headless = false;
  }

  private static NodeManager waitForLoad() throws Exception {
    final long deadline = System.currentTimeMillis() + 60000;
    while (System.currentTimeMillis() < deadline) {
      Thread.sleep(25);
      final NodeManager nm = ContextManager.getNodeManager();
      if (nm != null && nm.element.size() > 0) {
        Thread.sleep(200);
        return nm;
      }
    }
    throw new RuntimeException("Demo did not load");
  }

  private static void waitForTicks(final int ticks) throws Exception {
    final int start = RendererDelegator.generation;
    final long deadline = System.currentTimeMillis() + 120000;
    while (RendererDelegator.generation - start < ticks) {
      if (System.currentTimeMillis() > deadline) {
        throw new RuntimeException("Timed out waiting for " + ticks + " ticks");
      }
      Thread.sleep(25);
    }
  }
}
