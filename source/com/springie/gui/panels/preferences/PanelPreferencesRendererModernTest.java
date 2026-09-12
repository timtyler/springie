// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
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
  void eachPolyhedronOptionInstallsItsRendererShape() {
    final Choice choice = polyhedronDropdown();
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
}
