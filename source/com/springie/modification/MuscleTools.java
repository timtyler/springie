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
 * <p>Every attached controller is linked to the active oscillator
 * ({@link Muscles#active_oscillator}); amplitude and period live
 * in the oscillator, phase lives in each link.
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
      targets.get(i).controller = new GlobalOscillatorController(Muscles.active_oscillator);
    }
  }

  public static void remove() {
    final LinkManager link_manager = ContextManager.getNodeManager().getLinkManager();
    final int n_o_l = link_manager.element.size();
    for (int temp = n_o_l; --temp >= 0;) {
      final Link link = (Link) link_manager.element.get(temp);
      link.controller = null;
      link.adjusted_rest_length = link.type.length;
    }
  }
}
