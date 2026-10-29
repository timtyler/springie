// This program has been placed into the public domain by its author.

package com.springie.metrics;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradient-descent damping optimisation, run live while the Optimise
 * damping button is greyed out.
 *
 * <p>Pressing the button starts a 20-second run. The parameters are the
 * damping values of the link types in scope: every type with no selection,
 * only the selected types with one. Types that are identical in every
 * physical respect except length (same elasticity, radius, tension,
 * compression and disabled flag) form one material group and share a
 * single parameter, since damping is a material property, not a
 * geometric one -- Moscow's 86 types are 4 materials. Each gradient step
 * perturbs every parameter up by a fixed amount, measures wobble over a
 * full meter window, and steps every parameter in the downhill direction
 * by a fixed amount. The run ends by restoring the best damping seen, so
 * a step that overshoots can never leave the model worse than it started.
 *
 * <p>The optimiser advances one tick at a time from the physics path, so
 * the model keeps running normally throughout. It aborts (restoring the
 * best damping) if the model or the selection changes under it.
 */
public class DampingOptimizer {
  /** Called on the AWT thread when a run finishes or aborts. */
  public interface Listener {
    void optimizationFinished();
  }

  /** How long a run lasts, in milliseconds. */
  public static final long RUN_MILLIS = 20000;

  /** Ticks per measurement phase: one meter window, plus recompute slack. */
  private static final int PHASE_TICKS = 110;

  /** Damping change used to probe the gradient. */
  private static final int PROBE_STEP = 10;

  /** Damping change applied per gradient step. */
  private static final int STEP_SIZE = 20;

  private static final int DAMPING_MIN = 0;

  private static final int DAMPING_MAX = 200;

  private static final int PHASE_BASELINE = 0;

  private static final int PHASE_PROBE = 1;

  private static boolean running;

  private static Listener listener;

  private static LinkManager link_manager;

  private static List<List<LinkType>> groups;

  private static List<int[]> group_offsets;

  private static int[] baseline_damping;

  private static int[] best_damping;

  private static int best_wobble = Integer.MAX_VALUE;

  private static int phase;

  private static int phase_tick;

  private static int probe_index;

  private static int[] gradient_sign;

  private static int baseline_wobble;

  private static long end_millis;

  public static boolean isRunning() {
    return running;
  }

  public static void setListener(final Listener new_listener) {
    listener = new_listener;
  }

  public static boolean start(final LinkManager new_link_manager) {
    return start(new_link_manager, RUN_MILLIS);
  }

  static boolean start(final LinkManager new_link_manager,
      final long run_millis) {
    if (running || new_link_manager == null) {
      return false;
    }
    final List<List<LinkType>> found = collectParameters(new_link_manager);
    if (found.isEmpty()) {
      return false;
    }
    link_manager = new_link_manager;
    groups = found;
    group_offsets = new ArrayList<>();
    baseline_damping = new int[found.size()];
    best_damping = new int[found.size()];
    gradient_sign = new int[found.size()];
    best_wobble = Integer.MAX_VALUE;
    for (int i = 0; i < found.size(); i++) {
      final List<LinkType> group = found.get(i);
      final int[] offsets = new int[group.size()];
      for (int j = 0; j < group.size(); j++) {
        offsets[j] = group.get(j).damping - group.get(0).damping;
      }
      group_offsets.add(offsets);
      baseline_damping[i] = groupDamping(i);
      best_damping[i] = groupDamping(i);
    }
    phase = PHASE_BASELINE;
    phase_tick = 0;
    probe_index = 0;
    end_millis = System.currentTimeMillis() + run_millis;
    running = true;
    WobbleMeter.setOptimizing(true);
    return true;
  }

  public static void abort() {
    if (running) {
      finish();
    }
  }

  /**
   * Advance the run by one tick. Called from the physics path; when no
   * run is active this is a single boolean check.
   */
  public static void tickIfRunning(final LinkManager current_manager) {
    if (!running) {
      return;
    }
    if (current_manager != link_manager || !parametersMatch(current_manager)) {
      finish();
      return;
    }
    if (System.currentTimeMillis() >= end_millis) {
      finish();
      return;
    }
    phase_tick++;
    if (phase_tick < PHASE_TICKS) {
      return;
    }
    phase_tick = 0;
    if (phase == PHASE_BASELINE) {
      final int value = WobbleMeter.getWobbleTenthsPercent();
      if (value == WobbleMeter.NO_VALUE) {
        return;
      }
      baseline_wobble = value;
      if (value < best_wobble) {
        best_wobble = value;
        for (int i = 0; i < groups.size(); i++) {
          best_damping[i] = groupDamping(i);
        }
      }
      for (int i = 0; i < groups.size(); i++) {
        baseline_damping[i] = groupDamping(i);
      }
      probe_index = 0;
      if (applyProbe()) {
        phase = PHASE_PROBE;
      } else {
        doStep();
      }
    } else {
      final int value = WobbleMeter.getWobbleTenthsPercent();
      if (value == WobbleMeter.NO_VALUE) {
        return;
      }
      if (value > baseline_wobble) {
        gradient_sign[probe_index] = 1;
      } else if (value < baseline_wobble) {
        gradient_sign[probe_index] = -1;
      } else {
        gradient_sign[probe_index] = 0;
      }
      setGroupDamping(probe_index, baseline_damping[probe_index]);
      probe_index++;
      if (applyProbe()) {
        phase = PHASE_PROBE;
      } else {
        doStep();
        phase = PHASE_BASELINE;
      }
    }
  }

  private static void finish() {
    for (int i = 0; i < groups.size(); i++) {
      setGroupDamping(i, best_damping[i]);
    }
    running = false;
    WobbleMeter.setOptimizing(false);
    if (listener != null) {
      listener.optimizationFinished();
    }
    link_manager = null;
    groups = null;
    group_offsets = null;
  }

  private static void doStep() {
    for (int i = 0; i < groups.size(); i++) {
      setGroupDamping(i, clamp(groupDamping(i) - STEP_SIZE * gradient_sign[i],
          DAMPING_MIN, DAMPING_MAX));
    }
  }

  /**
   * Probe the next parameter by raising its damping. Returns false when
   * there is nothing left to probe (all at the ceiling), in which case
   * the caller steps immediately.
   */
  private static boolean applyProbe() {
    while (probe_index < groups.size()) {
      final int value = groupDamping(probe_index);
      if (value + PROBE_STEP <= DAMPING_MAX) {
        setGroupDamping(probe_index, value + PROBE_STEP);
        return true;
      }
      gradient_sign[probe_index] = 0;
      probe_index++;
    }
    return false;
  }

  private static int groupDamping(final int group_index) {
    return groups.get(group_index).get(0).damping;
  }

  private static void setGroupDamping(final int group_index,
      final int value) {
    final List<LinkType> members = groups.get(group_index);
    final int[] offsets = group_offsets.get(group_index);
    for (int i = 0; i < members.size(); i++) {
      members.get(i).damping = clamp(value + offsets[i],
          DAMPING_MIN, DAMPING_MAX);
    }
  }

  private static boolean parametersMatch(final LinkManager manager) {
    final List<List<LinkType>> current = collectParameters(manager);
    if (current.size() != groups.size()) {
      return false;
    }
    for (int i = 0; i < current.size(); i++) {
      final List<LinkType> current_group = current.get(i);
      final List<LinkType> group = groups.get(i);
      if (current_group.size() != group.size()) {
        return false;
      }
      for (int j = 0; j < current_group.size(); j++) {
        if (current_group.get(j) != group.get(j)) {
          return false;
        }
      }
    }
    return true;
  }

  private static List<List<LinkType>> collectParameters(
      final LinkManager manager) {
    final boolean any_selected = manager.getNumberOfSelected() > 0;
    final List<List<LinkType>> found_groups = new ArrayList<>();
    final List<LinkType> seen_types = new ArrayList<>();
    final int number_of_links = manager.element.size();
    for (int temp = number_of_links; --temp >= 0;) {
      final Link link = (Link) manager.element.get(temp);
      if (any_selected && !link.type.selected) {
        continue;
      }
      if (link.type.elasticity <= 0) {
        continue;
      }
      boolean seen = false;
      for (int i = 0; i < seen_types.size(); i++) {
        if (seen_types.get(i) == link.type) {
          seen = true;
          break;
        }
      }
      if (seen) {
        continue;
      }
      seen_types.add(link.type);
      boolean grouped = false;
      for (int i = 0; i < found_groups.size(); i++) {
        if (sameMaterial(found_groups.get(i).get(0), link.type)) {
          found_groups.get(i).add(link.type);
          grouped = true;
          break;
        }
      }
      if (!grouped) {
        final List<LinkType> group = new ArrayList<>();
        group.add(link.type);
        found_groups.add(group);
      }
    }
    return found_groups;
  }

  /**
   * Two types are the same material when they agree on every physical
   * property except length: damping is a material property, length is
   * geometry, so rest length alone never splits a material. Damping is
   * deliberately not compared -- a run changes it, and the grouping must
   * not move while it does. Members keep their starting offset from the
   * group's first member, so a run that changes nothing restores every
   * type exactly.
   */
  private static boolean sameMaterial(final LinkType a, final LinkType b) {
    return a.elasticity == b.elasticity
        && a.radius == b.radius
        && a.tension == b.tension
        && a.compression == b.compression
        && a.disabled == b.disabled;
  }

  private static int clamp(final int value, final int low, final int high) {
    if (value < low) {
      return low;
    }
    if (value > high) {
      return high;
    }
    return value;
  }
}
