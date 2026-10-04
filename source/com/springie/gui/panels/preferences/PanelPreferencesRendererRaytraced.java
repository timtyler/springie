// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import com.springie.FrEnd;
import com.springie.gui.components.TTChoice;
import com.springie.render.RendererDelegator;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Ray-traced renderer options: glossiness (the smooth satin sheen),
 * shadows, specular highlights, the Fresnel rim, and the fill light.
 *
 * <p>Each effect is a checkbox (the off switch) with a strength
 * dropdown beside it. The dropdown only shows while its effect is
 * enabled, so unused options stay out of the way.
 */
public class PanelPreferencesRendererRaytraced {
  public Panel panel = FrEnd.setUpPanelForFrame();


  private Effect effect_glossiness;

  private Checkbox checkbox_shadows;

  private Checkbox checkbox_soft_shadows;

  private Checkbox checkbox_reflections;

  private Checkbox checkbox_ambient_occlusion;

  private Panel shadows_row;

  private Effect effect_specular;

  private Effect effect_fresnel;

  /**
   * Container holding Glossiness and Fresnel on one row (Tim, 2026-10-03).
   */
  private Panel gloss_fresnel_row;

  /**
   * The five effect rows, detached from {@link #panel} by
   * {@link #takeEffectRows()}. The Renderer tab adds them directly to its
   * single layout (a GridLayout gives invisible components space, so they
   * are added and removed on renderer switch instead of shown/hidden).
   */
  Panel[] effect_rows;

  public PanelPreferencesRendererRaytraced() {
    makePanel();
  }

  void makePanel() {
    this.effect_glossiness = effectPanel("Glossiness",
        RendererDelegator.glossiness_enabled, RendererDelegator.glossiness,
        new EffectSetter() {
          public void setEnabled(final boolean on) {
            RendererDelegator.glossiness_enabled = on;
          }

          public void setStrength(final int percent) {
            RendererDelegator.glossiness = percent;
          }
        }, false, 50);
    this.panel.add(this.effect_glossiness.panel);

    this.panel.add(panelShadows());

    this.effect_specular = effectPanel("Specular",
        RendererDelegator.specular_enabled, RendererDelegator.specular,
        new EffectSetter() {
          public void setEnabled(final boolean on) {
            RendererDelegator.specular_enabled = on;
          }

          public void setStrength(final int percent) {
            RendererDelegator.specular = percent;
          }
        }, true, 100);
    this.panel.add(this.effect_specular.panel);

    this.effect_fresnel = effectPanel("Fresnel",
        RendererDelegator.fresnel_enabled, RendererDelegator.fresnel,
        new EffectSetter() {
          public void setEnabled(final boolean on) {
            RendererDelegator.fresnel_enabled = on;
          }

          public void setStrength(final int percent) {
            RendererDelegator.fresnel = percent;
          }
        }, false, 50);
    this.panel.add(this.effect_fresnel.panel);

    // Note: syncSimpleLighting() is NOT called here; the rows are
    // detached by takeEffectRows() and the Renderer tab syncs them
    // on renderer switch (Tim, 2026-10-03).
  }

  /**
   * Detaches the five effect rows from {@link #panel} and returns them.
   * The Renderer tab owns the single layout now; it adds and removes
   * these rows when the renderer is switched.
   */
  Panel[] takeEffectRows() {
    final Component[] rows = this.panel.getComponents();
    this.effect_rows = new Panel[rows.length];
    for (int i = 0; i < rows.length; i++) {
      this.effect_rows[i] = (Panel) rows[i];
      this.panel.remove(rows[i]);
    }
    return this.effect_rows;
  }

  private interface EffectSetter {
    void setEnabled(boolean on);

    void setStrength(int percent);
  }

  /** One effect row and its default, so resetToDefaults can reach it. */
  private static final class Effect {
    final Panel panel;

    final Checkbox checkbox;

    final TTChoice choice;

    final EffectSetter setter;

    final boolean default_on;

    final int default_strength;

    Effect(Panel panel, Checkbox checkbox, TTChoice choice,
        EffectSetter setter, boolean default_on, int default_strength) {
      this.panel = panel;
      this.checkbox = checkbox;
      this.choice = choice;
      this.setter = setter;
      this.default_on = default_on;
      this.default_strength = default_strength;
    }

    void resetToDefaults() {
      // Strength first: Choice.select fires no event, so set the
      // renderer field directly.
      this.setter.setStrength(this.default_strength);
      this.choice.choice.select(
          this.choice.num_to_str(this.default_strength));
      this.choice.choice.setVisible(this.default_on);
      // The checkbox listener copies the state into the renderer and
      // syncs the dropdown visibility when the state actually changes.
      this.checkbox.setState(this.default_on);
      // In case it was already in its default state (no item event).
      this.setter.setEnabled(this.default_on);
    }
  }

  /**
   * One effect row: a checkbox (the off switch) and a 10-100% strength
   * dropdown that only shows while the effect is enabled.
   */
  private Effect effectPanel(final String name, final boolean enabled, final int strength,
      final EffectSetter setter, final boolean default_on,
      int default_strength) {
    final Panel panel = new Panel();

    final Checkbox checkbox = new Checkbox(name, enabled);
    // Holder so the listener can use the TTChoice's own mapping.
    final TTChoice[] holder = new TTChoice[1];
    final TTChoice tt_choice = new TTChoice(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        final String scs = (String) e.getItem();
        setter.setStrength(holder[0].str_to_num(scs));
      }
    });
    holder[0] = tt_choice;
    for (int percent = 10; percent <= 100; percent += 10) {
      tt_choice.add(percent + "%", percent);
    }
    tt_choice.choice.select(tt_choice.num_to_str(strength));

    checkbox.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        final boolean on = ((Checkbox) e.getSource()).getState();
        setter.setEnabled(on);
        tt_choice.choice.setVisible(on);
        // Re-flow the strip now that the dropdown appeared or left.
        PanelPreferencesRendererRaytraced.this.panel.validate();
      }
    });

    tt_choice.choice.setVisible(enabled);
    panel.add(checkbox);
    panel.add(tt_choice.choice);
    return new Effect(panel, checkbox, tt_choice, setter, default_on,
        default_strength);
  }

  private Panel panelShadows() {
    final Panel panel = new Panel();

    this.checkbox_shadows = new Checkbox("Shadows",
        RendererDelegator.shadows);
    this.checkbox_shadows.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        RendererDelegator.shadows = PanelPreferencesRendererRaytraced.this.checkbox_shadows
            .getState();
      }
    });
    panel.add(this.checkbox_shadows);

    this.checkbox_soft_shadows = new Checkbox("Soft shadows",
        RendererDelegator.soft_shadows);
    this.checkbox_soft_shadows.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        RendererDelegator.soft_shadows = PanelPreferencesRendererRaytraced.this.checkbox_soft_shadows
            .getState();
      }
    });
    panel.add(this.checkbox_soft_shadows);

    this.checkbox_reflections = new Checkbox("Reflections",
        RendererDelegator.reflections_enabled);
    this.checkbox_reflections.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        RendererDelegator.reflections_enabled = PanelPreferencesRendererRaytraced.this.checkbox_reflections
            .getState();
      }
    });
    panel.add(this.checkbox_reflections);

    this.checkbox_ambient_occlusion = new Checkbox("Ambient occlusion",
        RendererDelegator.ambient_occlusion);
    this.checkbox_ambient_occlusion.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        RendererDelegator.ambient_occlusion = PanelPreferencesRendererRaytraced.this.checkbox_ambient_occlusion
            .getState();
      }
    });
    panel.add(this.checkbox_ambient_occlusion);

    this.shadows_row = panel;
    return panel;
  }

  /**
   * When Simple lighting is on (the "Ray-traced (fast)" renderer), the
   * phong/specular/gloss/Fresnel/shadow controls are irrelevant
   * (shade() bypasses them), so remove them from the UI. They are added
   * and removed (never just hidden) because the tab's GridLayout gives
   * invisible components space. (Tim, 2026-10-03: was setVisible, which
   * left the space occupied.)
   */
  public void syncSimpleLighting() {
    final boolean simple = RendererDelegator.simple_lighting;
    // The rows live in the shared tab (see takeEffectRows), not in
    // this.panel. Remove/add them from their actual parent.
    final java.awt.Container parent = this.effect_glossiness.panel.getParent();
    if (parent == null) {
      // Not yet attached to the tab; the tab will call this again.
      return;
    }
    final Panel[] rows = {
        this.effect_glossiness.panel,
        this.shadows_row,
        this.effect_specular.panel,
        this.effect_fresnel.panel,
    };
    for (final Panel row : rows) {
      parent.remove(row);
    }
    if (!simple) {
      for (final Panel row : rows) {
        parent.add(row);
      }
    }
    parent.validate();
  }

  public void resetToDefaults() {
    this.effect_glossiness.resetToDefaults();

    RendererDelegator.shadows = false;
    this.checkbox_shadows.setState(false);
    this.syncSimpleLighting();

    this.effect_specular.resetToDefaults();
    this.effect_fresnel.resetToDefaults();
  }
}
