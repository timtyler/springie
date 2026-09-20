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
import com.springie.render.RendererDelegator;
import org.junit.jupiter.api.Test;

/**
 * UI-path regression test for the spider tank.
 *
 * <p>Boots the real FrEnd UI, loads the spider via the exact
 * SpiderTankDemoMessage the Models &gt; Demos menu uses, lets the live
 * animation thread run it, and asserts it holds together. The old
 * tube-bodied spider disintegrated within 3 seconds on this path
 * (max speed ~9000px/tick); the rebuilt plate-bodied spider holds
 * (max speed &lt; 1px/tick, max passive strain &lt; 0.03).
 *
 * <p>The run is measured in physics ticks, not wall-clock seconds: the
 * test waits for 4000 ticks -- about what the old 10 wall-second run
 * delivered (measured ~4300 ticks at the normal animation rate,
 * 2026-09-20) -- so the physics exercised is the same; it just stops
 * as soon as the budget is met instead of sleeping out the clock. The
 * animation runs at its normal delay throughout.
 */
class SpiderTankLiveUiTest {
  /**
   * Physics ticks to run: matches the ~4300 ticks the old 10 wall-second
   * run delivered at the normal animation rate (measured 2026-09-20).
   */
  private static final int TICKS = 4000;

  @Test
  void spiderSurvivesTenSecondsLiveInUi() throws Exception {
    GuiTestSupport.bootApp();
    final boolean old_paused = FrEnd.paused;
    try {
      FrEnd.new_message_manager.add(new SpiderTankDemoMessage());
      // Wait for the demo to load (16 nodes: plate+ridge+turret+legs),
      // then for the link build to settle (links land after nodes).
      NodeManager node_manager = null;
      int last_links = -1;
      long links_settled_at = 0;
      final long loaded_by = System.currentTimeMillis() + 60000;
      while (System.currentTimeMillis() < loaded_by) {
        Thread.sleep(25);
        node_manager = ContextManager.getNodeManager();
        if (node_manager == null || node_manager.element.size() != 16) {
          continue;
        }
        final int links =
            node_manager.getLinkManager().element.size();
        if (links != last_links) {
          last_links = links;
          links_settled_at = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - links_settled_at > 150) {
          break;
        }
      }
      if (node_manager == null || node_manager.element.size() != 16) {
        fail("Spider demo did not load via the menu message path");
      }
      final LinkManager link_manager = node_manager.getLinkManager();
      // Let the live animation thread run it at full speed.
      FrEnd.paused = false;
      double worst_speed = 0;
      double worst_strain = 0;
      final NodeManager nm = node_manager;
      final int start_generation = RendererDelegator.generation;
      final long deadline = System.currentTimeMillis() + 120000;
      while (RendererDelegator.generation - start_generation < TICKS) {
        if (System.currentTimeMillis() > deadline) {
          fail("Animation thread did not deliver " + TICKS
              + " ticks in time");
        }
        Thread.sleep(25);
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
    } finally {
      FrEnd.paused = old_paused;
    }
  }
}
