// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

import com.springie.io.out.WriteFloatingPoint;

public class ColourPickerVHSBH extends ColourPickerBase {
  static final long serialVersionUID = 1250; 
  public void update(Graphics g) {
    paintHelper();

    for (int j = this.margin; j < this.range; j++) {
      final float h = j / (float) this.range;
      final float s = this.colour_picker.value_saturation;
      final float b = this.colour_picker.value_brightness;
      final float o = this.colour_picker.value_opacity;
      final int col = this.colour_picker.getColourUsingHSBColourModel(h, s, b,
        o);
      g.setColor(new java.awt.Color(col));
      g.fillRect(j, this.margin, 1, this.height - this.margin * 2);
    }

    final float h = this.colour_picker.value_hue;
    final String str_hue = WriteFloatingPoint.emit(h, 4, true);

    paintString(g, this, "Hue:" + str_hue);

    paintMarkers(g, this.colour_picker.value_hue);
  }

  public void mouseClicked(MouseEvent e) {
    clickHelper(e);

    this.colour_picker.value_hue = this.x / (float) this.range;
    this.colour_picker.setHSB();
    this.colour_picker.repaintChildren();
  }
}