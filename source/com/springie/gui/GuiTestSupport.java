// This program has been placed into the public domain by its author.

package com.springie.gui;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.world.World;

/**
 * Shared scaffolding for the GUI tests: booting the application, waiting
 * for the boot-time model load to settle, and tearing down the frames.
 *
 * (Named *Support rather than *Test so Surefire doesn't try to run it; it
 * is listed in the pom's compiler excludes/testIncludes alongside the
 * other test-scope helpers.)
 */
public final class GuiTestSupport {

  private GuiTestSupport() {
  }

  /**
   * Boots the application on the EDT and waits for the boot-time model
   * load to settle. GUI tests need a display.
   */
  public static void bootApp() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    SwingUtilities.invokeAndWait(() -> FrEnd.main(new String[0]));
    waitForBootModelLoadToSettle();
  }

  /**
   * Disposes every AWT frame the application created.
   */
  public static void disposeFrames() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      for (final Frame frame : Frame.getFrames()) {
        frame.dispose();
      }
    });
  }

  /**
   * The boot-time model load applies its universe settings asynchronously,
   * seconds after FrEnd.main returns. Wait until the world has gone quiet.
   */
  public static void waitForBootModelLoadToSettle() throws Exception {
    int last_gravity = Integer.MIN_VALUE;
    int last_nodes = -1;
    long last_change = System.currentTimeMillis();
    final long deadline = last_change + 60000;
    while (System.currentTimeMillis() < deadline) {
      final int[] state = new int[2];
      SwingUtilities.invokeAndWait(() -> {
        state[0] = World.gravity_strength;
        state[1] = ContextManager.getNodeManager().element.size();
      });
      if (state[0] != last_gravity || state[1] != last_nodes) {
        last_gravity = state[0];
        last_nodes = state[1];
        last_change = System.currentTimeMillis();
      }
      if (state[1] > 0 && System.currentTimeMillis() - last_change > 2000) {
        return;
      }
      Thread.sleep(250);
    }
  }
}
