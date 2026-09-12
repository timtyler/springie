// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.gui.components.TTChoice;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

/**
 * Ray-traced renderer options: glossiness (how mirror-like surfaces are)
 * and the maximum reflection bounce depth.
 */
public class PanelPreferencesRendererRaytraced {
  public Panel panel = FrEnd.setUpPanelForFrame();

  MessageManager message_manager;

  private TTChoice choose_glossiness;

  private TTChoice choose_max_bounces;

  public PanelPreferencesRendererRaytraced(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    this.panel.add(panelGlossiness());
    this.panel.add(panelMaxBounces());
  }

  private Panel panelGlossiness() {
    final Panel panel = new Panel();
    final Label label = new Label("Glossiness:");
    panel.add(label);

    this.choose_glossiness = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererRaytraced.this.choose_glossiness
            .str_to_num(scs);
        RendererDelegator.glossiness = val;
      }
    });

    for (int percent = 0; percent <= 100; percent += 10) {
      this.choose_glossiness.add(percent + "%", percent);
    }
    this.choose_glossiness.choice.select(
        this.choose_glossiness.num_to_str(RendererDelegator.glossiness));
    panel.add(this.choose_glossiness.choice);

    return panel;
  }

  private Panel panelMaxBounces() {
    final Panel panel = new Panel();
    final Label label = new Label("Max bounces:");
    panel.add(label);

    this.choose_max_bounces = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererRaytraced.this.choose_max_bounces
            .str_to_num(scs);
        RendererDelegator.max_bounces = val;
      }
    });

    for (int bounces = 0; bounces <= 4; bounces++) {
      this.choose_max_bounces.add("" + bounces, bounces);
    }
    this.choose_max_bounces.choice.select(
        this.choose_max_bounces.num_to_str(RendererDelegator.max_bounces));
    panel.add(this.choose_max_bounces.choice);

    return panel;
  }

  public void resetToDefaults() {
    RendererDelegator.glossiness = 50;
    this.choose_glossiness.choice.select(
        this.choose_glossiness.num_to_str(50));

    RendererDelegator.max_bounces = 2;
    this.choose_max_bounces.choice.select(
        this.choose_max_bounces.num_to_str(2));
  }
}
