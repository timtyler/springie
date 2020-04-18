// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.Color;

public class ColourPickerController {
  static final long serialVersionUID = 1250;

  float value_hue = 0.5f;

  float value_saturation = 0.7f;

  float value_brightness = 0.9f;

  float value_opacity = 1.0f;

  int red;

  int green;

  int blue;

  int colour;

  ColourPickerVHSBH cp_v_hsb_h;

  ColourPickerVHSBS cp_v_hsb_s;

  ColourPickerVHSBB cp_v_hsb_b;

  ColourPickerVRGBR cp_v_rgb_r;

  ColourPickerVRGBG cp_v_rgb_g;

  ColourPickerVRGBB cp_v_rgb_b;

  ColourPickerVT cpt;

  public ColourPickerPreview cp_preview;

  //ColourPickerValueDisplay cpfcv;

  ColourPickerGettersAndPutters cp_gas;

  // boolean colour_model_rgb;

  public ColourPickerController() {
    //final Label l = new Label("Colour model:");
    //add(l);
  }

  public void setColour(int x) {
    setColourHelper(x);

    this.cp_preview.setOriginalColour(getColour());

    repaintChildren();
  }

  public void setCurrentColour(int x) {
    setColourHelper(x);
    
    repaintChildren();
  }

  private void setColourHelper(int x) {
    final int o = (x >> 24) & 0xFF;
    final int r = (x >> 16) & 0xFF;
    final int g = (x >> 8) & 0xFF;
    final int b = x & 0xFF;

    this.red = r;
    this.green = g;
    this.blue = b;

    this.value_opacity = o / 255f;

    setRGB();
  }

  public void inform(ColourPickerPreview cpfc) {
    this.cp_preview = cpfc;
  }

  public void inform(ColourPickerVHSBB cp_v_hsb_b) {
    this.cp_v_hsb_b = cp_v_hsb_b;
  }

  public void inform(ColourPickerVHSBH cp_v_hsb_h) {
    this.cp_v_hsb_h = cp_v_hsb_h;
  }

  public void inform(ColourPickerVHSBS cp_v_hsb_s) {
    this.cp_v_hsb_s = cp_v_hsb_s;
  }

  public void inform(ColourPickerVRGBR cp_v_rgb_r) {
    this.cp_v_rgb_r = cp_v_rgb_r;
  }

  public void inform(ColourPickerVRGBG cp_v_rgb_g) {
    this.cp_v_rgb_g = cp_v_rgb_g;
  }

  public void inform(ColourPickerVRGBB cp_v_rgb_b) {
    this.cp_v_rgb_b = cp_v_rgb_b;
  }

  public void inform(ColourPickerVT cpt) {
    this.cpt = cpt;
  }

  //public void inform(ColourPickerValueDisplay cpfcv) {
    //this.cpfcv = cpfcv;
  //}

  public void inform(ColourPickerGettersAndPutters cp_gas) {
    this.cp_gas = cp_gas;
  }

  public void repaintChildren() {
    this.cp_preview.refresh();
    //this.cpfcv.refresh();
    repaintRGBR();
    repaintRGBG();
    repaintRGBB();
    repaintHSBH();
    repaintHSBS();
    repaintHSBB();
    repaintT();

    greyGetAndSetColourButtons();
  }

  public void repaintHSBH() {
    this.cp_v_hsb_h.repaint();
  }

  public void repaintHSBS() {
    this.cp_v_hsb_s.repaint();
  }

  public void repaintHSBB() {
    this.cp_v_hsb_b.repaint();
  }

  public void repaintRGBR() {
    this.cp_v_rgb_r.repaint();
  }

  public void repaintRGBG() {
    this.cp_v_rgb_g.repaint();
  }

  public void repaintRGBB() {
    this.cp_v_rgb_b.repaint();
  }

  public void repaintT() {
    this.cpt.repaint();
  }

  void convertRGBtoHSB() {
    final float[] fa = new float[3];

    Color.RGBtoHSB(this.red, this.green, this.blue, fa);
    this.value_hue = fa[0];
    this.value_saturation = fa[1];
    this.value_brightness = fa[2];
  }

  void convertHSBtoRGB() {
    final int rgb = Color.HSBtoRGB(this.value_hue, this.value_saturation,
        this.value_brightness);
    this.red = (rgb >> 16) & 0xFF;
    this.green = (rgb >> 8) & 0xFF;
    this.blue = rgb & 0xFF;
  }

  public void greyGetAndSetColourButtons() {
    this.cp_gas.greyGetAndSetColourButtons(getColour());
  }

  public int getColour() {
    return this.colour;
  }

  public void setHSB() {
    //this.lastHSB = true;
    this.colour = getColourUsingHSBColourModel(this.value_hue,
        this.value_saturation, this.value_brightness, this.value_opacity);
    convertHSBtoRGB();
    //this.cpfcv.setHSB();
  }

  public void setRGB() {
    //this.lastHSB = false;
    this.colour = getColourUsingRGBColourModel(this.red, this.green, this.blue,
        this.value_opacity);
    convertRGBtoHSB();
    //this.cpfcv.setRGB();
  }

  public int getColourUsingHSBColourModel(float h, float s, float b, float o) {
    final int opacity = (int) (o * 255) & 0xFF;
    final int rgb = Color.HSBtoRGB(h, s, b) & 0xFFFFFF;

    return rgb | (opacity << 24);
  }

  public int getColourUsingRGBColourModel(int red, int green, int blue, float o) {
    final int opacity = (int) (o * 255) & 0xFF;
    final int rgb = red << 16 | green << 8 | blue;

    return rgb | (opacity << 24);
  }

}
