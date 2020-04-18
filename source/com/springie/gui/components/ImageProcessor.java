package com.springie.gui.components;

/*
 * ToDo ==== Add more image processing functions... Generate less garbage by
 * allowing the sender to specify the output array...
 */

import java.awt.Color;

import com.springie.utilities.ImageWrapper;

public final class ImageProcessor {
  private ImageProcessor() {
    //...
  }

  /**
   * Mask Given an image apply a mask to it. Returns a new image.
   */
  public static ImageWrapper hsbFilter(ImageWrapper _i, float weight_h_float,
      float weight_s_float, float weight_b_float) {
    final float[] hsb = new float[3];

    final int w = _i.getWidth(null);
    final int h = _i.getHeight(null);

    final int[] pixels_out = new int[w * h];

    final int[] pixels4 = imageToArray(_i);

    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        final int pix = pixels4[i + w * j];
        final int b = pix & 0xff;
        final int g = (pix >> 8) & 0xff;
        final int r = (pix >> 16) & 0xff;
        Color.RGBtoHSB(r, g, b, hsb);

        if (weight_h_float < 0) {
          hsb[0] = 1 - ((hsb[0] - 1) * weight_h_float);
        } else {
          hsb[0] = hsb[0] * weight_h_float;
        }

        if (weight_s_float < 0) {
          hsb[1] = 1 - ((hsb[1] - 1) * weight_s_float);
        } else {
          hsb[1] = hsb[1] * weight_s_float;
        }

        if (weight_b_float < 0) {
          hsb[2] = 1 - ((hsb[2] - 1) * weight_b_float);
        } else {
          hsb[2] = hsb[2] * weight_b_float;
        }

        final int a = pix & 0xff000000;
        pixels_out[i + w * j] = a
            | (0xffffff & Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
      }
    }

    return new ImageWrapper(pixels_out, w, h);
  }

  // has side effects...
  // grab pixels into an array...
  public static int[] imageToArray(ImageWrapper i) {
    if (i == null) {
      return null;
    }
    return i.getSource();

  }

  // public static ImageWrapper clear(int w, int h) {
  // return new ImageWrapper(new int[w * h], w, h);
  // }
}
