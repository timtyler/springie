// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

/**
 * Miss rays must use the live background colour (Colours &gt; General &gt;
 * Background). The background used to be frozen in a static initializer at
 * class-load time, so recolouring the background mid-session had no
 * effect in ray-traced mode.
 */
public class RaytracerBackgroundTest {
  private static final int BLUE = 0xFF0000FF;

  private static final int BLACK = 0xFF000000;

  private int saved_bg_number;

  private Color saved_bg;

  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_x_pixelso2;

  private int saved_y_pixelso2;

  @BeforeEach
  public void setUp() {
    this.saved_bg_number = RendererDelegator.color_background_number;
    this.saved_bg = RendererDelegator.color_background;

    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_x_pixelso2 = Coords.x_pixelso2;
    this.saved_y_pixelso2 = Coords.y_pixelso2;

    Coords.x_pixels = 200;
    Coords.y_pixels = 200;
    Coords.x_pixelso2 = 100;
    Coords.y_pixelso2 = 100;
  }

  @AfterEach
  public void tearDown() {
    RendererDelegator.color_background_number = this.saved_bg_number;
    RendererDelegator.color_background = this.saved_bg;

    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = this.saved_x_pixelso2;
    Coords.y_pixelso2 = this.saved_y_pixelso2;
  }

  private int[] renderEmptyTile() {
    final BVH bvh = new BVH(new Primitive[0]);
    final RayCamera camera = new RayCamera();
    final int[] pixels = new int[200 * 200];
    Raytracer.renderTile(0, 0, 200, 200, camera, bvh, pixels);
    return pixels;
  }

  private static void assertAllPixels(int expected, int[] pixels,
      String what) {
    for (int i = 0; i < pixels.length; i++) {
      assertEquals(expected, pixels[i], what + " at pixel " + i);
    }
  }

  @Test
  public void missRaysUseTheConfiguredBackgroundColour() {
    RendererDelegator.color_background_number = BLUE;
    RendererDelegator.color_background = new Color(BLUE);
    assertAllPixels(BLUE, renderEmptyTile(),
        "every miss ray must return the background colour");
  }

  @Test
  public void backgroundChangeMidSessionTakesEffect() {
    RendererDelegator.color_background_number = BLACK;
    RendererDelegator.color_background = new Color(BLACK);
    assertAllPixels(BLACK, renderEmptyTile(),
        "first render must be black");

    // Recolour mid-session, exactly as the colour picker does.
    RendererDelegator.color_background_number = BLUE;
    RendererDelegator.color_background = new Color(BLUE);
    assertAllPixels(BLUE, renderEmptyTile(),
        "the recoloured background must take effect on the next tile");
  }
}
