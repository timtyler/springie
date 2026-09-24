// This program has been placed into the public domain by its author.
package com.springie.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.geometry.Vector3D;
import com.springie.render.modules.modern.PolygonComposite;
import com.springie.render.modules.modern.PolygonObject2D;

/**
 * The Olympics location markers only exist for demo models running
 * under the follow-cam; they ride the centering shift, are destroyed
 * off-screen, and respawn so a handful stays in play.
 */
class WorldMarkersTest {

  private boolean saved_demo;
  private boolean saved_show;
  private boolean saved_x;
  private boolean saved_y;
  private boolean saved_z;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.saved_demo = FrEnd.demo_model;
    this.saved_show = FrEnd.show_world_markers;
    this.saved_x = FrEnd.continuously_centre_x;
    this.saved_y = FrEnd.continuously_centre_y;
    this.saved_z = FrEnd.continuously_centre_z;
    FrEnd.demo_model = true;
    FrEnd.show_world_markers = true;
    FrEnd.continuously_centre_x = true;
    FrEnd.continuously_centre_y = false;
    FrEnd.continuously_centre_z = false;
    WorldMarkers.clear();
  }

  @Test
  void markersSpawnAcrossTheDepthBand() {
    WorldMarkers.onFrame(new Vector3D(0, 0, 0));
    assertEquals(WorldMarkers.TARGET_COUNT, WorldMarkers.size());

    final int z_depth = Coords.z_pixels << Coords.shift;
    final int z_min = (int) (z_depth * 0.10);
    final int z_max = (int) (z_depth * 0.90);
    int seen_min = Integer.MAX_VALUE;
    int seen_max = Integer.MIN_VALUE;
    for (int i = WorldMarkers.size(); --i >= 0;) {
      final int z = WorldMarkers.marker(i)[2];
      assertTrue(z >= z_min && z <= z_max,
          "marker depth inside the parallax band: " + z);
      seen_min = Math.min(seen_min, z);
      seen_max = Math.max(seen_max, z);
    }
    // The depths actually vary -- not all spawned at one depth.
    assertTrue(seen_max - seen_min > (z_max - z_min) / 2,
        "depths spread across the band: " + seen_min + ".." + seen_max);
  }

  @Test
  void nearMarkersStreamFasterThanFarOnes() {
    // A real canvas centres x_pixelso2; the unit default is 0.
    final int saved_xo2 = Coords.x_pixelso2;
    final int saved_yo2 = Coords.y_pixelso2;
    Coords.x_pixelso2 = Coords.x_pixels / 2;
    Coords.y_pixelso2 = Coords.y_pixels / 2;
    try {
      final int z_depth = Coords.z_pixels << Coords.shift;
      final int z_near = (int) (z_depth * 0.10);
      final int z_far = (int) (z_depth * 0.90);
      // Internal coords that project to screen centre at any depth.
      final int ix =
          (Coords.x_pixelso2 << Coords.shift) - Coords.shift_constant_x;
      final int iy =
          (Coords.y_pixelso2 << Coords.shift) - Coords.shift_constant_y;
      WorldMarkers.addForTest(ix, iy, z_near);
      WorldMarkers.addForTest(ix, iy, z_far);
      final int sx_near_before = Coords.getXCoords(ix, z_near);
      final int sx_far_before = Coords.getXCoords(ix, z_far);
      assertEquals(Coords.x_pixelso2, sx_near_before);
      assertEquals(Coords.x_pixelso2, sx_far_before);

      // The world streams left as the creature travels right.
      WorldMarkers.onFrame(new Vector3D(-(50 << Coords.shift), 0, 0));

      // Our two markers are indices 0 and 1: cull keeps on-screen
      // markers and spawn only appends.
      final int[] near = WorldMarkers.marker(0);
      final int[] far = WorldMarkers.marker(1);
      final int move_near = sx_near_before - Coords.getXCoords(near[0], near[2]);
      final int move_far = sx_far_before - Coords.getXCoords(far[0], far[2]);
      assertTrue(move_near > 0 && move_far > 0, "both stream left");
      assertTrue(move_near > move_far, "near marker streams faster: "
          + move_near + " screen px vs " + move_far);
    } finally {
      Coords.x_pixelso2 = saved_xo2;
      Coords.y_pixelso2 = saved_yo2;
    }
  }

  @Test
  void nearMarkersDrawBiggerThanFarOnes() {
    final int z_depth = Coords.z_pixels << Coords.shift;
    final int z_near = (int) (z_depth * 0.10);
    final int z_far = (int) (z_depth * 0.90);

    assertTrue(WorldMarkers.screenHalf(z_near)
        > WorldMarkers.screenHalf(z_far),
        "near markers draw bigger: " + WorldMarkers.screenHalf(z_near)
            + " vs " + WorldMarkers.screenHalf(z_far));
  }

  @AfterEach
  void tearDown() {
    FrEnd.demo_model = this.saved_demo;
    FrEnd.show_world_markers = this.saved_show;
    FrEnd.continuously_centre_x = this.saved_x;
    FrEnd.continuously_centre_y = this.saved_y;
    FrEnd.continuously_centre_z = this.saved_z;
    WorldMarkers.clear();
  }

  @Test
  void seedsTargetCountWhenActive() {
    WorldMarkers.onFrame(new Vector3D(0, 0, 0));

    assertEquals(WorldMarkers.TARGET_COUNT, WorldMarkers.size(),
        "a handful of markers is kept in play");
  }

  @Test
  void inactiveWhenNotDemo() {
    FrEnd.demo_model = false;

    WorldMarkers.onFrame(new Vector3D(0, 0, 0));

    assertEquals(0, WorldMarkers.size(), "no markers for regular models");
  }

  @Test
  void inactiveWhenHidden() {
    FrEnd.show_world_markers = false;

    WorldMarkers.onFrame(new Vector3D(0, 0, 0));

    assertEquals(0, WorldMarkers.size(), "no markers when switched off");
  }

  @Test
  void inactiveWhenNoCentering() {
    FrEnd.continuously_centre_x = false;

    WorldMarkers.onFrame(new Vector3D(0, 0, 0));

    assertEquals(0, WorldMarkers.size(), "no markers without the follow-cam");
  }

  @Test
  void markersRideTheCenteringShift() {
    final int x = Coords.x_pixels << (Coords.shift - 1);
    final int y = Coords.y_pixels << (Coords.shift - 1);
    WorldMarkers.addForTest(x, y, 0);

    WorldMarkers.onFrame(new Vector3D(100 << Coords.shift, 0, 0));

    assertEquals(x + (100 << Coords.shift), WorldMarkers.marker(0)[0],
        "marker rides the same shift as the creature");
    assertEquals(y, WorldMarkers.marker(0)[1], "y untouched");
  }

  @Test
  void offScreenMarkersAreDestroyedAndReplaced() {
    WorldMarkers.onFrame(new Vector3D(0, 0, 0));
    assertEquals(WorldMarkers.TARGET_COUNT, WorldMarkers.size());

    // Shove everything far past the edge in one frame.
    final int huge = (Coords.x_pixels + 1000) << Coords.shift;
    WorldMarkers.onFrame(new Vector3D(-huge, 0, 0));

    assertEquals(WorldMarkers.TARGET_COUNT, WorldMarkers.size(),
        "destroyed markers are replaced");
    final int margin = 24;
    for (int i = WorldMarkers.size(); --i >= 0;) {
      final int[] m = WorldMarkers.marker(i);
      final int sx = Coords.getXCoords(m[0], m[2]);
      final int sy = Coords.getYCoords(m[1], m[2]);
      assertTrue(sx >= -margin && sx <= Coords.x_pixels + margin,
          "respawned marker on screen in x: " + sx);
      assertTrue(sy >= -margin && sy <= Coords.y_pixels + margin,
          "respawned marker on screen in y: " + sy);
    }
  }

  @Test
  void drawDoesNotThrow() {
    WorldMarkers.onFrame(new Vector3D(0, 0, 0));
    final BufferedImage image = new BufferedImage(Coords.x_pixels,
        Coords.y_pixels, BufferedImage.TYPE_INT_RGB);
    final Graphics g = image.getGraphics();

    WorldMarkers.draw(g);

    g.dispose();
  }

  @Test
  void addToTilesFeedsOneQuadPerMarker() {
    WorldMarkers.onFrame(new Vector3D(0, 0, 0));
    final ArrayList<PolygonComposite> all = new ArrayList<>();

    WorldMarkers.addToTiles(all);

    assertEquals(WorldMarkers.TARGET_COUNT, all.size(),
        "one tile quad per marker, for the modern tiled renderer");
  }

  @Test
  void tileQuadsAreSquaresNotCollapsedLines() {
    final int half = 5;
    final PolygonObject2D quad = WorldMarkers.buildQuad(100, 100, half);
    final BufferedImage image =
        new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
    final Graphics g = image.getGraphics();

    quad.fill(g, 0xFFFFFF);

    g.dispose();
    int filled = 0;
    for (int y = 0; y < 200; y++) {
      for (int x = 0; x < 200; x++) {
        if (image.getRGB(x, y) == 0xFFFFFFFF) {
          filled++;
        }
      }
    }
    // A filled 2h x 2h square covers 4h^2 pixels; the collapsed
    // diagonal-line quad (swapped y pairing, caught by screenshot
    // 2026-09-23) covers only a thin sliver of that.
    assertTrue(filled > 2 * half * half,
        "quad fills its square: " + filled + " px");
  }

  @Test
  void addToTilesIsNoOpWhenInactive() {
    FrEnd.demo_model = false;
    final ArrayList<PolygonComposite> all = new ArrayList<>();

    WorldMarkers.addToTiles(all);

    assertEquals(0, all.size(), "no quads for regular models");
  }

  @Test
  void damageCoversEveryMarker() {
    WorldMarkers.onFrame(new Vector3D(0, 0, 0));

    final RectangleInt damage = WorldMarkers.getDamage();

    assertNotNull(damage, "damage rect while markers are up");
    for (int i = WorldMarkers.size(); --i >= 0;) {
      final int[] m = WorldMarkers.marker(i);
      final int sx = Coords.getXCoords(m[0], m[2]);
      final int sy = Coords.getYCoords(m[1], m[2]);
      assertTrue(sx >= damage.min_x && sx <= damage.max_x,
          "marker x inside damage: " + sx);
      assertTrue(sy >= damage.min_y && sy <= damage.max_y,
          "marker y inside damage: " + sy);
    }
  }

  @Test
  void damageIsNullWhenInactive() {
    FrEnd.demo_model = false;

    assertNull(WorldMarkers.getDamage(), "no damage without markers");
  }

  @Test
  void damageCoversTheOldPositionsToo() {
    WorldMarkers.onFrame(new Vector3D(0, 0, 0));
    final RectangleInt before = WorldMarkers.getDamage();
    assertNotNull(before);

    // Shove the world; the damage must span where the markers were
    // as well as where they are, so the ray tracer re-traces both.
    final int shift = 200 << Coords.shift;
    WorldMarkers.onFrame(new Vector3D(shift, 0, 0));
    final RectangleInt after = WorldMarkers.getDamage();

    assertNotNull(after);
    assertTrue(after.min_x <= before.min_x,
        "old left edge covered: " + after.min_x + " vs " + before.min_x);
    assertTrue(after.max_x >= before.max_x,
        "old right edge covered: " + after.max_x + " vs " + before.max_x);
  }
}
