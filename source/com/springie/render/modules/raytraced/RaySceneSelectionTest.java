// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;

/**
 * Node selection in the ray-traced scene: like the default renderer,
 * a selected node keeps its class colour and gets a red billboard
 * ring; unselected nodes get no ring.
 *
 * <p>Needs a display like RaySceneCableTest: building the scene reads
 * FrEnd.render_nodes, and FrEnd's class initialisation creates AWT
 * components.
 */
class RaySceneSelectionTest {
  private static final int NODE_COLOUR = 0x123456;

  private static final int RADIUS = 512;

  private NodeManager manager;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    this.manager.element.add(node(25600, 25600, 0, true));
    this.manager.element.add(node(0, 0, 0, false));
  }

  private static Node node(int x, int y, int z, boolean selected) {
    final Node node = new Node(new Point3D(x, y, z), 42,
        new NodeTypeFactory());
    node.clazz = new Clazz(NODE_COLOUR);
    node.type.selected = selected;
    node.type.radius = RADIUS;
    return node;
  }

  @Test
  void selectedNodeKeepsItsClassColour() {
    final Primitive[] primitives = RayScene.build(this.manager);
    assertEquals(2, primitives.length);
    // The selected node is not recoloured; the ring marks it.
    assertEquals(NODE_COLOUR, primitives[0].getColour());
    assertEquals(NODE_COLOUR, primitives[1].getColour());
  }

  @Test
  void oneRingPerSelectedNode() {
    final RTRing[] rings = RayScene.selectionRings(this.manager, 25600,
        25600, -196608);
    assertEquals(1, rings.length);
  }

  @Test
  void ringSitsJustOutsideTheNode() {
    // The ring plane is z = 0 with normal (0, 0, 1): a ray from the
    // eye, offset sideways, pierces the plane exactly its offset from
    // the node centre. Mid-annulus hits; the hole misses.
    final double world_per_pixel = Coords.shift_constant_z
        + (0 >> Coords.shift_z);
    final double mid = RADIUS + 6.0 * world_per_pixel;
    final double in_hole = RADIUS + 2.0 * world_per_pixel;

    final RTRing[] rings = RayScene.selectionRings(this.manager, 25600,
        25600, -196608);
    final Hit hit = new Hit();
    final Ray ray = new Ray();
    ray.ox = 25600 + mid;
    ray.oy = 25600;
    ray.oz = -196608;
    ray.dx = 0;
    ray.dy = 0;
    ray.dz = 1;
    assertTrue(rings[0].intersect(ray, hit),
        "a ray through the annulus must hit the ring");
    assertEquals(196608.0, hit.t, 1e-6);

    ray.ox = 25600 + in_hole;
    assertTrue(!rings[0].intersect(ray, new Hit()),
        "a ray through the hole must miss the ring");
  }
}
