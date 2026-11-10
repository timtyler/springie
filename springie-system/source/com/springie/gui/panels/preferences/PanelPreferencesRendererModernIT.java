// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

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
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "Node polyhedron" dropdown on the modern renderer tab must offer
 * all seven shapes with C60 first and selected by default, and picking
 * one must install the matching renderer shape. The synthetic ItemEvent
 * stands in for the one the native peer delivers; the listener only
 * flips a repaint flag besides swapping the shape.
 */
public class PanelPreferencesRendererModernIT {

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
        PreferencesPanels.panel_preferences_shared_show.panel_main);
    assertNotNull(choice,
        "expected the Node polyhedron dropdown on the Main sub-tab");
    return choice;
  }

  private static Choice findChoice(final Container container) {
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

  private static void pickPolyhedron(final Choice choice, final String item) {
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
  void eachPolyhedronOptionInstallsItsRendererShape() {
    final Choice choice = polyhedronDropdown();
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
   * The preferences tab bar holds four top-level tabs -- Renderer
   * (the old Main content), Colours, Fog and Labels: Tiles moved into
   * the Renderer Main tab (Tim, 2026-10-03), Fog moved up next to
   * Colours, and the Labels tab was added next to Fog (Tim,
   * 2026-10-17). "Render deepest objects first" keeps its slot at the
   * top of the Renderer tab, after Pixellation.
   */
  @Test
  void rendererTabHasMainTilesAndFogSubTabs() {
    final Panel shared_misc_panel = PreferencesPanels.panel_preferences_shared_misc.panel;
    assertEquals(0, shared_misc_panel.getComponentCount(),
        "the old Misc tab panel must be empty after its rows move");

    // No Options tab anymore, Filtering moved under Colours, and Tiles
    // moved into the Renderer Main tab (Tim, 2026-10-03); the Labels
    // tab was added next to Fog (Tim, 2026-10-17): the top-level bar
    // is Renderer | Colours | Fog | Labels.
    final TabbedPanel top_tabs = findTabbedPanel(
        PreferencesPanels.panel_preferences_renderer_modern.panel);
    assertNotNull(top_tabs, "expected the top-level tab bar");
    assertEquals(4, top_tabs.getComponentCount(),
        "the top-level tab bar must be Renderer, Colours, Fog and Labels");
    assertTrue(
        top_tabs.getComponent(0) == PreferencesPanels.panel_preferences_shared_show.panel,
        "the Renderer card must hold the shared-show panel");
    assertTrue(
        top_tabs.getComponent(1)
            == PreferencesPanels.panel_preferences_renderer_modern_colours.panel,
        "the Colours card must hold the colours panel");
    // Tiles moved into the Renderer Main tab (Tim, 2026-10-03); Fog is
    // now at index 2.
    assertTrue(
        top_tabs.getComponent(2)
            == PreferencesPanels.panel_preferences_shared_show.panel_fog,
        "the Fog card must hold the fog panel");
    // The Labels tab sits next to Fog (Tim, 2026-10-17).
    assertTrue(
        top_tabs.getComponent(3)
            == PreferencesPanels.panel_preferences_shared_show.panel_labels,
        "the Labels card must hold the labels panel");

    // Filtering now lives as the Filters card under Colours, holding
    // the filters panel (Filled:/Wireframe: rows plus the Colour-A/B
    // nested tabs). The old Label card left Colours when its pickers
    // moved to the top-level Labels tab (Tim, 2026-10-17).
    final TabbedPanel colours_tabs = PreferencesPanels.panel_preferences_renderer_modern_colours.tab_colours_main;
    assertEquals(2, colours_tabs.getComponentCount(),
        "the Colours bar must be General and Filters");
    assertTrue(
        colours_tabs.getComponent(1) == PreferencesPanels.panel_preferences_renderer_modern_filters.panel,
        "the Filters card must hold the filters panel");

    final Panel main_tab = PreferencesPanels.panel_preferences_shared_show.panel_main;
    final Panel tiles_tab = PreferencesPanels.panel_preferences_shared_show.panel_tiles;
    final Panel fog_tab = PreferencesPanels.panel_preferences_shared_show.panel_fog;
    // A modern row...
    assertNotNull(polyhedronDropdown(),
        "the Node polyhedron dropdown must move to the Main sub-tab");
    // ...the Tiles rows (moved into Main, Tim, 2026-10-03)...
    assertNotNull(findCheckbox(main_tab, "Show rendering tiles"),
        "the Show-tiles checkbox must move to the Main sub-tab");
    // ...and the fog rows.
    assertNotNull(findCheckbox(fog_tab, "Fog depth is relative"),
        "the shared fog checkbox must move to the Fog sub-tab");

    // Deepest-first keeps its slot after Pixellation. The absolute
    // slot moves: a fresh boot puts it right after Pixellation
    // (PanelPreferencesDisplay), while the renderer's visibility
    // helpers re-insert it one slot lower -- the relative order is
    // what the layout guarantees.
    final int pixellation = indexOfRowWithLabel(main_tab, "Pixellated:");
    assertTrue(pixellation >= 0,
        "expected a Pixellation row on the Renderer tab's Main sub-tab");
    final int deepest = indexOf(main_tab,
        PreferencesPanels.panel_preferences_shared_misc.panel_redraw_deepest_first);
    assertTrue(deepest > pixellation,
        "deepest-first must sit after Pixellation");
    // The "Show labels on:" row lives on the Labels tab, not Main
    // (moved there by Tim, 2026-10-17).
    final Panel labels_tab = PreferencesPanels.panel_preferences_shared_show.panel_labels;
    assertTrue(indexOf(labels_tab,
        PreferencesPanels.panel_preferences_renderer_modern.panel_labels_row) >= 0,
        "the labels row must sit on the Labels tab");
    assertEquals(-1, indexOf(main_tab,
        PreferencesPanels.panel_preferences_renderer_modern.panel_labels_row),
        "the labels row must not sit on the Main sub-tab");
  }

  /**
   * Finds the index of the given row component. Returns -1 when absent.
   */
  private static int indexOf(final Panel tab, final Component row) {
    final Component[] rows = tab.getComponents();
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] == row) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Finds the row of a sub-tab whose first Label reads the given text.
   * Returns -1 when no row carries that label.
   */
  private static int indexOfRowWithLabel(final Panel tab, final String text) {
    final Component[] rows = tab.getComponents();
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] instanceof Container) {
        for (final Component c : ((Container) rows[i]).getComponents()) {
          if (c instanceof Label && text.equals(((Label) c).getText())) {
            return i;
          }
        }
      }
    }
    return -1;
  }

  private static TabbedPanel findTabbedPanel(final Container container) {
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

  private static Checkbox findCheckbox(final Container container, final String label) {
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
    final Panel main_tab = PreferencesPanels.panel_preferences_shared_show.panel_main;
    final Panel tiles_tab = PreferencesPanels.panel_preferences_shared_show.panel_tiles;
    final Panel fog_tab = PreferencesPanels.panel_preferences_shared_show.panel_fog;
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
        PreferencesPanels.panel_preferences_renderer_modern;
    final int main_rows = main_tab.getComponentCount();

    // Switch to ray-traced: the five effect rows appear at the bottom
    // of Main (takeEffectRows detaches the five effect rows).
    javax.swing.SwingUtilities.invokeAndWait(() ->
        modern.setRaytracedRowsVisible(true));
    assertEquals(main_rows + 5, main_tab.getComponentCount(),
        "ray-traced must add its five rows to the Main sub-tab");
    assertNotNull(findCheckbox(main_tab, "Glossiness"),
        "the Glossiness row must be on the Main sub-tab");
    // Fill light moved to Universe tab (Tim, 2026-10-03); not here.

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
