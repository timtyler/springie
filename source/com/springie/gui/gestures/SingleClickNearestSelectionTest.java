// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.gui.GuiTestSupport;
import com.springie.render.Coords;

/**
 * A single click must select at most one item: the one nearest to the
 * viewer. The click used to be offered to the node, link and face pickers
 * in turn with no arbitration between the types, so a click overlapping
 * e.g. a node and a face selected both -- and when several types matched,
 * the node always won regardless of depth.
 *
 * <p>Each test builds a tiny deterministic model (the boot model is
 * cleared first), clicks the projected centre of the target, and asserts
 * that exactly one item -- the nearest -- ends up selected.
 */
class SingleClickNearestSelectionTest {

  @BeforeAll
  static void boot() throws Exception {
    GuiTestSupport.bootApp();
  }

  @AfterAll
  static void dispose() throws Exception {
    GuiTestSupport.disposeFrames();
  }

  // Depths in internal coordinates; smaller z is nearer the viewer.
  private static int zNear() {
    return Coords.getInternalFromPixelCoords(320);
  }

  private static int zFar() {
    return Coords.getInternalFromPixelCoords(1820);
  }

  private static int centreX() {
    return Coords.x_pixels / 2;
  }

  private static int centreY() {
    return Coords.y_pixels / 2;
  }

  private static void clearModel() {
    ContextManager.getFaceManager().element.clear();
    ContextManager.getLinkManager().element.clear();
    ContextManager.getNodeManager().element.clear();
    FrEnd.dragged_element = null;
    FrEnd.currently_dragging = false;
    FrEnd.perform_actions.drag_box_manager.drag_box_start = null;
    FrEnd.perform_actions.drag_box_manager.drag_box_end = null;
  }

  private static Node makeNode(int screenX, int screenY, int z,
      int radiusPixels) {
    final NodeManager nodes = ContextManager.getNodeManager();
    final int mx = Coords.inverseXCoords(screenX << Coords.shift, z);
    final int my = Coords.inverseYCoords(screenY << Coords.shift, z);
    final NodeType type = nodes.node_type_factory.getNew();
    type.hidden = false;
    type.selected = false;
    type.radius = radiusPixels << Coords.shift;
    final Clazz clazz = nodes.clazz_factory.getNew(0xFFFFFFFF);
    return nodes.addNewAgent(new Point3D(mx, my, z), clazz, type);
  }

  private static Face makeFace(Node... quad) {
    final FaceManager faces = ContextManager.getFaceManager();
    final ArrayList<Node> list = new ArrayList<>();
    for (final Node n : quad) {
      list.add(n);
    }
    final Face face = faces.setPolygon(list,
        faces.face_type_factory.getNew(),
        ContextManager.getNodeManager().clazz_factory.getNew(0xFFFFFFFF));
    face.type.hidden = false;
    face.type.selected = false;
    return face;
  }

  private static Link makeLink(Node a, Node b) {
    final LinkManager links = ContextManager.getLinkManager();
    final Link link = links.setLink(a, b,
        links.link_type_factory.getNew(10 << Coords.shift, 30),
        ContextManager.getNodeManager().clazz_factory.getNew(0xFFFFFFFF));
    link.type.hidden = false;
    link.type.selected = false;
    return link;
  }

  private static boolean[] saveBoxes() {
    return new boolean[] {
        FrEnd.panel_edit_select_main.checkbox_select_nodes.getState(),
        FrEnd.panel_edit_select_main.checkbox_select_links.getState(),
        FrEnd.panel_edit_select_main.checkbox_select_faces.getState(),
    };
  }

  private static void setBoxes(boolean nodes, boolean links, boolean faces) {
    FrEnd.panel_edit_select_main.checkbox_select_nodes.setState(nodes);
    FrEnd.panel_edit_select_main.checkbox_select_links.setState(links);
    FrEnd.panel_edit_select_main.checkbox_select_faces.setState(faces);
  }

  private static void restoreBoxes(boolean[] boxes) {
    setBoxes(boxes[0], boxes[1], boxes[2]);
  }

  private static int countSelected() {
    int count = 0;
    for (final Node n : ContextManager.getNodeManager().element) {
      if (n.type.selected) {
        count++;
      }
    }
    for (final Link l : ContextManager.getLinkManager().element) {
      if (l.type.selected) {
        count++;
      }
    }
    for (final Face f : ContextManager.getFaceManager().element) {
      if (f.type.selected) {
        count++;
      }
    }
    return count;
  }

  // The selection routines clone element types before flagging them, so
  // selected-ness must be re-read by element identity.
  private static boolean isSelected(Node node) {
    for (final Node n : ContextManager.getNodeManager().element) {
      if (n == node) {
        return n.type.selected;
      }
    }
    return false;
  }

  private static boolean isSelected(Link link) {
    for (final Link l : ContextManager.getLinkManager().element) {
      if (l == link) {
        return l.type.selected;
      }
    }
    return false;
  }

  private static boolean isSelected(Face face) {
    for (final Face f : ContextManager.getFaceManager().element) {
      if (f == face) {
        return f.type.selected;
      }
    }
    return false;
  }

  private static int projectedX(Node n) {
    return Coords.getXCoordsInternal(n.pos.x, n.pos.z);
  }

  private static int projectedY(Node n) {
    return Coords.getYCoordsInternal(n.pos.y, n.pos.z);
  }

  @Test
  void faceNearerThanNodeSelectsOnlyTheFace() throws Exception {
    final boolean[] faceSelected = new boolean[1];
    final boolean[] nodeSelected = new boolean[1];
    final int[] total = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      final boolean[] boxes = saveBoxes();
      try {
        setBoxes(true, false, true);
        clearModel();
        final int cx = centreX();
        final int cy = centreY();
        final Node node = makeNode(cx, cy, zFar(), 8);
        final Face face = makeFace(
            makeNode(cx - 70, cy - 70, zNear(), 1),
            makeNode(cx + 70, cy - 70, zNear(), 1),
            makeNode(cx + 70, cy + 70, zNear(), 1),
            makeNode(cx - 70, cy + 70, zNear(), 1));

        final int clickX = projectedX(node);
        final int clickY = projectedY(node);
        ContextManager.getNodeManager().nodeAndLinkRenderDummy();
        assertSame(node,
            ContextManager.getNodeManager().isThereOne(clickX, clickY),
            "scenario broken: the node picker must see the far node");
        assertSame(face,
            ContextManager.getFaceManager().isThereOne(clickX, clickY),
            "scenario broken: the face picker must see the near face");

        // The normal select tool: drag_is_possible = true.
        new PerformSelection().performSelection(clickX, clickY, true);

        faceSelected[0] = isSelected(face);
        nodeSelected[0] = isSelected(node);
        total[0] = countSelected();
      } finally {
        clearModel();
        restoreBoxes(boxes);
      }
    });
    assertTrue(faceSelected[0],
        "the nearer face must be selected");
    assertFalse(nodeSelected[0],
        "the farther node must not be selected as well");
    assertEquals(1, total[0],
        "a single click must select exactly one item");
  }

  @Test
  void nodeNearerThanFaceSelectsOnlyTheNode() throws Exception {
    final boolean[] faceSelected = new boolean[1];
    final boolean[] nodeSelected = new boolean[1];
    final int[] total = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      final boolean[] boxes = saveBoxes();
      try {
        setBoxes(true, false, true);
        clearModel();
        final int cx = centreX();
        final int cy = centreY();
        final Node node = makeNode(cx, cy, zNear(), 8);
        final Face face = makeFace(
            makeNode(cx - 70, cy - 70, zFar(), 1),
            makeNode(cx + 70, cy - 70, zFar(), 1),
            makeNode(cx + 70, cy + 70, zFar(), 1),
            makeNode(cx - 70, cy + 70, zFar(), 1));

        final int clickX = projectedX(node);
        final int clickY = projectedY(node);
        ContextManager.getNodeManager().nodeAndLinkRenderDummy();
        assertSame(node,
            ContextManager.getNodeManager().isThereOne(clickX, clickY),
            "scenario broken: the node picker must see the near node");
        assertSame(face,
            ContextManager.getFaceManager().isThereOne(clickX, clickY),
            "scenario broken: the face picker must see the far face");

        new PerformSelection().performSelection(clickX, clickY, true);

        faceSelected[0] = isSelected(face);
        nodeSelected[0] = isSelected(node);
        total[0] = countSelected();
      } finally {
        clearModel();
        restoreBoxes(boxes);
      }
    });
    assertTrue(nodeSelected[0],
        "the nearer node must be selected");
    assertFalse(faceSelected[0],
        "the farther face must not be selected as well");
    assertEquals(1, total[0],
        "a single click must select exactly one item");
  }

  @Test
  void linkNearerThanFaceSelectsOnlyTheLink() throws Exception {
    final boolean[] faceSelected = new boolean[1];
    final boolean[] linkSelected = new boolean[1];
    final int[] total = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      final boolean[] boxes = saveBoxes();
      try {
        setBoxes(false, true, true);
        clearModel();
        final int cx = centreX();
        final int cy = centreY();
        final Node endA = makeNode(cx - 90, cy, zNear(), 1);
        final Node endB = makeNode(cx + 90, cy, zNear(), 1);
        final Link link = makeLink(endA, endB);
        final Face face = makeFace(
            makeNode(cx - 70, cy - 70, zFar(), 1),
            makeNode(cx + 70, cy - 70, zFar(), 1),
            makeNode(cx + 70, cy + 70, zFar(), 1),
            makeNode(cx - 70, cy + 70, zFar(), 1));

        // Click the link's projected midpoint (both ends share zNear, so
        // the projection is linear in x/y and this lands on the strut).
        final int midX = (endA.pos.x + endB.pos.x) / 2;
        final int midY = (endA.pos.y + endB.pos.y) / 2;
        final int clickX = Coords.getXCoordsInternal(midX, zNear());
        final int clickY = Coords.getYCoordsInternal(midY, zNear());
        ContextManager.getNodeManager().nodeAndLinkRenderDummy();
        assertSame(link,
            ContextManager.getLinkManager().isThereOne(clickX, clickY),
            "scenario broken: the link picker must see the near link");
        assertSame(face,
            ContextManager.getFaceManager().isThereOne(clickX, clickY),
            "scenario broken: the face picker must see the far face");

        new PerformSelection().performSelection(clickX, clickY, true);

        faceSelected[0] = isSelected(face);
        linkSelected[0] = isSelected(link);
        total[0] = countSelected();
      } finally {
        clearModel();
        restoreBoxes(boxes);
      }
    });
    assertTrue(linkSelected[0],
        "the nearer link must be selected");
    assertFalse(faceSelected[0],
        "the farther face must not be selected as well");
    assertEquals(1, total[0],
        "a single click must select exactly one item");
  }

  @Test
  void nearerOfTwoOverlappingNodesWins() throws Exception {
    final boolean[] nearSelected = new boolean[1];
    final boolean[] farSelected = new boolean[1];
    final int[] total = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      final boolean[] boxes = saveBoxes();
      try {
        setBoxes(true, false, false);
        clearModel();
        final int cx = centreX();
        final int cy = centreY();
        final Node near = makeNode(cx, cy, zNear(), 8);
        final Node far = makeNode(cx, cy, zFar(), 8);

        final int clickX = projectedX(near);
        final int clickY = projectedY(near);

        new PerformSelection().performSelection(clickX, clickY, true);

        nearSelected[0] = isSelected(near);
        farSelected[0] = isSelected(far);
        total[0] = countSelected();
      } finally {
        clearModel();
        restoreBoxes(boxes);
      }
    });
    assertTrue(nearSelected[0],
        "the nearer node must be selected");
    assertFalse(farSelected[0],
        "the farther node must not be selected");
    assertEquals(1, total[0],
        "a single click must select exactly one item");
  }

  @Test
  void clickOnEmptySpaceSelectsNothing() throws Exception {
    final int[] total = new int[1];
    SwingUtilities.invokeAndWait(() -> {
      FrEnd.paused = true;
      final boolean[] boxes = saveBoxes();
      try {
        setBoxes(true, true, true);
        clearModel();
        final Node node = makeNode(centreX(), centreY(), zNear(), 8);
        node.type.selected = true;

        // Top-left corner: the cleared model has nothing there.
        new PerformSelection().performSelection(10 << Coords.shift,
            10 << Coords.shift, true);

        total[0] = countSelected();
      } finally {
        clearModel();
        restoreBoxes(boxes);
      }
    });
    assertEquals(0, total[0],
        "clicking empty space must deselect everything and select nothing");
  }
}
