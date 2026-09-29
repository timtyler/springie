// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.muscles.Controller;
import java.util.List;

/**
 * The suspended hamster (Tim, 2026-09-28): a very heavy node parked
 * forward-up inside the wheel, driving it via the back cables.
 *
 * <p>Physics stays honest: the controller only shortens cable rest
 * lengths (a muscle doing work, applied as an equal-and-opposite force
 * pair by the link solver). It never touches velocities directly --
 * the momentum cheat (damping the hamster toward hub velocity, which
 * deleted its backward reaction momentum and created forward momentum
 * from nothing) was removed on Tim's order, 2026-09-28.
 *
 * <p>One instance drives all the engine links. Back is classified in
 * the world frame every tick (anchor behind the hub), so the drive
 * stays aligned as the wheel rolls. Links are paired across the two
 * rims, so lateral forces stay symmetric.
 */
public final class HamsterEngineController implements Controller {
  private final Node hamster;
  private final Node hub0;
  private final Node hub1;
  private final List<Link> links;
  private final List<Node> anchors;
  private final int pull_pct;
  private final int deadband;
  private long last_tick = -1;

  public HamsterEngineController(Node hamster, Node hub0, Node hub1,
      List<Link> links, List<Node> anchors, int pull_pct,
      int deadband_px) {
    this.hamster = hamster;
    this.hub0 = hub0;
    this.hub1 = hub1;
    this.links = links;
    this.anchors = anchors;
    this.pull_pct = pull_pct;
    this.deadband = deadband_px;
  }

  @Override
  public void update(Link link, long tick) {
    if (tick == last_tick) {
      return;
    }
    last_tick = tick;

    final int hub_x = (hub0.pos.x + hub1.pos.x) / 2;

    for (int i = 0; i < links.size(); i++) {
      final Link l = links.get(i);
      final Node a = anchors.get(i);
      final int dx = a.pos.x - hub_x;
      final int actual = distance(hamster, a);
      int rest = actual;
      if (dx < -deadband) {
        // Back anchors: contract, hauling the wheel forward. The
        // hamster is too heavy to be dragged back.
        rest = actual - (int) ((long) actual * pull_pct / 100);
      }
      l.adjusted_rest_length = rest;
    }
  }

  private static int distance(Node a, Node b) {
    final int dx = a.pos.x - b.pos.x;
    final int dy = a.pos.y - b.pos.y;
    final int dz = a.pos.z - b.pos.z;
    return (int) Math.sqrt((long) dx * dx + (long) dy * dy + (long) dz * dz);
  }
}
