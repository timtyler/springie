// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import com.springie.render.modules.modern.RendererTileManager;
import com.springie.render.modules.raytraced.ModularRendererRaytraced.Tile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ray-traced renderer must tile the canvas with the same tiles as the
 * default renderer: divisor-sized blocks covering every pixel exactly once.
 */
public class TileGridTest {
  private int saved_divisor;

  private boolean saved_show_tiles;

  private boolean saved_one_big_tile;

  @BeforeEach
  public void saveDivisor() {
    this.saved_divisor = RendererTileManager.divisor;
    this.saved_show_tiles = RendererTileManager.show_tiles;
    this.saved_one_big_tile = RendererTileManager.one_big_tile;
    RendererTileManager.show_tiles = false;
    // These tests verify the grid tiling; force grid mode.
    RendererTileManager.one_big_tile = false;
  }

  @AfterEach
  public void restoreDivisor() {
    RendererTileManager.divisor = this.saved_divisor;
    RendererTileManager.show_tiles = this.saved_show_tiles;
    RendererTileManager.one_big_tile = this.saved_one_big_tile;
  }

  private void checkGrid(final int width, final int height, int divisor) {
    RendererTileManager.divisor = divisor;
    final Tile[] tiles = ModularRendererRaytraced
        .buildTileGrid(width, height);

    // Degenerate zero-area tiles (exact multiples of the divisor) are
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
   * "Show tiles" no longer shrinks the tiles: since 14a2513
   * (2026-10-04, Tim) the tiles cover every pixel with no gaps, and
   * the grid lines are a 1px black overlay drawn on the finished
   * frame by ModularRendererRaytraced. The grid itself must tile
   * the canvas exactly the same with the flag on or off. (The old
   * form of this test asserted a 4px margin with uncovered gutters;
   * that behaviour was removed in 14a2513. Re-scoped 2026-11-08.)
   */
  @Test
  public void showTilesStillCoversEveryPixel() {
    RendererTileManager.show_tiles = true;
    checkGrid(800, 600, 340);
  }
}
