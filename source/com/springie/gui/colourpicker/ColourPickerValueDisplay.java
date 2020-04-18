//This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;

import com.springie.io.out.WriteFloatingPoint;

public class ColourPickerValueDisplay extends Panel {
  static final long serialVersionUID = 1250; 

  int cx;

  int cy;

  int cw;

  int ch;

  int xsep;

  int ysep;

  int px;

  int py;

  int sy;

  int satx;

  int saty;

  int satw;

  int sath;

  int mainx;

  int mainy;

  int mainw;

  int mainh;

  String string;
  
  Label label;

  ColourPickerController colour_picker;

  private boolean lastHSB;

  public ColourPickerValueDisplay() {
    setLayout(new GridLayout(1, 0, 0, 0));
    this.label = new Label("", Label.CENTER);
    add(this.label);
    this.label.setFont(new Font("sansserif", Font.PLAIN, 10));
  }

  public void init() {
    this.cx = 10;
    this.cy = 0;

    this.cw = 4;
    this.ch = 8;

    this.xsep = 64;
    this.ysep = 16;

    this.satx = this.xsep * this.cw + this.cx * 2;
    this.satw = this.cx * 2;
    this.saty = this.cy;
    this.sath = this.ysep * this.ch;
    this.sy = this.saty + this.sath;
    this.mainx = this.cx;
    this.mainy = 0;
    this.px = this.cx;
    this.py = this.cy;
    this.mainw = this.xsep * this.cw;
    this.mainh = 0;
  }

  public void inform(ColourPickerController colour_picker) {
    this.colour_picker = colour_picker;
  }

  public void setBasedOnLast() {
    if (this.lastHSB) {
      setHSB();
    } else {
      setRGB();
    }
    }
    
  
  public void setRGB() {
    this.lastHSB = false;
    final int r = this.colour_picker.red; 
    final int g = this.colour_picker.green; 
    final int b = this.colour_picker.blue;
    final float t = this.colour_picker.value_opacity;

    this.string = "Red:" + r + " Green:" + g + " Blue:" + b;
    
    final String str_opaque = getOpaque(t);
    this.string += " Opaque:" + str_opaque;
  }

  public void setHSB() {
    this.lastHSB = true;
    final float h = this.colour_picker.value_hue;
    final float s = this.colour_picker.value_saturation;
    final float b = this.colour_picker.value_brightness;
    final float t = this.colour_picker.value_opacity;

    final String str_hue = WriteFloatingPoint.emit(h, 4, true);
    final String str_sat = WriteFloatingPoint.emit(s, 4, true);
    final String str_bri = WriteFloatingPoint.emit(b, 4, true);

    this.string = "Hue:" + str_hue + " Saturation:" + str_sat + " Brightness:"
      + str_bri;
    
    final String str_opaque = getOpaque(t);
    this.string += " Opaque:" + str_opaque;
  }

  private String getOpaque(final float t) {
    final String str_opaque = WriteFloatingPoint.emit(t, 4, true);
    
    return str_opaque;
  }
  
  void refresh() {

    //}
    //public void paint(Graphics g) {

    //String v = null;

    //if (!this.colour_picker.isColourModelRGB()) {
//    } else {
//      final int colour = this.colour_picker.getARGBColourUsingColourModel(h, s,
//        b, t);
//
//      final String str_red = "" + ((colour >> 16) & 0xFF);
//
//      final String str_green = "" + ((colour >> 8) & 0xFF);
//
//      final String str_blue = "" + (colour & 0xFF);
//
//      v = "Red:" + str_red + " Green:" + str_green + " Blue:" + str_blue;
//    }
    

    this.label.setText(this.string);
  }
}