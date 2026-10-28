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
 * only the selected types with one. Each gradient step perturbs every
 * parameter up by a fixed amount, measures wobble over a full meter
 * window, and steps every parameter in the downhill direction by a fixed
 * amount. The run ends by restoring the best damping seen, so a step
 * that overshoots can never leave the model worse than it started.
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

  private static List<LinkType> parameters;

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
    final List<LinkType> found = collectParameters(new_link_manager);
    if (found.isEmpty()) {
      return false;
    }
    link_manager = new_link_manager;
    parameters = found;
    baseline_damping = new int[found.size()];
    best_damping = new int[found.size()];
    gradient_sign = new int[found.size()];
    best_wobble = Integer.MAX_VALUE;
    for (int i = 0; i < found.size(); i++) {
      baseline_damping[i] = found.get(i).damping;
      best_damping[i] = found.get(i).damping;
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
        for (int i = 0; i < parameters.size(); i++) {
          best_damping[i] = parameters.get(i).damping;
        }
      }
      for (int i = 0; i < parameters.size(); i++) {
        baseline_damping[i] = parameters.get(i).damping;
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
      parameters.get(probe_index).damping = baseline_damping[probe_index];
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
    for (int i = 0; i < parameters.size(); i++) {
      parameters.get(i).damping = best_damping[i];
    }
    running = false;
    WobbleMeter.setOptimizing(false);
    if (listener != null) {
      listener.optimizationFinished();
    }
    link_manager = null;
    parameters = null;
  }

  private static void doStep() {
    for (int i = 0; i < parameters.size(); i++) {
      final LinkType type = parameters.get(i);
      type.damping = clamp(type.damping - STEP_SIZE * gradient_sign[i],
          DAMPING_MIN, DAMPING_MAX);
    }
  }

  /**
   * Probe the next parameter by raising its damping. Returns false when
   * there is nothing left to probe (all at the ceiling), in which case
   * the caller steps immediately.
   */
  private static boolean applyProbe() {
    while (probe_index < parameters.size()) {
      final LinkType type = parameters.get(probe_index);
      if (type.damping + PROBE_STEP <= DAMPING_MAX) {
        type.damping = type.damping + PROBE_STEP;
        return true;
      }
      gradient_sign[probe_index] = 0;
      probe_index++;
    }
    return false;
  }

  private static boolean parametersMatch(final LinkManager manager) {
    final List<LinkType> current = collectParameters(manager);
    if (current.size() != parameters.size()) {
      return false;
    }
    for (int i = 0; i < current.size(); i++) {
      if (current.get(i) != parameters.get(i)) {
        return false;
      }
    }
    return true;
  }

  private static List<LinkType> collectParameters(
      final LinkManager manager) {
    final boolean any_selected = manager.getNumberOfSelected() > 0;
    final List<LinkType> found = new ArrayList<>();
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
      for (int i = 0; i < found.size(); i++) {
        if (found.get(i) == link.type) {
          seen = true;
          break;
        }
      }
      if (!seen) {
        found.add(link.type);
      }
    }
    return found;
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
