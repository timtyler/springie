// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.springie.render.modules.raytraced.ModularRendererRaytraced.ShownTile;
import com.springie.render.modules.raytraced.ModularRendererRaytraced.Tile;

/**
 * Frames must appear all at once: a tile's snapshot is published only by
 * publishFrame, which the renderer calls once every tile of the frame is
 * done. Repaints draw the published snapshots, never work-in-progress
 * tiles.
 */
public class FramePublishTest {

  private Tile tileWithHits(int x0, int y0) {
    final Tile tile = new Tile(x0, y0, 64, 64);
    tile.image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    stats.add(10, 20);
    stats.add(30, 40);
    tile.stats = stats;
    tile.done = true;
    return tile;
  }

  @Test
  public void nothingIsShownBeforePublish() {
    final Tile tile = tileWithHits(0, 0);
    assertNull(tile.shown,
        "a finished tile must not display before the frame completes");
  }

  @Test
  public void publishRevealsWholeFrameAtOnce() {
    final Tile[] tiles = new Tile[] { tileWithHits(0, 0),
        tileWithHits(64, 0) };
    ModularRendererRaytraced.publishFrame(tiles);
    for (final Tile tile : tiles) {
      assertTrue(tile.shown != null);
      assertSame(tile.image, tile.shown.image);
    }
  }

  @Test
  public void contentRectIsTranslatedToScreenCoords() {
    final Tile tile = tileWithHits(100, 200);
    ModularRendererRaytraced.publishFrame(new Tile[] { tile });
    final ShownTile shown = tile.shown;
    assertTrue(shown.active);
    // Tile-local (10, 20)..(30, 40) plus the tile origin.
    assertEquals(110, shown.min_x);
    assertEquals(220, shown.min_y);
    assertEquals(130, shown.max_x);
    assertEquals(240, shown.max_y);
  }

  @Test
  public void tileWithoutHitsIsInactive() {
    final Tile tile = new Tile(0, 0, 64, 64);
    tile.image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    tile.stats = new Raytracer.HitStats();
    tile.done = true;
    ModularRendererRaytraced.publishFrame(new Tile[] { tile });
    assertFalse(tile.shown.active);
  }
}
