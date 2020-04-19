// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.gui.colourpicker.ColorPicker;
import com.springie.gui.colourpicker.ColorPickerInformer;
import com.springie.gui.components.TabbedPanel;
import com.springie.gui.components.TTChoice;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ColourModifier;
import com.springie.render.modules.modern.RendererBinManager;

public class PanelPreferencesRendererModernFilters {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_filter_colours = FrEnd.setUpPanelForFrame2();

  public Panel panel_filtering = FrEnd.setUpPanelForFrame2();

  public ColorPicker panel_colour_filter_a;

  public ColorPicker panel_colour_filter_b;

  MessageManager message_manager;

  private TTChoice choose_colour_modifier_filled;

  private TTChoice choose_colour_modifier_wireframe;

  public PanelPreferencesRendererModernFilters(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    this.panel_colour_filter_a = new ColorPicker(
        new ColorPickerInformer() {
          public void inform(int colour) {
            ColourModifier.colour_a_number = colour;
            RendererDelegator.repaintAll();
          }
        });
    this.panel_colour_filter_a.color_picker_controller.setColour(ColourModifier.colour_a_number);

    this.panel_colour_filter_b = new ColorPicker(
        new ColorPickerInformer() {
          public void inform(int colour) {
            ColourModifier.colour_b_number = colour;
            RendererDelegator.repaintAll();
          }
        });
    this.panel_colour_filter_b.color_picker_controller.setColour(ColourModifier.colour_b_number);

    final TabbedPanel tab_filter_colours = new TabbedPanel();
    tab_filter_colours.add("Colour-A", this.panel_colour_filter_a.panel);

    tab_filter_colours.add("Colour-B", this.panel_colour_filter_b.panel);

    this.panel_filter_colours.add(tab_filter_colours);

    final Panel panel_colour_modifier_filled = panelColourModifierFilled();

    final Panel panel_colour_modifier_wireframe = panelColourModifierWireframe();

    
    this.panel_filtering.add(panel_colour_modifier_filled);

    this.panel_filtering.add(panel_colour_modifier_wireframe);

    this.panel.setLayout(new BorderLayout());

    this.panel.add(this.panel_filtering, BorderLayout.NORTH);

    this.panel.add(tab_filter_colours, BorderLayout.CENTER);
  }

  private Panel panelColourModifierWireframe() {
    final Panel panel = new Panel();
    panel.add(new Label("Wireframe:", Label.RIGHT));

    this.choose_colour_modifier_wireframe = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererModernFilters.this.choose_colour_modifier_wireframe
            .str_to_num(scs);
        RendererBinManager.colour_modifier_wireframe = val;
        RendererDelegator.repaintAll();
      }
    });

    addColourOptions(this.choose_colour_modifier_wireframe);

    this.choose_colour_modifier_wireframe.choice
        .select(this.choose_colour_modifier_filled
            .num_to_str(RendererBinManager.colour_modifier_wireframe));
    panel.add(this.choose_colour_modifier_wireframe.choice);

    return panel;
  }

  private Panel panelColourModifierFilled() {
    final Panel panel = new Panel();
    panel.add(new Label("Filled:", Label.RIGHT));

    this.choose_colour_modifier_filled = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererModernFilters.this.choose_colour_modifier_filled
            .str_to_num(scs);
        RendererBinManager.colour_modifier_filled = val;
        RendererDelegator.repaintAll();
      }
    });

    addColourOptions(this.choose_colour_modifier_filled);

    this.choose_colour_modifier_filled.choice
        .select(this.choose_colour_modifier_filled
            .num_to_str(RendererBinManager.colour_modifier_filled));
    panel.add(this.choose_colour_modifier_filled.choice);

    return panel;
  }

  private void addColourOptions(TTChoice choice) {
    choice.add("Disabled", ColourModifier.disabled);
    choice.add("Natural", ColourModifier.natural);
    choice.add("Lighter", ColourModifier.lighter);
    choice.add("Darker", ColourModifier.darker);
    choice.add("Greyscale", ColourModifier.grey);
    choice.add("Colour-A", ColourModifier.colour_a);
    choice.add("Colour-B", ColourModifier.colour_b);
  }

  
  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}