// This code has been placed into the public domain by its author.

package com.springie.world;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;

/**
 * General mechanism that keeps models from starting in mid-air.
 *
 * <p>After a model is built or loaded, its lowest point can float above the
 * ground plane -- or sit below it, in which case the physics teleports it
 * upward on the first tick. {@link #restOnGround} shifts the whole model
 * vertically so its lowest node extent sits exactly on the ground plane, the
 * same level the physics clamps nodes to in {@code Node.boundaryCheck}, so
 * every model starts at rest on the floor.
 *
 * <p>No-op for an empty model or one already resting on the ground.
 */
public final class Grounding {
  private Grounding() {
  }

  /**
   * Shifts every node in the manager vertically so the model's lowest point
   * rests exactly on the ground plane. Relative geometry is unchanged.
   */
  public static void restOnGround(NodeManager node_manager) {
    final int n = node_manager.element.size();
    if (n == 0) {
      return;
    }

    final int ground = Coords.y_pixels << Coords.shift;
    int lowest = Integer.MIN_VALUE;
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      final int extent = node.pos.y + node.type.radius;
      if (extent > lowest) {
        lowest = extent;
      }
    }

    final int dy = ground - lowest;
    if (dy == 0) {
      return;
    }

    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      node.pos.y += dy;
      node.boundaryCheck();
      node.findNewBin(node_manager.node_grid);
    }
  }
}
