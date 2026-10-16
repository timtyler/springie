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
 * Parity check: the same demo run through the headless runner and through
 * the live UI must produce the same trajectory. Tim's rule (2026-09-17):
 * headless-judge numbers that don't reproduce in the UI are worthless.
 *
 * <p>Runs WheelbarrowDemo for 600 ticks via HeadlessRunner (pinned physics,
 * no UI) and via the live UI (FrEnd, menu message path, animation thread),
 * then compares final node positions. A large divergence means the two
 * paths are not equivalent -- the headless numbers can't be trusted.
 */
class HeadlessUiParityTest {
  private static final int TICKS = 600;

  @Test
  void wheelbarrowParity() throws Exception {
    // Headless path: pinned physics, direct tick loop.
    final double[] headless_pos = runHeadless();

    // UI path: real FrEnd, menu message, animation thread.
    final double[] ui_pos = runViaUi();

    // Compare: max position difference across all nodes.
    double max_diff = 0;
    for (int i = 0; i < headless_pos.length; i++) {
      max_diff = Math.max(max_diff, Math.abs(headless_pos[i] - ui_pos[i]));
    }

    // Diagnostics.
    System.out.println("Headless nodes: " + (headless_pos.length / 3) +
        ", UI nodes: " + (ui_pos.length / 3));
    System.out.println("Headless first node: " +
        headless_pos[0] + "," + headless_pos[1] + "," + headless_pos[2]);
    System.out.println("UI first node: " +
        ui_pos[0] + "," + ui_pos[1] + "," + ui_pos[2]);

    // Report the divergence; fail if it's large.
    // (Tolerance is generous -- we're checking for gross divergence,
    // not bit-identical results. UI timing can introduce small differences.)
    System.out.println("Parity max position difference: " + max_diff + "px");
    assertTrue(max_diff < 50,
        "Headless and UI paths diverged by " + max_diff + "px -- " +
        "headless numbers don't reproduce in the UI");
  }

  private static double[] runHeadless() {
    return HeadlessRunner.withModelLock(() -> {
      HeadlessRunner.resetWorldRandom();
      HeadlessRunner.pinPhysicsState();
      final NodeManager nm = HeadlessRunner.newJudgedRun();
      WheelbarrowDemo.buildAt(300);
      // Match the demo message: the UI path sets these after buildAt.
      World.continuously_centre_x = true;
      World.demo_model = true;
      HeadlessRunner.settle(nm, 60);
      for (int i = 0; i < TICKS; i++) {
        nm.nodeAndLinkUpdate();
      }
      return snapshotPositions(nm);
    });
  }

  private static double[] runViaUi() throws Exception {
    GuiTestSupport.bootApp();
    final boolean old_paused = World.paused;
    try {
      World.new_message_manager.add(new WheelbarrowDemoMessage());
      // Wait for the demo to load.
      NodeManager node_manager = null;
      final long loaded_by = System.currentTimeMillis() + 60000;
      while (System.currentTimeMillis() < loaded_by) {
        Thread.sleep(25);
        node_manager = ContextManager.getNodeManager();
        if (node_manager != null && node_manager.element.size() > 0) {
          // Wait for links to settle.
          Thread.sleep(200);
          break;
        }
      }
      if (node_manager == null) {
        fail("Wheelbarrow demo did not load via the UI path");
      }
      // Run via the animation thread, counting generations.
      World.paused = false;
      final int start_generation = RendererDelegator.generation;
      final long deadline = System.currentTimeMillis() + 120000;
      while (RendererDelegator.generation - start_generation < TICKS) {
        if (System.currentTimeMillis() > deadline) {
          fail("UI run timed out waiting for " + TICKS + " ticks");
        }
        Thread.sleep(25);
      }
      World.paused = true;
      // Snapshot under the model lock.
      final NodeManager nm = node_manager;
      return HeadlessRunner.withModelLock(() -> snapshotPositions(nm));
    } finally {
      World.paused = old_paused;
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
