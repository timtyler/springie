// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.render.modules.raytraced.ModularRendererRaytraced.Tile;
import com.springie.render.modules.modern.RendererBinManager;

/**
 * The ray-traced renderer must tile the canvas with the same bins as the
 * default renderer: divisor-sized blocks covering every pixel exactly once.
 */
public class TileGridTest {
  private int saved_divisor;

  private boolean saved_show_bins;

  @BeforeEach
  public void saveDivisor() {
    this.saved_divisor = RendererBinManager.divisor;
    this.saved_show_bins = RendererBinManager.show_bins;
    RendererBinManager.show_bins = false;
  }

  @AfterEach
  public void restoreDivisor() {
    RendererBinManager.divisor = this.saved_divisor;
    RendererBinManager.show_bins = this.saved_show_bins;
  }

  private void checkGrid(int width, int height, int divisor) {
    RendererBinManager.divisor = divisor;
    final Tile[] tiles = ModularRendererRaytraced
        .buildTileGrid(width, height);

    // Degenerate zero-area bins (exact multiples of the divisor) are
    // dropped: they cover no pixels.
    final int expected_nx = (width + divisor - 1) / divisor;
    final int expected_ny = (height + divisor - 1) / divisor;
    assertEquals(expected_nx * expected_ny, tiles.length);

    final boolean[] covered = new boolean[width * height];
    for (final Tile tile : tiles) {
      assertTrue(tile.x0 % divisor == 0);
      assertTrue(tile.y0 % divisor == 0);
      assertTrue(tile.width > 0 && tile.width <= divisor);
      assertTrue(tile.height > 0 && tile.height <= divisor);
      assertEquals(Math.min(divisor, width - tile.x0), tile.width);
      assertEquals(Math.min(divisor, height - tile.y0), tile.height);
      for (int y = 0; y < tile.height; y++) {
        for (int x = 0; x < tile.width; x++) {
          final int px = tile.x0 + x;
          final int py = tile.y0 + y;
          assertTrue(px < width && py < height);
          final int index = py * width + px;
          assertTrue(!covered[index],
              "pixel covered twice at " + px + "," + py);
          covered[index] = true;
        }
      }
    }
    for (int i = 0; i < covered.length; i++) {
      assertTrue(covered[i], "pixel not covered at index " + i);
    }
  }

  @Test
  public void defaultDivisorCoversCanvas() {
    checkGrid(800, 600, 340);
  }

  @Test
  public void smallDivisorCoversCanvas() {
    checkGrid(1280, 1024, 50);
  }

  @Test
  public void exactMultipleStillCovers() {
    checkGrid(680, 340, 340);
  }

  @Test
  public void tinyCanvasIsOneTile() {
    checkGrid(100, 80, 340);
  }

  /**
   * With "show bins" each tile is shrunk by the same 4px margin the
   * default renderer leaves, so the background shows through as black
   * grid lines. Origins stay on the divisor grid; tiles never overlap.
   */
  @Test
  public void showBinsShrinksTilesByMargin() {
    RendererBinManager.show_bins = true;
    final int divisor = 340;
    RendererBinManager.divisor = divisor;
    final int width = 800;
    final int height = 600;
    final Tile[] tiles = ModularRendererRaytraced
        .buildTileGrid(width, height);

    final boolean[] covered = new boolean[width * height];
    for (final Tile tile : tiles) {
      assertTrue(tile.x0 % divisor == 0);
      assertTrue(tile.y0 % divisor == 0);
      assertTrue(tile.width > 0 && tile.width <= divisor - 4);
      assertTrue(tile.height > 0 && tile.height <= divisor - 4);
      assertEquals(Math.min(divisor - 4, width - tile.x0), tile.width);
      assertEquals(Math.min(divisor - 4, height - tile.y0), tile.height);
      for (int y = 0; y < tile.height; y++) {
        for (int x = 0; x < tile.width; x++) {
          final int index = (tile.y0 + y) * width + tile.x0 + x;
          assertTrue(!covered[index], "pixel covered twice");
          covered[index] = true;
        }
      }
    }
    // The 4px gutters are deliberately uncovered: they show the
    // background as black grid lines.
    int gutter_pixels = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (!covered[y * width + x]) {
          gutter_pixels++;
        }
      }
    }
    assertTrue(gutter_pixels > 0, "expected uncovered gutter pixels");
  }
}
