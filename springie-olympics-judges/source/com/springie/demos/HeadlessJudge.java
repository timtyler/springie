// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Coords;
import com.springie.utilities.random.Hortensius32Fast;
import com.springie.world.World;

/**
 * Shared pipeline for the truly headless judges. Every judge was
 * duplicating the same setup: pin the global physics state (GUI tests
 * leak values into these statics), reset the world's RNG for
 * determinism, hold the model lock (a GUI test's animation thread never
 * stops), run settle ticks, then measure. That all lives here now; each
 * specific judge keeps only its scoring logic.
 *
 * <p>Does NOT start FrEnd (no GUI, no animation thread).
 * Needs DISPLAY set for AWT static init (Xvfb is fine).
 */
public abstract class HeadlessJudge extends com.springie.demos.HeadlessRunner {
  // All runner methods inherited from HeadlessRunner (athletes module).
}
