// This program has been placed into the public domain by its author.

package com.springie.gui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Panel;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for WrapLayout: the wrapping layout that reports its wrapped
 * height, so parents sizing from the preferred size give a wrapped
 * container the rows it needs instead of clipping them (the FlowLayout
 * wrap-and-clip bug class).
 */
class WrapLayoutTest {

  /** A component with a fixed preferred/minimum size; needs no peer. */
  static final class Fixed extends Canvas {
    static final long serialVersionUID = 1L;
    private final Dimension size;

    Fixed(int w, int h) {
      this.size = new Dimension(w, h);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(this.size);
    }

    @Override
    public Dimension getMinimumSize() {
      return new Dimension(this.size);
    }
  }

  private static Panel panelWith(int width, Component... children) {
    final Panel panel = new Panel();
    panel.setLayout(new WrapLayout());
    for (final Component c : children) {
      panel.add(c);
    }
    panel.setSize(width, 100);
    return panel;
  }

  @Test
  void singleRowPreferredHeightIsOneRow() {
    final Panel panel = panelWith(500, new Fixed(100, 20), new Fixed(100, 20),
        new Fixed(100, 20));
    final Dimension pref = panel.getPreferredSize();
    assertEquals(20, pref.height,
        "three 100px children fit in one 500px row: height is one row");
  }

  @Test
  void preferredHeightGrowsWhenWrapped() {
    final Panel panel = panelWith(250, new Fixed(100, 20), new Fixed(100, 20),
        new Fixed(100, 20));
    final Dimension pref = panel.getPreferredSize();
    // Two rows of 20px with the default 5px vertical gap.
    assertEquals(45, pref.height,
        "at 250px the third child wraps: preferred height must be two rows");
  }

  @Test
  void layoutContainerKeepsEveryChildInside() {
    final Panel panel = panelWith(250, new Fixed(100, 20), new Fixed(100, 20),
        new Fixed(100, 20));
    panel.setSize(250, 45);
    panel.doLayout();
    for (final Component c : panel.getComponents()) {
      assertTrue(c.getX() >= 0 && c.getY() >= 0
          && c.getX() + c.getWidth() <= panel.getWidth()
          && c.getY() + c.getHeight() <= panel.getHeight(),
          "child must sit fully inside the panel after wrapping, but was "
              + c.getBounds());
    }
  }

  @Test
  void nestedWrapContainerGrowsTallerWhenSqueezed() {
    final Panel inner = new Panel();
    inner.setLayout(new WrapLayout());
    inner.add(new Fixed(100, 20));
    inner.add(new Fixed(100, 20));
    // The inner bar's natural width is one row: 100 + 5 + 100 = 205.
    final Panel outer = panelWith(150, inner);
    final Dimension pref = outer.getPreferredSize();
    // Squeezed to 150px, the inner bar wraps to two rows: 20 + 5 + 20.
    assertEquals(45, pref.height,
        "a nested wrapping container squeezed below its natural width "
            + "must grow taller instead of clipping");
  }

  @Test
  void unboundedWidthReportsSingleRowSize() {
    final Panel panel = new Panel();
    panel.setLayout(new WrapLayout());
    panel.add(new Fixed(100, 20));
    panel.add(new Fixed(100, 20));
    final Dimension pref = panel.getPreferredSize();
    assertEquals(205, pref.width,
        "with no width yet, the preferred width is the single-row width");
    assertEquals(20, pref.height,
        "with no width yet, the preferred height is one row");
  }
}
