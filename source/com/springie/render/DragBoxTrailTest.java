// This program has been placed into the public domain by its author.

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Panel;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;

/**
 * A moved drag-box selection must not leave a trail. The drag rectangle
 * is draw-only; the tiled renderer skips empty bins, so the bins under
 * the old and new rectangles are explicitly repainted during a drag.
 * Without that, the old rectangle persisted over empty bins.
 */
public class DragBoxTrailTest {

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
      RendererDelegator.repaintAll();
    });
    // Let the repaint complete.
    Thread.sleep(800);
  }

  private static BufferedImage capture() throws Exception {
    final BufferedImage[] img = new BufferedImage[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel canvas = FrEnd.main_canvas.panel;
      img[0] = new BufferedImage(canvas.getWidth(), canvas.getHeight(),
          BufferedImage.TYPE_INT_RGB);
      canvas.printAll(img[0].getGraphics());
    });
    return img[0];
  }

  @Test
  void movedDragBoxLeavesNoTrail() throws Exception {
    // First drag over an empty area (top-left, away from the model).
    setDrag(20, 20, 60, 60);
    capture();

    // Move to a disjoint rectangle; the old one must be fully erased.
    setDrag(200, 200, 260, 260);
    final BufferedImage img = capture();

    final int sel_rgb = RendererDelegator.colour_selected.getRGB();
    int trail = 0;
    for (int y = 15; y < 65; y++) {
      for (int x = 15; x < 65; x++) {
        if (img.getRGB(x, y) == sel_rgb) {
          trail++;
        }
      }
    }
    assertEquals(0, trail,
        "the old drag-box rectangle must be erased, not trailed");

    // The new rectangle must be drawn.
    boolean drawn = false;
    for (int y = 195; y < 265 && !drawn; y++) {
      for (int x = 195; x < 265 && !drawn; x++) {
        if (img.getRGB(x, y) == sel_rgb) {
          drawn = true;
        }
      }
    }
    assertEquals(true, drawn, "the new drag-box rectangle must be drawn");

    // Clean up: end the drag.
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.perform_actions.drag_box_manager.drag_box_start = null;
      FrEnd.perform_actions.drag_box_manager.drag_box_end = null;
      FrEnd.paused = false;
    });
  }
}
