// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.ScenicBackground;

/**
 * Missed rays must paint the scenic grass/sky texture when it is enabled,
 * and the flat background colour when it is not.
 */
public class RaytracerScenicBackgroundTest {
  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private NodeManager saved_manager;

  private boolean saved_scenic_background;

  private int saved_background_number;

  private int saved_antialiasing;

  @BeforeEach
  public void setUp() {
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;

    Coords.x_pixels = 200;
    Coords.y_pixels = 200;
    Coords.x_pixelso2 = 100;
    Coords.y_pixelso2 = 100;
    Coords.shift_constant_x = 0;
    Coords.shift_constant_y = 0;
    Coords.shift_constant_z = 192;

    this.saved_manager = ContextManager.getNodeManager();
    // Fog is a no-op without a model; without this, a GUI test that
    // booted the app earlier in the same JVM would leave fog active.
    ContextManager.setNodeManager(null);

    this.saved_scenic_background = RendererDelegator.scenic_background;
    this.saved_background_number =
        RendererDelegator.color_background_number;
    this.saved_antialiasing = RendererDelegator.antialiasing;

    RendererDelegator.antialiasing = 1;
  }

  @AfterEach
  public void tearDown() {
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = Coords.x_pixels >> 1;
    Coords.y_pixelso2 = Coords.y_pixels >> 1;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
    ContextManager.setNodeManager(this.saved_manager);

    RendererDelegator.scenic_background = this.saved_scenic_background;
    RendererDelegator.color_background_number = this.saved_background_number;
    RendererDelegator.antialiasing = this.saved_antialiasing;
  }

  /** Renders a tile against an empty scene: every ray misses. */
  private int[] renderAllMiss(boolean scenic) {
    RendererDelegator.scenic_background = scenic;
    final BVH bvh = new BVH(new Primitive[0]);
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, new RayCamera(), bvh, pixels);
    return pixels;
  }

  @Test
  public void missesSampleTheScenicTexture() {
    final int[] pixels = renderAllMiss(true);
    final BufferedImage scenic = ScenicBackground.imageFor(200, 200);
    // Spot-check sky, horizon and grass rows across the tile.
    for (final int y : new int[] { 10, 50, 99, 100, 150, 190 }) {
      for (final int x : new int[] { 10, 100, 190 }) {
        assertEquals(scenic.getRGB(x, y), pixels[y * 200 + x],
            "miss at (" + x + ", " + y
                + ") must sample the scenic texture");
      }
    }
  }

  @Test
  public void missesUseTheFlatColourWhenScenicIsOff() {
    RendererDelegator.color_background_number = 0x112233;
    final int[] pixels = renderAllMiss(false);
    for (final int p : pixels) {
      assertEquals(0xFF112233, p,
          "miss must fall back to the flat background colour");
    }
  }

  @Test
  public void panSlidesMissSamplesLikeAFixedBackdrop() {
    // Pan the view right: geometry moves right on screen, so the
    // world-fixed backdrop must sample left of the screen pixel.
    Coords.shift_constant_x = 1920;
    final int[] pixels = renderAllMiss(true);
    final BufferedImage scenic = ScenicBackground.imageFor(200, 200);
    final int grass_tx = (int) Math.round(
        100 - ScenicBackground.GRASS_PARALLAX * 1920 / 192);
    final int sky_tx = (int) Math.round(
        100 - ScenicBackground.SKY_PARALLAX * 1920 / 192);
    // Grass pixel: dy > 0 below the middle.
    assertEquals(scenic.getRGB(grass_tx, 150), pixels[150 * 200 + 100],
        "grass miss must sample left of the pixel after a right pan");
    // Sky pixel: dy < 0 above the middle; moves less (distant).
    assertEquals(scenic.getRGB(sky_tx, 50), pixels[50 * 200 + 100],
        "sky miss must sample left of the pixel after a right pan");
    assertTrue(100 - grass_tx > 100 - sky_tx,
        "grass must slide further than the sky");
  }
}
