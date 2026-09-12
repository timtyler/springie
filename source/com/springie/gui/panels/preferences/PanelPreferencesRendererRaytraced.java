// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Checkbox;
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
 * Ray-traced renderer options: glossiness (how mirror-like surfaces are),
 * the maximum reflection bounce depth, shadows, and specular highlights.
 */
public class PanelPreferencesRendererRaytraced {
  public Panel panel = FrEnd.setUpPanelForFrame();

  MessageManager message_manager;

  private TTChoice choose_glossiness;

  private TTChoice choose_max_bounces;

  private Checkbox checkbox_shadows;

  private TTChoice choose_specular;

  public PanelPreferencesRendererRaytraced(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    this.panel.add(panelGlossiness());
    this.panel.add(panelMaxBounces());
    this.panel.add(panelShadows());
    this.panel.add(panelSpecular());
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

  private Panel panelShadows() {
    final Panel panel = new Panel();

    this.checkbox_shadows = new Checkbox("Shadows",
        RendererDelegator.shadows);
    this.checkbox_shadows.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        RendererDelegator.shadows = PanelPreferencesRendererRaytraced.this.checkbox_shadows
            .getState();
      }
    });
    panel.add(this.checkbox_shadows);

    return panel;
  }

  private Panel panelSpecular() {
    final Panel panel = new Panel();
    final Label label = new Label("Specular:");
    panel.add(label);

    this.choose_specular = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererRaytraced.this.choose_specular
            .str_to_num(scs);
        RendererDelegator.specular = val;
      }
    });

    for (int percent = 0; percent <= 100; percent += 10) {
      this.choose_specular.add(percent + "%", percent);
    }
    this.choose_specular.choice.select(
        this.choose_specular.num_to_str(RendererDelegator.specular));
    panel.add(this.choose_specular.choice);

    return panel;
  }

  public void resetToDefaults() {
    RendererDelegator.glossiness = 0;
    this.choose_glossiness.choice.select(
        this.choose_glossiness.num_to_str(0));

    RendererDelegator.max_bounces = 2;
    this.choose_max_bounces.choice.select(
        this.choose_max_bounces.num_to_str(2));

    RendererDelegator.shadows = false;
    this.checkbox_shadows.setState(false);

    RendererDelegator.specular = 50;
    this.choose_specular.choice.select(
        this.choose_specular.num_to_str(50));
  }
}
