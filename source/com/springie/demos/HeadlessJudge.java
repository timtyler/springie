// This code has been placed into the public domain by its author.

package com.springie.demos;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
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
public abstract class HeadlessJudge {
  /**
   * Runs the given scored run with the model lock held. A GUI test's
   * animation thread never stops: it keeps repainting, and the AWT
   * thread would otherwise step physics on this run's NodeManager
   * concurrently with the judging loop (extra ticks plus data races on
   * node positions), so two back-to-back runs diverge. This is the same
   * lock the AWT renderer and the message pump already use (see
   * RendererDelegator.redrawChanged); the physics path never needs the
   * AWT tree lock, so this cannot deadlock.
   */
  protected static <T> T withModelLock(java.util.function.Supplier<T> run) {
    synchronized (ContextManager.class) {
      return run.get();
    }
  }

  /**
   * Pins down all global physics state. Earlier tests (especially GUI
   * tests) leak values into these statics; the demos' buildAt methods
   * set only a subset, so two consecutive judged runs could otherwise
   * diverge in a polluted suite.
   *
   * <p>NOTE: temperature is NOT pinned here -- WheelbarrowDemo.buildAt
   * sets World.global_temperature = 0 (deterministic build;
   * Caterpillar2Demo and SlinkyDemo do the same). Pinning it here would
   * be dead code because buildAt overrides it.
   */
  protected static void pinPhysicsState() {
    World.gravity_active = true;
    World.gravity_strength = 5;
    World.ground_friction = 100;
    World.minimum_magnitude = 0;
    World.maximum_magnitude = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.max_speed = Integer.MAX_VALUE;
    com.springie.elements.nodes.Node.viscocity = 0;
    com.springie.FrEnd.three_d = true;
    com.springie.FrEnd.check_collisions = true;
    com.springie.FrEnd.continuously_centre_x = false;
    com.springie.FrEnd.continuously_centre_y = false;
    com.springie.FrEnd.continuously_centre_z = false;
    com.springie.FrEnd.boundaries = true;
    com.springie.FrEnd.explosions = true;
    com.springie.FrEnd.oscd = true;
    com.springie.FrEnd.dragged_element = null;
    com.springie.FrEnd.forces_disabled_during_gesture = false;
    com.springie.FrEnd.paused = false;
    com.springie.FrEnd.frame_frequency = 0;
    com.springie.muscles.Muscles.enabled = false;
    com.springie.muscles.Muscles.active_oscillator = 0;
    // Pin the universe size: a booted GUI resizes Coords to its canvas,
    // moving the ground walls and changing the absolute score.
    com.springie.render.Coords.x_pixels = 800;
    com.springie.render.Coords.y_pixels = 600;
    com.springie.render.Coords.z_pixels = 1024;
  }

  /**
   * Resets the world's random number generator to its initial seed.
   * The physics uses this for temperature jitter and node seeds; without
   * a reset, consecutive judged runs diverge (the models are chaotic).
   */
  protected static void resetWorldRandom() {
    try {
      final java.lang.reflect.Field field =
          World.class.getDeclaredField("rnd");
      field.setAccessible(true);
      final Hortensius32Fast rnd =
          (Hortensius32Fast) field.get(null);
      // GOOD_SEED = 4357 (the default seed for a new generator).
      rnd.setSeed(4357);
    } catch (Exception e) {
      throw new RuntimeException("Failed to reset World.rnd", e);
    }
  }

  /**
   * Creates a fresh NodeManager, pins the physics state, and resets the
   * RNG -- the standard judged-run preamble. Returns the new manager.
   */
  protected static NodeManager newJudgedRun() {
    resetWorldRandom();
    pinPhysicsState();
    ContextManager.setNodeManager(new NodeManager());
    return ContextManager.getNodeManager();
  }

  /** Runs the given number of physics ticks without measuring. */
  protected static void settle(NodeManager node_manager, int ticks) {
    for (int i = 0; i < ticks; i++) {
      node_manager.nodeAndLinkUpdate();
    }
  }

  /**
   * 2D travel on the floor between two positions, in pixels. Vertical
   * motion (hops, bobs, falls) does not count -- Tim's 2D judging rule.
   */
  protected static int distance2DPx(int x0, int z0, int x1, int z1) {
    final long dx = (long) x1 - x0;
    final long dz = (long) z1 - z0;
    return (int) (Math.sqrt(dx * dx + dz * dz) / (1 << Coords.shift));
  }

  /**
   * True if any node has flown further than limit_px from the reference
   * node -- the model has fallen apart (links overstretched or nodes
   * teleported). Without this, a shattered model whose tracked node
   * happens to stay put can score a bogus clean run.
   */
  protected static boolean shattered(NodeManager node_manager, Node ref,
      int limit_px) {
    final int n = node_manager.element.size();
    final long limit = (long) limit_px << Coords.shift;
    final long limit2 = limit * limit;
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      final long dx = (long) node.pos.x - ref.pos.x;
      final long dy = (long) node.pos.y - ref.pos.y;
      final long dz = (long) node.pos.z - ref.pos.z;
      if (dx * dx + dy * dy + dz * dz > limit2) {
        return true;
      }
    }
    return false;
  }

  /**
   * Structural collapse check (Tim, 2026-10-01): has the model flattened
   * into a puddle? Compares current Y/Z extents against the initial
   * (post-settle) extents -- collapsed if either drops below 30% of
   * initial. The shattered() check (node flung far) misses this; all
   * nodes stay near the hub, just flat on the floor.
   */
  protected static boolean collapsed(NodeManager node_manager,
      long init_y_px, long init_z_px) {
    final int n = node_manager.element.size();
    if (n == 0) {
      return true;
    }
    long min_y = Long.MAX_VALUE;
    long max_y = Long.MIN_VALUE;
    long min_z = Long.MAX_VALUE;
    long max_z = Long.MIN_VALUE;
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      if (node.pos.y < min_y) {
        min_y = node.pos.y;
      }
      if (node.pos.y > max_y) {
        max_y = node.pos.y;
      }
      if (node.pos.z < min_z) {
        min_z = node.pos.z;
      }
      if (node.pos.z > max_z) {
        max_z = node.pos.z;
      }
    }
    final long y_extent_px = (max_y - min_y) >> Coords.shift;
    final long z_extent_px = (max_z - min_z) >> Coords.shift;
    // Collapsed if either extent drops below 30% of its initial size.
    return y_extent_px < init_y_px * 3 / 10
        || z_extent_px < init_z_px * 3 / 10;
  }

  /** Y and Z extents of the model in pixels, as {y_extent, z_extent}. */
  protected static long[] extentsPx(NodeManager node_manager) {
    final int n = node_manager.element.size();
    long min_y = Long.MAX_VALUE;
    long max_y = Long.MIN_VALUE;
    long min_z = Long.MAX_VALUE;
    long max_z = Long.MIN_VALUE;
    for (int i = 0; i < n; i++) {
      final Node node = (Node) node_manager.element.get(i);
      if (node.pos.y < min_y) {
        min_y = node.pos.y;
      }
      if (node.pos.y > max_y) {
        max_y = node.pos.y;
      }
      if (node.pos.z < min_z) {
        min_z = node.pos.z;
      }
      if (node.pos.z > max_z) {
        max_z = node.pos.z;
      }
    }
    return new long[] {
        (max_y - min_y) >> Coords.shift,
        (max_z - min_z) >> Coords.shift,
    };
  }

  /** Mean node height (centre-of-mass height), in pixels. */
  protected static double comHeightPx(NodeManager node_manager) {
    final int n = node_manager.element.size();
    long sum = 0;
    for (int i = 0; i < n; i++) {
      sum += ((Node) node_manager.element.get(i)).pos.y;
    }
    return (double) (sum >> Coords.shift) / n;
  }
}
