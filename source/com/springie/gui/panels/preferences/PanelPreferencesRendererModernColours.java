// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.Color;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.colourpicker.ColourPicker;
import com.springie.gui.colourpicker.ColourPickerInformer;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ElementRendererLink;

public class PanelPreferencesRendererModernColours {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  final TabbedPanel tab_colours_main = new TabbedPanel();

  final TabbedPanel tab_colours_general = new TabbedPanel();

  final TabbedPanel tab_colours_label = new TabbedPanel();

  MessageManager message_manager;

  ColourPicker panel_colours_charges;

  ColourPicker panel_colours_selection;

  ColourPicker panel_colours_background;

  ColourPicker panel_colours_label;

  ColourPicker panel_colours_label_background;

  public PanelPreferencesRendererModernColours(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    this.panel_colours_background = new ColourPicker(
        new ColourPickerInformer() {
          public void inform(int colour) {
            RendererDelegator.colour_background_number = colour;
            RendererDelegator.colour_background = new Color(colour);
            RendererDelegator.repaint_all_objects = true;
          }
        });
    this.panel_colours_background.colour_picker_controller
        .setColour(RendererDelegator.colour_background_number);

    this.panel_colours_selection = new ColourPicker(new ColourPickerInformer() {
      public void inform(int colour) {
        RendererDelegator.colour_selected_number = colour;
        RendererDelegator.colour_selected = new Color(colour);
        RendererDelegator.repaint_all_objects = true;
      }
    });
    this.panel_colours_selection.colour_picker_controller
        .setColour(RendererDelegator.colour_selected_number);

    this.panel_colours_charges = new ColourPicker(new ColourPickerInformer() {
      public void inform(int colour) {
        RendererDelegator.colour_charge_number = colour;
        RendererDelegator.repaint_all_objects = true;
      }
    });
    this.panel_colours_charges.colour_picker_controller
        .setColour(RendererDelegator.colour_charge_number);

    this.panel_colours_label = new ColourPicker(new ColourPickerInformer() {
      public void inform(int colour) {
        ElementRendererLink.colour_fg = colour;
        RendererDelegator.repaint_all_objects = true;
      }
    });
    this.panel_colours_label.colour_picker_controller
        .setColour(ElementRendererLink.colour_fg);

    this.panel_colours_label_background = new ColourPicker(
        new ColourPickerInformer() {
          public void inform(int colour) {
            ElementRendererLink.colour_bg = colour;
            RendererDelegator.repaint_all_objects = true;
          }
        });
    this.panel_colours_label_background.colour_picker_controller
        .setColour(ElementRendererLink.colour_bg);

    this.tab_colours_general.add("Background", this.panel_colours_background.panel);
    this.tab_colours_general.add("Selection", this.panel_colours_selection.panel);

    this.tab_colours_label.add("Background", this.panel_colours_label_background.panel);
    this.tab_colours_label.add("Foreground", this.panel_colours_label.panel);

    this.tab_colours_main.add("General", this.tab_colours_general);
    this.tab_colours_main.add("Label", this.tab_colours_label);
    this.tab_colours_main.add("Charges", this.panel_colours_charges.panel);

    this.panel.add(this.tab_colours_main);
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
