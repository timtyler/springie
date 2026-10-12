//This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Panel;

public class ZColourPickerVHSB extends Panel {
  static final long serialVersionUID = 1250;

  ColourPickerVHSBB colour_picker_v_hsb_b;

  ColourPickerVHSBS colour_picker_v_hsb_s;

  ColourPickerVHSBH colour_picker_v_hsb_h;

  ZColourPickerVHSB() {
    this.colour_picker_v_hsb_h = new ColourPickerVHSBH();
    this.colour_picker_v_hsb_s = new ColourPickerVHSBS();
    this.colour_picker_v_hsb_b = new ColourPickerVHSBB();
    add(this.colour_picker_v_hsb_h);
    add(this.colour_picker_v_hsb_s);
    add(this.colour_picker_v_hsb_b);
  }
}
