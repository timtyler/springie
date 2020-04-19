// This program has been placed into the public domain by its author.

package com.springie.gui.colourpicker;

import java.awt.GridLayout;
import java.awt.Panel;

import com.springie.FrEnd;
import com.springie.gui.components.TabbedPanel;

public class ColorPicker {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public ColourPickerController color_picker_controller;

  public ColourPickerGettersAndPutters cp_gas;

  ColourPickerPreview cp_fc;

  public ColorPicker(ColorPickerInformer informer) {
    makePanel(informer);
  }

  void makePanel(ColorPickerInformer informer) {
    this.color_picker_controller = new ColourPickerController();

    final ColourPickerVRGBR cp_red = new ColourPickerVRGBR();
    cp_red.init();

    final ColourPickerVRGBG cp_grn = new ColourPickerVRGBG();
    cp_grn.init();

    final ColourPickerVRGBB cp_blu = new ColourPickerVRGBB();
    cp_blu.init();

    final ColourPickerVHSBH cp_hue = new ColourPickerVHSBH();
    cp_hue.init();

    final ColourPickerVHSBS cp_sat = new ColourPickerVHSBS();
    cp_sat.init();

    final ColourPickerVHSBB cp_bri = new ColourPickerVHSBB();
    cp_bri.init();

    final ColourPickerVT cpt = new ColourPickerVT();
    cpt.init();

    this.cp_fc = new ColourPickerPreview(informer);

    //final ColourPickerValueDisplay cpfcv = new ColourPickerValueDisplay();
    //cpfcv.init();

    this.cp_gas = new ColourPickerGettersAndPutters();

    this.color_picker_controller.inform(this.cp_fc);
    this.cp_fc.inform(this.color_picker_controller);

    this.color_picker_controller.inform(cp_hue);
    cp_hue.inform(this.color_picker_controller);

    this.color_picker_controller.inform(cp_sat);
    cp_sat.inform(this.color_picker_controller);

    this.color_picker_controller.inform(cp_bri);
    cp_bri.inform(this.color_picker_controller);

    this.color_picker_controller.inform(cp_red);
    cp_red.inform(this.color_picker_controller);

    this.color_picker_controller.inform(cp_grn);
    cp_grn.inform(this.color_picker_controller);

    this.color_picker_controller.inform(cp_blu);
    cp_blu.inform(this.color_picker_controller);

    this.color_picker_controller.inform(cpt);
    cpt.inform(this.color_picker_controller);

    //this.colour_picker_controller.inform(cpfcv);
    //cpfcv.inform(this.colour_picker_controller);

    this.cp_gas.inform(this.color_picker_controller);
    this.color_picker_controller.inform(this.cp_gas);

    // panel_rgb.add(this.colour_picker_controller);

    final Panel panel_hsb = new Panel();
    panel_hsb.setLayout(new GridLayout(0, 1, 0, 0));

    panel_hsb.add(cp_hue);
    panel_hsb.add(cp_sat);
    panel_hsb.add(cp_bri);

    final Panel panel_rgb = new Panel();
    panel_rgb.setLayout(new GridLayout(0, 1, 0, 0));

    panel_rgb.add(cp_red);
    panel_rgb.add(cp_grn);
    panel_rgb.add(cp_blu);

    final TabbedPanel tab_colour_model = new TabbedPanel();
    tab_colour_model.add("HSB", panel_hsb);
    tab_colour_model.add("RGB", panel_rgb);

    final Panel panel_colour_misc = new Panel();
    panel_colour_misc.setLayout(new GridLayout(0, 1, 0, 0));

    panel_colour_misc.add(cpt);
    panel_colour_misc.add(this.cp_fc);
    panel_colour_misc.add(this.cp_gas);
    //panel_colour_misc.add(cpfcv);

    this.panel.add(tab_colour_model);
    this.panel.add(panel_colour_misc);
  }

  public int getColour() {
    return this.color_picker_controller.getColour();
  }
}
