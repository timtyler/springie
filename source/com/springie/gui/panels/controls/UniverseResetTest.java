// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.demos.CaterpillarDemo;
import com.springie.elements.nodes.Node;
import com.springie.gui.GuiTestSupport;
import com.springie.muscles.Muscles;
import com.springie.world.UniverseDefaults;
import com.springie.world.World;

/**
 * The Universe tab's Reset button must restore every universe setting to its
 * default and update the controls to match.
 */
class UniverseResetTest {

  @Test
  void resetUniverseRestoresDefaults() throws Exception {
    GuiTestSupport.bootApp();
    try {
      // The snapshot is global: a test that loaded a model earlier in
      // this JVM would otherwise pollute it.
      UniverseDefaults.resetToFactoryDefaults();

      SwingUtilities.invokeAndWait(() -> {
        final PanelControlsUniverse panel = FrEnd.panel_universe;

        // Scramble everything the reset covers.
        World.gravity_strength = 99;
        World.gravity_active = true;
        World.global_temperature = 500;
        Node.viscocity = 42;
        FrEnd.three_d = false;
        FrEnd.check_collisions = false;
        FrEnd.continuously_centre = true;
        ContextManager.getNodeManager().electrostatic.charge_active = false;

        panel.resetUniverse();

        // The simulation statics are back at their defaults...
        assertEquals(2, World.gravity_strength);
        assertFalse(World.gravity_active);
        assertEquals(6, World.global_temperature);
        assertEquals(0, Node.viscocity);
        assertTrue(FrEnd.three_d);
        assertTrue(FrEnd.check_collisions);
        assertFalse(FrEnd.continuously_centre);
        assertTrue(
            ContextManager.getNodeManager().electrostatic.charge_active);

        // ...and the controls match.
        assertEquals(2, panel.scroll_bar_gravity.getValue());
        assertEquals("2", panel.label_gravity.getText());
        assertEquals(6, panel.scroll_bar_temperature.getValue());
        assertEquals("6", panel.label_temperature.getText());
        assertEquals(0, panel.scroll_bar_viscocity.getValue());
        assertEquals("0", panel.label_viscocity.getText());

        assertTrue(panel.checkbox_3D.getState(), "3D");
        assertFalse(panel.checkbox_gravity_switch.getState(),
            "gravity switch");
        assertTrue(panel.checkbox_charge_switch.getState(), "charge switch");
        assertTrue(panel.checkbox_collision_check.getState(),
            "collision check");
        assertFalse(panel.checkbox_continuously_centre.getState(),
            "continuously centre");
      });
    } finally {
      GuiTestSupport.disposeFrames();
    }
  }

  /**
   * After loading a demo model, "Reset universe" must restore the model's
   * settings, not the factory defaults.
   */
  @Test
  void resetUniverseRestoresModelSettings() throws Exception {
    GuiTestSupport.bootApp();
    try {
      SwingUtilities.invokeAndWait(() -> {
        final PanelControlsUniverse panel = FrEnd.panel_universe;

        // Load Caterpillar, which sets gravity 5/on, friction 50,
        // charge off, collisions off, muscles on at 6%/120 ticks.
        CaterpillarDemo.buildAt(100);
        UniverseDefaults.snapshot();

        // Scramble everything the reset covers.
        World.gravity_strength = 99;
        World.gravity_active = false;
        World.ground_friction = 0;
        World.global_temperature = 500;
        Node.viscocity = 42;
        FrEnd.check_collisions = true;
        ContextManager.getNodeManager().electrostatic.charge_active = true;
        Muscles.enabled = false;

        panel.resetUniverse();

        // Back to the model's settings...
        assertEquals(5, World.gravity_strength);
        assertTrue(World.gravity_active);
        assertEquals(50, World.ground_friction);
        assertEquals(0, World.global_temperature);
        assertEquals(2, Node.viscocity);
        assertFalse(FrEnd.check_collisions);
        assertFalse(
            ContextManager.getNodeManager().electrostatic.charge_active);
        assertTrue(Muscles.enabled);
        assertEquals(6 * Muscles.UNITY / 100,
            Muscles.activeOscillator().getAmplitude());
        assertEquals(120, Muscles.activeOscillator().getPeriodTicks());

        // ...and the controls match.
        assertEquals(5, panel.scroll_bar_gravity.getValue());
        assertTrue(panel.checkbox_gravity_switch.getState(),
            "gravity switch");
        assertFalse(panel.checkbox_charge_switch.getState(),
            "charge switch");
        assertFalse(panel.checkbox_collision_check.getState(),
            "collision check");
      });
    } finally {
      UniverseDefaults.resetToFactoryDefaults();
      GuiTestSupport.disposeFrames();
    }
  }

  /**
   * The universe toggle checkboxes must mirror the simulation statics after
   * anything that changes the statics from under the UI -- notably loading a
   * model file, whose universe tag can disagree with the previous model
   * (Moscow ships collision_check=false while the box stayed on). Reflecting
   * must never toggle the statics themselves.
   */
  @Test
  void reflectUniverseTogglesMirrorsTheSimulationStatics() throws Exception {
    GuiTestSupport.bootApp();
    try {
      SwingUtilities.invokeAndWait(() -> {
        final PanelControlsUniverse panel = FrEnd.panel_universe;

        // What loading Moscow does: the file disagrees with the UI.
        FrEnd.check_collisions = false;
        FrEnd.continuously_centre = true;
        ContextManager.getNodeManager().electrostatic.charge_active = false;

        panel.reflectUniverseToggles();

        assertFalse(panel.checkbox_collision_check.getState(),
            "collision check");
        assertTrue(panel.checkbox_continuously_centre.getState(),
            "continuously centre");
        assertFalse(panel.checkbox_charge_switch.getState(),
            "charge switch");

        // The statics themselves are untouched: reflecting never toggles.
        assertFalse(FrEnd.check_collisions);
        assertTrue(FrEnd.continuously_centre);
        assertFalse(ContextManager.getNodeManager().electrostatic.charge_active);
      });
    } finally {
      FrEnd.check_collisions = true;
      FrEnd.continuously_centre = false;
      ContextManager.getNodeManager().electrostatic.charge_active = true;
      GuiTestSupport.disposeFrames();
    }
  }
}
