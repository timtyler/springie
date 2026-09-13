// This code has been placed into the public domain by its author.

package com.springie.modification;

import java.util.ArrayList;
import java.util.List;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.muscles.GlobalOscillatorController;
import com.springie.muscles.Muscles;

/**
 * Attaches and detaches muscle controllers to links.
 *
 * <p>Phases are staggered across the muscled links so the global pulse
 * travels as a wave instead of breathing in unison.
 */
public final class MuscleTools {
  private MuscleTools() {
    // static-only
  }

  public static void addToSelected() {
    final List<Link> targets = new ArrayList<>();
    final LinkManager link_manager = ContextManager.getNodeManager().getLinkManager();
    final int n_o_l = link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) link_manager.element.get(temp);
      if (link.type.selected) {
        targets.add(link);
      }
    }
    attach(targets);
  }

  public static void addToAll() {
    final List<Link> targets = new ArrayList<>();
    final LinkManager link_manager = ContextManager.getNodeManager().getLinkManager();
    final int n_o_l = link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      targets.add((Link) link_manager.element.get(temp));
    }
    attach(targets);
  }

  private static void attach(List<Link> targets) {
    final int count = targets.size();
    for (int i = 0; i < count; i++) {
      final int phase = count == 0 ? 0 : (i * Muscles.period_ticks) / count;
      targets.get(i).controller = new GlobalOscillatorController(phase);
    }
  }

  public static void remove() {
    final LinkManager link_manager = ContextManager.getNodeManager().getLinkManager();
    final int n_o_l = link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) link_manager.element.get(temp);
      link.controller = null;
      link.rest_length_scale = Muscles.UNITY;
    }
  }
}
