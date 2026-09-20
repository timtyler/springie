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
import com.springie.render.modules.modern.SimpleC60;
import com.springie.render.modules.modern.SimpleCube;
import com.springie.render.modules.modern.SimpleDodecahedron;
import com.springie.render.modules.modern.SimpleHexagon;
import com.springie.render.modules.modern.SimpleIcosahedron;
import com.springie.render.modules.modern.SimpleOctahedron;
import com.springie.render.modules.modern.SimpleSquare;

/**
 * The "Node polyhedron" dropdown on the modern renderer tab must offer
 * all seven shapes with C60 first and selected by default, and picking
 * one must install the matching renderer shape. The synthetic ItemEvent
 * stands in for the one the native peer delivers; the listener only
 * flips a repaint flag besides swapping the shape.
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
        FrEnd.panel_preferences_shared_show.panel_main);
    assertNotNull(choice,
        "expected the Node polyhedron dropdown on the Main sub-tab");
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
  void polyhedronDropdownOffersAllSevenShapes() {
    final Choice choice = polyhedronDropdown();
    final String[] expected = {"C60", "Dodecahedron", "Octahedron", "Cube",
        "Icosahedron", "Square", "Hexagon"};
    assertEquals(expected.length, choice.getItemCount(),
        "the Node polyhedron dropdown must offer seven shapes");
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], choice.getItem(i),
          "dropdown entry " + i + " must be " + expected[i]);
    }
  }

  @Test
  void c60IsTheDefaultPolyhedron() {
    // The dropdown selection is the user-facing default; assert it rather
    // than the mutable sphere_object field, which other test classes swap.
    assertEquals("C60", polyhedronDropdown().getSelectedItem(),
        "the dropdown must select C60 by default");
  }

  @Test
  void eachPolyhedronOptionInstallsItsRendererShape() {    final Choice choice = polyhedronDropdown();
    final String[] items = {"C60", "Dodecahedron", "Octahedron", "Cube",
        "Icosahedron", "Square", "Hexagon"};
    final Class<?>[] shapes = {SimpleC60.class, SimpleDodecahedron.class,
        SimpleOctahedron.class, SimpleCube.class, SimpleIcosahedron.class,
        SimpleSquare.class, SimpleHexagon.class};
    for (int i = 0; i < items.length; i++) {
      pickPolyhedron(choice, items[i]);
      assertTrue(shapes[i].isInstance(ModularRendererNew.sphere_object),
          "picking " + items[i] + " must install " + shapes[i].getSimpleName());
    }
    // Leave the default shape installed.
    pickPolyhedron(choice, "C60");
  }

  /**
   * The Renderer tab holds "Main", "Tiles" and "Fog" sub-tabs: the Tiles
   * rows move to Tiles, the fog rows to Fog, everything else to Main.
   * "Render deepest objects first" and "Show labels on:" keep their
   * slots at the top of Main, after Pixellation.
   */
  @Test
  void rendererTabHasMainTilesAndFogSubTabs() {
    final Panel shared_misc_panel = FrEnd.panel_preferences_shared_misc.panel;
    assertEquals(0, shared_misc_panel.getComponentCount(),
        "the old Misc tab panel must be empty after its rows move");

    // No Options tab anymore, and Filtering moved under Colours: the
    // top-level bar is Renderer | Colours.
    final TabbedPanel top_tabs = findTabbedPanel(
        FrEnd.panel_preferences_renderer_modern.panel);
    assertNotNull(top_tabs, "expected the top-level tab bar");
    assertEquals(2, top_tabs.getComponentCount(),
        "the top-level tab bar must be Renderer and Colours");

    // Filtering now lives as the Filters card under Colours, holding
    // the filters panel (Filled:/Wireframe: rows plus the Colour-A/B
    // nested tabs).
    final TabbedPanel colours_tabs = FrEnd.panel_preferences_renderer_modern_colours.tab_colours_main;
    assertEquals(3, colours_tabs.getComponentCount(),
        "the Colours bar must be General, Label and Filters");
    assertTrue(
        colours_tabs.getComponent(2) == FrEnd.panel_preferences_renderer_modern_filters.panel,
        "the Filters card must hold the filters panel");

    final Panel main_tab = FrEnd.panel_preferences_shared_show.panel_main;
    final Panel tiles_tab = FrEnd.panel_preferences_shared_show.panel_tiles;
    final Panel fog_tab = FrEnd.panel_preferences_shared_show.panel_fog;
    // A modern row...
    assertNotNull(polyhedronDropdown(),
        "the Node polyhedron dropdown must move to the Main sub-tab");
    // ...the Tiles rows...
    assertNotNull(findCheckbox(tiles_tab, "Show rendering tiles"),
        "the Show-tiles checkbox must move to the Tiles sub-tab");
    // ...and the fog rows.
    assertNotNull(findCheckbox(fog_tab, "Fog depth is relative"),
        "the shared fog checkbox must move to the Fog sub-tab");

    // Deepest-first keeps its slot, after Pixellation...
    assertTrue(
        main_tab.getComponent(4)
            == FrEnd.panel_preferences_shared_misc.panel_redraw_deepest_first,
        "deepest-first must sit at Main sub-tab row 4, after Pixellation");
    // ...followed by the labels row.
    assertTrue(
        main_tab.getComponent(5)
            == FrEnd.panel_preferences_renderer_modern.panel_labels_row,
        "the labels row must sit at Main sub-tab row 5, after deepest-first");
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
   * Each Renderer sub-tab (Main, Tiles, Fog) is one layout: a single
   * GridLayout whose rows are direct children, so every row gets the
   * same height. The ray-traced rows are added and removed at the
   * bottom of Main on renderer switch.
   */
  @Test
  void rendererSubTabsAreOneLayoutEach() throws Exception {
    final Panel main_tab = FrEnd.panel_preferences_shared_show.panel_main;
    final Panel tiles_tab = FrEnd.panel_preferences_shared_show.panel_tiles;
    final Panel fog_tab = FrEnd.panel_preferences_shared_show.panel_fog;
    for (final Panel tab : new Panel[] { main_tab, tiles_tab, fog_tab }) {
      assertTrue(tab.getLayout() instanceof java.awt.GridLayout,
          "each Renderer sub-tab must be a single GridLayout");
      // No nested panels holding rows: every row is a direct child.
      for (final Component row : tab.getComponents()) {
        assertTrue(row instanceof Panel,
            "each Renderer sub-tab row must be a direct child Panel");
      }
    }

    final PanelPreferencesRendererModern modern =
        FrEnd.panel_preferences_renderer_modern;
    final int main_rows = main_tab.getComponentCount();

    // Switch to ray-traced: the five rows appear at the bottom of Main.
    javax.swing.SwingUtilities.invokeAndWait(() ->
        modern.setRaytracedRowsVisible(true));
    assertEquals(main_rows + 5, main_tab.getComponentCount(),
        "ray-traced must add its five rows to the Main sub-tab");
    assertNotNull(findCheckbox(main_tab, "Glossiness"),
        "the Glossiness row must be on the Main sub-tab");
    assertNotNull(findCheckbox(main_tab, "Fill light"),
        "the Fill light row must be on the Main sub-tab");

    // Uniform spacing: one GridLayout gives every row the same height.
    main_tab.doLayout();
    int height = -1;
    for (final Component row : main_tab.getComponents()) {
      if (height < 0) {
        height = row.getHeight();
      }
      assertEquals(height, row.getHeight(),
          "every Main sub-tab row must have the same height");
    }

    // Switch back: the rows leave, no duplicates on repeat.
    javax.swing.SwingUtilities.invokeAndWait(() -> {
      modern.setRaytracedRowsVisible(false);
      modern.setRaytracedRowsVisible(false);
    });
    assertEquals(main_rows, main_tab.getComponentCount(),
        "leaving ray-traced must remove its rows again");
    javax.swing.SwingUtilities.invokeAndWait(() -> {
      modern.setRaytracedRowsVisible(true);
      modern.setRaytracedRowsVisible(true);
    });
    assertEquals(main_rows + 5, main_tab.getComponentCount(),
        "repeated switches must not duplicate the rows");
    javax.swing.SwingUtilities.invokeAndWait(() ->
        modern.setRaytracedRowsVisible(false));
  }
}
