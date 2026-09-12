// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Label;
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
 * The ray-traced renderer's Glossiness, Shadows, Specular and Fresnel
 * controls must offer the right entries, start at the defaults, and
 * drive the renderer.
 */
class PanelPreferencesRendererRaytracedTest {
  private int saved_glossiness;

  private boolean saved_shadows;

  private int saved_specular;

  private int saved_fresnel;

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
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_specular = RendererDelegator.specular;
    this.saved_fresnel = RendererDelegator.fresnel;
  }

  @AfterEach
  void restore() {
    RendererDelegator.glossiness = this.saved_glossiness;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.specular = this.saved_specular;
    RendererDelegator.fresnel = this.saved_fresnel;
  }

  private static Choice glossinessDropdown() {
    final Choice choice = findChoice(
        FrEnd.panel_preferences_renderer_raytraced.panel, "50%");
    assertNotNull(choice, "expected the Glossiness dropdown");
    return choice;
  }

  private static Choice specularDropdown() {
    // The Glossiness and Specular dropdowns offer identical entries,
    // so find the one sitting next to the "Specular:" label.
    final Container parent = findLabelledPanel(
        FrEnd.panel_preferences_renderer_raytraced.panel, "Specular:");
    assertNotNull(parent, "expected the Specular panel");
    final Choice choice = findChoice(parent, "50%");
    assertNotNull(choice, "expected the Specular dropdown");
    return choice;
  }

  private static Choice fresnelDropdown() {
    final Container parent = findLabelledPanel(
        FrEnd.panel_preferences_renderer_raytraced.panel, "Fresnel:");
    assertNotNull(parent, "expected the Fresnel panel");
    final Choice choice = findChoice(parent, "50%");
    assertNotNull(choice, "expected the Fresnel dropdown");
    return choice;
  }

  private static Checkbox shadowsCheckbox() {
    final Checkbox checkbox = findCheckbox(
        FrEnd.panel_preferences_renderer_raytraced.panel, "Shadows");
    assertNotNull(checkbox, "expected the Shadows checkbox");
    return checkbox;
  }

  private static Container findLabelledPanel(Container container,
      String label_text) {
    for (final Component c : container.getComponents()) {
      if (c instanceof Container) {
        boolean labelled = false;
        for (final Component inner : ((Container) c).getComponents()) {
          if (inner instanceof Label
              && label_text.equals(((Label) inner).getText())) {
            labelled = true;
            break;
          }
        }
        if (labelled) {
          return (Container) c;
        }
        final Container found = findLabelledPanel((Container) c,
            label_text);
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

  private static void tick(Checkbox checkbox, boolean state)
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      // The panel reads the checkbox state, so set it before
      // delivering the event the native peer would have delivered.
      checkbox.setState(state);
      final ItemEvent event = new ItemEvent(checkbox,
          ItemEvent.ITEM_STATE_CHANGED, checkbox.getLabel(),
          state ? ItemEvent.SELECTED : ItemEvent.DESELECTED);
      for (final ItemListener listener : checkbox.getItemListeners()) {
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
    assertEquals("0%", choice.getSelectedItem(),
        "Glossiness must default to 0%");
  }

  @Test
  void selectingGlossinessUpdatesTheRenderer() throws Exception {
    pick(glossinessDropdown(), "70%");
    assertEquals(70, RendererDelegator.glossiness);
    pick(glossinessDropdown(), "0%");
    assertEquals(0, RendererDelegator.glossiness);
  }

  @Test
  void resetRestoresRaytracedDefaults() throws Exception {
    RendererDelegator.glossiness = 90;
    RendererDelegator.shadows = true;
    RendererDelegator.specular = 80;
    RendererDelegator.fresnel = 70;
    SwingUtilities.invokeAndWait(
        () -> FrEnd.panel_preferences_display.resetToDefaults());
    assertEquals(0, RendererDelegator.glossiness);
    assertEquals(false, RendererDelegator.shadows);
    assertEquals(90, RendererDelegator.specular);
    assertEquals(0, RendererDelegator.fresnel);
    assertEquals("0%", glossinessDropdown().getSelectedItem());
    assertEquals(false, shadowsCheckbox().getState());
    assertEquals("90%", specularDropdown().getSelectedItem());
    assertEquals("0%", fresnelDropdown().getSelectedItem());
  }

  @Test
  void shadowsCheckboxDefaultsOffAndDrivesTheRenderer() throws Exception {
    final Checkbox checkbox = shadowsCheckbox();
    assertEquals(false, checkbox.getState(),
        "Shadows must default to off");
    tick(checkbox, true);
    assertEquals(true, RendererDelegator.shadows);
    tick(checkbox, false);
    assertEquals(false, RendererDelegator.shadows);
  }

  @Test
  void specularDropdownOffersZeroToOneHundredInTens() {
    final Choice choice = specularDropdown();
    assertEquals(11, choice.getItemCount(),
        "Specular must offer 0% to 100% in 10% steps");
    for (int percent = 0; percent <= 100; percent += 10) {
      assertEquals(percent + "%", choice.getItem(percent / 10),
          "specular entry " + percent / 10 + " must be " + percent + "%");
    }
    assertEquals("90%", choice.getSelectedItem(),
        "Specular must default to 90%");
  }

  @Test
  void selectingSpecularUpdatesTheRenderer() throws Exception {
    pick(specularDropdown(), "100%");
    assertEquals(100, RendererDelegator.specular);
    pick(specularDropdown(), "0%");
    assertEquals(0, RendererDelegator.specular);
  }

  @Test
  void fresnelDropdownOffersZeroToOneHundredInTens() {
    final Choice choice = fresnelDropdown();
    assertEquals(11, choice.getItemCount(),
        "Fresnel must offer 0% to 100% in 10% steps");
    for (int percent = 0; percent <= 100; percent += 10) {
      assertEquals(percent + "%", choice.getItem(percent / 10),
          "fresnel entry " + percent / 10 + " must be " + percent + "%");
    }
    assertEquals("0%", choice.getSelectedItem(),
        "Fresnel must default to 0%");
  }

  @Test
  void selectingFresnelUpdatesTheRenderer() throws Exception {
    pick(fresnelDropdown(), "100%");
    assertEquals(100, RendererDelegator.fresnel);
    pick(fresnelDropdown(), "0%");
    assertEquals(0, RendererDelegator.fresnel);
  }
}
