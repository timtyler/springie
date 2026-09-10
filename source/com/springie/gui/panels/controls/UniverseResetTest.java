// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.nodes.Node;
import com.springie.world.World;

/**
 * The Universe tab's Reset button must restore every universe setting to its
 * default and update the controls to match.
 */
class UniverseResetTest {

  @Test
  void resetUniverseRestoresDefaults() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");

    SwingUtilities.invokeAndWait(() -> FrEnd.main(new String[0]));
    try {
      waitForBootModelLoadToSettle();

      SwingUtilities.invokeAndWait(() -> {
        final PanelControlsUniverse panel = FrEnd.panel_edit_universe;

        // Scramble everything the reset covers.
        World.gravity_strength = 99;
        World.gravity_active = true;
        World.global_temperature = 500;
        Node.viscocity = 42;
        FrEnd.three_d = false;
        FrEnd.check_collisions = false;
        FrEnd.links_disabled = true;
        FrEnd.collide_self_only = true;
        FrEnd.continuously_centre = true;
        FrEnd.node_growth = true;
        ContextMananger.getNodeManager().electrostatic.charge_active = false;

        panel.resetUniverse();

        // The simulation statics are back at their defaults...
        assertEquals(2, World.gravity_strength);
        assertFalse(World.gravity_active);
        assertEquals(6, World.global_temperature);
        assertEquals(0, Node.viscocity);
        assertTrue(FrEnd.three_d);
        assertTrue(FrEnd.check_collisions);
        assertFalse(FrEnd.links_disabled);
        assertFalse(FrEnd.collide_self_only);
        assertFalse(FrEnd.continuously_centre);
        assertFalse(FrEnd.node_growth);
        assertTrue(
            ContextMananger.getNodeManager().electrostatic.charge_active);

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
        assertFalse(panel.checkbox_collide_self_only.getState(),
            "collide self only");
        assertFalse(panel.checkbox_continuously_centre.getState(),
            "continuously centre");
        assertFalse(panel.checkbox_node_growth.getState(), "node growth");
      });
    } finally {
      SwingUtilities.invokeAndWait(() -> {
        for (final Frame frame : Frame.getFrames()) {
          frame.dispose();
        }
      });
    }
  }

  /**
   * The boot-time model load applies its universe settings asynchronously,
   * seconds after FrEnd.main returns. Wait until the world has gone quiet
   * before scrambling anything, or the load will stomp the test mid-flight.
   */
  private static void waitForBootModelLoadToSettle() throws Exception {
    int last_gravity = Integer.MIN_VALUE;
    int last_nodes = -1;
    long last_change = System.currentTimeMillis();
    final long deadline = last_change + 60000;
    while (System.currentTimeMillis() < deadline) {
      final int[] state = new int[2];
      SwingUtilities.invokeAndWait(() -> {
        state[0] = World.gravity_strength;
        state[1] = ContextMananger.getNodeManager().element.size();
      });
      if (state[0] != last_gravity || state[1] != last_nodes) {
        last_gravity = state[0];
        last_nodes = state[1];
        last_change = System.currentTimeMillis();
      }
      if (state[1] > 0 && System.currentTimeMillis() - last_change > 2000) {
        return;
      }
      Thread.sleep(250);
    }
  }
}
