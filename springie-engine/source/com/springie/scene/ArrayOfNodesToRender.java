// This code has been placed into the public domain by its author
package com.springie.scene;

public class ArrayOfNodesToRender {
  // Never null: see ArrayOfLinksToRender -- off-render-thread readers
  // must always find an array, empty until the first render pass.
  public CachedNode[] array = new CachedNode[0];

  public void ensureCapacity(final int size) {
    if (this.array.length != size) {
      this.array = new CachedNode[size];
      for (int i = size; --i >= 0;) {
        this.array[i] = new CachedNode();
      }
    }
  }

  public void clear() {
    this.array = new CachedNode[0];
  }
}
