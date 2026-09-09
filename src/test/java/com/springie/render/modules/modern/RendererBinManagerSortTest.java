package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.junit.jupiter.api.Test;

// The depth index must order polygons ascending by z (the renderer draws
// the index from the end backwards, deepest first) and must be stable:
// equal z values keep their original relative order.
class RendererBinManagerSortTest {

  private static ArrayList<PolygonComposite> composites(int... zs) {
    final ArrayList<PolygonComposite> list = new ArrayList<>(zs.length);
    for (final int z : zs) {
      list.add(new PolygonComposite(new PolygonObject2D[0], z));
    }
    return list;
  }

  private static int[] depthIndex(RendererBinManager manager,
      ArrayList<PolygonComposite> list) throws Exception {
    final Method sort = RendererBinManager.class.getDeclaredMethod(
        "getSortedNodeDepthIndex", ArrayList.class, boolean.class);
    sort.setAccessible(true);
    sort.invoke(manager, list, true);
    final Field field = RendererBinManager.class
        .getDeclaredField("node_depth_index");
    field.setAccessible(true);
    final int[] full = (int[]) field.get(manager);
    final int[] result = new int[list.size()];
    System.arraycopy(full, 0, result, 0, result.length);
    return result;
  }

  private static int[] zSequence(ArrayList<PolygonComposite> list, int[] index) {
    final int[] zs = new int[index.length];
    for (int i = 0; i < index.length; i++) {
      zs[i] = list.get(index[i]).z;
    }
    return zs;
  }

  @Test
  void emptyListProducesEmptyIndex() throws Exception {
    final ArrayList<PolygonComposite> list = composites();
    assertArrayEquals(new int[0],
        depthIndex(new RendererBinManager(), list));
  }

  @Test
  void singleElementIsTriviallySorted() throws Exception {
    final ArrayList<PolygonComposite> list = composites(42);
    assertArrayEquals(new int[] { 0 },
        depthIndex(new RendererBinManager(), list));
  }

  @Test
  void sortsAscendingByZ() throws Exception {
    final ArrayList<PolygonComposite> list = composites(5, 1, 4, 2, 3);
    final int[] index = depthIndex(new RendererBinManager(), list);
    assertArrayEquals(new int[] { 1, 2, 3, 4, 5 },
        zSequence(list, index));
  }

  @Test
  void equalDepthsKeepOriginalOrder() throws Exception {
    final ArrayList<PolygonComposite> list = composites(3, 1, 3, 2, 3, 1);
    final int[] index = depthIndex(new RendererBinManager(), list);
    // Stable: the three z=3 entries stay in positions 0, 2, 4 order,
    // and the two z=1 entries stay in positions 1, 5 order.
    assertArrayEquals(new int[] { 1, 5, 3, 0, 2, 4 }, index);
  }

  @Test
  void matchesStableOracleOnRandomInput() throws Exception {
    final Random random = new Random(12345);
    final RendererBinManager manager = new RendererBinManager();
    for (int trial = 0; trial < 50; trial++) {
      final int size = random.nextInt(60);
      final ArrayList<PolygonComposite> list = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        // Small z range forces plenty of duplicates (stability check).
        list.add(new PolygonComposite(new PolygonObject2D[0],
            random.nextInt(7) - 3));
      }
      final Integer[] expected = new Integer[size];
      for (int i = 0; i < size; i++) {
        expected[i] = i;
      }
      // TimSort is stable: ties keep original index order.
      Arrays.sort(expected,
          Comparator.comparingInt(i -> list.get(i).z));
      final int[] actual = depthIndex(manager, list);
      assertEquals(size, actual.length);
      for (int i = 0; i < size; i++) {
        assertEquals(expected[i].intValue(), actual[i],
            "trial " + trial + " position " + i);
      }
      // Reusing the manager must not leak state between calls.
      assertArrayEquals(zSequence(list, actual),
          zSequence(list, depthIndex(new RendererBinManager(), list)));
    }
  }
}
