/**
 * A class containing static methods which load images -
 * including images within jar files...
 * <p>
 * @author Tim Tyler
 * @version 1.11
 **/

package com.springie.utilities;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.springie.FrEnd;
import com.springie.utilities.log.Log;

public final class ImageLoader {
  //private static ImageLoader image_loader;
  private static Toolkit toolkit;

  /**
   * ImageLoader, constructor.
   * <p>
   * The constructor is private.
   * There should not be any instances of this
   * class created outside the private one used
   * in the class.
   **/
  private ImageLoader() {
    //...
  }

  /**
   * Get an image.
   * <p>
   * Loads a specified image, either from the currect directory,
   * Or from inside the relevant jar file, whichever is appropriate.
   **/
  public static ImageWrapper getImage(String name) {
    InputStream in;
    ImageWrapper image;
    boolean ispng;

    //Log.log("Loading: " + name + ".");

    byte[] _array;
    int _array_size;

    toolkit = Toolkit.getDefaultToolkit();
    ispng = name.indexOf(".png") > 0;

    try {
      //Log.log("Starting to load: " + name);

      in = FrEnd.class.getResourceAsStream(name);

      //in = ImageLoader.class.getResourceAsStream(name);
      if (in == null) {
        throw new RuntimeException("Problem locating image file: " + name);
      }

      if (ispng) {
        //com.sixlegs.image.png.PngImage png = new com.sixlegs.image.png.PngImage(in);
        //image = toolkit.createImage(png);
        // image = com.sun.jimi.core.Jimi.createImage(in);
        image = null;

        try {
          // base = getCodeBase();
          // java.io.InputStream inputstream = (new URL(base, pngPar)).openStream();
          final PNGReader rdr = new PNGReader(in);
          final int w = rdr.getWidth();
          // Log.log("111");
          final int h = rdr.getHeight();
          // image = toolkit.createImage(new MemoryImageSource(w, h, rdr.getPixels(), 0, w));
          image = new ImageWrapper(rdr.getPixels(), w, h, true);
          // Log.log("111");
          // Log.log("PNG->NULL?:" + image.i);
        } catch (IOException ioexception) {
          Log.log("Problem loading image: " + name);
          Log.exception(ioexception);
        }
      } else {
        // Thanks to Karl Schmidt for the followig code...
        ByteArrayOutputStream bytes;

        bytes = new ByteArrayOutputStream();
        _array_size = 1024;
        _array = new byte[_array_size];

        int rb;

        while ((rb = in.read(_array, 0, _array_size)) > -1) {
          bytes.write(_array, 0, rb);
        }

        bytes.close();

        _array = bytes.toByteArray();

        image = new ImageWrapper(toolkit.createImage(_array));

        // !?!?!?!?
        // do { } while(!toolkit.prepareImage(image.i, -1, -1, null));
        // Log.log("JPEG->NULL?:" + image.i);
      }

      in.close();

      //Log.log("Finished loading: " + name + ".");

      return image;
    } catch (IOException e) {
      e.printStackTrace();
    }

    //Log.log("BAD EXIT: "+ name);

    return null;
  }

  /** 
   * If you want to wait for your images to load, you should
   * seriously consider using the ImageLoadingManager class...
   */
  public static ImageWrapper getImageNow(String name) {
    final ImageWrapper temp_image = getImage(name);
    do {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Log.exception(e);
      }
    } while (!toolkit.prepareImage(temp_image.getImage(), -1, -1, null));
    // Log.log("Finished loading: " + name + ".");
    return temp_image;
  }

  /**
    * Returns the Toolkit used...
    * <p>
    * Allow access to the toolkit used (i.e. access from outside this class)...
    **/
  public static Toolkit getToolkit() {
    return toolkit;
  }
}
