// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.world.Grounding;
import com.springie.world.World;

/**
 * Caterpillar track (Tim, 2026-09-26): starting over from scratch.
 *
 * <p>Three concentric rings with the same center, 21 nodes per ring
 * (63 nodes total), no links yet. The rings form a donut with a
 * triangular cross-section:
 * <ul>
 * <li>Ring 0 (inner): radius 120, z=0 (wheel plane)</li>
 * <li>Ring 1 (outer left): radius 140, z=+30</li>
 * <li>Ring 2 (outer right): radius 140, z=-30</li>
 * </ul>
 * In the (radial, z) plane the three rings sit at (120,0), (140,+30),
 * (140,-30): an isosceles triangle.
 *
 * <p>Node order: ring0[0..20], ring1[0..20], ring2[0..20] (element
 * indices 0-20, 21-41, 42-62). buildAt returns ring0[0].
 */
public final class CaterpillarTrackDemo {
  private CaterpillarTrackDemo() {
  }

  /** Nodes per ring. */
  public static final int NODES_PER_RING = 21;

  /** Inner ring radius, in pixels. */
  public static final int INNER_RADIUS_PX = 120;

  /** Outer ring radius, in pixels. */
  public static final int OUTER_RADIUS_PX = 140;

  /** Outer ring z offset (half the track width), in pixels. */
  public static final int HALF_WIDTH_PX = 30;

  /** Node size, in pixels (physical radius). */
  public static int node_size_px = 16;

  /** Ground friction. */
  public static int friction = 100;

  /** Log mass for all nodes. */
  public static int log_mass = 0;

  /**
   * Builds the three rings centred at the given x (pixels), resting
   * on the ground. Returns the first inner-ring node.
   */
  public static Node buildAt(int x_px) {
    final NodeManager node_manager = ContextManager.getNodeManager();
    node_manager.initial_reset();
    final LinkManager link_manager = node_manager.getLinkManager();
    link_manager.reset();
    node_manager.getFaceManager().reset();

    final Clazz clazz = node_manager.clazz_factory.getNew(0xFFFFFFFF);
    final NodeType node_type = node_manager.node_type_factory.getNew();
    node_type.log_mass = log_mass;
    node_type.setSize(node_size_px);

    World.gravity_active = true;
    World.gravity_strength = 2;
    World.ground_friction = friction;
    World.global_temperature = 0;

    // Ground is the high-Y wall (positive gravity pulls toward +Y).
    final int ground_y_px = Coords.y_pixels - node_size_px - 1;
    final int centre_y_px = ground_y_px - OUTER_RADIUS_PX;
    final int centre_y = centre_y_px << Coords.shift;
    final int cx = x_px << Coords.shift;

    final Node[] ring0 = new Node[NODES_PER_RING];
    final Node[] ring1 = new Node[NODES_PER_RING];
    final Node[] ring2 = new Node[NODES_PER_RING];
    final int half_width = HALF_WIDTH_PX << Coords.shift;

    for (int i = 0; i < NODES_PER_RING; i++) {
      final double angle = 2.0 * Math.PI * i / NODES_PER_RING;
      final int dx_inner =
          (int) (INNER_RADIUS_PX * Math.cos(angle)) << Coords.shift;
      final int dy_inner =
          (int) (INNER_RADIUS_PX * Math.sin(angle)) << Coords.shift;
      final int dx_outer =
          (int) (OUTER_RADIUS_PX * Math.cos(angle)) << Coords.shift;
      final int dy_outer =
          (int) (OUTER_RADIUS_PX * Math.sin(angle)) << Coords.shift;

      ring0[i] = node_manager.addNewAgent(
          new Point3D(cx + dx_inner, centre_y + dy_inner, 0),
          clazz, node_type);
      ring1[i] = node_manager.addNewAgent(
          new Point3D(cx + dx_outer, centre_y + dy_outer, half_width),
          clazz, node_type);
      ring2[i] = node_manager.addNewAgent(
          new Point3D(cx + dx_outer, centre_y + dy_outer, -half_width),
          clazz, node_type);
    }

    // No links yet -- just the nodes.

    // Rest on the ground (no mid-air start, per Tim's grounding rule).
    Grounding.restOnGround(node_manager);

    return ring0[0];
  }

  /** Builds the rings at the default x position. */
  public static Node build() {
    return buildAt(300);
  }
}
