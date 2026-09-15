// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;

/**
 * Headless judge that scores a crawler design by distance walked.
 *
 * <p>Usage: java com.springie.demos.CrawlerJudge [ticks]
 * Prints: SCORE <pixels> (positive X displacement of the body).
 */
public final class CrawlerJudge {
  private CrawlerJudge() {
  }

  public static void main(String[] args) throws Exception {
    final int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 600;

    // Boot the app (headless under Xvfb is fine).
    FrEnd.main(new String[0]);
    Thread.sleep(1500);

    final NodeManager node_manager = ContextManager.getNodeManager();

    // Build the crawler.
    final Node body = CrawlerDemo.buildAt(0);
    final int start_x = body.pos.x;
    final int start_y = body.pos.y;
    final int start_z = body.pos.z;

    // Let it settle for a moment, then run.
    FrEnd.paused = false;
    for (int i = 0; i < ticks; i++) {
      node_manager.nodeAndLinkUpdate();
    }

    // Score: straight-line distance from the starting spot (pixels).
    // Direction-agnostic — no need to solve the orientation problem.
    final long dx = (long) body.pos.x - start_x;
    final long dy = (long) body.pos.y - start_y;
    final long dz = (long) body.pos.z - start_z;
    final int dist_internal = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
    final int displacement_px = dist_internal >> com.springie.render.Coords.shift;

    // Also report height (did it fall over?) and tick count.
    final int height_px = body.pos.y >> com.springie.render.Coords.shift;

    System.out.println("SCORE " + displacement_px);
    System.out.println("HEIGHT " + height_px);
    System.out.println("TICKS " + ticks);

    System.exit(0);
  }
}
