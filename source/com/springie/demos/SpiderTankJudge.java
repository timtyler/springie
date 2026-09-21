// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;

/**
 * Truly headless judge for the spider tank. Does NOT start FrEnd
 * (no GUI, no animation thread). Single-threaded and deterministic.
 * Scores by signed progress along the declared compass heading.
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

    // No free shove: the model must start at rest along its heading.
    // Checked before the first tick; a violation zeroes the score.
    final double initial_velocity =
        InitialVelocityCheck.meanAlong(node_manager,
            SpiderTankDemo.compassHeading());
    final boolean shoved =
        !InitialVelocityCheck.atRestAlong(node_manager,
            SpiderTankDemo.compassHeading());

    for (int i = 0; i < ticks; i++) {
      node_manager.nodeAndLinkUpdate();
    }

    // Score: signed progress along the model's declared compass heading
    // (pixels). Sideways crabbing scores nothing; walking backwards
    // scores negative. Vertical motion doesn't count. A starting shove
    // disqualifies the run (score 0).
    final long dx = (long) body.pos.x - start_x;
    final long dz = (long) body.pos.z - start_z;
    final int displacement_px = (int) (SpiderTankDemo.compassHeading()
        .progress(dx, dz) >> com.springie.render.Coords.shift);
    final int height_px = body.pos.y >> com.springie.render.Coords.shift;

    System.out.println("INITIAL_VELOCITY "
        + String.format("%.4f", initial_velocity));
    System.out.println("SCORE " + (shoved ? 0 : displacement_px));
    System.out.println("HEIGHT " + height_px);
    System.out.println("TICKS " + ticks);

    System.exit(0);
  }
}
