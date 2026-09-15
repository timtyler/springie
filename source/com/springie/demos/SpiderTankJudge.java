// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;

/**
 * Headless judge for the spider tank. Scores by distance from start.
 */
public final class SpiderTankJudge {
  private SpiderTankJudge() {
  }

  public static void main(String[] args) throws Exception {
    final int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 600;

    FrEnd.main(new String[0]);
    Thread.sleep(1500);

    final NodeManager node_manager = ContextManager.getNodeManager();
    final Node body = SpiderTankDemo.buildAt(0);
    final int start_x = body.pos.x;
    final int start_y = body.pos.y;
    final int start_z = body.pos.z;

    FrEnd.paused = false;
    for (int i = 0; i < ticks; i++) {
      node_manager.nodeAndLinkUpdate();
    }

    final long dx = (long) body.pos.x - start_x;
    final long dy = (long) body.pos.y - start_y;
    final long dz = (long) body.pos.z - start_z;
    final int dist_internal = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
    final int displacement_px = dist_internal >> com.springie.render.Coords.shift;
    final int height_px = body.pos.y >> com.springie.render.Coords.shift;

    System.out.println("SCORE " + displacement_px);
    System.out.println("HEIGHT " + height_px);
    System.out.println("TICKS " + ticks);

    System.exit(0);
  }
}
