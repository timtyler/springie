package com.springie.utilities;

public class PixelArray {
  public int[] pixels;

  PixelArray() {
    this.pixels = new int[1 << 12]; // should be big enough...
  }

  public int[] ensureArraySize(int z) {
    if (this.pixels.length < z) {
      this.pixels = new int[z];
    }

    return this.pixels;
  }
}
