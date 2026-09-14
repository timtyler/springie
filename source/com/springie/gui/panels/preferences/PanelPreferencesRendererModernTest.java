// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Choice;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Container;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.gui.components.TabbedPanel;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.modern.SimpleCube;
import com.springie.render.modules.modern.SimpleDodecahedron;
import com.springie.render.modules.modern.SimpleHexagon;
import com.springie.render.modules.modern.SimpleIcosahedron;
import com.springie.render.modules.modern.SimpleOctahedron;
import com.springie.render.modules.modern.SimpleSquare;

/**
 * The "Node polyhedron" dropdown on the modern renderer tab must offer
 * all six shapes, and picking one must install the matching renderer shape.
 * The synthetic ItemEvent stands in for the one the native peer delivers;
 * the listener only flips a repaint flag besides swapping the shape.
 */
public class PanelPreferencesRendererModernTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  private static Choice polyhedronDropdown() {
    final Choice choice = findChoice(
        FrEnd.panel_preferences_shared_show.panel);
    assertNotNull(choice, "expected the Node polyhedron dropdown on the Renderer tab");
    return choice;
  }

  private static Choice findChoice(Container container) {
    for (final Component c : container.getComponents()) {
      if (c instanceof Choice) {
        final Choice choice = (Choice) c;
        for (int i = 0; i < choice.getItemCount(); i++) {
          if ("Dodecahedron".equals(choice.getItem(i))) {
            return choice;
          }
        }
      }
      if (c instanceof Container) {
        final Choice found = findChoice((Container) c);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void pickPolyhedron(Choice choice, String item) {
    // AWT's programmatic select() fires no event, so deliver the ItemEvent
    // the native peer would have delivered straight to the listeners.
    final ItemEvent event = new ItemEvent(choice,
        ItemEvent.ITEM_STATE_CHANGED, item, ItemEvent.SELECTED);
    for (final ItemListener listener : choice.getItemListeners()) {
      listener.itemStateChanged(event);
    }
  }

  @Test
  void polyhedronDropdownOffersAllSixShapes() {
    final Choice choice = polyhedronDropdown();
    final String[] expected = {"Dodecahedron", "Octahedron", "Cube",
        "Icosahedron", "Square", "Hexagon"};
    assertEquals(expected.length, choice.getItemCount(),
        "the Node polyhedron dropdown must offer six shapes");
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], choice.getItem(i),
          "dropdown entry " + i + " must be " + expected[i]);
    }
  }

  @Test
  void eachPolyhedronOptionInstallsItsRendererShape() {    final Choice choice = polyhedronDropdown();
    final String[] items = {"Dodecahedron", "Octahedron", "Cube",
        "Icosahedron", "Square", "Hexagon"};
    final Class<?>[] shapes = {SimpleDodecahedron.class, SimpleOctahedron.class,
        SimpleCube.class, SimpleIcosahedron.class, SimpleSquare.class,
        SimpleHexagon.class};
    for (int i = 0; i < items.length; i++) {
      pickPolyhedron(choice, items[i]);
      assertTrue(shapes[i].isInstance(ModularRendererNew.sphere_object),
          "picking " + items[i] + " must install " + shapes[i].getSimpleName());
    }
    // Leave the default shape installed.
    pickPolyhedron(choice, "Dodecahedron");
  }

  /**
   * The old Options tab is flattened into the Renderer tab: the Bins
   * and Misc rows move under the shared rows there, so there is just
   * the one layout. "Render deepest objects first" and "Show labels
   * on:" keep their slots at the top, after Pixellation.
   */
  @Test
  void optionsTabIsFlattenedIntoTheRendererTab() {
    final Panel shared_misc_panel = FrEnd.panel_preferences_shared_misc.panel;
    assertEquals(0, shared_misc_panel.getComponentCount(),
        "the old Misc tab panel must be empty after its rows move");

    // No Options tab anymore: the top-level bar is Renderer |
    // Filtering | Colours.
    final TabbedPanel top_tabs = findTabbedPanel(
        FrEnd.panel_preferences_renderer_modern.panel);
    assertNotNull(top_tabs, "expected the top-level tab bar");
    assertEquals(3, top_tabs.getComponentCount(),
        "the top-level tab bar must be Renderer, Filtering and Colours");

    final Panel renderer_tab = FrEnd.panel_preferences_shared_show.panel;
    // A modern row...
    assertNotNull(polyhedronDropdown(),
        "the Node polyhedron dropdown must move to the Renderer tab");
    // ...and the shared rows.
    assertNotNull(findCheckbox(renderer_tab, "Fog depth is relative"),
        "the shared fog checkbox must move to the Renderer tab");

    // Deepest-first keeps its slot, after Pixellation...
    assertTrue(
        renderer_tab.getComponent(4)
            == FrEnd.panel_preferences_shared_misc.panel_redraw_deepest_first,
        "deepest-first must sit at Renderer tab row 4, after Pixellation");
    // ...followed by the labels row.
    assertTrue(
        renderer_tab.getComponent(5)
            == FrEnd.panel_preferences_renderer_modern.panel_labels_row,
        "the labels row must sit at Renderer tab row 5, after deepest-first");
  }

  private static TabbedPanel findTabbedPanel(Container container) {
    for (final Component c : container.getComponents()) {
      if (c instanceof TabbedPanel) {
        return (TabbedPanel) c;
      }
      if (c instanceof Container) {
        final TabbedPanel found = findTabbedPanel((Container) c);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static Checkbox findCheckbox(Container container, String label) {
    for (final Component c : container.getComponents()) {
      if (c instanceof Checkbox
          && label.equals(((Checkbox) c).getLabel())) {
        return (Checkbox) c;
      }
      if (c instanceof Container) {
        final Checkbox found = findCheckbox((Container) c, label);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  /**
   * The Renderer tab is one layout: the tab card is the shared panel
   * itself (a single GridLayout), not a BorderLayout wrapping two nested
   * panels. The ray-traced rows are added and removed at the bottom on
   * renderer switch, and every row gets the same height.
   */
  @Test
  void rendererTabIsOneLayout() throws Exception {
    final Panel tab = FrEnd.panel_preferences_shared_show.panel;
    assertTrue(tab.getLayout() instanceof java.awt.GridLayout,
        "the Renderer tab must be a single GridLayout");
    // No nested panels holding rows: every row is a direct child.
    for (final Component row : tab.getComponents()) {
      assertTrue(row instanceof Panel,
          "each Renderer tab row must be a direct child Panel");
    }

    final PanelPreferencesRendererModern modern =
        FrEnd.panel_preferences_renderer_modern;
    final int shared_rows = tab.getComponentCount();

    // Switch to ray-traced: the five rows appear at the bottom.
    javax.swing.SwingUtilities.invokeAndWait(() ->
        modern.setRaytracedRowsVisible(true));
    assertEquals(shared_rows + 5, tab.getComponentCount(),
        "ray-traced must add its five rows to the one layout");
    assertNotNull(findCheckbox(tab, "Glossiness"),
        "the Glossiness row must be on the Renderer tab");
    assertNotNull(findCheckbox(tab, "Fill light"),
        "the Fill light row must be on the Renderer tab");

    // Uniform spacing: one GridLayout gives every row the same height.
    tab.doLayout();
    int height = -1;
    for (final Component row : tab.getComponents()) {
      if (height < 0) {
        height = row.getHeight();
      }
      assertEquals(height, row.getHeight(),
          "every Renderer tab row must have the same height");
    }

    // Switch back: the rows leave, no duplicates on repeat.
    javax.swing.SwingUtilities.invokeAndWait(() -> {
      modern.setRaytracedRowsVisible(false);
      modern.setRaytracedRowsVisible(false);
    });
    assertEquals(shared_rows, tab.getComponentCount(),
        "leaving ray-traced must remove its rows again");
    javax.swing.SwingUtilities.invokeAndWait(() -> {
      modern.setRaytracedRowsVisible(true);
      modern.setRaytracedRowsVisible(true);
    });
    assertEquals(shared_rows + 5, tab.getComponentCount(),
        "repeated switches must not duplicate the rows");
    javax.swing.SwingUtilities.invokeAndWait(() ->
        modern.setRaytracedRowsVisible(false));
  }
}
