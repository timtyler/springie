// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.GuiTestSupport;
import com.springie.render.Coords;

/**
 * Drag-box selection must use the gesture's own coordinates (press point
 * and release point), not the renderer's paint-cached box. The cache is
 * only updated when a paint happens; a release processed before the next
 * paint would otherwise select with a stale rectangle -- or with the
 * (0,0)-(0,0) box, selecting nothing at all.
 *
 * <p>This test performs the gesture without any paint in between: it sets
 * the press point and calls terminate() with the release point directly,
 * leaving the renderer's cached box at its reset (0,0) state. On the old
 * code the node is not selected; on the fixed code it is.
 */
class DragBoxSelectionTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @Test
  void terminateUsesReleaseCoordinatesNotThePaintCachedBox() throws Exception {
    final boolean[] selected = new boolean[1];
    final boolean[] nodeBox = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      nodeBox[0] = FrEnd.panel_edit_select_main.checkbox_select_nodes
          .getState();
      FrEnd.panel_edit_select_main.checkbox_select_nodes.setState(true);

      final NodeManager nodes = ContextManager.getNodeManager();
      final List<?> elements = nodes.element;
      // Pick the node nearest the canvas centre: it is on screen with
      // a comfortable margin for a small box around it.
      Node best = null;
      int best_dist = Integer.MAX_VALUE;
      int best_x = 0;
      int best_y = 0;
      for (int i = 0; i < elements.size(); i++) {
        final Node n = (Node) elements.get(i);
        final int sx = Coords.getXCoords(n.pos.x, n.pos.z);
        final int sy = Coords.getYCoords(n.pos.y, n.pos.z);
        final int dx = sx - Coords.x_pixels / 2;
        final int dy = sy - Coords.y_pixels / 2;
        final int d = dx * dx + dy * dy;
        if (d < best_dist) {
          best_dist = d;
          best = n;
          best_x = sx;
          best_y = sy;
        }
      }

      // Deselect everything first.
      for (int i = 0; i < elements.size(); i++) {
        ((Node) elements.get(i)).type.selected = false;
      }

      final DragBoxManager manager =
          FrEnd.perform_actions.drag_box_manager;
      // Simulate the press: drag_box_start set, drag_box_end left null,
      // and crucially no paint happens, so the renderer's cached box
      // is explicitly left at its reset (0,0) state -- not the gesture's
      // box. The old code selected with this stale cache.
      final int s = Coords.shift;
      final com.springie.render.RendererDragBox cached =
          ContextManager.getNodeManager().renderer.renderer_drag_box;
      cached.min = new Point(0, 0);
      cached.max = new Point(0, 0);
      manager.drag_box_start =
          new Point((best_x - 30) << s, (best_y - 30) << s);
      manager.drag_box_end = null;
      // Release 60x60 pixels around the node, in internal coordinates.
      manager.terminate((best_x + 30) << s, (best_y + 30) << s);

      // terminate() clones the node types before selecting, so re-fetch
      // the node's current type by identity.
      for (int i = 0; i < elements.size(); i++) {
        if (elements.get(i) == best) {
          selected[0] = ((Node) elements.get(i)).type.selected;
          break;
        }
      }

      FrEnd.panel_edit_select_main.checkbox_select_nodes
          .setState(nodeBox[0]);
      // Clean up the gesture state for other tests.
      manager.drag_box_start = null;
      manager.drag_box_end = null;
    });
    assertTrue(selected[0],
        "terminate() must select the node inside the press/release box");
  }

  @Test
  void terminateSelectsNothingWhenBoxMissesEveryNode() throws Exception {
    final boolean[] any_selected = new boolean[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      final boolean nodeBox =
          FrEnd.panel_edit_select_main.checkbox_select_nodes.getState();
      FrEnd.panel_edit_select_main.checkbox_select_nodes.setState(true);

      final NodeManager nodes = ContextManager.getNodeManager();
      final List<?> elements = nodes.element;
      for (int i = 0; i < elements.size(); i++) {
        ((Node) elements.get(i)).type.selected = false;
      }

      final DragBoxManager manager =
          FrEnd.perform_actions.drag_box_manager;
      // A 10x10 box in the top-left corner, where the model never is.
      final int s = Coords.shift;
      manager.drag_box_start = new Point(5 << s, 5 << s);
      manager.drag_box_end = null;
      manager.terminate(15 << s, 15 << s);

      for (int i = 0; i < elements.size(); i++) {
        if (((Node) elements.get(i)).type.selected) {
          any_selected[0] = true;
          break;
        }
      }

      FrEnd.panel_edit_select_main.checkbox_select_nodes.setState(nodeBox);
      manager.drag_box_start = null;
      manager.drag_box_end = null;
    });
    assertFalse(any_selected[0],
        "a box that misses every node must select nothing");
  }
}
