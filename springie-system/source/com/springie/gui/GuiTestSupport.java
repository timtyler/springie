// This program has been placed into the public domain by its author.

package com.springie.gui;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.world.World;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
   * Schedules a repaint of the main canvas and waits until the EDT has
   * processed it. Replaces blind Thread.sleep() waits for paints to land:
   * the second invokeAndWait queues behind the paint event, so when it
   * returns the paint is done.
   */
  public static void repaintAndWait() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> FrEnd.main_canvas.panel.repaint());
    SwingUtilities.invokeAndWait(() -> {
    });
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
   * after FrEnd.main returns. Wait until the world has gone quiet. The
   * boot enqueues a single restart message on the animation thread's
   * message pump and that message loads the model and its universe
   * settings synchronously (ModelManager.replaceCurrentModel does no
   * follow-up enqueueing), so once the queue has drained, the model is
   * up and nothing has changed for a short quiet spell, the boot has
   * settled. Polling the drain beats the old fixed 500ms quiet period,
   * which was almost entirely dead time (28 GUI test classes boot the
   * app).
   */
  public static void waitForBootModelLoadToSettle() throws Exception {
    int last_gravity = Integer.MIN_VALUE;
    int last_nodes = -1;
    long last_change = System.currentTimeMillis();
    final long deadline = last_change + 60000;
    while (System.currentTimeMillis() < deadline) {
      final int[] state = new int[3];
      SwingUtilities.invokeAndWait(() -> {
        state[0] = World.gravity_strength;
        state[1] = ContextManager.getNodeManager().element.size();
        state[2] = World.new_message_manager.size();
      });
      if (state[0] != last_gravity || state[1] != last_nodes) {
        last_gravity = state[0];
        last_nodes = state[1];
        last_change = System.currentTimeMillis();
      }
      if (state[1] > 0 && state[2] == 0
          && System.currentTimeMillis() - last_change > 150) {
        return;
      }
      Thread.sleep(25);
    }
  }

  // ----------------------------------------------------------------
  // Clean-desktop guard for the screen-capture tests.
  //
  // A handful of tests assert on real screen pixels read through
  // Robot. On a desktop that is being used -- another window in
  // front, the pointer being moved, the display asleep -- those
  // captures show the desktop, not the app, and the tests fail for
  // reasons that have nothing to do with the code. These tests now
  // prove the desktop is usable before they start, and re-check it
  // when a wait times out: if the proof fails, the test aborts
  // (reported as skipped, with the reason) instead of failing. On a
  // clean desktop -- an idle machine, the nightly Xvfb run -- the
  // proof passes and the tests run at full strength, so a real
  // regression in what the app draws still fails as before.
  // ----------------------------------------------------------------

  /**
   * Entry guard for a screen-capture test class, called once after
   * bootApp(): proves that Robot captures show what is put on the
   * screen, brings the main window to the front and proves it became
   * the active window, and proves the pointer is not being moved by
   * anyone. Aborts the test class (skip) when any proof fails.
   */
  public static void assumeCleanDesktop() throws Exception {
    assumeScreenCaptureTruthful();
    toFrontAndFocus();
    assumeTrue(appWindowActive(),
        "screen-capture test skipped: the app window never became the"
            + " active window, so another window holds the foreground"
            + " and screen captures cannot be trusted");
    assumePointerStill();
  }

  /**
   * Re-check for a wait that has just timed out inside a
   * screen-capture test: if the desktop has stopped being clean
   * since the entry guard ran (the app window lost the foreground,
   * or the pointer is being moved), abort the test as skipped
   * instead of failing it. Returns silently when the desktop still
   * looks clean, leaving the caller to fail as usual -- a timeout in
   * front of an attentive, idle desktop is a real failure.
   */
  public static void abortIfDesktopUnclean(final String what)
      throws Exception {
    if (!appWindowActive()) {
      assumeTrue(false,
          "skipped: " + what + " -- the app window is no longer the"
              + " active window, so the desktop changed under the test");
    }
    if (pointerIsBeingMoved()) {
      assumeTrue(false,
          "skipped: " + what + " -- the pointer is being moved, so"
              + " the desktop is in use");
    }
  }

  /**
   * Whether every coarsely sampled pixel of the capture is pure
   * black. A capture of a live app window is never uniformly black
   * (even the dusk scene has a gradient sky); a locked or sleeping
   * display, or a capture of nothing, is.
   */
  public static boolean captureIsBlank(final BufferedImage img) {
    for (int y = 0; y < img.getHeight(); y += 16) {
      for (int x = 0; x < img.getWidth(); x += 16) {
        if (img.getRGB(x, y) != 0xFF000000) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Puts a small magenta window on the screen and proves a Robot
   * capture sees it. If a window the test itself just showed does
   * not appear in captures, captures cannot be trusted at all --
   * the display is asleep, locked, or not the one being read.
   */
  private static void assumeScreenCaptureTruthful() throws Exception {
    final Window[] sentinel = new Window[1];
    SwingUtilities.invokeAndWait(() -> {
      final Window window = new Window(FrEnd.frame_main);
      window.setBackground(new Color(0xFF00FF));
      window.setSize(24, 24);
      window.setLocation(4, 4);
      window.setAlwaysOnTop(true);
      window.setVisible(true);
      sentinel[0] = window;
    });
    try {
      final Robot robot = new Robot();
      final long deadline = System.currentTimeMillis() + 3000;
      boolean truthful = false;
      while (System.currentTimeMillis() < deadline && !truthful) {
        final BufferedImage img = robot.createScreenCapture(
            new Rectangle(4, 4, 24, 24));
        final int rgb = img.getRGB(12, 12);
        truthful = ((rgb >> 16) & 0xFF) > 245
            && ((rgb >> 8) & 0xFF) < 10
            && (rgb & 0xFF) > 245;
        if (!truthful) {
          Thread.sleep(50);
        }
      }
      assumeTrue(truthful,
          "screen-capture test skipped: a window put on screen did"
              + " not appear in Robot captures -- the display is"
              + " asleep, locked, or not the one being captured");
    } finally {
      SwingUtilities.invokeAndWait(() -> sentinel[0].dispose());
    }
  }

  /** Brings the main window forward and waits for it to take focus. */
  private static void toFrontAndFocus() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_main.toFront();
      FrEnd.frame_main.requestFocus();
    });
    final long deadline = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < deadline) {
      if (appWindowActive()) {
        return;
      }
      Thread.sleep(50);
    }
  }

  /** Whether the main window is the keyboard focus owner. */
  private static boolean appWindowActive() throws Exception {
    final boolean[] active = new boolean[1];
    SwingUtilities.invokeAndWait(() -> active[0] =
        FrEnd.frame_main.isFocused()
            || KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getActiveWindow() == FrEnd.frame_main);
    return active[0];
  }

  /** Proves nobody is moving the pointer right now. */
  private static void assumePointerStill() throws Exception {
    assumeTrue(!pointerIsBeingMoved(),
        "screen-capture test skipped: the pointer moved during the"
            + " probe -- someone is using the mouse");
  }

  /**
   * Whether the pointer moves at all during a one-second watch.
   * Sampling a whole second matters: two samples a fixed interval
   * apart can alias against a moving pointer's rhythm and both land
   * on the same spot, reporting a busy desktop as still (a first cut
   * of this probe did exactly that against a pointer jiggled every
   * 100ms). Any excursion beyond a couple of pixels in the window
   * counts as movement.
   */
  private static boolean pointerIsBeingMoved() throws Exception {
    final Point first = MouseInfo.getPointerInfo().getLocation();
    int min_x = first.x;
    int max_x = first.x;
    int min_y = first.y;
    int max_y = first.y;
    final long deadline = System.currentTimeMillis() + 1000;
    while (System.currentTimeMillis() < deadline) {
      final Point p = MouseInfo.getPointerInfo().getLocation();
      min_x = Math.min(min_x, p.x);
      max_x = Math.max(max_x, p.x);
      min_y = Math.min(min_y, p.y);
      max_y = Math.max(max_y, p.y);
      Thread.sleep(25);
    }
    return max_x - min_x > 2 || max_y - min_y > 2;
  }
}
