package com.springie.render;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.springie.FrEnd;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.render.modules.modern.PolygonComposite;
import com.springie.render.modules.modern.PolygonObject2D;

/**
 * Olympics follow-cam decoration: small gold location markers that live
 * in world space and stream past while continuous centering holds a
 * demo creature on screen centre.
 *
 * The markers are not model nodes: they have no physics, take no part
 * in collisions, never enter the bounding box, and are never centered
 * themselves. Each tick they ride the same world offset the centering
 * applied to the creature, which keeps them world-locked. The markers
 * are spread through the depth of the world, so the perspective
 * projection gives parallax scrolling: near markers stream past
 * faster (and draw a touch bigger) than far ones. Markers that leave
 * the screen are destroyed and respawn scattered, so the swarm never
 * marches across in lockstep.
 *
 * Rendering follows the drag-box pattern, one path per renderer, so
 * moving markers never leave trails:
 * <ul>
 * <li>modern tiled renderer: {@link #addToTiles} feeds one quad per
 * marker into the tile geometry -- the tiled damage repair erases the
 * old quads when they move;
 * <li>ray-traced renderer: {@link #getDamage} joins the old and new
 * marker bounds to the dirty rectangles, so the old dots are re-traced
 * away, and {@link #draw} plots the current dots over the blit;
 * <li>old polygon renderer: {@link #draw} plots screen-space, with a
 * full clear-and-redraw every frame while markers are up.
 * </ul>
 */
public final class WorldMarkers {
  /** Markers kept in play. */
  static final int TARGET_COUNT = 100;

  /**
   * Marker half-size at mid depth, in screen pixels. Nearer markers
   * draw bigger, farther ones smaller -- the same perspective the
   * projection gives their streaming motion.
   */
  private static final int MARKER_HALF_MID = 2;

  private static final int MARKER_HALF_MIN = 1;

  private static final int MARKER_HALF_MAX = 4;

  /**
   * Depth band the markers spawn in, as fractions of the z-glass
   * depth: spread through the depth for parallax, kept clear of the
   * eye plane (z = 0, where the projection divides) and the far glass.
   */
  private static final double Z_NEAR_FRAC = 0.10;

  private static final double Z_FAR_FRAC = 0.90;

  private static final int MARKER_COLOUR = new Color(255, 200, 60).getRGB();

  /** Marker depth: on top of everything, like the drag box. */
  private static final int MARKER_Z = Integer.MAX_VALUE;

  private static final List<Point3D> markers = new ArrayList<>();

  private static final Random rnd = new Random();

  /**
   * Previous damage rect for the ray tracer: the union of the marker
   * bounds before and after the last move, so one dirty computation
   * covers both.
   */
  private static RectangleInt last_damage;

  private WorldMarkers() {
  }

  /**
   * Advances the markers one tick by the world offset the centering
   * just applied. Called from the per-tick path, not the collision
   * path, so it runs for the demo models too.
   */
  public static void onFrame(Vector3D delta) {
    synchronized (markers) {
      if (!olympicsActive()) {
        markers.clear();
        return;
      }
      for (Point3D p : markers) {
        p.x += delta.x;
        p.y += delta.y;
        p.z += delta.z;
      }
      cullOffscreen();
      while (markers.size() < TARGET_COUNT) {
        spawn();
      }
    }
  }

  /** Clears all markers; tests and probes start from here. */
  static void clear() {
    synchronized (markers) {
      markers.clear();
      last_damage = null;
    }
  }

  /** Adds one marker at the given internal coords; tests only. */
  static void addForTest(int x, int y, int z) {
    synchronized (markers) {
      markers.add(new Point3D(x, y, z));
    }
  }

  /** Internal coords of one marker as {x, y, z}; tests only. */
  static int[] marker(int i) {
    synchronized (markers) {
      final Point3D p = markers.get(i);
      return new int[] {(int) p.x, (int) p.y, (int) p.z};
    }
  }

  /** Number of markers in play; exposed for tests and probes. */
  static int size() {
    synchronized (markers) {
      return markers.size();
    }
  }

  /**
   * Screen-space plot of the current markers, for renderers that paint
   * straight onto the screen with no damage repair (the old polygon
   * renderer). A no-op unless a demo model is showing markers.
   */
  public static void draw(Graphics graphics) {
    if (!olympicsActive()) {
      return;
    }
    graphics.setColor(new Color(MARKER_COLOUR));
    synchronized (markers) {
      for (Point3D p : markers) {
        final int sx = Coords.getXCoords((int) p.x, (int) p.z);
        final int sy = Coords.getYCoords((int) p.y, (int) p.z);
        final int half = screenHalf((int) p.z);
        graphics.fillRect(sx - half, sy - half, half * 2, half * 2);
      }
    }
  }

  /**
   * Tim's separate-space renderer: draws the markers as a simple 2D
   * overlay BEFORE the main model tiles. Removes the old dots, draws
   * the new dots. The tiles then obliterate any dots underneath them.
   * Same for ray-tracer and polygon renderer.
   */
  private static final List<RectangleInt> old_dots = new ArrayList<>();

  public static void drawUnder(Graphics graphics) {
    if (!olympicsActive()) {
      // Clear any leftover dots.
      for (RectangleInt r : old_dots) {
        graphics.clearRect(r.min_x, r.min_y, r.max_x - r.min_x, r.max_y - r.min_y);
      }
      old_dots.clear();
      return;
    }

    // Remove the old dots.
    for (RectangleInt r : old_dots) {
      graphics.clearRect(r.min_x, r.min_y, r.max_x - r.min_x, r.max_y - r.min_y);
    }
    old_dots.clear();

    // Draw the new dots.
    graphics.setColor(new Color(MARKER_COLOUR));
    // Tim: cull dots outside the physics boundary box (the dotted
    // outline). We don't need those.
    final int x_max = Coords.x_pixels << Coords.shift;
    final int y_max = Coords.y_pixels << Coords.shift;
    final int z_max = Coords.z_pixels << Coords.shift;
    synchronized (markers) {
      for (Point3D p : markers) {
        final int px = (int) p.x;
        final int py = (int) p.y;
        final int pz = (int) p.z;
        if (px < 0 || px > x_max || py < 0 || py > y_max || pz < 0 || pz > z_max) {
          continue;
        }
        final int sx = Coords.getXCoords(px, pz);
        final int sy = Coords.getYCoords(py, pz);
        final int half = screenHalf(pz);
        graphics.fillRect(sx - half, sy - half, half * 2, half * 2);
        old_dots.add(new RectangleInt(sx - half, sy - half, sx + half, sy + half));
      }
    }
  }

  /**
   * Feeds one quad per marker into the modern tiled renderer's polygon
   * list. The quads ride the normal tile damage repair, so the old
   * marker images are scrubbed when the markers move -- no trails.
   */
  public static void addToTiles(ArrayList<PolygonComposite> all) {
    if (!olympicsActive()) {
      return;
    }
    synchronized (markers) {
      for (Point3D p : markers) {
        final int sx = Coords.getXCoords((int) p.x, (int) p.z);
        final int sy = Coords.getYCoords((int) p.y, (int) p.z);
        final int half = screenHalf((int) p.z);
        all.add(new PolygonComposite(new PolygonObject2D[] {
            buildQuad(sx, sy, half),
        }, MARKER_Z));
      }
    }
  }

  /**
   * One marker's screen-space quad. Package-visible for tests: the
   * corner pairing is easy to get wrong (a swapped y pairing collapses
   * the quad into a diagonal line, caught by screenshot 2026-09-23).
   */
  static PolygonObject2D buildQuad(int sx, int sy, int half) {
    return new PolygonObject2D(
        new int[] {sx - half, sx + half,
            sx + half, sx - half, },
        new int[] {sy - half, sy - half,
            sy + half, sy + half, },
        MARKER_COLOUR);
  }

  /**
   * The screen region damaged by the markers: the union of the
   * previous and current marker bounds, expanded by the marker size.
   * Null when there are no markers now and were none before. The ray
   * tracer joins this to its dirty rectangles so re-traced tiles erase
   * the old dots.
   */
  public static RectangleInt getDamage() {
    final RectangleInt current = currentBounds();
    final RectangleInt damage;
    if (current == null) {
      // No markers now: one last damage rect for the previous ones,
      // so their tiles get re-traced and the dots vanish for good.
      damage = last_damage;
      last_damage = null;
    } else if (last_damage == null) {
      damage = current;
      last_damage = new RectangleInt(current.min_x, current.min_y,
          current.max_x, current.max_y);
    } else {
      damage = new RectangleInt(
          Math.min(current.min_x, last_damage.min_x),
          Math.min(current.min_y, last_damage.min_y),
          Math.max(current.max_x, last_damage.max_x),
          Math.max(current.max_y, last_damage.max_y));
      last_damage.min_x = current.min_x;
      last_damage.min_y = current.min_y;
      last_damage.max_x = current.max_x;
      last_damage.max_y = current.max_y;
    }
    return damage;
  }

  /** Screen bounds of the current markers, or null when there are none. */
  private static RectangleInt currentBounds() {
    synchronized (markers) {
      if (markers.isEmpty() || !olympicsActive()) {
        return null;
      }
      int min_x = Integer.MAX_VALUE;
      int min_y = Integer.MAX_VALUE;
      int max_x = Integer.MIN_VALUE;
      int max_y = Integer.MIN_VALUE;
      for (Point3D p : markers) {
        final int sx = Coords.getXCoords((int) p.x, (int) p.z);
        final int sy = Coords.getYCoords((int) p.y, (int) p.z);
        final int half = screenHalf((int) p.z);
        min_x = Math.min(min_x, sx - half);
        min_y = Math.min(min_y, sy - half);
        max_x = Math.max(max_x, sx + half);
        max_y = Math.max(max_y, sy + half);
      }
      return new RectangleInt(min_x, min_y, max_x, max_y);
    }
  }

  private static boolean olympicsActive() {
    return FrEnd.show_world_markers
        && (FrEnd.continuously_centre_x || FrEnd.continuously_centre_y
            || FrEnd.continuously_centre_z);
  }

  /**
   * Screen half-size of a marker at the given internal depth: the
   * perspective divisor at mid depth over the divisor here, so nearer
   * markers draw bigger and farther ones smaller.
   */
  static int screenHalf(int z) {
    final int divisor = Coords.shift_constant_z + (z >> Coords.shift_z);
    final int mid_z = (Coords.z_pixels << Coords.shift) >> 1;
    final int mid_divisor =
        Coords.shift_constant_z + (mid_z >> Coords.shift_z);
    final int half = (MARKER_HALF_MID * mid_divisor) / Math.max(1, divisor);
    return Math.min(MARKER_HALF_MAX, Math.max(MARKER_HALF_MIN, half));
  }

  /** Drops markers that have left the screen (with a small margin). */
  private static void cullOffscreen() {
    final int margin = 40;
    final int x_pixels = Coords.x_pixels;
    final int y_pixels = Coords.y_pixels;
    // Tim: also cull markers outside the physics boundary box.
    final int x_max = Coords.x_pixels << Coords.shift;
    final int y_max = Coords.y_pixels << Coords.shift;
    final int z_max = Coords.z_pixels << Coords.shift;
    markers.removeIf(p -> {
      final int px = (int) p.x;
      final int py = (int) p.y;
      final int pz = (int) p.z;
      if (px < 0 || px > x_max || py < 0 || py > y_max || pz < 0 || pz > z_max) {
        return true;
      }
      final int sx = Coords.getXCoords(px, pz);
      final int sy = Coords.getYCoords(py, pz);
      return sx < -margin || sx > x_pixels + margin
          || sy < -margin || sy > y_pixels + margin;
    });
  }

  /**
   * Spawns one marker at a random screen position (marginally
   * off-screen included, so markers drift in) and a random depth
   * inside the parallax band. The internal coords invert the
   * perspective projection exactly, so the marker lands on the chosen
   * screen pixel at its depth. The markers all ride the same world
   * offset, so their shared streaming motion is what shows the
   * viewport moving -- the depth spread turns it into parallax, and
   * the scatter keeps them from marching across in lockstep.
   */
  private static void spawn() {
    final int x_pixels = Coords.x_pixels;
    final int y_pixels = Coords.y_pixels;
    final int margin = 20;
    final int sx = -margin + rnd.nextInt(Math.max(1, x_pixels + margin * 2));
    final int sy = -margin + rnd.nextInt(Math.max(1, y_pixels + margin * 2));
    final int z_depth = Coords.z_pixels << Coords.shift;
    final int z = (int) (z_depth * (Z_NEAR_FRAC
        + rnd.nextDouble() * (Z_FAR_FRAC - Z_NEAR_FRAC)));
    final int divisor = Coords.shift_constant_z + (z >> Coords.shift_z);
    final int ix = (sx - Coords.x_pixelso2) * divisor
        + (Coords.x_pixelso2 << Coords.shift) - Coords.shift_constant_x;
    final int iy = (sy - Coords.y_pixelso2) * divisor
        + (Coords.y_pixelso2 << Coords.shift) - Coords.shift_constant_y;
    markers.add(new Point3D(ix, iy, z));
  }
}
