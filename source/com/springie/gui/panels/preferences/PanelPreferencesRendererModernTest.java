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
   * sub-tab alongside the modern rows.
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
    // ...and the shared rows.
    assertNotNull(findCheckbox(panel_misc, "Render deepest objects first"),
        "the shared deepest-first checkbox must move to the Misc sub-tab");
    assertNotNull(findCheckbox(panel_misc, "Fog depth is relative"),
        "the shared fog checkbox must move to the Misc sub-tab");
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
}
