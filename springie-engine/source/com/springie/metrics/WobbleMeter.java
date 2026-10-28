// This program has been placed into the public domain by its author.

package com.springie.metrics;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Wobble: the average link-length oscillation, as a percentage.
 *
 * <p>While the Wobble page is open, each physics tick records every link's
 * actual length. Over the last 100 ticks, each link's oscillation is its
 * trimmed length range (highest minus lowest sample, with the top and
 * bottom 5% of samples discarded so one-off spikes don't count), divided
 * by that link's mean length. Wobble is the average of those percentages
 * over all links: 0% means no link length is changing, 10% means the
 * average link length oscillates by about 10% of its length.
 *
 * <p>The meter is off by default. It records nothing until setActive(true),
 * so opening the Wobble page is the only thing that starts the sampling.
 */
public class WobbleMeter {
  /** Called on the AWT thread when the computed value changes. */
  public interface Listener {
    void wobbleUpdated(int wobble_tenths_percent, boolean selected_only);
  }

  /** Ticks of link-length history kept per link (about a second). */
  public static final int WINDOW_TICKS = 100;

  /** Recompute the value (and prune dead links) this often, in ticks. */
  public static final int RECOMPUTE_EVERY_TICKS = 10;

  /** Percent of samples trimmed from each end before taking the range. */
  private static final int TRIM_PERCENT = 5;

  /** No value yet (page just opened, or no links with usable lengths). */
  public static final int NO_VALUE = -1;

  private static boolean active;

  private static Listener listener;

  private static int ticks_since_recompute;

  private static int wobble_tenths_percent = NO_VALUE;

  private static boolean scope_known;

  private static boolean last_any_selected;

  private static final Map<Link, LinkSamples> samples_by_link =
      new IdentityHashMap<>();

  private static final class LinkSamples {
    final int[] ring = new int[WINDOW_TICKS];
    int next;
    int count;
  }

  public static boolean isActive() {
    return active;
  }

  public static void setListener(final Listener new_listener) {
    listener = new_listener;
  }

  public static void setActive(final boolean new_active) {
    active = new_active;
    ticks_since_recompute = 0;
    wobble_tenths_percent = NO_VALUE;
    scope_known = false;
    samples_by_link.clear();
  }

  public static int getWobbleTenthsPercent() {
    return wobble_tenths_percent;
  }

  /**
   * Sample one tick's link lengths, if the meter is active. Called from
   * the physics path; when inactive this is a single boolean check.
   * With no links selected, every link is sampled; with some selected,
   * only the selected links are.
   */
  public static void sampleIfActive(final LinkManager link_manager) {
    if (!active || link_manager == null) {
      return;
    }
    final boolean any_selected = link_manager.getNumberOfSelected() > 0;
    if (!scope_known) {
      scope_known = true;
      last_any_selected = any_selected;
    } else if (any_selected != last_any_selected) {
      samples_by_link.clear();
      last_any_selected = any_selected;
    }
    final int number_of_links = link_manager.element.size();
    for (int temp = number_of_links; --temp >= 0;) {
      final Link link = (Link) link_manager.element.get(temp);
      if (any_selected && !link.type.selected) {
        continue;
      }
      LinkSamples samples = samples_by_link.get(link);
      if (samples == null) {
        samples = new LinkSamples();
        samples_by_link.put(link, samples);
      }
      samples.ring[samples.next] = link.getActualLength();
      samples.next = (samples.next + 1) % WINDOW_TICKS;
      if (samples.count < WINDOW_TICKS) {
        samples.count++;
      }
    }
    if (++ticks_since_recompute >= RECOMPUTE_EVERY_TICKS) {
      ticks_since_recompute = 0;
      recompute(link_manager);
    }
  }

  private static void recompute(final LinkManager link_manager) {
    final Map<Link, Boolean> live_links = new IdentityHashMap<>();
    final int number_of_links = link_manager.element.size();
    for (int temp = number_of_links; --temp >= 0;) {
      live_links.put((Link) link_manager.element.get(temp), Boolean.TRUE);
    }
    samples_by_link.keySet().retainAll(live_links.keySet());

    final boolean any_selected = link_manager.getNumberOfSelected() > 0;
    final int[] scratch = new int[WINDOW_TICKS];
    long total_tenths = 0;
    int links_counted = 0;
    for (final Map.Entry<Link, LinkSamples> entry : samples_by_link.entrySet()) {
      if (any_selected && !entry.getKey().type.selected) {
        continue;
      }
      final LinkSamples samples = entry.getValue();
      final int count = samples.count;
      if (count < 2) {
        continue;
      }
      for (int i = 0; i < count; i++) {
        scratch[i] = samples.ring[i];
      }
      Arrays.sort(scratch, 0, count);
      int trim = (count * TRIM_PERCENT) / 100;
      if (count - (2 * trim) < 2) {
        trim = 0;
      }
      final int kept = count - (2 * trim);
      long sum = 0;
      for (int i = trim; i < count - trim; i++) {
        sum += scratch[i];
      }
      final long mean = sum / kept;
      if (mean <= 0) {
        continue;
      }
      final int range = scratch[count - 1 - trim] - scratch[trim];
      total_tenths += (1000L * range) / mean;
      links_counted++;
    }
    final int new_value = links_counted == 0 ? NO_VALUE
        : (int) (total_tenths / links_counted);
    if (new_value != wobble_tenths_percent) {
      wobble_tenths_percent = new_value;
      if (listener != null) {
        listener.wobbleUpdated(new_value, any_selected);
      }
    }
  }
}
