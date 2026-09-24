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
