// This program has been placed into the public domain by its author.

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.original.ModularRendererOld;

/**
 * A single click with the polygon (original) renderer must not leave a
 * drag-box rectangle behind on the screen.
 *
 * <p>A click on empty space starts a zero-area drag box on press; on
 * release the box is dropped without ever being erased (it is draw-only
 * since 5cea965). The modern tiled renderer repairs the damage via its
 * dirty bins and the ray tracer re-blits the whole frame, but the old
 * renderer paints straight onto the screen with no damage repair -- and
 * the release only asked for a partial repaint, which never clears. The
 * click's tiny red square stayed on the screen until something else
 * happened to repaint over it.
 *
 * <p>The fix makes every frame painted while the box is up a full
 * clear-and-redraw for the old renderer (see
 * RendererDelegator.renderDragBox).
 *
 * <p>The click is delivered as genuine AWT mouse events, exercising the
 * real press/release path (MainCanvas listeners, message pump,
 * DragBoxManager). The assertions read real screen pixels (via Robot):
 * a component print into a fresh image can never see stale screen
 * pixels, which is exactly what this bug leaves behind.
 */
public class PolygonRendererClickBoxTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  private static ModularRendererBase saved_renderer;
  private static boolean saved_paused;

  private static void useOldRenderer() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      saved_renderer = RendererDelegator.renderer;
      saved_paused = FrEnd.paused;
      RendererDelegator.renderer = new ModularRendererOld();
      FrEnd.main_canvas.forceResize();
      RendererDelegator.repaintAll();
    });
    // Let a full clear-and-redraw settle.
    Thread.sleep(800);
  }

  private static void restoreRenderer() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = saved_renderer;
      FrEnd.paused = saved_paused;
      FrEnd.perform_actions.drag_box_manager.drag_box_start = null;
      FrEnd.perform_actions.drag_box_manager.drag_box_end = null;
      FrEnd.main_canvas.forceResize();
      RendererDelegator.repaintAll();
    });
    Thread.sleep(500);
  }

  private static void dispatch(MouseEvent e) throws Exception {
    SwingUtilities.invokeAndWait(
        () -> FrEnd.main_canvas.panel.dispatchEvent(e));
  }

  private static void press(int x, int y) throws Exception {
    final Panel panel = FrEnd.main_canvas.panel;
    dispatch(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
        System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
        x, y, 1, false, MouseEvent.BUTTON1));
  }

  private static void release(int x, int y) throws Exception {
    final Panel panel = FrEnd.main_canvas.panel;
    dispatch(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,
        System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
        x, y, 1, false, MouseEvent.BUTTON1));
  }

  private interface Condition {
    boolean isTrue() throws Exception;
  }

  private static void waitFor(Condition condition, String what)
      throws Exception {
    final long deadline = System.currentTimeMillis() + 20000;
    while (System.currentTimeMillis() < deadline) {
      if (condition.isTrue()) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("timed out waiting for: " + what);
  }

  // Real screen pixels around the given panel coordinates.
  private static BufferedImage captureScreen(int px, int py) throws Exception {
    final Robot robot = new Robot();
    final Point[] loc = new Point[1];
    SwingUtilities.invokeAndWait(
        () -> loc[0] = FrEnd.main_canvas.panel.getLocationOnScreen());
    return robot.createScreenCapture(
        new Rectangle(loc[0].x + px - 12, loc[0].y + py - 12, 25, 25));
  }

  private static int countBoxPixels(BufferedImage img) {
    int count = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        final int rgb = img.getRGB(x, y);
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        // Selection red, tolerant of display colour depth.
        if (r > 200 && g < 80 && b < 80) {
          count++;
        }
      }
    }
    return count;
  }

  @Test
  void clickOnEmptySpaceLeavesNoBoxBehind() throws Exception {
    useOldRenderer();
    try {
      // Top-left corner, away from the boot model: the click hits
      // nothing, so the press starts a zero-area drag box.
      final int x = 30;
      final int y = 30;
      press(x, y);

      // The press message is processed on the animation thread; wait
      // until the drag box is up, proving the drag (not selection) path.
      waitFor(
          () -> FrEnd.perform_actions.drag_box_manager.drag_box_start != null,
          "press to start a drag box");
      // The box is drawn by the EDT while the animation thread repaints
      // the model; the two paint paths race, so the box flickers. Poll
      // until a frame catches it (a separate pre-existing race, not the
      // bug under test).
      waitFor(
          () -> countBoxPixels(captureScreen(x, y)) > 0,
          "the click's drag box to be drawn while the button is down");

      release(x, y);

      // The release drops the box; wait until the terminating paint has
      // run, then let the erasing repaint happen.
      waitFor(
          () -> FrEnd.perform_actions.drag_box_manager.drag_box_end == null,
          "release to drop the drag box");
      Thread.sleep(800);

      assertEquals(0, countBoxPixels(captureScreen(x, y)),
          "no drag-box rectangle may remain on screen after a click");
    } finally {
      restoreRenderer();
    }
  }
}
