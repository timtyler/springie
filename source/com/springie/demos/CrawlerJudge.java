// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;

/**
 * Truly headless judge that scores a crawler design by distance walked.
 * Does NOT start FrEnd (no GUI, no animation thread) — single-threaded
 * and deterministic. Needs DISPLAY set for AWT static init (Xvfb is fine).
 *
 * <p>Usage: java com.springie.demos.CrawlerJudge [ticks]
 * Prints: SCORE <pixels> (2D travel on the floor), HEIGHT, TICKS.
 */
public final class CrawlerJudge {
  private CrawlerJudge() {
  }

  public static void main(String[] args) throws Exception {
    final int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 600;

    // Headless: just create a NodeManager, no FrEnd.
    ContextManager.setNodeManager(new NodeManager());
    final NodeManager node_manager = ContextManager.getNodeManager();

    // Build the crawler.
    final Node body = CrawlerDemo.buildAt(0);
    final int start_x = body.pos.x;
    final int start_y = body.pos.y;
    final int start_z = body.pos.z;

    for (int i = 0; i < ticks; i++) {
      node_manager.nodeAndLinkUpdate();
    }

    // Score: straight-line travel on the floor from the starting spot (pixels).
    // Direction-agnostic — no need to solve the orientation problem.
    // Vertical motion doesn't count: this is 2D travel in the floor plane.
    final long dx = (long) body.pos.x - start_x;
    final long dz = (long) body.pos.z - start_z;
    final int dist_internal = (int) Math.sqrt(dx * dx + dz * dz);
    final int displacement_px = dist_internal >> com.springie.render.Coords.shift;

    // Also report height (did it fall over?) and tick count.
    final int height_px = body.pos.y >> com.springie.render.Coords.shift;

    System.out.println("SCORE " + displacement_px);
    System.out.println("HEIGHT " + height_px);
    System.out.println("TICKS " + ticks);

    System.exit(0);
  }
}
