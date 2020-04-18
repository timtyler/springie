// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.GridLayout;
import java.awt.Panel;

public class ColourPickerGettersAndPutters extends Panel {
  static final long serialVersionUID = 1250;

  private ColourPickerController colour_picker;

  public ColourPickerGetAndPut one;

  public ColourPickerGetAndPut two;

  public ColourPickerGetAndPut three;

  public void inform(ColourPickerController colour_picker) {
    this.setLayout(new GridLayout(1, 0, 0, 0));

    this.colour_picker = colour_picker;
    this.one.inform(colour_picker);
    this.two.inform(colour_picker);
    this.three.inform(colour_picker);
  }

  public ColourPickerGettersAndPutters() {
    this.one = new ColourPickerGetAndPut();
    this.add(this.one);

    this.two = new ColourPickerGetAndPut();
    this.add(this.two);

    this.three = new ColourPickerGetAndPut();
    this.add(this.three);
  }

  public ColourPickerController getColourPicker() {
    return this.colour_picker;
  }

  public void greyGetAndSetColourButtons(int colour) {
    this.one.greyGetAndSetColourButtons(colour);
    this.two.greyGetAndSetColourButtons(colour);
    this.three.greyGetAndSetColourButtons(colour);
  }
}
