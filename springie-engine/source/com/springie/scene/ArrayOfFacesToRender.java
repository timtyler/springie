// This code has been placed into the public domain by its author
package com.springie.scene;

public class ArrayOfFacesToRender {
  // Never null: see ArrayOfLinksToRender -- off-render-thread readers
  // must always find an array, empty until the first render pass.
  public CachedFace[] array = new CachedFace[0];

  public void ensureCapacity(final int size) {
    if (this.array.length != size) {
      this.array = new CachedFace[size];
      for (int i = size; --i >= 0;) {
        this.array[i] = new CachedFace();
      }
    }
  }

  public void clear() {
    this.array = new CachedFace[0];
  }
}
