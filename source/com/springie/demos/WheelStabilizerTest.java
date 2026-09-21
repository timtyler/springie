// This code has been placed into the public domain by its author.

package com.springie.demos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.NodeManager;

/**
 * The wheel's axle yaw stabilizer (Tim's directive: N and S bias on
 * opposite ends of the wheel axle, stabilizing the initial rolling
 * direction without turning the wheel around).
 */
class WheelStabilizerTest {

  private int old_bias;
  private int old_z_offset;

  @BeforeEach
  void setUp() {
    old_bias = WheelDemo.axle_stabilizer_bias;
    old_z_offset = WheelDemo.z_offset_px;
    // Pin the tuned values so the tests are deterministic even if the
    // statics were changed by an earlier test.
    WheelDemo.axle_stabilizer_bias = 13;
    WheelDemo.z_offset_px = 100;
    ContextManager.setNodeManager(new NodeManager());
  }

  @AfterEach
  void tearDown() {
    WheelDemo.axle_stabilizer_bias = old_bias;
    WheelDemo.z_offset_px = old_z_offset;
  }

  @Test
  void stabilizerAttachedExactlyOnce() {
    WheelDemo.buildAt(120);
    final LinkManager lm =
        ContextManager.getNodeManager().getLinkManager();
    int count = 0;
    for (int i = 0; i < lm.element.size(); i++) {
      final Link link = (Link) lm.element.get(i);
      if (link.controller instanceof AxleStabilizerController) {
        count++;
      }
    }
    // One instance on one passive link: the bias must apply exactly
    // once per dynamics step.
    assertEquals(1, count,
        "exactly one link should carry the axle stabilizer");
  }

  @Test
  void judgedRunStaysWithinZDriftBar() {
    final RollingJudge.Result result = RollingJudge.score(600, false);
    // The stabilized wheel must roll straight, not veer sideways
    // (same bar as WheelDemoTest.staysUprightWhileRolling).
    assertTrue(Math.abs(result.z_drift_px) < 50,
        "zDrift=" + result.z_drift_px + " (expected < 50px)");
  }
}
