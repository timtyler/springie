// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;

/**
 * The ray-traced renderer's Glossiness and Max bounces dropdowns must offer
 * the right entries, start at the defaults, and drive the renderer.
 */
class PanelPreferencesRendererRaytracedTest {
  private int saved_glossiness;

  private int saved_max_bounces;

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @BeforeEach
  void save() {
    this.saved_glossiness = RendererDelegator.glossiness;
    this.saved_max_bounces = RendererDelegator.max_bounces;
  }

  @AfterEach
  void restore() {
    RendererDelegator.glossiness = this.saved_glossiness;
    RendererDelegator.max_bounces = this.saved_max_bounces;
  }

  private static Choice glossinessDropdown() {
    final Choice choice = findChoice(
        FrEnd.panel_preferences_renderer_raytraced.panel, "50%");
    assertNotNull(choice, "expected the Glossiness dropdown");
    return choice;
  }

  private static Choice maxBouncesDropdown() {
    // The Glossiness dropdown's first entry is "0%"; the bounce
    // dropdown's is "0".
    final Choice choice = findChoice(
        FrEnd.panel_preferences_renderer_raytraced.panel, "0");
    assertNotNull(choice, "expected the Max bounces dropdown");
    return choice;
  }

  private static Choice findChoice(Container container, String marker) {
    for (final Component c : container.getComponents()) {
      if (c instanceof Choice) {
        final Choice choice = (Choice) c;
        for (int i = 0; i < choice.getItemCount(); i++) {
          if (marker.equals(choice.getItem(i))) {
            return choice;
          }
        }
      }
      if (c instanceof Container) {
        final Choice found = findChoice((Container) c, marker);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void pick(Choice choice, String item) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      // AWT's programmatic select() fires no event, so deliver the
      // ItemEvent the native peer would have delivered straight to the
      // listeners.
      final ItemEvent event = new ItemEvent(choice,
          ItemEvent.ITEM_STATE_CHANGED, item, ItemEvent.SELECTED);
      for (final ItemListener listener : choice.getItemListeners()) {
        listener.itemStateChanged(event);
      }
    });
  }

  @Test
  void glossinessDropdownOffersZeroToOneHundredInTens() {
    final Choice choice = glossinessDropdown();
    assertEquals(11, choice.getItemCount(),
        "Glossiness must offer 0% to 100% in 10% steps");
    for (int percent = 0; percent <= 100; percent += 10) {
      assertEquals(percent + "%", choice.getItem(percent / 10),
          "glossiness entry " + percent / 10 + " must be " + percent + "%");
    }
    assertEquals("50%", choice.getSelectedItem(),
        "Glossiness must default to 50%");
  }

  @Test
  void maxBouncesDropdownOffersZeroToFour() {
    final Choice choice = maxBouncesDropdown();
    assertEquals(5, choice.getItemCount(),
        "Max bounces must offer 0 to 4");
    for (int bounces = 0; bounces <= 4; bounces++) {
      assertEquals("" + bounces, choice.getItem(bounces),
          "bounce entry " + bounces + " must be \"" + bounces + "\"");
    }
    assertEquals("2", choice.getSelectedItem(),
        "Max bounces must default to 2");
  }

  @Test
  void selectingGlossinessUpdatesTheRenderer() throws Exception {
    pick(glossinessDropdown(), "70%");
    assertEquals(70, RendererDelegator.glossiness);
    pick(glossinessDropdown(), "0%");
    assertEquals(0, RendererDelegator.glossiness);
  }

  @Test
  void selectingMaxBouncesUpdatesTheRenderer() throws Exception {
    pick(maxBouncesDropdown(), "4");
    assertEquals(4, RendererDelegator.max_bounces);
    pick(maxBouncesDropdown(), "0");
    assertEquals(0, RendererDelegator.max_bounces);
  }

  @Test
  void resetRestoresGlossinessAndBounces() throws Exception {
    RendererDelegator.glossiness = 90;
    RendererDelegator.max_bounces = 4;
    SwingUtilities.invokeAndWait(
        () -> FrEnd.panel_preferences_display.resetToDefaults());
    assertEquals(50, RendererDelegator.glossiness);
    assertEquals(2, RendererDelegator.max_bounces);
    assertEquals("50%", glossinessDropdown().getSelectedItem());
    assertEquals("2", maxBouncesDropdown().getSelectedItem());
  }
}
