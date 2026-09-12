// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Checkbox;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.gui.components.TTChoice;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.tifsoft.Forget;

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

  MessageManager message_manager;

  private Effect effect_glossiness;

  private Checkbox checkbox_shadows;

  private Effect effect_specular;

  private Effect effect_fresnel;

  private Effect effect_fill_light;

  public PanelPreferencesRendererRaytraced(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    this.effect_glossiness = effectPanel("Glossiness",
        RendererDelegator.glossiness_enabled, RendererDelegator.glossiness,
        new EffectSetter() {
          public void setEnabled(boolean on) {
            RendererDelegator.glossiness_enabled = on;
          }

          public void setStrength(int percent) {
            RendererDelegator.glossiness = percent;
          }
        }, false, 50);
    this.panel.add(this.effect_glossiness.panel);

    this.panel.add(panelShadows());

    this.effect_specular = effectPanel("Specular",
        RendererDelegator.specular_enabled, RendererDelegator.specular,
        new EffectSetter() {
          public void setEnabled(boolean on) {
            RendererDelegator.specular_enabled = on;
          }

          public void setStrength(int percent) {
            RendererDelegator.specular = percent;
          }
        }, true, 90);
    this.panel.add(this.effect_specular.panel);

    this.effect_fresnel = effectPanel("Fresnel",
        RendererDelegator.fresnel_enabled, RendererDelegator.fresnel,
        new EffectSetter() {
          public void setEnabled(boolean on) {
            RendererDelegator.fresnel_enabled = on;
          }

          public void setStrength(int percent) {
            RendererDelegator.fresnel = percent;
          }
        }, false, 50);
    this.panel.add(this.effect_fresnel.panel);

    this.effect_fill_light = effectPanel("Fill light",
        RendererDelegator.fill_light_enabled, RendererDelegator.fill_light,
        new EffectSetter() {
          public void setEnabled(boolean on) {
            RendererDelegator.fill_light_enabled = on;
          }

          public void setStrength(int percent) {
            RendererDelegator.fill_light = percent;
          }
        }, false, 50);
    this.panel.add(this.effect_fill_light.panel);
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
  private Effect effectPanel(String name, boolean enabled, int strength,
      final EffectSetter setter, boolean default_on,
      int default_strength) {
    final Panel panel = new Panel();

    final Checkbox checkbox = new Checkbox(name, enabled);
    // Holder so the listener can use the TTChoice's own mapping.
    final TTChoice[] holder = new TTChoice[1];
    final TTChoice tt_choice = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
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
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
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
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        RendererDelegator.shadows = PanelPreferencesRendererRaytraced.this.checkbox_shadows
            .getState();
      }
    });
    panel.add(this.checkbox_shadows);

    return panel;
  }

  public void resetToDefaults() {
    this.effect_glossiness.resetToDefaults();

    RendererDelegator.shadows = false;
    this.checkbox_shadows.setState(false);

    this.effect_specular.resetToDefaults();
    this.effect_fresnel.resetToDefaults();
    this.effect_fill_light.resetToDefaults();
  }
}
