// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Choice;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;

/**
 * The rows that do not apply to the ray-traced renderer ("Node
 * polyhedron", "Cable divisions", "Strut divisions", "Strut/cable
 * sides", "Face lines") configure rasterizer-only concepts. They are
 * removed from the Renderer tab's Main sub-tab while ray-tracing is
 * active and restored to their usual slot for the rasterizers -- added
 * and removed rather than shown/hidden, because the tab's GridLayout
 * gives invisible components space.
 */
class RaytracedHiddenRowsVisibilityTest {

  private static final String[] LABELS = {
      "Node polyhedron:", "Cable divisions:", "Strut divisions:",
      "Strut/cable sides:", "Face lines:" };

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  /** Pin the Polygon renderer before each test. */
  @BeforeEach
  void pinPolygonRenderer() throws Exception {
    selectRenderer("Polygon");
  }

  /**
   * Leave the Polygon renderer selected after each test. Without this,
   * whichever test runs last leaves its renderer behind: the ray-traced
   * renderer paints asynchronously, which breaks later tests that capture
   * synchronous paints (it leaked all the way to DragBoxTrailTest).
   */
  @AfterEach
  void restorePolygonRenderer() throws Exception {
    selectRenderer("Polygon");
  }

  /** The shared Renderer tab's "Display type" dropdown. */
  private static Choice sharedDropdown() throws Exception {
    final Choice[] found = new Choice[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
      for (int i = 0; i < tab.getComponentCount() && found[0] == null; i++) {
        found[0] = findChoice(tab.getComponent(i));
      }
    });
    return found[0];
  }

  private static Choice findChoice(Component component) {
    if (component instanceof Choice) {
      return (Choice) component;
    }
    if (component instanceof Panel) {
      final Panel panel = (Panel) component;
      for (int i = 0; i < panel.getComponentCount(); i++) {
        final Choice choice = findChoice(panel.getComponent(i));
        if (choice != null) {
          return choice;
        }
      }
    }
    return null;
  }

  /**
   * Drives the real item listener the way a user picking from the
   * dropdown would (Choice.select() alone fires no event).
   */
  private static void selectRenderer(String namePart) throws Exception {
    final Choice dropdown = sharedDropdown();
    SwingUtilities.invokeAndWait(() -> {
      String target = null;
      for (int i = 0; i < dropdown.getItemCount(); i++) {
        if (dropdown.getItem(i).contains(namePart)) {
          target = dropdown.getItem(i);
        }
      }
      assertNotNull(target, "no renderer option containing '" + namePart + "'");
      final ItemEvent event = new ItemEvent(dropdown,
          ItemEvent.ITEM_STATE_CHANGED, target, ItemEvent.SELECTED);
      for (final ItemListener listener : dropdown.getItemListeners()) {
        listener.itemStateChanged(event);
      }
    });
  }

  /** Finds the tab row holding the given label, or null. */
  private static Panel findRowByLabel(final String label) throws Exception {
    final Panel[] found = new Panel[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
      for (int i = 0; i < tab.getComponentCount() && found[0] == null; i++) {
        final Component component = tab.getComponent(i);
        if (component instanceof Panel && hasLabel((Panel) component, label)) {
          found[0] = (Panel) component;
        }
      }
    });
    return found[0];
  }

  private static boolean hasLabel(Panel panel, String label) {
    for (int i = 0; i < panel.getComponentCount(); i++) {
      final Component component = panel.getComponent(i);
      if (component instanceof Label
          && label.equals(((Label) component).getText())) {
        return true;
      }
    }
    return false;
  }

  private static int indexOfRow(Panel row) throws Exception {
    final int[] index = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
      index[0] = -1;
      for (int i = 0; i < tab.getComponentCount(); i++) {
        if (tab.getComponent(i) == row) {
          index[0] = i;
        }
      }
    });
    return index[0];
  }

  @Test
  void geometryRowsAreShownUnderThePolygonRenderer() throws Exception {
    for (final String label : LABELS) {
      assertNotNull(findRowByLabel(label),
          "the '" + label + "' row must be on the Main sub-tab");
    }
  }

  @Test
  void geometryRowsAreHiddenUnderTheRaytracedRenderer() throws Exception {
    selectRenderer("Ray-traced");
    for (final String label : LABELS) {
      assertNull(findRowByLabel(label),
          "the '" + label + "' row must leave the Renderer tab");
    }
  }

  @Test
  void geometryRowsComeBackInOrderWhenSwitchingBack() throws Exception {
    selectRenderer("Ray-traced");
    selectRenderer("Polygon");
    final Panel explosions_row = (Panel) FrEnd.panel_preferences_shared_misc.checkbox_explosions
        .getParent();
    final int explosions_index = indexOfRow(explosions_row);
    assertTrue(explosions_index >= LABELS.length,
        "the explosions row anchors the restore slot");
    for (int i = 0; i < LABELS.length; i++) {
      final Panel row = findRowByLabel(LABELS[i]);
      assertNotNull(row, "the '" + LABELS[i] + "' row must return");
      assertEquals(explosions_index - LABELS.length + i, indexOfRow(row),
          "the '" + LABELS[i] + "' row must keep its usual slot");
    }
  }

  @Test
  void repeatedSwitchesNeverDuplicateTheRows() throws Exception {
    selectRenderer("Ray-traced");
    selectRenderer("Polygon");
    selectRenderer("Polygon");
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
      for (final String label : LABELS) {
        int count = 0;
        for (int i = 0; i < tab.getComponentCount(); i++) {
          final Component component = tab.getComponent(i);
          if (component instanceof Panel
              && hasLabel((Panel) component, label)) {
            count++;
          }
        }
        assertEquals(1, count, "exactly one '" + label + "' row, got " + count);
      }
    });
  }
}
