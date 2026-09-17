// This code has been placed into the public domain by its author.
package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.GuiTestSupport;
import com.springie.messages.commands.SpiderTankDemoMessage;
import com.springie.muscles.Sensors;
import org.junit.jupiter.api.Test;

/**
 * UI-path regression test for the spider tank.
 *
 * <p>Boots the real FrEnd UI, loads the spider via the exact
 * SpiderTankDemoMessage the Models &gt; Demos menu uses, lets the live
 * animation thread run it for 10 seconds, and asserts it holds together.
 * The old tube-bodied spider disintegrated within 3 seconds on this path
 * (max speed ~9000px/tick); the rebuilt plate-bodied spider holds
 * (max speed &lt; 1px/tick, max passive strain &lt; 0.03).
 */
class SpiderTankLiveUiTest {
  @Test
  void spiderSurvivesTenSecondsLiveInUi() throws Exception {
    GuiTestSupport.bootApp();
    FrEnd.new_message_manager.add(new SpiderTankDemoMessage());
    // Wait for the demo to load (16 nodes: plate+ridge+turret+legs).
    NodeManager node_manager = null;
    for (int i = 0; i < 100; i++) {
      Thread.sleep(100);
      node_manager = ContextManager.getNodeManager();
      if (node_manager != null && node_manager.element.size() == 16) {
        break;
      }
    }
    if (node_manager == null || node_manager.element.size() != 16) {
      fail("Spider demo did not load via the menu message path");
    }
    final LinkManager link_manager = node_manager.getLinkManager();
    // Settle, then let the live animation thread run it.
    FrEnd.paused = true;
    Thread.sleep(500);
    FrEnd.paused = false;
    double worst_speed = 0;
    double worst_strain = 0;
    final NodeManager nm = node_manager;
    for (int s = 0; s < 10; s++) {
      Thread.sleep(1000);
      double max_speed = 0;
      double max_strain = 0;
      synchronized (ContextManager.class) {
        for (int i = 0; i < nm.element.size(); i++) {
          final Node node = (Node) nm.element.get(i);
          final double vx = node.velocity.x / 4096.0;
          final double vy = node.velocity.y / 4096.0;
          final double vz = node.velocity.z / 4096.0;
          final double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
          if (speed > max_speed) {
            max_speed = speed;
          }
        }
        for (int i = 0; i < link_manager.element.size(); i++) {
          final double strain =
              Math.abs(Sensors.strain((Link) link_manager.element.get(i)) / 4096.0);
          if (strain > max_strain) {
            max_strain = strain;
          }
        }
      }
      if (max_speed > worst_speed) {
        worst_speed = max_speed;
      }
      if (max_strain > worst_strain) {
        worst_strain = max_strain;
      }
      // Bail early on disintegration: no point watching it fly apart.
      if (worst_speed > 250 || worst_strain > 1.5) {
        break;
      }
    }
    FrEnd.paused = true;
    assertTrue(worst_speed < 50,
        "Spider disintegrated live in the UI: worst speed " + worst_speed + "px/tick");
    assertTrue(worst_strain < 0.3,
        "Spider over-strained live in the UI: worst strain " + worst_strain);
  }
}
