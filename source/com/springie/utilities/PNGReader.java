package com.springie.utilities;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class PNGReader {
  static final boolean verbose = true;

  private int ya;

  private int[] pixels;

  private InputStream input_stream;

  private CRC32 crc;

  private Inflater inflater;

  PNGMaker png_maker;

  private int yc;

  private int yd;

  private long long_v;

  private static byte[] byte_array;

  public static final int[] lut1 = {0x49484452, 0x504c5445, 0x49444154,
      0x49454e44, 0x624b4744, 0x73424954, 0x6348524d, 0x67414d41, 0x68495354,
      0x70485973, 0x74524e53, 0x74494d45, 0x74455874, 0x7a545874 };

  public static final byte[] lut2 = {-119, 80, 78, 71, 13, 10, 26, 10 };

  private Vector vector;

  private boolean is_colour_type;

  private boolean bool2;

  private boolean bool3;

  private boolean bool4;

  private int width;

  private int height;

  private int bit_depth;

  private int colour_type;

  // private int yr;
  // private int ys;
  private int yt;

  private int state;

  private int[] yv;

  private byte[] colour_map;

  public static final int xa = 512;

  byte[] xb;

  int[] xc;

  int[] xd;

  // private int gamma;
  
  private static int[] xl;

  private static int[] xm;

  static String error;

  static {
    // Log.log("PNG_IN1");
    if (verbose) {
      error = "Error: ";
    }

    byte_array = new byte[8192];
    xl = new int[256];
    xm = new int[256];
    // Log.log("PNG_IN2");
  }

  public PNGReader(InputStream input_stream_two) throws IOException {
    // Log.log("PNG_IN InputStream = " + input_stream_two);
    this.ya = 1167;
    setCRC(new CRC32());
    // yf = new byte[8192];
    this.vector = new Vector();
    this.state = 1;
    this.inflater = new Inflater();
    // gamma = 45000;

    // gamma = 100000;
    // changeGamma(gamma);
    // xr();

    // xl = new int[256];
    // xm = new int[256];
    this.input_stream = input_stream_two;
    input_stream_two.read(byte_array, 0, 8);
    for (int i = 0; i < 8; i++) {
      if (byte_array[i] != lut2[i]) {
        throw error(verbose ? "Wrong signature" : "");
      }
    }

    if (read32BitValue() == 13) {
      input_stream_two.read(byte_array, 0, 21);
      setYe(0xffffffffL & xh(byte_array, 17));
      getCRC().update(byte_array, 0, 17);
      setYD(xh(byte_array, 0));
      if (getLocation() == lut1[0] && calculateCRC() == getCRC().getValue()) {
        this.width = xh(byte_array, 4);
        this.height = xh(byte_array, 8);
        this.bit_depth = 0xff & byte_array[12];
        this.colour_type = 0xff & byte_array[13];
        // yr = 0xff & yf[14];
        // ys = 0xff & yf[15];
        this.yt = 0xff & byte_array[16];
        if (this.colour_type == 2) {
          this.state = 3;
        }

        if (this.colour_type == 4) {
          this.state = 2;
        }

        if (this.colour_type == 6) {
          this.state = 4;
        }

        this.yv = new int[this.state];
      } else {
        throw error(verbose ? "Invalid IHDR chunk" : "");
      }
    } else {
      throw error(verbose ? "Invalid IHDR chunk" : "");
    }
    if (this.colour_type == 3) {
      for (int j = 0; j < 256; j++) {
        xm[j] = 255;
      }
    }
    do {
      setCurrent32BitValue(read32BitValue());
      getCRC().reset();
      input_stream_two.read(byte_array, 0, 4);
      getCRC().update(byte_array, 0, 4);
      setYD(xh(byte_array, 0));
      if (getPLTEOrderProblem() && getLocation() != lut1[2]) {
        this.bool3 = true;
      }
      switch (getLocation()) {
        case 1347179589:
          checkSomeErrors();
          break;

        case 1229209940:
          if (getPLTEOrderProblem() && !this.bool3) {
            xp();
          } else {
            if (!containsInteger(lut1[7]) || (this.ya & 0x80) == 0) {
              gammaProcess();
            }
            xs();
          }
          break;

        case 1229278788:
          santiyCheck();
          break;

        case 1732332865:
          changeGamma();
          break;

        case 1951551059:
          tRNSCheck();
          break;

        default:
          getChunk();
          break;
      }
    } while (getLocation() != lut1[3]);
    // Log.log("PNG_OUT");
  }

  private void checkSomeErrors() throws IOException {
    if (getPLTEOrderProblem() || this.is_colour_type) {
      throw error(verbose ? "PLTE order" : "");
    }

    this.is_colour_type = true;
    this.vector.addElement(new Integer(getLocation()));
    if (getColourMapSize() > 8192) {
      throw error(verbose ? "Implementation - too long colour map: " + getColourMapSize()
          : "");
    }
    this.colour_map = new byte[getColourMapSize()];
    this.input_stream.read(this.colour_map, 0, getColourMapSize());
    setYe(0xffffffffL & read32BitValue());
    getCRC().update(this.colour_map, 0, getColourMapSize());
    if (calculateCRC() != getCRC().getValue()) {
      throw error(verbose ? "CRC32 check in PLTE" : "");
    }
    return;
  }

  private void xs() throws IOException {
    final int[] ai = {0, 4, 0, 2, 0, 1, 0 };
    final int[] ai1 = {8, 8, 4, 4, 2, 2, 1 };
    final int[] ai2 = {0, 0, 4, 0, 2, 0, 1 };
    final int[] ai3 = {8, 8, 8, 4, 4, 2, 2 };
    final int[] ai4 = new int[7];
    final int[] ai5 = new int[7];
    final byte byte0 = this.yt != 1 ? ((byte) 1) : 7;
    if (byte0 == 7) {
      for (int i = 0; i < 7; i++) {
        ai4[i] = ((this.width + ai1[i]) - 1 - ai[i]) / ai1[i];
        ai5[i] = ((this.height + ai3[i]) - 1 - ai2[i]) / ai3[i];
      }
    } else {
      ai[0] = 0;
      ai1[0] = 1;
      ai2[0] = 0;
      ai3[0] = 1;
      ai4[0] = this.width;
      ai5[0] = this.height;
    }

    // new code...
    PixelArrays.array_1.ensureArraySize(this.width * this.height);
    this.pixels = PixelArrays.array_1.pixels;

    // old code...
    // pixels = new int[width * height];
    this.png_maker = new PNGMaker(this.input_stream, this.inflater, 512);
    this.vector.addElement(new Integer(getLocation()));
    final int j = (this.state * this.bit_depth + 7) / 8;
    // boolean flag = false;
    this.xb = new byte[j * this.width + 1];
    this.xc = new int[j * this.width];
    this.xd = new int[j * this.width];
    // Object obj = null;
    for (int l = 0; l < byte0; l++) {
      final int i1 = (ai4[l] * this.bit_depth * this.state + 7) / 8;
      if (i1 != 0) {
        for (int j1 = 0; j1 < i1; j1++) {
          this.xc[j1] = 0;
        }

        for (int k1 = 0; k1 < ai5[l]; k1++) {
          final int k = this.png_maker.read();
          if (k > 4) {
            throw error(verbose ? "Unknown filter" : "");
          }
          if (k == -1) {
            throw error(verbose ? "Early EOF in IDAT" : "");
          }
          int i2;
          for (int l1 = 0; l1 < i1; l1 += i2) {
            i2 = 0;
            if ((i2 = this.png_maker.read(this.xb, l1, i1 - l1)) == -1) {
              throw error(verbose ? "Early EOF in IDAT" : "");
            }
          }

          zn(k, j, i1, this.xb, this.xc, this.xd);
          // / boolean flag1 = false;
          // boolean flag2 = false;
          final int l2 = ~(-1 << this.bit_depth);
          for (int i3 = 0; i3 < ai4[l]; i3++) {
            final int k2 = i3 * this.bit_depth * this.state;
            final int j2 = 8 - k2 % 8 - this.bit_depth;
            for (int j3 = 0; j3 < this.state; j3++) {
              switch (this.bit_depth) {
                case 1:
                case 2:
                case 4:
                  this.yv[j3] = l2 & this.xd[k2 >> 3] >> j2;
                  break;

                case 8:
                  // '\b'
                  this.yv[j3] = l2 & this.xd[(k2 >> 3) + j3];
                  break;

                case 16:
                  // '\020'
                  this.yv[j3] = l2 & this.xd[(k2 >> 3) + (j3 << 1)];
                  break;

                default:
                  throw error(verbose ? "bit depth " + this.bit_depth : "");
              }
            }

            this.pixels[this.width * (ai2[l] + k1 * ai3[l]) + ai[l] + i3
                * ai1[l]] = zv(this.yv);
          }

          int[] temp_array = this.xc;
          this.xc = this.xd;
          this.xd = temp_array;
        }
      }
    }
  }

  private int zv(int[] ai) throws IOException {
    int i = this.bit_depth;
    int colour = 0;
    switch (this.colour_type) {
      case 3:
        colour |= xl[0xff & this.colour_map[3 * ai[0]]] << 16;
        colour |= xl[0xff & this.colour_map[3 * ai[0] + 1]] << 8;
        colour |= xl[0xff & this.colour_map[3 * ai[0] + 2]];
        colour |= xm[ai[0]] << 24;
        break;

      case 2:
        if (!this.bool4) {
          colour = 0xff000000;
        } else {
          colour = ai[0] != xm[0] || ai[1] != xm[1] || ai[2] != xm[2] ? 0xff000000
              : 0;
        }
        colour |= xl[0xff & ai[0]] << 16;
        colour |= xl[0xff & ai[1]] << 8;
        colour |= xl[0xff & ai[2]];
        break;

      case 6:
        colour = 0;
        colour |= xl[0xff & ai[0]] << 16;
        colour |= xl[0xff & ai[1]] << 8;
        colour |= xl[0xff & ai[2]];
        colour |= (0xff & ai[3]) << 24;
        break;

      case 0:
        colour = ai[0];
        switch (this.bit_depth) {
          case 1:
            colour = ai[0] != 0 ? xl[255] : 0;
            colour |= colour << 8;
            colour |= (0xff00 & colour) << 8;
            break;

          case 2:
            colour |= colour << i;
            i <<= 1;
            colour |= colour << i;
            colour = handleDefault(colour);
            break;

          case 4:
            colour |= colour << i;
            colour = handleDefault(colour);
            break;

          case 3:
          default:
            colour = handleDefault(colour);
            break;
        }
        if (!this.bool4) {
          colour |= 0xff000000;
        } else {
          colour |= ai[0] != xm[0] ? 0xff000000 : 0;
        }
        break;

      case 4:
        colour = xl[ai[0]];
        colour |= colour << 8;
        colour |= (0xff00 & colour) << 8;
        colour |= (0xff & ai[1]) << 24;
        break;

      case 1:
      case 5:
      default:
        throw error(verbose ? "Colour type " + this.colour_type : "");
    }

    return colour;
  }

  private int handleDefault(int j) {
    int j2 = xl[j];
    j2 |= j2 << 8;
    j2 |= (0xff00 & j2) << 8;
    return j2;
  }

  private void santiyCheck() throws IOException {
    this.vector.addElement(new Integer(getLocation()));
    if (!getPLTEOrderProblem() || this.colour_type == 3 && !this.is_colour_type) {
      throw error(verbose ? "IDAT or PLTE absence" : "");
    }

    setYe(0xffffffffL & read32BitValue());
    if (calculateCRC() != getCRC().getValue()) {
      throw error(verbose ? "CRC32 check in IEND" : "");
    }
    return;
  }

  private void changeGamma() throws IOException {
    this.vector.addElement(new Integer(getLocation()));
    if (this.is_colour_type || getPLTEOrderProblem()) {
      throw error(verbose ? "Late gAMA appeares" : "");
    }
    if ((this.ya & 0x80) == 0) {
      xp();
      return;
    }

    this.input_stream.read(byte_array, 0, 8);
    getCRC().update(byte_array, 0, 4);
    // gamma =
    xh(byte_array, 0);
    setYe(0xffffffffL & xh(byte_array, 4));
    if (calculateCRC() != getCRC().getValue()) {
      throw error(verbose ? "CRC32 check in gAMA" : "");
    }
    gammaProcess();
    return;
  }

  private void tRNSCheck() throws IOException {
    if (this.colour_type == 3 && !this.is_colour_type) {
      throw error(verbose ? "Early tRNS appeared" : "");
    }
    this.vector.addElement(new Integer(getLocation()));
    if ((this.ya & 0x400) == 0) {
      xp();
      return;
    }
    this.bool4 = true;
    this.input_stream.read(byte_array, 0, getColourMapSize() + 4);
    getCRC().update(byte_array, 0, getColourMapSize());
    setYe(0xffffffffL & xh(byte_array, getColourMapSize()));
    if (calculateCRC() != getCRC().getValue()) {
      throw error(verbose ? "CRC32 check in tRNS" : "");
    }
    switch (this.colour_type) {
      case 3:
        for (int i = 0; i < getColourMapSize(); i++) {
          xm[i] = 0xff & byte_array[i];
        }

        return;

      case 0:
      case 2:
        for (int j = 0; j < this.state; j++) {
          xm[j] = zr(byte_array, getColourMapSize() + (j << 1));
        }

        return;

      case 1:
      default:
        throw error(verbose ? "Unsupported tRNS for colour type "
            + this.colour_type : "");
    }
  }

  private void zn(int i, int j, int k, byte[] abyte0, int[] ai, int[] ai1) {
    switch (i) {
      case 0:
        for (int l = 0; l < k; l++) {
          ai1[l] = 0xff & abyte0[l];
        }

        return;

      case 1:
        for (int i1 = 0; i1 < j; i1++) {
          ai1[i1] = 0xff & abyte0[i1];
        }

        for (int j1 = j; j1 < k; j1++) {
          ai1[j1] = 0xff & ai1[j1 - j] + (0xff & abyte0[j1]);
        }

        return;

      case 2:
        for (int k1 = 0; k1 < k; k1++) {
          ai1[k1] = 0xff & ai[k1] + (0xff & abyte0[k1]);
        }

        return;

      case 3:
        for (int l1 = 0; l1 < j; l1++) {
          ai1[l1] = 0xff & ai[l1] / 2 + (0xff & abyte0[l1]);
        }

        for (int i2 = j; i2 < k; i2++) {
          ai1[i2] = 0xff & (ai[i2] + ai1[i2 - j]) / 2 + (0xff & abyte0[i2]);
        }

        return;

      case 4:
        for (int j2 = 0; j2 < j; j2++) {
          ai1[j2] = 0xff & (0xff & abyte0[j2]) + paeth(0, ai[j2], 0);
        }

        for (int k2 = j; k2 < k; k2++) {
          ai1[k2] = 0xff & (0xff & abyte0[k2])
              + paeth(ai1[k2 - j], ai[k2], ai[k2 - j]);
        }

        return;

      default:
        return;
    }
  }

  private int paeth(int i, int j, int k) {
    final int l = (i + j) - k;
    final int i1 = l <= i ? i - l : l - i;
    final int j1 = l <= j ? j - l : l - j;
    final int k1 = l <= k ? k - l : l - k;

    if (i1 <= j1 && i1 <= k1) {
      return i;
    }

    if (j1 <= k1) {
      return j;
    }
    return k;
  }

  private void gammaProcess() {
    // double d;
    // d = 45000D / (double)gamma;

    xl[0] = 0;
    for (int i = 1; i < 256; i++) {
      final double d1 = i / 255D;
      // "d * " -
      // inside
      // (before
      // Math)
      xl[i] = 0xff & (int) Math.round(255D * Math.exp(Math.log(d1)));
    }
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  // copies into a new array...
  public int[] getPixels() {
    final int[] pixels = new int[this.width * this.height];
    System.arraycopy(this.pixels, 0, pixels, 0, this.width * this.height);
    return pixels;
  }

  /*
   * public int getGamma() {return gamma; }
   * void changeGamma(int i) {gamma = i; xr(); for(int k = 0; k < pixels.length;
   * k++) {int j = 0xff000000 & pixels[k]; for(int l = 0; l < 24; l += 8) j |=
   * xl[0xff & pixels[k] >> l] << l;
   * pixels[k] = j; } }
   */

  private void getChunk() throws IOException {
    if (getPLTEOrderProblem()) {
      this.bool3 = true;
    }
    this.vector.addElement(new Integer(getLocation()));
    if ((getLocation() & 0x20000000) != 0) {
      xp();
      return;
    }
    throw error(verbose ? "Unknown critical chunk " + getLocation() : "");

  }

  private void xp() throws IOException {
    // boolean flag = false;
    for (int j = 0; 8192 * j < getColourMapSize(); j++) {
      final int i = getColourMapSize() - 8192 * j < 8192 ? getColourMapSize() % 8192 : 8192;
      if (this.input_stream.read(byte_array, 0, i) <= 0) {
        throw errorEOF(verbose ? "Unexpected la??ut" : "");
      }
      getCRC().update(byte_array, 0, i);
    }

    setYe(0xffffffffL & read32BitValue());
    if (calculateCRC() != getCRC().getValue()) {
      throw error(verbose ? "CRC32 check in " + getLocation() : "");
    }
    return;

  }

  private boolean containsInteger(int i) {
    boolean flag = false;
    for (final Enumeration enumeration = this.vector.elements(); enumeration
        .hasMoreElements();) {
      if (((Integer) enumeration.nextElement()).intValue() == i) {
        flag = true;
        break;
      }
    }

    return flag;
  }

  private int read16BitValue() throws IOException {
    final int i = this.input_stream.read() & 0xff;
    return i << 8 | this.input_stream.read() & 0xff;
  }

  private int read32BitValue() throws IOException {
    final int i = read16BitValue();
    return i << 16 | read16BitValue();
  }

  private int zr(byte[] abyte0, int i) {
    return (0xff & abyte0[i]) << 8 | 0xff & abyte0[i + 1];
  }

  public int xh(byte[] abyte0, int i) {
    return zr(abyte0, i) << 16 | zr(abyte0, i + 2);
  }

  static IOException error(String s) {
    return new IOException(s);
  }

  static EOFException errorEOF(String s) {
    return new EOFException(error + s);
  }

  public void setCurrent32BitValue(int yc) {
    this.yc = yc;
  }

  public int getColourMapSize() {
    return this.yc;
  }

  public void setYD(int yd) {
    this.yd = yd;
  }

  public int getLocation() {
    return this.yd;
  }

  public void setCRC(CRC32 crc) {
    this.crc = crc;
  }

  public CRC32 getCRC() {
    return this.crc;
  }

  protected void setYk(boolean yk) {
    this.bool2 = yk;
  }

  protected boolean getPLTEOrderProblem() {
    return this.bool2;
  }

  protected void setYe(long ye) {
    this.long_v = ye;
  }

  protected long calculateCRC() {
    return this.long_v;
  }

  private class PNGMaker extends InflaterInputStream {
    int total;

    public PNGMaker(InputStream input_stream_two, Inflater inflater, int i) {
      super(input_stream_two, inflater, i);
    }

    protected void fill() throws IOException {
      final byte[] abyte0 = new byte[8];
      if (this.total == 0 && getPLTEOrderProblem()) {
        this.in.read(abyte0, 0, 8);
        setCurrent32BitValue(xh(abyte0, 0));
        setYD(xh(abyte0, 4));
        if (getLocation() != PNGReader.lut1[2]) {
          throw error(verbose ? "next not IDAT" + getLocation() : "");
        }

        getCRC().reset();
        getCRC().update(abyte0, 4, 4);
      }

      setYk(true);
      final int i = getColourMapSize() - this.total;
      if (i > this.buf.length) {
        this.len = this.in.read(this.buf, 0, this.buf.length);
        if (this.len == -1) {
          throw error(verbose ? "EOF in IDAT" : "");
        }
        getCRC().update(this.buf, 0, this.len);
        this.total += this.len;
        this.inf.setInput(this.buf, 0, this.len);
        return;
      }

      this.len = this.in.read(this.buf, 0, i);
      if (this.len == -1) {
        throw error(verbose ? "EOF in IDAT" : "");
      }

      getCRC().update(this.buf, 0, this.len);
      this.total += this.len;
      if (this.total == getColourMapSize()) {
        this.total = 0;
        this.in.read(abyte0, 0, 4);
        setYe(0xffffffffL & xh(abyte0, 0));
        if (calculateCRC() != getCRC().getValue()) {
          throw error(verbose ? "CRC32 check in IDAT" : "");
        }
      }
      this.inf.setInput(this.buf, 0, this.len);
    }
  }

}
