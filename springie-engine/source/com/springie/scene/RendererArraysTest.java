// This code has been placed into the public domain by its author

package com.springie.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The render-cache arrays must never be null. Readers on other
 * threads (the selection hit-test LinkManager.isThereOne runs as a
 * message on the animation thread) read renderer_link.array outside
 * the render pass; the array used to be null from construction until
 * the first ensureCapacity, and again after every clear(), so a click
 * landing in that window threw a NullPointerException that the
 * message pump swallowed -- the click silently did nothing (the
 * PolygonRendererClickBoxIT flake, root-caused 2026-11-08).
 */
class RendererArraysTest {

  @Test
  void arraysAreNonNullFromConstruction() {
    assertNotNull(new ArrayOfNodesToRender().array);
    assertNotNull(new ArrayOfLinksToRender().array);
    assertNotNull(new ArrayOfFacesToRender().array);
    assertEquals(0, new ArrayOfLinksToRender().array.length);
  }

  @Test
  void clearLeavesEmptyArraysNotNull() {
    final RendererArrays arrays = new RendererArrays();
    arrays.renderer_node.ensureCapacity(3);
    arrays.renderer_link.ensureCapacity(3);
    arrays.renderer_face.ensureCapacity(3);

    arrays.clear();

    assertNotNull(arrays.renderer_node.array);
    assertNotNull(arrays.renderer_link.array);
    assertNotNull(arrays.renderer_face.array);
    assertEquals(0, arrays.renderer_node.array.length);
    assertEquals(0, arrays.renderer_link.array.length);
    assertEquals(0, arrays.renderer_face.array.length);
  }

  @Test
  void ensureCapacityStillBuildsAndReuses() {
    final ArrayOfLinksToRender links = new ArrayOfLinksToRender();

    links.ensureCapacity(4);
    assertEquals(4, links.array.length);
    for (CachedLink link : links.array) {
      assertNotNull(link);
    }

    // Same size again: the existing array (and its elements) is kept.
    final CachedLink[] built = links.array;
    links.ensureCapacity(4);
    assertSame(built, links.array);

    // A different size rebuilds; zero is a valid, non-null size.
    links.ensureCapacity(2);
    assertEquals(2, links.array.length);
    links.ensureCapacity(0);
    assertNotNull(links.array);
    assertEquals(0, links.array.length);
  }
}
