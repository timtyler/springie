// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Button;
import java.awt.Color;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.springie.gui.components.ComponentAccess;
import com.tifsoft.Forget;

public class ColourPickerGetAndPut extends Panel {
  static final long serialVersionUID = 1250; 
  private ColourPickerController colour_picker;

  int colour;

  public Button button_get;

  public Button button_put;

  public void inform(ColourPickerController colour_picker) {
    this.colour_picker = colour_picker;
  }

  public ColourPickerGetAndPut() {
    this.button_get = new Button("Get");
    this.button_get.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        setColour();
      }
    });

    add(this.button_get);

    this.button_put = new Button("Put");
    this.button_put.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getColourPicker().setCurrentColour(getColour());
      }
    });

    add(this.button_put);
  }

  public void setColour() {
    this.colour = this.colour_picker.getColour();

    getColourPickerGetAndSet().setBackground(new Color(this.colour));
    this.colour_picker.greyGetAndSetColourButtons();
  }

  public ColourPickerController getColourPicker() {
    return this.colour_picker;
  }

  public int getColour() {
    return this.colour;
  }

  public ColourPickerGetAndPut getColourPickerGetAndSet() {
    return this;
  }

  public void greyGetAndSetColourButtons(int colour) {
    final boolean same = getColour() == colour;
    //this.cp_gas.greyGetAndSetColourButtons(getColour());
    ComponentAccess.setAccess(this.button_get, !same);
    ComponentAccess.setAccess(this.button_put, !same);
  }
}