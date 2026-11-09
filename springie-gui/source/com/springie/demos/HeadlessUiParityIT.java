// This program has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.GuiTestSupport;
import com.springie.world.World;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Parity check: the same demo run headless (renderer off) and through
 * the live UI must produce the same trajectory. Tim's rule (2026-09-17):
 * headless numbers that don't reproduce in the UI are worthless.
 *
 * <p>Both paths boot the real FrEnd and load the demo via the real
 * message path; the only difference is World.headless (the renderer
 * skips pixel drawing, physics runs via the same AWT path). The runs
 * are driven deterministically -- the model is loaded while paused and
 * then advanced by exactly TICKS dynamics steps via World.stepping --
 * because the old free-running protocol compared runs whose tick
 * counts differed by however long each load happened to take, which
 * swamped the path difference this test exists to measure.
 */
class HeadlessUiParityIT {
  private static final int TICKS = 600;

  @Test
  void wheelbarrowParity() throws Exception {
    final double[] headless_pos = runPath(true);
    final double[] ui_pos = runPath(false);

    double max_diff = 0;
    for (int i = 0; i < headless_pos.length; i++) {
      max_diff = Math.max(max_diff, Math.abs(headless_pos[i] - ui_pos[i]));
    }

    System.out.println("Parity max position difference: " + max_diff + "px");
    assertTrue(max_diff < 50,
        "Headless and UI paths diverged by " + max_diff + "px");
  }

  /** One run: real FrEnd, real message path, exactly TICKS steps. */
  private static double[] runPath(final boolean headless) throws Exception {
    GuiTestSupport.bootApp();
    final boolean old_paused = World.paused;
    final boolean old_headless = World.headless;
    try {
      World.headless = headless;
      // Load frozen: no dynamics ticks may slip in between the build
      // and the stepped run (a tick of difference shifts the muscle
      // oscillator phase for the whole run).
      World.paused = true;
      World.new_message_manager.add(new WheelbarrowDemoMessage());
      final NodeManager nm = waitForLoad(16);
      final CountDownLatch done = new CountDownLatch(1);
      World.stepping = TICKS;
      World.stepping_finished = done::countDown;
      World.paused = false;
      if (!done.await(120, TimeUnit.SECONDS)) {
        fail("Timed out stepping " + TICKS + " ticks");
      }
      return HeadlessRunner.withModelLock(() -> snapshotPositions(nm));
    } finally {
      World.stepping_finished = null;
      World.paused = old_paused;
      World.headless = old_headless;
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
