// This program has been placed into the public domain by its author.
package com.springie.elements.nodes;
import com.springie.world.World;

import com.springie.context.ContextManager;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.geometry.Coords;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * While the user is dragging something, the per-frame continuous
 * centering must stand down: otherwise the centering snap yanks the
 * bounding box back every frame and the dragged link vibrates instead
 * of following the pointer. On release, centering resumes.
 */
class NodeManagerCenteringDragTest {

  private NodeManager manager;
  private boolean saved_x;
  private boolean saved_y;
  private boolean saved_z;
  private boolean saved_dragging;
  private boolean saved_collisions;
  private boolean saved_paused;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.saved_x = World.continuously_centre_x;
    this.saved_y = World.continuously_centre_y;
    this.saved_z = World.continuously_centre_z;
    this.saved_dragging = World.currently_dragging;
    this.saved_collisions = World.check_collisions;
    this.saved_paused = World.paused;
    World.continuously_centre_x = true;
    World.continuously_centre_y = false;
    World.continuously_centre_z = false;

    this.manager = new NodeManager();
    // Off-centre on x.
    this.manager.element.add(new Node(new Point3D(1000, 3000, 5000), 42,
      new NodeTypeFactory()));
    this.manager.element.add(new Node(new Point3D(2000, 4000, 6000), 42,
      new NodeTypeFactory()));
  }

  @AfterEach
  void tearDown() {
    World.continuously_centre_x = this.saved_x;
    World.continuously_centre_y = this.saved_y;
    World.continuously_centre_z = this.saved_z;
    World.currently_dragging = this.saved_dragging;
    World.check_collisions = this.saved_collisions;
    World.paused = this.saved_paused;
  }

  @Test
  void draggingSuspendsCentering() {
    World.currently_dragging = true;

    this.manager.agentExpansion();

    assertEquals(1000, node(0).pos.x, "node 0 x untouched while dragging");
    assertEquals(2000, node(1).pos.x, "node 1 x untouched while dragging");
  }

  @Test
  void releaseResumesCentering() {
    World.currently_dragging = false;

    this.manager.agentExpansion();

    final int screen_x = Coords.getInternalFromPixelCoords(Coords.x_pixels);
    final int offset_x = (screen_x - 1000 - 2000) >> 1;
    assertEquals(1000 + offset_x, node(0).pos.x, "node 0 x centred");
    assertEquals(2000 + offset_x, node(1).pos.x, "node 1 x centred");
  }

  /**
   * The demo models switch node-node collisions off, which used to
   * gate the whole agentExpansion() call -- so "Continuously center"
   * silently did nothing for every demo. The follow-cam must run on
   * the per-tick path regardless of the collision flag.
   */
  @Test
  void centeringRunsWhenCollisionsAreOff() {
    World.currently_dragging = false;
    World.check_collisions = false;
    World.paused = false;
    node(0).velocity = new Vector3D(0, 0, 0);
    node(1).velocity = new Vector3D(0, 0, 0);
    final NodeManager saved_manager = ContextManager.getNodeManager();
    ContextManager.setNodeManager(this.manager);
    try {
      this.manager.nodeAndLinkUpdate();
    } finally {
      ContextManager.setNodeManager(saved_manager);
    }

    final int screen_x = Coords.getInternalFromPixelCoords(Coords.x_pixels);
    final int offset_x = (screen_x - 1000 - 2000) >> 1;
    assertEquals(1000 + offset_x, node(0).pos.x,
        "node 0 x centred with collisions off");
    assertEquals(2000 + offset_x, node(1).pos.x,
        "node 1 x centred with collisions off");
  }

  private Node node(final int index) {
    return (Node) this.manager.element.get(index);
  }
}
