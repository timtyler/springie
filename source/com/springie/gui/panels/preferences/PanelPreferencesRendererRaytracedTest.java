// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ray-traced renderer's Glossiness, Shadows, Specular and Fresnel
 * controls must offer the right entries, start at the defaults, drive
 * the renderer, and hide each strength dropdown while its effect is
 * switched off. (Tim, 2026-10-03: Fill light moved to the Universe tab.)
 */
class PanelPreferencesRendererRaytracedTest {
  private int saved_glossiness;

  private boolean saved_glossiness_enabled;

  private boolean saved_shadows;

  private int saved_specular;

  private boolean saved_specular_enabled;

  private int saved_fresnel;

  private boolean saved_fresnel_enabled;

  private int saved_fill_light;

  private boolean saved_fill_light_enabled;

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
    this.saved_glossiness_enabled = RendererDelegator.glossiness_enabled;
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_specular = RendererDelegator.specular;
    this.saved_specular_enabled = RendererDelegator.specular_enabled;
    this.saved_fresnel = RendererDelegator.fresnel;
    this.saved_fresnel_enabled = RendererDelegator.fresnel_enabled;
    this.saved_fill_light = RendererDelegator.fill_light;
    this.saved_fill_light_enabled = RendererDelegator.fill_light_enabled;
    // Reset to defaults (Tim, 2026-10-03): other tests may pollute these.
    // Must reset both the fields AND the checkboxes (the panel is a
    // singleton whose checkboxes retain polluted state).
    RendererDelegator.shadows = false;
    RendererDelegator.specular_enabled = true;
    RendererDelegator.specular = 100;
    try {
      SwingUtilities.invokeAndWait(() -> {
        final Checkbox shadows = effectCheckbox("Shadows");
        if (shadows != null) shadows.setState(false);
        final Checkbox specular = effectCheckbox("Specular");
        if (specular != null) specular.setState(true);
      });
    } catch (Exception e) {
      // Panel not yet created; ignore.
    }
  }

  @AfterEach
  void restore() {
    RendererDelegator.glossiness = this.saved_glossiness;
    RendererDelegator.glossiness_enabled = this.saved_glossiness_enabled;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.specular = this.saved_specular;
    RendererDelegator.specular_enabled = this.saved_specular_enabled;
    RendererDelegator.fresnel = this.saved_fresnel;
    RendererDelegator.fresnel_enabled = this.saved_fresnel_enabled;
    RendererDelegator.fill_light = this.saved_fill_light;
    RendererDelegator.fill_light_enabled = this.saved_fill_light_enabled;
  }

  /** The row panel holding the checkbox with the given label. */
  private static Container effectRow(final String label) {
    for (final Panel row : FrEnd.panel_preferences_renderer_raytraced.effect_rows) {
      if (findCheckbox(row, label) != null) {
        return row;
      }
    }
    assertNotNull(null, "expected the " + label + " row");
    return null;
  }

  private static Checkbox effectCheckbox(final String label) {
    for (final Panel row : FrEnd.panel_preferences_renderer_raytraced.effect_rows) {
      final Checkbox checkbox = findCheckbox(row, label);
      if (checkbox != null) {
        return checkbox;
      }
    }
    assertNotNull(null, "expected the " + label + " checkbox");
    return null;
  }

  private static Choice effectDropdown(final String label) {
    final Choice choice = findChoice(effectRow(label));
    assertNotNull(choice, "expected the " + label + " dropdown");
    return choice;
  }

  private static Container findCheckboxRow(final Container container,
      String label) {
    for (final Component c : container.getComponents()) {
      if (c instanceof Container) {
        final Container inner = (Container) c;
        if (findCheckbox(inner, label) != null) {
          return inner;
        }
        final Container found = findCheckboxRow(inner, label);
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

  private static Choice findChoice(final Container container) {
    for (final Component c : container.getComponents()) {
      if (c instanceof Choice) {
        return (Choice) c;
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

  private static void pick(final Choice choice, final String item) throws Exception {
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

  private static void tick(final Checkbox checkbox, final boolean state)
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

  private static void assertTenToOneHundredInTens(final Choice choice,
      String name) {
    assertEquals(10, choice.getItemCount(),
        name + " must offer 10% to 100% in 10% steps");
    for (int percent = 10; percent <= 100; percent += 10) {
      assertEquals(percent + "%", choice.getItem(percent / 10 - 1),
          name + " entry " + (percent / 10 - 1) + " must be " + percent
              + "%");
    }
  }

  @Test
  void glossinessDefaultsOffWithHiddenDropdown() {
    final Checkbox checkbox = effectCheckbox("Glossiness");
    final Choice choice = effectDropdown("Glossiness");
    assertFalse(checkbox.getState(), "Glossiness must default to off");
    assertFalse(choice.isVisible(),
        "the Glossiness dropdown must hide while the effect is off");
    assertTenToOneHundredInTens(choice, "Glossiness");
    assertEquals("50%", choice.getSelectedItem(),
        "Glossiness strength must default to 50%");
  }

  @Test
  void enablingGlossinessShowsItsDropdown() throws Exception {
    final Checkbox checkbox = effectCheckbox("Glossiness");
    final Choice choice = effectDropdown("Glossiness");
    tick(checkbox, true);
    assertTrue(RendererDelegator.glossiness_enabled);
    assertTrue(choice.isVisible(),
        "ticking Glossiness on must show its dropdown");
    pick(choice, "70%");
    assertEquals(70, RendererDelegator.glossiness);
    tick(checkbox, false);
    assertFalse(RendererDelegator.glossiness_enabled);
    assertFalse(choice.isVisible(),
        "ticking Glossiness off must hide its dropdown again");
  }

  @Test
  void shadowsCheckboxDefaultsOffAndDrivesTheRenderer() throws Exception {
    final Checkbox checkbox = effectCheckbox("Shadows");
    assertFalse(checkbox.getState(), "Shadows must default to off");
    tick(checkbox, true);
    assertTrue(RendererDelegator.shadows);
    tick(checkbox, false);
    assertFalse(RendererDelegator.shadows);
  }

  @Test
  void specularDefaultsOnWithVisibleDropdown() {
    final Checkbox checkbox = effectCheckbox("Specular");
    final Choice choice = effectDropdown("Specular");
    assertTrue(checkbox.getState(), "Specular must default to on");
    assertTrue(choice.isVisible(),
        "the Specular dropdown must show while the effect is on");
    assertTenToOneHundredInTens(choice, "Specular");
    assertEquals("100%", choice.getSelectedItem(),
        "Specular strength must default to 100%");
  }

  @Test
  void disablingSpecularHidesItsDropdown() throws Exception {
    final Checkbox checkbox = effectCheckbox("Specular");
    final Choice choice = effectDropdown("Specular");
    tick(checkbox, false);
    assertFalse(RendererDelegator.specular_enabled);
    assertFalse(choice.isVisible(),
        "ticking Specular off must hide its dropdown");
    tick(checkbox, true);
    assertTrue(RendererDelegator.specular_enabled);
    assertTrue(choice.isVisible(),
        "ticking Specular on must show its dropdown again");
  }

  @Test
  void selectingSpecularUpdatesTheRenderer() throws Exception {
    pick(effectDropdown("Specular"), "100%");
    assertEquals(100, RendererDelegator.specular);
    pick(effectDropdown("Specular"), "10%");
    assertEquals(10, RendererDelegator.specular);
  }

  @Test
  void fresnelDefaultsOffWithHiddenDropdown() {
    final Checkbox checkbox = effectCheckbox("Fresnel");
    final Choice choice = effectDropdown("Fresnel");
    assertFalse(checkbox.getState(), "Fresnel must default to off");
    assertFalse(choice.isVisible(),
        "the Fresnel dropdown must hide while the effect is off");
    assertTenToOneHundredInTens(choice, "Fresnel");
    assertEquals("50%", choice.getSelectedItem(),
        "Fresnel strength must default to 50%");
  }

  @Test
  void enablingFresnelShowsItsDropdown() throws Exception {
    final Checkbox checkbox = effectCheckbox("Fresnel");
    final Choice choice = effectDropdown("Fresnel");
    tick(checkbox, true);
    assertTrue(RendererDelegator.fresnel_enabled);
    assertTrue(choice.isVisible(),
        "ticking Fresnel on must show its dropdown");
    pick(choice, "100%");
    assertEquals(100, RendererDelegator.fresnel);
    tick(checkbox, false);
    assertFalse(RendererDelegator.fresnel_enabled);
    assertFalse(choice.isVisible(),
        "ticking Fresnel off must hide its dropdown again");
  }

  @Test
  void resetRestoresRaytracedDefaults() throws Exception {
    tick(effectCheckbox("Glossiness"), true);
    pick(effectDropdown("Glossiness"), "100%");
    tick(effectCheckbox("Shadows"), true);
    tick(effectCheckbox("Specular"), false);
    pick(effectDropdown("Specular"), "10%");
    tick(effectCheckbox("Fresnel"), true);
    SwingUtilities.invokeAndWait(
        () -> FrEnd.panel_preferences_display.resetToDefaults());

    assertFalse(RendererDelegator.glossiness_enabled);
    assertEquals(50, RendererDelegator.glossiness);
    assertFalse(RendererDelegator.shadows);
    assertTrue(RendererDelegator.specular_enabled);
    assertEquals(100, RendererDelegator.specular);
    assertFalse(RendererDelegator.fresnel_enabled);
    assertEquals(50, RendererDelegator.fresnel);
    assertFalse(RendererDelegator.fill_light_enabled);
    assertEquals(50, RendererDelegator.fill_light);

    assertFalse(effectCheckbox("Glossiness").getState());
    assertFalse(effectDropdown("Glossiness").isVisible());
    assertEquals("50%", effectDropdown("Glossiness").getSelectedItem());
    assertFalse(effectCheckbox("Shadows").getState());
    assertTrue(effectCheckbox("Specular").getState());
    assertTrue(effectDropdown("Specular").isVisible());
    assertEquals("100%", effectDropdown("Specular").getSelectedItem());
    assertFalse(effectCheckbox("Fresnel").getState());
    assertFalse(effectDropdown("Fresnel").isVisible());
  }
}
