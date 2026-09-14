// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;

/**
 * The "Render deepest objects first" option does not apply to the
 * ray-traced renderer (occlusion is resolved per ray by the BVH), so
 * its row is removed from the Renderer tab while ray-tracing is
 * active and restored for the rasterizer renderers. Like the
 * ray-traced-only rows, the row is added and removed rather than
 * shown/hidden, because the tab's GridLayout gives invisible
 * components space.
 */
class DeepestFirstRowVisibilityTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  /** Pin the Polygon renderer before each test; also restores the state. */
  @BeforeEach
  void pinPolygonRenderer() throws Exception {
    selectRenderer("Polygon");
  }

  /** The shared Renderer tab's "Display type" dropdown. */
  private static Choice sharedDropdown() throws Exception {
    final Choice[] found = new Choice[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel;
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

  private static Panel deepestFirstRow() {
    return FrEnd.panel_preferences_shared_misc.panel_redraw_deepest_first;
  }

  private static boolean rowShownOnTab() throws Exception {
    final boolean[] shown = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel;
      final Panel row = deepestFirstRow();
      shown[0] = false;
      for (int i = 0; i < tab.getComponentCount(); i++) {
        if (tab.getComponent(i) == row) {
          shown[0] = true;
        }
      }
    });
    return shown[0];
  }

  private static int rowIndexOnTab() throws Exception {
    final int[] index = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel;
      final Panel row = deepestFirstRow();
      index[0] = -1;
      for (int i = 0; i < tab.getComponentCount(); i++) {
        if (tab.getComponent(i) == row) {
          index[0] = i;
        }
      }
    });
    return index[0];
  }

  private static Checkbox deepestFirstCheckbox() {
    return FrEnd.panel_preferences_shared_misc.checkbox_redraw_deepest_first;
  }

  @Test
  void rowIsShownUnderThePolygonRenderer() throws Exception {
    assertTrue(rowShownOnTab(),
        "the deepest-first row must be on the Renderer tab for the polygon renderer");
    assertEquals(3, rowIndexOnTab(),
        "the deepest-first row must keep its usual slot, after Pixellation");
  }

  @Test
  void rowIsHiddenUnderTheRaytracedRenderer() throws Exception {
    selectRenderer("Ray-traced");
    assertFalse(rowShownOnTab(),
        "the deepest-first row must leave the Renderer tab while ray-tracing");
  }

  @Test
  void rowComesBackWhenSwitchingBackToThePolygonRenderer() throws Exception {
    selectRenderer("Ray-traced");
    assertFalse(rowShownOnTab());
    selectRenderer("Polygon");
    assertTrue(rowShownOnTab(),
        "the deepest-first row must return to the Renderer tab");
    assertEquals(3, rowIndexOnTab(),
        "the restored row must sit in its usual slot, after Pixellation");
  }

  @Test
  void repeatedSwitchesNeverDuplicateTheRow() throws Exception {
    selectRenderer("Ray-traced");
    selectRenderer("Polygon");
    selectRenderer("Polygon");
    final int[] count = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      final Panel tab = FrEnd.panel_preferences_shared_show.panel;
      final Panel row = deepestFirstRow();
      for (int i = 0; i < tab.getComponentCount(); i++) {
        if (tab.getComponent(i) == row) {
          count[0]++;
        }
      }
    });
    assertEquals(1, count[0], "the row must appear exactly once");
  }

  @Test
  void checkboxStillDrivesTheFlagWhileVisible() throws Exception {
    final boolean[] cleared = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      final Checkbox checkbox = deepestFirstCheckbox();
      // setState() alone fires no event, so drive the real listener
      // the way a user click would.
      checkbox.setState(false);
      final ItemEvent off = new ItemEvent(checkbox,
          ItemEvent.ITEM_STATE_CHANGED, checkbox.getLabel(),
          ItemEvent.DESELECTED);
      for (final ItemListener listener : checkbox.getItemListeners()) {
        listener.itemStateChanged(off);
      }
      cleared[0] = FrEnd.redraw_deepest_first;
      checkbox.setState(true);
      final ItemEvent on = new ItemEvent(checkbox,
          ItemEvent.ITEM_STATE_CHANGED, checkbox.getLabel(),
          ItemEvent.SELECTED);
      for (final ItemListener listener : checkbox.getItemListeners()) {
        listener.itemStateChanged(on);
      }
    });
    assertFalse(cleared[0], "unchecking must clear FrEnd.redraw_deepest_first");
    assertTrue(FrEnd.redraw_deepest_first,
        "re-checking must restore FrEnd.redraw_deepest_first");
  }
}
