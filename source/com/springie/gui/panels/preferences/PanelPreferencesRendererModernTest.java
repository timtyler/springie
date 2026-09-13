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
 * The "Node polyhedron" dropdown on the modern renderer Misc tab must offer
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
        FrEnd.panel_preferences_renderer_modern.panel_misc);
    assertNotNull(choice, "expected the Node polyhedron dropdown on the Misc tab");
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
   * The old top-level Misc tab is merged into Options: the shared Misc
   * panel is no longer a tab card, its rows moved into the nested Misc
   * sub-tab alongside the modern rows -- except "Render deepest objects
   * first", which lives on the Renderer tab.
   */
  @Test
  void miscTabIsMergedIntoTheOptionsTab() {
    final Panel shared_misc_panel = FrEnd.panel_preferences_shared_misc.panel;
    assertEquals(0, shared_misc_panel.getComponentCount(),
        "the old Misc tab panel must be empty after its rows move");

    final TabbedPanel top_tabs = findTabbedPanel(
        FrEnd.panel_preferences_renderer_modern.panel);
    assertNotNull(top_tabs, "expected the top-level tab bar");
    for (final Component card : top_tabs.getComponents()) {
      assertTrue(card != shared_misc_panel,
          "the shared Misc panel must not be a top-level tab card");
    }

    final Panel panel_misc = FrEnd.panel_preferences_renderer_modern.panel_misc;
    // A modern row...
    assertNotNull(polyhedronDropdown(),
        "the Node polyhedron dropdown must stay on the Misc sub-tab");
    // ...and the shared rows (but not deepest-first: Renderer tab).
    assertNotNull(findCheckbox(panel_misc, "Fog depth is relative"),
        "the shared fog checkbox must move to the Misc sub-tab");
    assertTrue(findCheckbox(panel_misc, "Render deepest objects first")
        == null, "deepest-first must not be on the Misc sub-tab");

    // Deepest-first sits on the Renderer tab, between Pixellated and FPS.
    final Panel renderer_tab = FrEnd.panel_preferences_shared_show.panel;
    final Checkbox deepest_first =
        findCheckbox(renderer_tab, "Render deepest objects first");
    assertNotNull(deepest_first,
        "deepest-first must be on the Renderer tab");
    assertTrue(
        renderer_tab.getComponent(3)
            == FrEnd.panel_preferences_shared_misc.panel_redraw_deepest_first,
        "deepest-first must sit at Renderer tab row 3, before the FPS readout");
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
