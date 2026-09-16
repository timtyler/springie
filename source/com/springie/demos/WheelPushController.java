// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.muscles.Controller;
import com.springie.render.Coords;

/**
 * A ground-contact push-off reflex for wheel spokes.
 *
 * <p>When the spoke's rim node is on the ground behind the hub (in the
 * rolling direction), the spoke extends, pushing the hub forward and up
 * -- like a leg pushing off. At all other times the spoke holds its
 * base rest length. Unlike the open-loop travelling wave, this is
 * self-synchronizing: the ground contact sets the timing, so the drive
 * cannot fall out of step with the rolling.
 *
 * <p>Each spoke gets its own controller holding its hub and rim nodes.
 */
public final class WheelPushController implements Controller {
  private final Node hub;
  private final Node rim;
  private final int base_length;
  private final int push_length;
  private final int pull_length;
  private final int ground_y;
  private final int direction;

  /**
   * @param hub the hub node
   * @param rim the rim node of this spoke
   * @param base_length the spoke's natural rest length (internal units)
   * @param push_pct percent to extend when pushing off (back stance)
   * @param pull_pct percent to contract when pulling (front stance)
   * @param ground_y the ground level in internal units
   * @param direction +1 to roll toward +X, -1 toward -X
   */
  public WheelPushController(Node hub, Node rim, int base_length,
      int push_pct, int pull_pct, int ground_y, int direction) {
    this.hub = hub;
    this.rim = rim;
    this.base_length = base_length;
    this.push_length = base_length + (base_length * push_pct / 100);
    this.pull_length = base_length - (base_length * pull_pct / 100);
    this.ground_y = ground_y;
    this.direction = direction;
  }

  @Override
  public void update(Link link, long tick) {
    // Near the ground? (within 8px above it)
    final boolean on_ground =
        rim.pos.y >= ground_y - (8 << Coords.shift);
    if (!on_ground) {
      link.adjusted_rest_length = base_length;
      return;
    }
    // Behind or in front of the hub (in the rolling direction)?
    final int dx = rim.pos.x - hub.pos.x;
    final int threshold = 4 << Coords.shift;
    if (direction > 0) {
      if (dx < -threshold) {
        // Back stance: extend, push the hub forward.
        link.adjusted_rest_length = push_length;
      } else if (dx > threshold) {
        // Front stance: contract, pull the hub forward.
        link.adjusted_rest_length = pull_length;
      } else {
        link.adjusted_rest_length = base_length;
      }
    } else {
      if (dx > threshold) {
        link.adjusted_rest_length = push_length;
      } else if (dx < -threshold) {
        link.adjusted_rest_length = pull_length;
      } else {
        link.adjusted_rest_length = base_length;
      }
    }
  }
}
