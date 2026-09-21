// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;

/**
 * "No free shove" check for the locomotion judges: a demo must start at
 * rest along its direction of travel. Muscle power only -- any initial
 * velocity in the target direction is cheating, and disqualifies the run.
 *
 * <p>Call the mean* methods immediately after the demo's build method,
 * before the first physics tick. The mean is over all nodes (the
 * centre-of-mass velocity): a pure spin kick, like the wheel's tangential
 * self-start, nets to zero along any heading and passes, while a
 * translational shove toward the goal does not.
 *
 * <p>The engine spawns every node with a tiny uniform velocity of
 * (3, 4, 5) internal units, and per-node integer kicks truncate, so the
 * tolerance sits well above those residuals (~0.02 px/frame) and well
 * below any shove worth cheating with.
 */
public final class InitialVelocityCheck {
  private InitialVelocityCheck() {
  }

  /**
   * Tolerance on the mean initial velocity, px/frame. Anything at or
   * below this counts as starting at rest.
   */
  public static final double TOLERANCE_PX_PER_FRAME = 0.05;

  /**
   * Mean node velocity along the given compass heading, in pixels per
   * frame. Positive means toward the heading; negative means away.
   */
  public static double meanAlong(NodeManager node_manager,
      CompassPoint heading) {
    long sum_vx = 0;
    long sum_vz = 0;
    final int n = node_manager.element.size();
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      sum_vx += node.velocity.x;
      sum_vz += node.velocity.z;
    }
    if (n == 0) {
      return 0.0;
    }
    return heading.progress(sum_vx, sum_vz)
        / (double) (n * (1 << Coords.shift));
  }

  /**
   * Mean node speed in the floor plane (x/z), in pixels per frame.
   * For judges like the sidewinder's that score unsigned 2D travel, a
   * shove in any floor direction cheats, so there is no single heading.
   */
  public static double meanFloorSpeed(NodeManager node_manager) {
    long sum_vx = 0;
    long sum_vz = 0;
    final int n = node_manager.element.size();
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      sum_vx += node.velocity.x;
      sum_vz += node.velocity.z;
    }
    if (n == 0) {
      return 0.0;
    }
    final double units = 1 << Coords.shift;
    final double mean_vx = sum_vx / (double) n / units;
    final double mean_vz = sum_vz / (double) n / units;
    return Math.sqrt(mean_vx * mean_vx + mean_vz * mean_vz);
  }

  /**
   * Mean node velocity on the vertical axis, in pixels per frame.
   * Positive is downward (screen coordinates); negative is upward.
   * For the hopper, whose target direction is up, not a compass point.
   */
  public static double meanVertical(NodeManager node_manager) {
    long sum_vy = 0;
    final int n = node_manager.element.size();
    for (int i = 0; i < n; i++) {
      sum_vy += ((Node) node_manager.element.get(i)).velocity.y;
    }
    if (n == 0) {
      return 0.0;
    }
    return sum_vy / (double) (n * (1 << Coords.shift));
  }

  /** True when the model starts at rest along the heading. */
  public static boolean atRestAlong(NodeManager node_manager,
      CompassPoint heading) {
    return Math.abs(meanAlong(node_manager, heading))
        <= TOLERANCE_PX_PER_FRAME;
  }

  /** True when the model starts at rest in the floor plane. */
  public static boolean atRestOnFloor(NodeManager node_manager) {
    return meanFloorSpeed(node_manager) <= TOLERANCE_PX_PER_FRAME;
  }

  /** True when the model starts at rest on the vertical axis. */
  public static boolean atRestVertically(NodeManager node_manager) {
    return Math.abs(meanVertical(node_manager)) <= TOLERANCE_PX_PER_FRAME;
  }
}
