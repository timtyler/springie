// This program has been placed into the public domain by its author.

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;

/**
 * A moved drag-box selection must not leave a trail in ray-traced mode
 * either. The drag rectangle is draw-only (painted after the blit); the
 * ray-traced frame's dirty rectangles must include the box's old and
 * new rectangles, or no re-traced tile repaints the old rectangle's
 * pixels and the red box persists.
 *
 * <p>The capture uses a Robot screen grab, not printAll: MainCanvas.paint
 * starts with repaintAll, which clears the screen first and would erase
 * the very trail this test looks for.
 */
class RaytracedDragBoxTrailTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  private static void setDrag(int x0, int y0, int x1, int y1)
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      FrEnd.perform_actions.drag_box_manager.drag_box_start =
          new Point(x0 << Coords.shift, y0 << Coords.shift);
      FrEnd.perform_actions.drag_box_manager.drag_box_end =
          new Point(x1 << Coords.shift, y1 << Coords.shift);
    });
    // Two paint cycles: the first starts the frame covering the damage
    // and draws the box, the second blits the re-traced damage tiles
    // (erasing the old rectangle) and draws the box again.
    repaintAndSettle();
    repaintAndSettle();
  }

  private static void repaintAndSettle() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.main_canvas.panel.repaint();
    });
    // Ensure the EDT has processed the repaint, so the ray-traced frame
    // has started (holdModelForFrame goes true while a frame is in
    // flight). Polling for completion beats the old fixed 1.5s sleep:
    // a small drag-box re-render typically finishes in a few hundred ms.
    SwingUtilities.invokeAndWait(() -> {
    });
    final ModularRendererRaytraced rt =
        (ModularRendererRaytraced) RendererDelegator.renderer;
    final long deadline = System.currentTimeMillis() + 1500;
    while (rt.holdModelForFrame()
        && System.currentTimeMillis() < deadline) {
      Thread.sleep(25);
    }
    // Let the EDT blit the finished frame to the screen.
    SwingUtilities.invokeAndWait(() -> {
    });
  }

  private static BufferedImage capture() throws Exception {
    final Rectangle[] area = new Rectangle[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.frame_main.toFront();
      final Point p = FrEnd.main_canvas.panel.getLocationOnScreen();
      area[0] = new Rectangle(p, FrEnd.main_canvas.panel.getSize());
    });
    return new Robot().createScreenCapture(area[0]);
  }

  private static int countSelected(BufferedImage img, int x0, int y0,
      int x1, int y1) {
    final int sel_rgb = RendererDelegator.colour_selected.getRGB();
    int count = 0;
    for (int y = y0; y < y1; y++) {
      for (int x = x0; x < x1; x++) {
        if (img.getRGB(x, y) == sel_rgb) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Polls until a screen capture shows the selection colour in the
   * region, or the deadline passes. A single capture is not enough: the
   * box is redrawn at the end of every paint but erased by the next
   * paint's blit, so it flickers.
   */
  private static void waitForSelected(int x0, int y0, int x1, int y1,
      String what) throws Exception {
    final long deadline = System.currentTimeMillis() + 8000;
    while (System.currentTimeMillis() < deadline) {
      if (countSelected(capture(), x0, y0, x1, y1) > 0) {
        return;
      }
      Thread.sleep(250);
    }
    assertTrue(false, what);
  }

  @Test
  void movedDragBoxLeavesNoTrail() throws Exception {
    final ModularRendererBase[] previous = new ModularRendererBase[1];
    SwingUtilities.invokeAndWait(() -> {
      previous[0] = RendererDelegator.renderer;
      RendererDelegator.renderer = new ModularRendererRaytraced();
      FrEnd.main_canvas.forceResize();
    });
    try {
      // First drag over an empty area (top-left, away from the model).
      setDrag(20, 20, 60, 60);

      // Move to a disjoint rectangle; the old one must be fully erased.
      setDrag(200, 200, 260, 260);
      final BufferedImage img = capture();

      assertEquals(0, countSelected(img, 15, 15, 65, 65),
          "the old drag-box rectangle must be erased, not trailed");

      // The new rectangle must be drawn. It flickers: every paint's
      // blit erases it before it is redrawn at the end of the paint, so
      // poll until a capture catches it.
      waitForSelected(195, 195, 265, 265,
          "the new drag-box rectangle must be drawn");

      // Release the drag the way DragBoxManager.terminate does: the
      // start point is gone but the cached end rectangle must still be
      // erased on the next frames.
      SwingUtilities.invokeAndWait(() -> {
        FrEnd.perform_actions.drag_box_manager.drag_box_start = null;
        RendererDelegator.repaint_some_objects = true;
      });
      repaintAndSettle();
      repaintAndSettle();
      final BufferedImage released = capture();
      assertEquals(0, countSelected(released, 195, 195, 265, 265),
          "the released drag-box rectangle must be erased");
    } finally {
      SwingUtilities.invokeAndWait(() -> {
        FrEnd.perform_actions.drag_box_manager.drag_box_start = null;
        FrEnd.perform_actions.drag_box_manager.drag_box_end = null;
        FrEnd.paused = false;
        RendererDelegator.renderer = previous[0];
      });
    }
  }
}
