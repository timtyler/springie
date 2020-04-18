package com.springie.utilities;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

import com.springie.utilities.log.Log;
import com.tifsoft.Forget;

// Don't try writing to JPEGs. It will have no effect...
public class ImageWrapper {
  private Image image;

  private int[] source;

  private int width;

  private int height;

  private static Toolkit toolkit;

  static {
    toolkit = Toolkit.getDefaultToolkit();
  }

  // Constructor...
  public ImageWrapper(final ImageWrapper image) {
    this.width = image.getWidth(null);
    this.height = image.getHeight(null);
    final int[] in_pix = image.getSource();
    final int[] out_pix = new int[in_pix.length];
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        out_pix[i + this.width * j] = in_pix[i + this.width * j];
      }
    }

    createImageFromArray(out_pix, this.width, this.height);
  }

  // duplicate existing

  public ImageWrapper(final Image image) {
    setImage(image);
  }

  public ImageWrapper(final int[] a, final int w, final int h, final boolean x) {
    Forget.about(x);
    this.image = toolkit.createImage(new MemoryImageSource(w, h, a, 0, w));
  }

  public ImageWrapper(final int[] a, final int w, final int h) {
    createImageFromArray(a, w, h);
  }

  public final void createImageFromArray(final int[] a, final int w, final int h) {
    this.source = a;
    this.width = w;
    this.height = h;
    this.image = toolkit.createImage(new MemoryImageSource(w, h, ColorModel
        .getRGBdefault(), a, 0, w));
  }

  // used for JPEGs...
  public final void setImage(final Image image) {
    this.image = image;
    this.width = image.getWidth(null);
    this.height = image.getHeight(null);
    this.source = null;
  }

  public final Image getImage() {
    return this.image;
  }

  // public final int getWidth() {
  // return width;
  // }
  //
  // public final int getHeight() {
  // return height;
  // }

  // /!??!
  public final int getWidth(final Object o) {
    Forget.about(o);
    return this.image.getWidth(null);
  }

  // /!?!
  public final int getHeight(final Object o) {
    Forget.about(o);
    return this.image.getHeight(null);
  }

  public final int[] getSource() {
    if ((this.source == null) || (this.source.length < 1)) {
      makeSource();
    }

    return this.source;
  }

  private void makeSource() {
    this.width = getWidth(null);
    this.height = getHeight(null);

    this.source = new int[this.width * this.height];

    // Log.log("Making new image:" + (width * height));

    final PixelGrabber pg = new PixelGrabber(this.image, 0, 0, this.width,
        this.height, this.source, 0, this.width);
    try {
      pg.grabPixels();
    } catch (InterruptedException e) {
      Log.log(e.toString());
    }
  }

  public static final void setToolkit(final Toolkit toolkit) {
    ImageWrapper.toolkit = toolkit;
  }

  public static final Toolkit getToolkit() {
    return toolkit;
  }

  public final void setHeight(final int height) {
    this.height = height;
  }

  public final int getHeight() {
    return this.height;
  }

  public final void setWidth(final int width) {
    this.width = width;
  }

  public final int getWidth() {
    return this.width;
  }

  public final void setSource(final int[] source) {
    this.source = source;
  }

  // public final int[] getSource() {
  // return source;
  // }

  public int getPixelColour(int x, int y) {
    return this.source[x + this.width * y];
  }
}
