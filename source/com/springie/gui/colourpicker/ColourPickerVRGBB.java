// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

public class ColourPickerVRGBB extends ColourPickerBase {
  static final long serialVersionUID = 1250;

  public void update(Graphics g) {
    paintHelper();

    for (int j = this.margin; j < this.range; j++) {
      final int red = this.colour_picker.red;
      final int green = this.colour_picker.green;
      final int blue = (j * 255) / this.range;

      final float o = this.colour_picker.value_opacity;

      final int col = this.colour_picker.getColourUsingRGBColourModel(red,
          green, blue, o);
      g.setColor(new java.awt.Color(col));
      g.fillRect(j, this.margin, 1, this.height - this.margin * 2);
    }

    final int value = this.colour_picker.blue;

    paintString(g, this, "Blue: " + value);
    paintMarkers(g, value);
  }

  public void mouseClicked(MouseEvent e) {
    clickHelper(e);

    this.colour_picker.blue = (this.x * 255) / this.range;
    this.colour_picker.setRGB();
    this.colour_picker.repaintChildren();
  }
}
