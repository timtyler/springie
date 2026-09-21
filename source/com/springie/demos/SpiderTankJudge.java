// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;

/**
 * Truly headless judge for the spider tank. Does NOT start FrEnd
 * (no GUI, no animation thread). Single-threaded and deterministic.
 * Scores by 2D travel on the floor from the start.
 */
public final class SpiderTankJudge {
  private SpiderTankJudge() {
  }

  public static void main(String[] args) throws Exception {
    final int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 600;

    // Headless: just create a NodeManager, no FrEnd.
    ContextManager.setNodeManager(new NodeManager());
    final NodeManager node_manager = ContextManager.getNodeManager();
    final Node body = SpiderTankDemo.buildAt(0);
    final int start_x = body.pos.x;
    final int start_y = body.pos.y;
    final int start_z = body.pos.z;

    for (int i = 0; i < ticks; i++) {
      node_manager.nodeAndLinkUpdate();
    }

    // 2D travel on the floor: vertical motion doesn't count.
    final long dx = (long) body.pos.x - start_x;
    final long dz = (long) body.pos.z - start_z;
    final int dist_internal = (int) Math.sqrt(dx * dx + dz * dz);
    final int displacement_px = dist_internal >> com.springie.render.Coords.shift;
    final int height_px = body.pos.y >> com.springie.render.Coords.shift;

    System.out.println("SCORE " + displacement_px);
    System.out.println("HEIGHT " + height_px);
    System.out.println("TICKS " + ticks);

    System.exit(0);
  }
}
