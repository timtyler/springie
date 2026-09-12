// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.gui.GuiTestSupport;
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

      SwingUtilities.invokeAndWait(() -> {
        final PanelControlsUniverse panel = FrEnd.panel_universe;

        // Scramble everything the reset covers.
        World.gravity_strength = 99;
        World.gravity_active = true;
        World.global_temperature = 500;
        Node.viscocity = 42;
        FrEnd.three_d = false;
        FrEnd.check_collisions = false;
        FrEnd.links_disabled = true;
        FrEnd.continuously_centre = true;
        FrEnd.node_growth = true;
        ContextManager.getNodeManager().electrostatic.charge_active = false;

        panel.resetUniverse();

        // The simulation statics are back at their defaults...
        assertEquals(2, World.gravity_strength);
        assertFalse(World.gravity_active);
        assertEquals(6, World.global_temperature);
        assertEquals(0, Node.viscocity);
        assertTrue(FrEnd.three_d);
        assertTrue(FrEnd.check_collisions);
        assertFalse(FrEnd.links_disabled);
        assertFalse(FrEnd.continuously_centre);
        assertFalse(FrEnd.node_growth);
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
        // The links-disabled checkbox is inverted: checked means enabled.
        assertTrue(panel.checkbox_links_disabled.getState(),
            "links disabled checkbox");
        assertFalse(panel.checkbox_continuously_centre.getState(),
            "continuously centre");
        assertFalse(panel.checkbox_node_growth.getState(), "node growth");
      });
    } finally {
      GuiTestSupport.disposeFrames();
    }
  }
}
