// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Choice;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.demos.CompassPoint;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.GuiTestSupport;

/**
 * The Scalars tab's Compass drop-down sets the N/S/E/W/null heading on
 * every selected node (driving the per-frame universe compass bias), and
 * reflects the selection back: unanimous headings show, mixed shows "-".
 */
class CompassChoiceTest {

  private static List<CompassPoint> saved_compass;

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void disposeFrames() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  private static NodeManager managers() {
    return ContextManager.getNodeManager();
  }

  private static Node firstNode() {
    return (Node) managers().element.get(0);
  }

  private static void selectFirstNodeType() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final NodeManager manager = managers();
      final int n = manager.element.size();
      saved_compass = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        final Node node = (Node) manager.element.get(i);
        node.type.selected = true;
        saved_compass.add(node.compass);
        node.compass = null;
      }
      FrEnd.panel_edit_properties_scalars.resetPanel(true, false, false);
    });
  }

  private static void clearSelection() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final NodeManager manager = managers();
      final int n = manager.element.size();
      for (int i = 0; i < n; i++) {
        final Node node = (Node) manager.element.get(i);
        node.type.selected = false;
        if (saved_compass != null && i < saved_compass.size()) {
          node.compass = saved_compass.get(i);
        }
      }
      saved_compass = null;
      FrEnd.panel_edit_properties_scalars.resetPanel(false, false, false);
    });
  }

  /**
   * Simulates a user picking an item from the drop-down: the native peer
   * sets the Choice state and delivers an ItemEvent; programmatic
   * Choice.select() alone does not notify listeners.
   */
  private static void pickCompass(final String item) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final Choice choice = FrEnd.panel_edit_properties_scalars.choice_compass;
      choice.select(item);
      final ItemEvent event = new ItemEvent(choice,
          ItemEvent.ITEM_STATE_CHANGED, item, ItemEvent.SELECTED);
      for (ItemListener listener : choice.getItemListeners()) {
        listener.itemStateChanged(event);
      }
      FrEnd.new_message_manager.process();
    });
  }

  @Test
  void choiceSetsHeadingOnSelectedNodes() throws Exception {
    selectFirstNodeType();
    try {
      pickCompass("N");

      SwingUtilities.invokeAndWait(() -> {
        assertEquals(CompassPoint.N, firstNode().compass,
            "choosing N must head the selected nodes north");
      });
    } finally {
      clearSelection();
    }
  }

  @Test
  void dashClearsHeading() throws Exception {
    selectFirstNodeType();
    try {
      SwingUtilities.invokeAndWait(() -> {
        firstNode().compass = CompassPoint.S;
      });
      pickCompass("-");

      SwingUtilities.invokeAndWait(() -> {
        assertNull(firstNode().compass,
            "choosing - must clear the heading");
      });
    } finally {
      clearSelection();
    }
  }

  @Test
  void reflectShowsUnanimousHeading() throws Exception {
    selectFirstNodeType();
    try {
      SwingUtilities.invokeAndWait(() -> {
        final NodeManager manager = managers();
        final int n = manager.element.size();
        for (int i = 0; i < n; i++) {
          ((Node) manager.element.get(i)).compass = CompassPoint.E;
        }
        FrEnd.panel_edit_properties_scalars.reflectCompass();
        assertEquals("E",
            FrEnd.panel_edit_properties_scalars.choice_compass
                .getSelectedItem(),
            "unanimous E selection must show E");
      });
    } finally {
      clearSelection();
    }
  }

  @Test
  void reflectShowsDashForMixedSelection() throws Exception {
    selectFirstNodeType();
    try {
      SwingUtilities.invokeAndWait(() -> {
        final NodeManager manager = managers();
        ((Node) manager.element.get(0)).compass = CompassPoint.N;
        ((Node) manager.element.get(1)).compass = CompassPoint.S;
        FrEnd.panel_edit_properties_scalars.reflectCompass();
        assertEquals("-",
            FrEnd.panel_edit_properties_scalars.choice_compass
                .getSelectedItem(),
            "mixed N/S selection must show -");
      });
    } finally {
      clearSelection();
    }
  }
}
