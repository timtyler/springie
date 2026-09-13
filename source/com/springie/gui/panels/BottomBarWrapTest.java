// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.gui.components.ButtonBar;

/**
 * The bottom control bar must degrade gracefully as the main window
 * narrows: it wraps onto more rows and every control stays reachable.
 *
 * <p>Regression test for the FlowLayout wrap-and-clip bug class: the old
 * single FlowLayout reported a one-row preferred height no matter how it
 * wrapped, so at ~520px the preset dropdowns and Restart landed on a
 * clipped second row while still reporting isShowing() == true. Clipping
 * is not hiding, so these tests walk the ancestors and assert each child
 * rectangle sits fully inside every one of them.
 */
class BottomBarWrapTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void disposeFrames() throws Exception {
    // Leave the presets card showing, as the app started.
    SwingUtilities.invokeAndWait(
        () -> FrEnd.panel_fundamental.showPresetsCard(true));
    GuiTestSupport.disposeFrames();
  }

  @Test
  void bottomBarKeepsEveryControlVisibleAt520px() throws Exception {
    final int one_row_height = resizeAndSettle(900);
    final int wrapped_height = resizeAndSettle(520);
    assertTrue(wrapped_height > one_row_height,
        "at 520px the bar must grow taller than its single-row height "
            + one_row_height + " (was " + wrapped_height + ")");
    assertAllChildrenFitInsideAncestors("520px");
  }

  @Test
  void bottomBarKeepsEveryControlVisibleAt360px() throws Exception {
    resizeAndSettle(360);
    assertAllChildrenFitInsideAncestors("360px");
  }

  @Test
  void bottomBarIsSingleRowWhenWide() throws Exception {
    resizeAndSettle(900);
    assertAllChildrenFitInsideAncestors("900px");
  }

  @Test
  void fileCardAlsoSurvivesNarrowWidth() throws Exception {
    resizeAndSettle(520);
    SwingUtilities.invokeAndWait(
        () -> FrEnd.panel_fundamental.showPresetsCard(false));
    settleBarHeight();
    assertAllChildrenFitInsideAncestors("520px file card");
    SwingUtilities.invokeAndWait(
        () -> FrEnd.panel_fundamental.showPresetsCard(true));
    settleBarHeight();
  }

  /**
   * Resizes the main frame and waits for the bottom bar's height to
   * settle (the wrap layout converges it with one async revalidate after
   * a row-count change). Returns the settled bar height.
   */
  private static int resizeAndSettle(int width) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      FrEnd.frame_main.setSize(width, 700);
      FrEnd.frame_main.validate();
    });
    return settleBarHeight();
  }

  private static int settleBarHeight() throws Exception {
    final long deadline = System.currentTimeMillis() + 10000;
    int stable = 0;
    int last = -1;
    int height = -1;
    while (System.currentTimeMillis() < deadline) {
      // Flush the event queue: any pending convergence revalidate runs.
      SwingUtilities.invokeAndWait(() -> {
      });
      final int[] h = new int[1];
      SwingUtilities.invokeAndWait(
          () -> h[0] = FrEnd.panel_with_controls_at_bottom.getHeight());
      height = h[0];
      if (height == last) {
        if (++stable >= 3) {
          return height;
        }
      } else {
        stable = 0;
        last = height;
      }
      Thread.sleep(100);
    }
    return height;
  }

  /**
   * Every direct child of the bottom-bar panel -- the mouse-action
   * ButtonBar, Controls, Pause, Step, Delete, Select-all-of-class, the
   * zoom buttons, the floppy toggle, the presets/file card and Restart --
   * must sit fully inside every ancestor. A clipped child still reports
   * isShowing() == true, so containment is the check that matters.
   */
  private static void assertAllChildrenFitInsideAncestors(String where)
      throws Exception {
    final Component[][] children = new Component[1][];
    final String[] names = new String[1];
    SwingUtilities.invokeAndWait(() -> {
      children[0] = FrEnd.panel_fundamental.panel.getComponents();
      names[0] = describe(children[0]);
    });
    assertEquals(11, children[0].length,
        "the bottom bar must still hold its 11 controls at " + where
            + " (saw: " + names[0] + ")");
    assertTrue(containsButtonBar(children[0]),
        "the mouse-action ButtonBar must be on the bottom bar at " + where);
    assertTrue(containsCardPanel(children[0]),
        "the presets/file card must be on the bottom bar at " + where);
    for (final Component child : children[0]) {
      assertFitsInsideAncestors(child,
          child.getClass().getSimpleName() + " at " + where);
    }
  }

  private static boolean containsButtonBar(Component[] children) {
    for (final Component c : children) {
      if (c instanceof ButtonBar) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsCardPanel(Component[] children) {
    for (final Component c : children) {
      if (c instanceof Container
          && ((Container) c).getLayout() instanceof CardLayout) {
        return true;
      }
    }
    return false;
  }

  private static String describe(Component[] children) {
    final StringBuilder sb = new StringBuilder();
    for (final Component c : children) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(c.getClass().getSimpleName());
    }
    return sb.toString();
  }

  private static void assertFitsInsideAncestors(Component child, String name)
      throws Exception {
    final boolean[] clipped = new boolean[1];
    final String[] details = new String[1];
    SwingUtilities.invokeAndWait(() -> {
      Rectangle rect = child.getBounds();
      for (Container parent = child.getParent(); parent != null;
          parent = parent.getParent()) {
        if (rect.x < 0 || rect.y < 0
            || rect.x + rect.width > parent.getWidth()
            || rect.y + rect.height > parent.getHeight()) {
          clipped[0] = true;
          details[0] = "clipped by "
              + parent.getClass().getSimpleName() + " "
              + parent.getWidth() + "x" + parent.getHeight()
              + ", child at " + rect;
          break;
        }
        rect = new Rectangle(parent.getX() + rect.x,
            parent.getY() + rect.y, rect.width, rect.height);
      }
    });
    assertTrue(!clipped[0],
        name + " must fit fully inside every ancestor"
            + (details[0] == null ? "" : ": " + details[0]));
  }
}
