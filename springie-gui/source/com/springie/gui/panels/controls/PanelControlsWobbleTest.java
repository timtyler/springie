// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.components.TabbedPanel;
import com.springie.metrics.DampingOptimizer;
import com.springie.metrics.WobbleMeter;
import com.springie.world.World;
import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The Wobble page must start the meter when shown and stop it when hidden,
 * and the meter must report the average link-length oscillation as a
 * percentage, ignoring one-off spikes.
 */
class PanelControlsWobbleTest {

  private static void bootApp() throws Exception {
    SwingUtilities.invokeAndWait(() -> FrEnd.main(new String[0]));
    final long loaded_by = System.currentTimeMillis() + 30000;
    while (ContextManager.getLinkManager() == null
        || ContextManager.getLinkManager().element.isEmpty()) {
      if (System.currentTimeMillis() > loaded_by) {
        throw new IllegalStateException("startup model did not load");
      }
      Thread.sleep(50);
    }
  }

  private static void disposeFrames() {
    for (final Frame frame : Frame.getFrames()) {
      frame.dispose();
    }
  }

  @BeforeAll
  static void boot() throws Exception {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    bootApp();
  }

  @AfterAll
  static void dispose() {
    WobbleMeter.setListener(null);
    WobbleMeter.setActive(false);
    disposeFrames();
  }

  @Test
  void showingTheWobblePageStartsTheMeter() throws Exception {
    final Frame[] frame_holder = new Frame[1];
    final TabbedPanel[] tabs_holder = new TabbedPanel[1];
    SwingUtilities.invokeAndWait(() -> {
      final Frame frame = new Frame();
      final TabbedPanel tabs = new TabbedPanel();
      tabs.add("A", new Label("a"));
      tabs.add("Wobble", new PanelControlsWobble().panel);
      frame.add(tabs);
      frame.setVisible(true);
      frame_holder[0] = frame;
      tabs_holder[0] = tabs;
    });
    SwingUtilities.invokeAndWait(() -> {
      ((CardLayout) tabs_holder[0].getLayout()).show(tabs_holder[0], "Wobble");
    });
    SwingUtilities.invokeAndWait(() -> {
    });
    assertTrue(WobbleMeter.isActive());
    SwingUtilities.invokeAndWait(() -> {
      ((CardLayout) tabs_holder[0].getLayout()).show(tabs_holder[0], "A");
    });
    SwingUtilities.invokeAndWait(() -> {
    });
    assertFalse(WobbleMeter.isActive());
    SwingUtilities.invokeAndWait(() -> {
      frame_holder[0].dispose();
    });
  }

  @Test
  void wobbleLabelUpdatesAtMostOnceASecond() throws Exception {
    final PanelControlsWobble[] panel_holder = new PanelControlsWobble[1];
    SwingUtilities.invokeAndWait(() -> {
      final PanelControlsWobble wobble_panel = new PanelControlsWobble();
      panel_holder[0] = wobble_panel;
      wobble_panel.wobbleUpdated(123, true);
      assertEquals("Wobble (selected links): 12.3%",
          wobble_panel.label_wobble.getText());
      wobble_panel.wobbleUpdated(456, true);
      assertEquals("Wobble (selected links): 12.3%",
          wobble_panel.label_wobble.getText());
    });
    Thread.sleep(1100);
    SwingUtilities.invokeAndWait(() -> {
      panel_holder[0].wobbleUpdated(456, false);
      assertEquals("Wobble: 45.6%", panel_holder[0].label_wobble.getText());
    });
  }

  @Test
  void optimiseButtonDisablesWhileRunning() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final PanelControlsWobble wobble_panel = FrEnd.panel_controls_wobble;
      try {
        for (final ActionListener listener
            : wobble_panel.button_optimise_damping.getActionListeners()) {
          listener.actionPerformed(new ActionEvent(
              wobble_panel.button_optimise_damping,
              ActionEvent.ACTION_PERFORMED, "press"));
        }
        assertTrue(DampingOptimizer.isRunning());
        assertFalse(wobble_panel.button_optimise_damping.isEnabled());
      } finally {
        DampingOptimizer.abort();
      }
      assertFalse(DampingOptimizer.isRunning());
      assertTrue(wobble_panel.button_optimise_damping.isEnabled());
    });
  }

  private static void scaleAllNodes(final NodeManager node_manager,
      final int[] orig_x, final int[] orig_y, final int[] orig_z,
      final int numerator, final int denominator) {
    final int number_of_nodes = node_manager.element.size();
    for (int temp = 0; temp < number_of_nodes; temp++) {
      final Node node = (Node) node_manager.element.get(temp);
      node.pos.x = (int) (((long) orig_x[temp] * numerator) / denominator);
      node.pos.y = (int) (((long) orig_y[temp] * numerator) / denominator);
      node.pos.z = (int) (((long) orig_z[temp] * numerator) / denominator);
    }
  }

  @Test
  void wobbleIsAverageLengthOscillationIgnoringSpikes() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final NodeManager node_manager = ContextManager.getNodeManager();
      final LinkManager link_manager = ContextManager.getLinkManager();
      final int number_of_nodes = node_manager.element.size();
      final int[] orig_x = new int[number_of_nodes];
      final int[] orig_y = new int[number_of_nodes];
      final int[] orig_z = new int[number_of_nodes];
      for (int temp = 0; temp < number_of_nodes; temp++) {
        final Node node = (Node) node_manager.element.get(temp);
        orig_x[temp] = node.pos.x;
        orig_y[temp] = node.pos.y;
        orig_z[temp] = node.pos.z;
      }
      final boolean was_paused = World.paused;
      World.paused = true;
      try {
        WobbleMeter.setActive(true);
        for (int tick = 0; tick < WobbleMeter.WINDOW_TICKS; tick++) {
          if (tick % 2 == 0) {
            scaleAllNodes(node_manager, orig_x, orig_y, orig_z, 10, 10);
          } else {
            scaleAllNodes(node_manager, orig_x, orig_y, orig_z, 11, 10);
          }
          WobbleMeter.sampleIfActive(link_manager);
        }
        final int oscillating = WobbleMeter.getWobbleTenthsPercent();
        assertTrue(oscillating >= 85 && oscillating <= 105,
            "wobble was " + oscillating + " tenths, expected about 95");
        scaleAllNodes(node_manager, orig_x, orig_y, orig_z, 1, 1);
        for (int tick = 0; tick < WobbleMeter.WINDOW_TICKS; tick++) {
          WobbleMeter.sampleIfActive(link_manager);
        }
        assertEquals(0, WobbleMeter.getWobbleTenthsPercent());
        scaleAllNodes(node_manager, orig_x, orig_y, orig_z, 2, 1);
        WobbleMeter.sampleIfActive(link_manager);
        scaleAllNodes(node_manager, orig_x, orig_y, orig_z, 1, 1);
        for (int tick = 0; tick < WobbleMeter.WINDOW_TICKS - 1; tick++) {
          WobbleMeter.sampleIfActive(link_manager);
        }
        assertEquals(0, WobbleMeter.getWobbleTenthsPercent());
      } finally {
        scaleAllNodes(node_manager, orig_x, orig_y, orig_z, 1, 1);
        World.paused = was_paused;
        WobbleMeter.setActive(false);
      }
    });
  }
}
