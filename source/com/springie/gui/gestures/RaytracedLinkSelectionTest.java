// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.gui.GuiTestSupport;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.raytraced.ModularRendererRaytraced;

/**
 * Link selection must work in the ray-traced renderer. Picking reads the
 * projected link caches that nodeAndLinkRenderDummy() builds; the tiled
 * renderers do not maintain those caches while drawing, so
 * PerformSelection has to build them on demand for every tiled renderer.
 * Missing the ray-traced renderer from that check made every link click
 * throw a NullPointerException.
 */
class RaytracedLinkSelectionTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @Test
  void clickingALinkSelectsItInRaytracedMode() throws Exception {
    final ModularRendererBase[] previous = new ModularRendererBase[1];
    SwingUtilities.invokeAndWait(() -> {
      previous[0] = RendererDelegator.renderer;
      RendererDelegator.renderer = new ModularRendererRaytraced();
      FrEnd.main_canvas.forceResize();
    });
    try {
      final boolean[] selected = new boolean[1];
      final boolean[] nodeBox = new boolean[1];
      final boolean[] linkBox = new boolean[1];
      SwingUtilities.invokeAndWait(() -> {
        // Isolate link picking so the click cannot be stolen by a node.
        nodeBox[0] = FrEnd.panel_edit_select_main.checkbox_select_nodes
            .getState();
        linkBox[0] = FrEnd.panel_edit_select_main.checkbox_select_links
            .getState();
        FrEnd.panel_edit_select_main.checkbox_select_nodes.setState(false);
        FrEnd.panel_edit_select_main.checkbox_select_links.setState(true);
        // The exact path a mouse click takes in FrEnd.processMouseClick.
        new PerformSelection().performSelection(clickX(), clickY(), false);
        final LinkManager links = ContextManager.getLinkManager();
        for (int i = 0; i < links.element.size(); i++) {
          final Link link = (Link) links.element.get(i);
          if (link.type.selected) {
            selected[0] = true;
          }
        }
        FrEnd.panel_edit_select_main.checkbox_select_nodes
            .setState(nodeBox[0]);
        FrEnd.panel_edit_select_main.checkbox_select_links
            .setState(linkBox[0]);
      });
      assertTrue(selected[0],
          "clicking a link's midpoint must select a link, not throw");
    } finally {
      SwingUtilities.invokeAndWait(() -> {
        RendererDelegator.renderer = previous[0];
      });
    }
  }

  /**
   * The screen midpoint of the first link, in the shifted coordinates the
   * selection code expects. Projected directly through Coords so the test
   * never builds the pick caches itself -- that is PerformSelection's job.
   */
  private static int clickX() {
    return linkMidpoint()[0] << Coords.shift;
  }

  private static int clickY() {
    return linkMidpoint()[1] << Coords.shift;
  }

  private static int[] linkMidpoint() {
    final Link link = (Link) ContextManager.getLinkManager().element.get(0);
    final int x0 = Coords.getXCoords(link.nodes[0].pos.x,
        link.nodes[0].pos.z);
    final int y0 = Coords.getYCoords(link.nodes[0].pos.y,
        link.nodes[0].pos.z);
    final int x1 = Coords.getXCoords(link.nodes[1].pos.x,
        link.nodes[1].pos.z);
    final int y1 = Coords.getYCoords(link.nodes[1].pos.y,
        link.nodes[1].pos.z);
    return new int[] { (x0 + x1) / 2, (y0 + y1) / 2 };
  }
}
