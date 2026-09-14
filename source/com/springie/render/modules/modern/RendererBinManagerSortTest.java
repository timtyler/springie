package com.springie.render.modules.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The frame's composites are sorted once, globally, by distribute():
 * with deepest-first on, every bin's vector arrives in ascending-z
 * order (stable); with it off, in creation order at zero sort cost.
 * render() walks each bin's vector from the end backwards, so the draw
 * order must match the old per-bin-sort scheme exactly.
 */
class RendererBinManagerSortTest {

  private int saved_divisor;

  @BeforeEach
  void saveDivisor() {
    this.saved_divisor = RendererBinManager.divisor;
    RendererBinManager.divisor = 100;
  }

  @AfterEach
  void restoreDivisor() {
    RendererBinManager.divisor = this.saved_divisor;
  }

  private static PolygonComposite composite(int z, int x0, int y0, int x1,
      int y1) {
    return new PolygonComposite(new PolygonObject2D[] {
        new PolygonObject2D(new int[] {x0, x1, x1, x0},
            new int[] {y0, y0, y1, y1}, 0xFFFFFFFF) }, z);
  }

  private static RendererBinManager manager() {
    final RendererBinManager manager = new RendererBinManager();
    manager.resize(400, 400);
    return manager;
  }

  /** Bin (x, y) holds pixels x*100..x*100+99, y*100..y*100+99. */
  private static ArrayList<PolygonComposite> bin(RendererBinManager manager,
      int x, int y) {
    try {
      final java.lang.reflect.Field field =
          RendererBinManager.class.getDeclaredField("array");
      field.setAccessible(true);
      final RendererBin[][] array = (RendererBin[][]) field.get(manager);
      return array[x][y].vector;
    } catch (final ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static int[] zs(List<PolygonComposite> list) {
    final int[] result = new int[list.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = list.get(i).z;
    }
    return result;
  }

  @Test
  void distributeSortsEachBinAscendingByZWhenDeepestFirstIsOn() {
    final RendererBinManager manager = manager();
    final PolygonComposite a = composite(30, 10, 10, 20, 20);
    final PolygonComposite b = composite(10, 10, 10, 20, 20);
    final PolygonComposite c = composite(20, 150, 10, 160, 20);

    manager.distribute(new ArrayList<>(Arrays.asList(a, b, c)), true);

    assertEquals(Arrays.asList(b, a), bin(manager, 0, 0),
        "bin (0,0) must hold its composites ascending by z");
    assertEquals(Arrays.asList(c), bin(manager, 1, 0));
    assertTrue(bin(manager, 0, 1).isEmpty());
  }

  @Test
  void distributeIsStableForEqualDepths() {
    final RendererBinManager manager = manager();
    // Creation order zs: 3, 1, 3, 2, 3, 1 -- all in bin (0,0).
    final PolygonComposite[] pcs = new PolygonComposite[6];
    final int[] zsin = {3, 1, 3, 2, 3, 1};
    for (int i = 0; i < pcs.length; i++) {
      pcs[i] = composite(zsin[i], 10, 10, 20, 20);
    }

    manager.distribute(new ArrayList<>(Arrays.asList(pcs)), true);

    // Stable: the three z=3 entries keep creation order 0, 2, 4 and the
    // two z=1 entries keep order 1, 5.
    assertEquals(Arrays.asList(pcs[1], pcs[5], pcs[3], pcs[0], pcs[2], pcs[4]),
        bin(manager, 0, 0));
  }

  @Test
  void distributePreservesCreationOrderWhenDeepestFirstIsOff() {
    final RendererBinManager manager = manager();
    final PolygonComposite a = composite(30, 10, 10, 20, 20);
    final PolygonComposite b = composite(10, 10, 10, 20, 20);
    final PolygonComposite c = composite(20, 150, 10, 160, 20);

    manager.distribute(new ArrayList<>(Arrays.asList(a, b, c)), false);

    // No sort at all: the bin holds the creation order, which render()
    // walks from the end backwards -- exactly the old identity index.
    assertEquals(Arrays.asList(a, b), bin(manager, 0, 0));
    assertEquals(Arrays.asList(c), bin(manager, 1, 0));
  }

  @Test
  void emptyFrameDistributesNothing() {
    final RendererBinManager manager = manager();
    manager.distribute(new ArrayList<PolygonComposite>(), true);
    manager.distribute(new ArrayList<PolygonComposite>(), false);
    assertTrue(bin(manager, 0, 0).isEmpty());
  }

  /**
   * The draw order per bin -- the sequence render() walks, i.e. the bin
   * vector from the end backwards -- must be identical to the legacy
   * scheme's: each bin's creation-ordered vector, stably sorted
   * ascending by z (or the identity for deepest-first off), walked
   * backwards.
   */
  @Test
  void drawOrderMatchesLegacyPerBinSortScheme() {
    final Random random = new Random(987654321);
    for (int trial = 0; trial < 60; trial++) {
      final boolean deepest_first = (trial % 2) == 0;
      final int count = 1 + random.nextInt(40);
      final ArrayList<PolygonComposite> all = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        // Small z range forces plenty of duplicates (stability check);
        // small bboxes scattered over the 4x4 bins, some straddling.
        final int x0 = random.nextInt(380);
        final int y0 = random.nextInt(380);
        all.add(composite(random.nextInt(7) - 3, x0, y0,
            x0 + 1 + random.nextInt(40), y0 + 1 + random.nextInt(40)));
      }

      final RendererBinManager modern = manager();
      modern.distribute(new ArrayList<>(all), deepest_first);

      final RendererBinManager legacy = manager();
      for (final PolygonComposite pc : all) {
        legacy.add(pc);
      }

      for (int bx = 0; bx < 5; bx++) {
        for (int by = 0; by < 5; by++) {
          final List<PolygonComposite> new_draw =
              drawSequence(bin(modern, bx, by));
          final List<PolygonComposite> old_draw = legacyDrawSequence(
              bin(legacy, bx, by), deepest_first);
          assertEquals(old_draw, new_draw,
              "trial " + trial + " bin (" + bx + "," + by
                  + ") deepest_first=" + deepest_first);
        }
      }
    }
  }

  /** The order render() paints a bin: its vector from the end backwards. */
  private static List<PolygonComposite> drawSequence(
      List<PolygonComposite> vector) {
    final ArrayList<PolygonComposite> result =
        new ArrayList<>(vector.size());
    for (int c = vector.size(); --c >= 0;) {
      result.add(vector.get(c));
    }
    return result;
  }

  /**
   * The legacy draw order: the bin's creation-ordered vector run through
   * the old per-bin stable ascending-z sort (or the identity index when
   * deepest-first was off), walked from the end backwards.
   */
  private static List<PolygonComposite> legacyDrawSequence(
      List<PolygonComposite> vector, boolean deepest_first) {
    final int size = vector.size();
    final Integer[] index = new Integer[size];
    for (int i = 0; i < size; i++) {
      index[i] = i;
    }
    if (deepest_first) {
      // TimSort is stable: ties keep creation order, exactly like the
      // old merge sort.
      Arrays.sort(index,
          Comparator.comparingInt(i -> vector.get(i).z));
    }
    final ArrayList<PolygonComposite> result = new ArrayList<>(size);
    for (int c = size; --c >= 0;) {
      result.add(vector.get(index[c]));
    }
    return result;
  }

  @Test
  void distributeReusesItsBuffersAcrossFrames() {
    final RendererBinManager manager = manager();
    final ArrayList<PolygonComposite> frame = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      frame.add(composite(25 - i, 10, 10, 20, 20));
    }
    for (int f = 0; f < 3; f++) {
      manager.clear();
      manager.distribute(new ArrayList<>(frame), true);
      final List<PolygonComposite> vector = bin(manager, 0, 0);
      assertEquals(25, vector.size());
      final int[] ordered = zs(vector);
      for (int i = 1; i < ordered.length; i++) {
        assertTrue(ordered[i - 1] <= ordered[i],
            "frame " + f + " must stay ascending by z");
      }
      assertSame(frame.get(24), vector.get(0),
          "frame " + f + ": z=1 composite first");
    }
  }
}
