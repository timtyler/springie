// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Button;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.tifsoft.Forget;

public class ColourPickerPreview extends Panel {
  static final long serialVersionUID = 1250;

  public Panel panel_proposed = new Panel();

  public Panel panel_original = new Panel();

  public Panel panel_last = new Panel();

  public Button button_set;

  public Button button_last;

  public Button button_reset;

  private ColourPickerController colour_picker;

  private int colour;

  private int last_colour;

  private int original_colour;

  ColourPickerInformer informer;

  public ColourPickerPreview(ColourPickerInformer informer) {
    this.informer = informer;

    final ActionListener action_listener_set = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        final int colour = getColour();
        ColourPickerPreview.this.last_colour = colour;
        ColourPickerPreview.this.original_colour = colour;
        ColourPickerPreview.this.informer.inform(colour);
        refresh();
      }
    };

    this.button_set = new Button("Set");
    this.button_set.addActionListener(action_listener_set);
    this.panel_proposed.add(this.button_set);

    final ActionListener action_listener_put_original = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        final int colour = ColourPickerPreview.this.original_colour;
        ColourPickerPreview.this.colour_picker.setColour(colour);
      }
    };

    this.button_reset = new Button("Put");
    this.button_reset.addActionListener(action_listener_put_original);
    this.panel_original.add(this.button_reset);

    final ActionListener action_listener_put_last = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        final int colour = ColourPickerPreview.this.last_colour;
        ColourPickerPreview.this.colour_picker.setColour(colour);
      }
    };

    this.button_last = new Button("Put");
    this.button_last.addActionListener(action_listener_put_last);
    this.panel_last.add(this.button_last);

    this.setLayout(new GridLayout(1, 0, 0, 0));
    this.add(this.panel_original);
    this.add(this.panel_last);
    this.add(this.panel_proposed);

    this.setBackground(Color.black);
  }

  public void inform(ColourPickerController colour_picker) {
    this.colour_picker = colour_picker;
  }

  public void refresh() {
    this.colour = this.colour_picker.getColour();
    this.panel_original.setBackground(new Color(this.original_colour));
    this.panel_last.setBackground(new Color(this.last_colour));
    this.panel_proposed.setBackground(new Color(this.colour));
  }

  public ColourPickerController getColourPicker() {
    return this.colour_picker;
  }

  public int getColour() {
    return this.colour;
  }

  public void setOriginalColour(int colour) {
    this.original_colour = colour;
  }

  public void setColourPicker(ColourPickerController colour_picker) {
    this.colour_picker = colour_picker;
  }
}
