// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;

/**
 * The wheel's version of Tim's "not tipping over" rule: the axle must stay
 * level. Two nodes, one on each end of the axle -- their heights must stay
 * within max_difference_px of each other for the whole run. A rolling wheel
 * yaws and pitches but its axle stays horizontal; tipping over puts one end
 * of the axle on (or toward) the ground, which disqualifies the run.
 */
public final class AxleLevelRule {
  private final int left_index;
  private final int right_index;
  private final int max_difference;

  /**
   * @param left_index element index of a node on one end of the axle
   * @param right_index element index of a node on the other end
   * @param max_difference_px max allowed height difference in px
   */
  public AxleLevelRule(int left_index, int right_index, int max_difference_px) {
    this.left_index = left_index;
    this.right_index = right_index;
    this.max_difference = max_difference_px << Coords.shift;
  }

  /** True when one end of the axle has dropped too far below the other. */
  public boolean violated(NodeManager node_manager) {
    final Node left = (Node) node_manager.element.get(left_index);
    final Node right = (Node) node_manager.element.get(right_index);
    final int dy = left.pos.y - right.pos.y;
    return dy > max_difference || dy < -max_difference;
  }
}
