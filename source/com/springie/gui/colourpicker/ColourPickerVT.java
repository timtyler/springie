//This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import com.springie.io.out.WriteFloatingPoint;

public class ColourPickerVT extends ColourPickerBase {
  static final long serialVersionUID = 1250; 
  public void update(Graphics g) {
    paintHelper();

    for (int j = this.margin; j < this.range; j++) {
      float b = (j + (j & 15)) / (float) this.range;
      if (b > 1f) {
        b = 1f;
      }
      
      final int col = Color.HSBtoRGB(0, 0, b);
      g.setColor(new java.awt.Color(col));
      g.fillRect(j, this.margin, 1, this.height - this.margin * 2);
    }

    final float o = this.colour_picker.value_opacity;
    final String str = WriteFloatingPoint.emit(o, 4, true);

    paintString(g, this, "Opacity:" + str, 0);
    paintMarkers(g, this.colour_picker.value_opacity);
  }

  public void mouseClicked(MouseEvent e) {
    clickHelper(e);

    this.colour_picker.value_opacity = this.x / (float) this.range;
    //setBasedOnLast();
    //this.colour_picker.cpfcv.setBasedOnLast();
    this.colour_picker.repaintChildren();
  }
}
