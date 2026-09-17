// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;

/**
 * Tim's "not tipping over" disqualification rule, in its simplest form:
 * choose two nodes and make sure the top one stays on top.
 *
 * <p>The top node's height must stay above the bottom node's height by at
 * least min_separation_px, for the whole run (after settling). If the
 * model tips onto its side, rolls over, or churns end over end, the top
 * node drops to (or below) the bottom node's level and the run is
 * disqualified -- regardless of distance, periodicity, or any other score.
 *
 * <p>Heights are screen Y (grows downward): "above" means smaller Y.
 */
public final class TipOverRule {
  private final int top_index;
  private final int bottom_index;
  private final int min_separation;

  /**
   * @param top_index element index of the node that should stay on top
   *   (e.g. a dorsal ridge node)
   * @param bottom_index element index of the reference node that should
   *   stay below it (e.g. a belly/base node)
   * @param min_separation_px how far above (in px) the top must stay;
   *   small tolerance so normal rocking doesn't trip it
   */
  public TipOverRule(int top_index, int bottom_index, int min_separation_px) {
    this.top_index = top_index;
    this.bottom_index = bottom_index;
    this.min_separation = min_separation_px << Coords.shift;
  }

  /** True when the top node has dropped to/below the bottom node's level. */
  public boolean violated(NodeManager node_manager) {
    final Node top = (Node) node_manager.element.get(top_index);
    final Node bottom = (Node) node_manager.element.get(bottom_index);
    return top.pos.y > bottom.pos.y - min_separation;
  }
}
