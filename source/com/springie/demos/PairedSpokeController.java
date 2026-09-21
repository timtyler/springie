// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.muscles.Controller;
import com.springie.render.Coords;

/**
 * A paired ground-contact push-off reflex for wheel spokes.
 *
 * <p>One controller drives the two spokes of a rim pair (hub-rim0[i] and
 * hub-rim1[i]) together, from their midpoint geometry. Firing the two rims
 * independently lets a small tilt grow: the lower rim's spokes fire harder
 * and shove the hub further sideways (positive feedback that capsizes the
 * wheel -- wider tracks made it worse, not better). Paired firing keeps the
 * sideways spoke forces symmetric so they cancel, breaking the feedback;
 * the stance decision uses the pair midpoint, and when one rim lifts the
 * extended spoke on that side pushes it back down toward the ground.
 *
 * <p>The same controller instance is attached to both links; each update
 * sets both links' rest lengths (idempotent).
 */
public final class PairedSpokeController implements Controller {
  private final Node hub;
  private final Node rim_a;
  private final Node rim_b;
  private final Link link_a;
  private final Link link_b;
  private final int base_a;
  private final int base_b;
  private final int push_a;
  private final int push_b;
  private final int pull_a;
  private final int pull_b;
  private final int ground_y;
  private final int direction;

  public PairedSpokeController(Node hub, Node rim_a, Node rim_b,
      Link link_a, Link link_b, int base_a, int base_b,
      int push_pct, int pull_pct, int ground_y, int direction,
      int stance_threshold_px) {
    this(hub, rim_a, rim_b, link_a, link_b, base_a, base_b, push_pct,
        pull_pct, ground_y, direction, stance_threshold_px, false, 60, 0);
  }

  /**
   * @param proportional when true, the push/pull ramps smoothly with how
   *   far behind/in front the spoke pair is (0 at the stance threshold,
   *   full at horizontal), instead of snapping on/off. This removes the
   *   impulsive kick that pumps the rocking mode, and concentrates the
   *   strongest push where the spoke is most horizontal (best forward
   *   leverage, least skyward launch).
   * @param radius_px spoke length, used to normalize the ramp
   * @param roll_gain active roll-damping gain, in rest-length units per
   *   unit of inter-rim y-difference (256 = 1.0x). When the pair's two
   *   rims ride at different heights, the high side's spoke extends
   *   proportionally, pushing the high rim back down toward level.
   *   0 disables.
   */
  public PairedSpokeController(Node hub, Node rim_a, Node rim_b,
      Link link_a, Link link_b, int base_a, int base_b,
      int push_pct, int pull_pct, int ground_y, int direction,
      int stance_threshold_px, boolean proportional, int radius_px,
      int roll_gain) {
    this.hub = hub;
    this.rim_a = rim_a;
    this.rim_b = rim_b;
    this.link_a = link_a;
    this.link_b = link_b;
    this.base_a = base_a;
    this.base_b = base_b;
    this.push_a = base_a + (base_a * push_pct / 100);
    this.push_b = base_b + (base_b * push_pct / 100);
    this.pull_a = base_a - (base_a * pull_pct / 100);
    this.pull_b = base_b - (base_b * pull_pct / 100);
    this.ground_y = ground_y;
    this.direction = direction;
    this.stance_threshold = stance_threshold_px << Coords.shift;
    this.proportional = proportional;
    this.radius = radius_px << Coords.shift;
    this.roll_gain = roll_gain;
  }

  private final int stance_threshold;
  private final boolean proportional;
  private final int radius;
  private final int roll_gain;

  @Override
  public void update(Link link, long tick) {
    // Pair midpoint geometry -- symmetric by construction.
    final int mid_x = (rim_a.pos.x + rim_b.pos.x) / 2;
    final int mid_y = (rim_a.pos.y + rim_b.pos.y) / 2;
    // Near the ground? Either rim touching counts, so drive continues
    // through a mild tilt while the lifted side gets pushed back down.
    final int slack = 8 << Coords.shift;
    final boolean on_ground =
        rim_a.pos.y >= ground_y - slack || rim_b.pos.y >= ground_y - slack
            || mid_y >= ground_y - slack;
    int rest_a = base_a;
    int rest_b = base_b;
    if (on_ground) {
      final int dx = mid_x - hub.pos.x;
      // Fire only when the spoke pair is well angled fore/aft: near-vertical
      // spokes push the hub skyward (hopping) instead of forward.
      final int threshold = stance_threshold;
      final int behind;
      final int ahead;
      if (direction > 0) {
        behind = dx < -threshold ? -dx - threshold : 0;
        ahead = dx > threshold ? dx - threshold : 0;
      } else {
        behind = dx > threshold ? dx - threshold : 0;
        ahead = dx < -threshold ? -dx - threshold : 0;
      }
      if (proportional) {
        // Smooth ramp: 0 at the stance threshold, full at horizontal.
        final int range = Math.max(1, radius - threshold);
        if (behind > 0) {
          final int depth = Math.min(behind, range);
          rest_a = base_a + (push_a - base_a) * depth / range;
          rest_b = base_b + (push_b - base_b) * depth / range;
        } else if (ahead > 0) {
          final int depth = Math.min(ahead, range);
          rest_a = base_a - (base_a - pull_a) * depth / range;
          rest_b = base_b - (base_b - pull_b) * depth / range;
        }
      } else if (behind > 0) {
        rest_a = push_a;
        rest_b = push_b;
      } else if (ahead > 0) {
        rest_a = pull_a;
        rest_b = pull_b;
      }
    }
    link_a.adjusted_rest_length = rest_a;
    link_b.adjusted_rest_length = rest_b;
    // Active roll damping, applied while the pair is below the hub
    // (near the ground, where the drive fires): if the pair's two rims
    // ride at different heights, extend the high side's spoke. Below the
    // hub the spoke points downward, so extending it pushes the high rim
    // back down toward level. (Screen coords: smaller y is higher.)
    if (this.roll_gain != 0 && mid_y >= this.hub.pos.y) {
      final int dy = this.rim_b.pos.y - this.rim_a.pos.y;
      // dy > 0: rim_a is higher; extend spoke_a.
      final int correction = (int) ((long) dy * this.roll_gain / 256);
      link_a.adjusted_rest_length = rest_a + correction;
      link_b.adjusted_rest_length = rest_b - correction;
    }
  }
}
