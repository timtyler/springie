// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.elements.nodes.NodeManager;

/**
 * Tim's "not tipping over" disqualification, in rule form. Each athlete
 * declares its own posture rule (axle stays level, top stays on top,
 * ...); the judges call violated() every tick and disqualify the run on
 * the first violation.
 */
public interface PostureRule {
  /** True when the model's posture has failed -- the run is disqualified. */
  boolean violated(NodeManager node_manager);
}
