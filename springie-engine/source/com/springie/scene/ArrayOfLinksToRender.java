// This code has been placed into the public domain by its author
package com.springie.scene;

public class ArrayOfLinksToRender {
  // Never null: readers on other threads (e.g. the selection hit-test
  // running as a message on the animation thread) may look before the
  // first render pass has built the cache, or after a clear() and
  // before the next render pass rebuilds it. An empty array reads as
  // "nothing rendered yet", which those readers already handle.
  public CachedLink[] array = new CachedLink[0];

  public void ensureCapacity(final int size) {
    if (this.array.length != size) {
      this.array = new CachedLink[size];
      for (int i = size; --i >= 0;) {
        this.array[i] = new CachedLink();
      }
    }
  }

  public void clear() {
    this.array = new CachedLink[0];
  }
}
