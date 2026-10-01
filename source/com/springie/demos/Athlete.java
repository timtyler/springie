// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.nodes.Node;

/**
 * The contract every judged demo ("athlete") fulfills. The judges score
 * athletes through this interface instead of reaching into each demo's
 * static fields directly, so the build/heading/posture wiring is uniform
 * across the Olympics.
 */
public interface Athlete {
  /**
   * Builds the model at the given x position (pixels) and returns the
   * node the judge should track (hub, body centroid node, mid-body node
   * -- whatever best represents the athlete's travel).
   */
  Node buildAt(int x_px);

  /**
   * The compass heading the athlete is supposed to travel toward
   * (E = +x, W = -x, S = +z, N = -z). Used for the no-initial-velocity
   * check and for signed-progress scoring.
   */
  CompassPoint compassHeading();

  /**
   * The posture rule for Tim's "not tipping over" disqualification, or
   * null when the athlete has no tip-over rule (e.g. the sidewinder,
   * whose gait rolls about the body axis as part of healthy motion).
   */
  PostureRule postureRule();
}
