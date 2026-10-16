// This program has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;
import com.springie.world.World;

import org.junit.jupiter.api.Test;

/**
 * Parity check: the same demo run headless (skip rendering) and through
 * the live UI must produce the same trajectory. Tim's rule (2026-09-17):
 * headless numbers that don't reproduce in the UI are worthless.
 *
 * <p>Both paths boot the real FrEnd and load the demo via the real
 * message path. The only difference: headless sets World.headless, so
 * the animation loop skips repaint() and runs physics directly.
 */
class HeadlessUiParityTest {
  private static final int TICKS = 600;

  @Test
  void wheelbarrowParity() throws Exception {
    final double[] headless_pos = runHeadless();
    final double[] ui_pos = runViaUi();

    double max_diff = 0;
    for (int i = 0; i < headless_pos.length; i++) {
      max_diff = Math.max(max_diff, Math.abs(headless_pos[i] - ui_pos[i]));
    }

    System.out.println("Parity max position difference: " + max_diff + "px");
    assertTrue(max_diff < 50,
        "Headless and UI paths diverged by " + max_diff + "px");
  }

  /** Headless: real FrEnd, real message path, skip rendering. */
  private static double[] runHeadless() throws Exception {
    GuiTestSupport.bootApp();
    final boolean old_paused = World.paused;
    final boolean old_headless = World.headless;
    try {
      World.headless = true;
      World.new_message_manager.add(new WheelbarrowDemoMessage());
      final NodeManager nm = waitForLoad(16);
      World.paused = false;
      waitForTicks(TICKS);
      World.paused = true;
      final NodeManager node_manager = nm;
      return HeadlessRunner.withModelLock(() -> snapshotPositions(node_manager));
    } finally {
      World.paused = old_paused;
      World.headless = old_headless;
    }
  }

  /** UI: real FrEnd, real message path, with rendering. */
  private static double[] runViaUi() throws Exception {
    GuiTestSupport.bootApp();
    final boolean old_paused = World.paused;
    try {
      World.new_message_manager.add(new WheelbarrowDemoMessage());
      final NodeManager nm = waitForLoad(16);
      World.paused = false;
      waitForTicks(TICKS);
      World.paused = true;
      final NodeManager node_manager = nm;
      return HeadlessRunner.withModelLock(() -> snapshotPositions(node_manager));
    } finally {
      World.paused = old_paused;
    }
  }

  private static NodeManager waitForLoad(int expected_nodes) throws Exception {
    final long loaded_by = System.currentTimeMillis() + 60000;
    while (System.currentTimeMillis() < loaded_by) {
      Thread.sleep(25);
      final NodeManager nm = ContextManager.getNodeManager();
      if (nm != null && nm.element.size() == expected_nodes) {
        Thread.sleep(200);
        return nm;
      }
    }
    fail("Demo did not load");
    return null;
  }

  private static void waitForTicks(int ticks) throws Exception {
    final int start = RendererDelegator.generation;
    final long deadline = System.currentTimeMillis() + 120000;
    while (RendererDelegator.generation - start < ticks) {
      if (System.currentTimeMillis() > deadline) {
        fail("Timed out waiting for " + ticks + " ticks");
      }
      Thread.sleep(25);
    }
  }

  private static double[] snapshotPositions(final NodeManager nm) {
    final int n = nm.element.size();
    final double[] pos = new double[n * 3];
    final double scale = 1.0 / (1 << com.springie.geometry.Coords.shift);
    for (int i = 0; i < n; i++) {
      final Node node = (Node) nm.element.get(i);
      pos[i * 3] = node.pos.x * scale;
      pos[i * 3 + 1] = node.pos.y * scale;
      pos[i * 3 + 2] = node.pos.z * scale;
    }
    return pos;
  }
}
