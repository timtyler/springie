// This program has been placed into the public domain by its author.

package com.springie.elements.nodes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.geometry.Point3D;
import com.springie.gui.GuiTestSupport;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.raytraced.ModularRendererRaytraced;

/**
 * The depth-first node sort must run for the rasterizer renderers when
 * "Render deepest objects first" is on, and must be skipped entirely
 * while the ray-traced renderer is active: the ray tracer resolves
 * occlusion per ray through its BVH and never reads the sorted order,
 * so the sort (an O(n^2) bubble sort) is pure cost there -- and it runs
 * inside nodeAndLinkRenderDummy() on every selection gesture.
 */
class NodeManagerSortIndexTest {

  private boolean saved_redraw_deepest_first;

  private ModularRendererBase saved_renderer;

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  @BeforeEach
  void saveAndBuildModel() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      this.saved_redraw_deepest_first = FrEnd.redraw_deepest_first;
      this.saved_renderer = RendererDelegator.renderer;
      buildThreeNodeModel();
    });
  }

  @AfterEach
  void restore() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.redraw_deepest_first = this.saved_redraw_deepest_first;
      RendererDelegator.renderer = this.saved_renderer;
    });
  }

  /** Three nodes in element order far, near, middle by z. */
  private static void buildThreeNodeModel() {
    final NodeManager nodes = ContextManager.getNodeManager();
    nodes.element.clear();
    makeNode(nodes, 900);
    makeNode(nodes, 100);
    makeNode(nodes, 500);
  }

  private static Node makeNode(NodeManager nodes, int z) {
    final NodeType type = nodes.node_type_factory.getNew();
    type.hidden = false;
    final Clazz clazz = nodes.clazz_factory.getNew(0xFFFFFFFF);
    return nodes.addNewAgent(new Point3D(0, 0, z), clazz, type);
  }

  private static int[] depthIndexCopy() {
    final NodeManager nodes = ContextManager.getNodeManager();
    final int[] index = nodes.node_depth_index;
    final int[] copy = new int[nodes.element.size()];
    System.arraycopy(index, 0, copy, 0, copy.length);
    return copy;
  }

  private static int zAt(int elementIndex) {
    final Node node = (Node) ContextManager.getNodeManager()
        .element.get(elementIndex);
    return node.pos.z;
  }

  @Test
  void sortRunsForThePolygonRendererWhenDeepestFirstIsOn()
      throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = new ModularRendererNew();
      FrEnd.redraw_deepest_first = true;
      ContextManager.getNodeManager().sortIndex();
    });
    // Ascending by z: the caller draws the index from the end
    // backwards, deepest first. Element order is far(900),
    // near(100), middle(500): sorted is near, middle, far.
    assertArrayEquals(new int[] {1, 2, 0}, depthIndexCopy());
  }

  @Test
  void sortIsSkippedWhileTheRaytracedRendererIsActive() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = new ModularRendererRaytraced();
      FrEnd.redraw_deepest_first = true;
      ContextManager.getNodeManager().sortIndex();
    });
    // The identity index set up for the three nodes is untouched: the
    // sort never ran.
    assertArrayEquals(new int[] {0, 1, 2}, depthIndexCopy());
  }

  @Test
  void sortIsSkippedWhenDeepestFirstIsOff() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = new ModularRendererNew();
      FrEnd.redraw_deepest_first = false;
      ContextManager.getNodeManager().sortIndex();
    });
    assertArrayEquals(new int[] {0, 1, 2}, depthIndexCopy());
  }

  @Test
  void sortedIndexOrdersNodesByAscendingDepth() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = new ModularRendererNew();
      FrEnd.redraw_deepest_first = true;
      ContextManager.getNodeManager().sortIndex();
    });
    final int[] index = depthIndexCopy();
    for (int i = 1; i < index.length; i++) {
      assertTrue(zAt(index[i - 1]) <= zAt(index[i]),
          "the depth index must order nodes ascending by z");
    }
  }
}
