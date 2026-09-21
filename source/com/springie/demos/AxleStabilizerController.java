// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.muscles.Controller;

/**
 * Yaw stabilizer for the wheel demo (Tim's directive: N bias on one end
 * of the wheel axle, S bias on the other).
 *
 * <p>Every dynamics step it nudges the north rim end's nodes toward N
 * (velocity.z -= bias) and the south rim end's nodes toward S
 * (velocity.z += bias) -- i.e. the two ends are pulled <em>apart</em>
 * along the axle. The net force is zero, so the wheel is never shoved
 * sideways; while rolling straight the two biases are collinear with
 * the axle, producing no torque (only a slight axle tension the cross
 * links absorb).
 *
 * <p>Sign note: this "natural" assignment (N on the north end, S on the
 * south end) is the stable one. The static torque is yaw-restoring, and
 * although the wheel's spin couples yaw torque into roll via gyroscopic
 * precession, the gravity torque on the rolled wheel closes the loop
 * with the opposite sign, giving a stable, damped yaw/roll oscillation
 * around the initial heading. The swapped assignment (ends pulled
 * together) was tried: the loop gain flips sign and the wheel yaws
 * exponentially, spinning all the way around within 600 ticks. The
 * 180&deg;-turned-around orientation is an unstable equilibrium, so the
 * stabilizer cannot settle the wheel facing backwards -- it only damps
 * yaw wander around the initial heading.
 *
 * <p>Why a {@link Controller} that writes node velocities instead of
 * {@code adjusted_rest_length}: the Controller interface is the only
 * per-dynamics-step hook the engine offers a demo (LinkManager calls
 * {@code update} once per step per link carrying a controller). A yaw
 * torque is a force, not a rest-length change, so this controller
 * ignores its link and writes velocities directly. Attach exactly one
 * instance to exactly one link so the bias applies once per frame.
 */
public final class AxleStabilizerController implements Controller {
  private final Node[] rim0;
  private final Node[] rim1;
  private final int bias;

  /**
   * @param rim0 the north-end rim nodes (smaller z)
   * @param rim1 the south-end rim nodes (larger z)
   * @param bias nudge size in internal velocity units per frame
   *             (256 units = 1 px/frame)
   */
  public AxleStabilizerController(Node[] rim0, Node[] rim1, int bias) {
    this.rim0 = rim0;
    this.rim1 = rim1;
    this.bias = bias;
  }

  @Override
  public void update(Link link, long tick) {
    // N bias on the north end, S bias on the south end: each end is
    // pulled outward along the axle. Compass mapping: N = -z, S = +z.
    // (The swapped assignment was tried and destabilizes the wheel --
    // see the class doc.)
    for (int i = 0; i < this.rim0.length; i++) {
      this.rim0[i].velocity.z -= this.bias;
      this.rim1[i].velocity.z += this.bias;
    }
  }
}
