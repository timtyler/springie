// This code has been placed into the public domain by its author

package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.gui.gestures.DragBoxManager;

/**
 * The drag-selection rectangle must be draw-only. It used to erase its
 * old position with the background colour before drawing the new one;
 * on the visible screen that blinked the rectangle off and on every
 * paint (the selection rectangle flickered). Every frame painted
 * during a drag is a full repaint, which already covers the previous
 * rectangle, so the erase was pure flicker.
 *
 * <p>Headless-safe: draws into a BufferedImage, never a Component.
 */
public class RendererDragBoxTest {
  private static final int BLACK = 0xFF000000;

  private static final int WHITE = 0xFFFFFFFF;

  private static final int RED = 0xFFFF0000;

  private int saved_bg_number;

  private Color saved_bg;

  private int saved_selected_number;

  private Color saved_selected;

  @BeforeEach
  public void setUp() {
    this.saved_bg_number = RendererDelegator.color_background_number;
    this.saved_bg = RendererDelegator.color_background;
    this.saved_selected_number = RendererDelegator.colour_selected_number;
    this.saved_selected = RendererDelegator.colour_selected;

    RendererDelegator.color_background_number = BLACK;
    RendererDelegator.color_background = new Color(BLACK);
    RendererDelegator.colour_selected_number = RED;
    RendererDelegator.colour_selected = new Color(RED);
  }

  @AfterEach
  public void tearDown() {
    RendererDelegator.color_background_number = this.saved_bg_number;
    RendererDelegator.color_background = this.saved_bg;
    RendererDelegator.colour_selected_number = this.saved_selected_number;
    RendererDelegator.colour_selected = this.saved_selected;
  }

  private static Point internal(int pixel) {
    return new Point(pixel << Coords.shift, pixel << Coords.shift);
  }

  @Test
  public void dragBoxNeverPaintsTheBackgroundColour() {
    final BufferedImage image =
        new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
    final Graphics g = image.getGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, 400, 400);

    final DragBoxManager manager = new DragBoxManager();
    final RendererDragBox drag_box = new RendererDragBox();

    // First paint of the drag: rectangle at (10,10)-(50,50).
    manager.drag_box_start = internal(10);
    manager.drag_box_end = internal(50);
    drag_box.draw(g, manager);

    // Second paint, mouse moved: rectangle now (10,10)-(100,100).
    // The old code erased the first rectangle with the background
    // colour here; the right (x=50) and bottom (y=50) edges lay inside
    // the new rectangle's interior, so the erase left black pixels.
    manager.drag_box_end = internal(100);
    drag_box.draw(g, manager);
    g.dispose();

    int black = 0;
    for (int y = 0; y < 400; y++) {
      for (int x = 0; x < 400; x++) {
        if (image.getRGB(x, y) == BLACK) {
          black++;
        }
      }
    }
    assertEquals(0, black,
        "the drag box must never paint the background colour");

    // And the rectangle itself must actually be there, in the
    // selection colour: the shared top edge at y=10.
    assertEquals(RED, image.getRGB(60, 10),
        "the current rectangle must be drawn in the selection colour");
  }
}
