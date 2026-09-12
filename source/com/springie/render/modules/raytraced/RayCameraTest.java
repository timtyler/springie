// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.render.Coords;

/**
 * Pins the RayCamera to the default renderer's projection: every camera ray
 * must pass exactly through the 3D points that project onto its pixel, and
 * all rays must share the projection's eye point.
 */
public class RayCameraTest {
  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  @BeforeEach
  public void saveCoords() {
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;
  }

  @AfterEach
  public void restoreCoords() {
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = Coords.x_pixels >> 1;
    Coords.y_pixelso2 = Coords.y_pixels >> 1;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
  }

  private void setUpView(int x_pixels, int y_pixels, int shift_x,
      int shift_y, int shift_z) {
    Coords.x_pixels = x_pixels;
    Coords.y_pixels = y_pixels;
    Coords.x_pixelso2 = x_pixels >> 1;
    Coords.y_pixelso2 = y_pixels >> 1;
    Coords.shift_constant_x = shift_x;
    Coords.shift_constant_y = shift_y;
    Coords.shift_constant_z = shift_z;
  }

  /** Continuous (untruncated) version of Coords.getXCoords/getYCoords. */
  private static double projectX(double x, double z) {
    final double a = Coords.shift_constant_x - (Coords.x_pixelso2 << 8);
    return Coords.x_pixelso2
        + (x + a) / (Coords.shift_constant_z + z / 1024.0);
  }

  private static double projectY(double y, double z) {
    final double b = Coords.shift_constant_y - (Coords.y_pixelso2 << 8);
    return Coords.y_pixelso2
        + (y + b) / (Coords.shift_constant_z + z / 1024.0);
  }

  private static double distancePointToRay(double px, double py, double pz,
      Ray ray) {
    final double vx = px - ray.ox;
    final double vy = py - ray.oy;
    final double vz = pz - ray.oz;
    // |(v x d)|, with d normalized.
    final double cx = vy * ray.dz - vz * ray.dy;
    final double cy = vz * ray.dx - vx * ray.dz;
    final double cz = vx * ray.dy - vy * ray.dx;
    return Math.sqrt(cx * cx + cy * cy + cz * cz);
  }

  @Test
  public void raysPassThroughTheirProjectedPoints() {
    setUpView(800, 600, 0, 0,
        Coords.shift_shifted - (Coords.shift_shifted >> 2));
    final RayCamera camera = new RayCamera();
    final Ray ray = new Ray();
    final Random random = new Random(42);

    for (int i = 0; i < 200; i++) {
      final double x = (random.nextDouble() - 0.5) * 200000.0;
      final double y = (random.nextDouble() - 0.5) * 200000.0;
      final double z = (random.nextDouble() - 0.5) * 200000.0;
      final double sx = projectX(x, z);
      final double sy = projectY(y, z);
      camera.makeRay(sx, sy, ray);
      final double distance = distancePointToRay(x, y, z, ray);
      assertTrue(distance < 1e-6,
          "point should lie on its pixel's ray, distance=" + distance);
    }
  }

  @Test
  public void raysPassThroughPointsWhenPannedAndZoomed() {
    setUpView(1280, 1024, 37 << 11, -12 << 11, 96);
    final RayCamera camera = new RayCamera();
    final Ray ray = new Ray();
    final Random random = new Random(7);

    for (int i = 0; i < 200; i++) {
      final double x = (random.nextDouble() - 0.5) * 400000.0;
      final double y = (random.nextDouble() - 0.5) * 400000.0;
      final double z = (random.nextDouble() - 0.5) * 100000.0;
      camera.makeRay(projectX(x, z), projectY(y, z), ray);
      assertTrue(distancePointToRay(x, y, z, ray) < 1e-6);
    }
  }

  @Test
  public void allRaysShareTheEyePoint() {
    setUpView(800, 600, 5000, -3000, 192);
    final RayCamera camera = new RayCamera();
    final Ray ray = new Ray();
    final Random random = new Random(13);

    for (int i = 0; i < 50; i++) {
      final int sx = random.nextInt(800);
      final int sy = random.nextInt(600);
      camera.makeRay(sx, sy, ray);
      assertEquals(camera.getEyeX(), ray.ox, 1e-12);
      assertEquals(camera.getEyeY(), ray.oy, 1e-12);
      assertEquals(camera.getEyeZ(), ray.oz, 1e-12);
      final double length = Math.sqrt(
          ray.dx * ray.dx + ray.dy * ray.dy + ray.dz * ray.dz);
      assertEquals(1.0, length, 1e-12);
    }
  }

  @Test
  public void continuousProjectionMatchesIntegerCoords() {
    setUpView(800, 600, 0, 0,
        Coords.shift_shifted - (Coords.shift_shifted >> 2));
    final Random random = new Random(99);

    for (int i = 0; i < 200; i++) {
      final int x = random.nextInt(200000) - 100000;
      final int y = random.nextInt(200000) - 100000;
      final int z = random.nextInt(200000) - 100000;
      // The integer code uses (z >> 10), which rounds down for negative z;
      // replicate that here. The remaining difference is truncation,
      // under a pixel.
      final double a = Coords.shift_constant_x - (Coords.x_pixelso2 << 8);
      final double b = Coords.shift_constant_y - (Coords.y_pixelso2 << 8);
      final double divisor = Coords.shift_constant_z + (z >> Coords.shift_z);
      final double ref_x = Coords.x_pixelso2 + (x + a) / divisor;
      final double ref_y = Coords.y_pixelso2 + (y + b) / divisor;
      assertTrue(Math.abs(ref_x - Coords.getXCoords(x, z)) < 1.0);
      assertTrue(Math.abs(ref_y - Coords.getYCoords(y, z)) < 1.0);
    }
  }
}
