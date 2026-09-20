// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RectangleInt;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.RendererTileManager;

/**
 * The ray-traced renderer must not repaint the whole window on every
 * frame: like the polygon renderer's dirty tiles, only re-traced tiles'
 * rectangles are blitted to the screen, and when no tile was re-traced
 * nothing is blitted at all -- the screen already shows the frame, so
 * screen-space overlays (the boundary-box dots) survive everywhere the
 * model did not paint.
 */
public class TileBlitTest {
  private static final int SIZE = 400;

  private static final int DIVISOR = 100;

  private static final int SENTINEL = 0xFFFF00FF;

  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_x_pixelso2;

  private int saved_y_pixelso2;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private int saved_divisor;

  private boolean saved_show_tiles;

  private boolean saved_show_active_tiles;

  private boolean saved_render_nodes;

  private boolean saved_render_links;

  private boolean saved_shadows;

  private boolean saved_scenic;

  private int saved_antialiasing;

  private int saved_pixellation;

  private ModularRendererBase saved_renderer;

  private NodeManager manager;

  private ModularRendererRaytraced renderer;

  private static int internal(int screen) {
    return (200 << Coords.shift) + (screen - 200) * 192;
  }

  private static Node nodeAt(int sx, int sy) {
    final Node node = new Node(new Point3D(internal(sx), internal(sy), 0),
        42, new NodeTypeFactory());
    node.clazz = new Clazz(0x123456);
    node.type.radius = 512;
    return node;
  }

  @BeforeEach
  public void setUp() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        !GraphicsEnvironment.isHeadless(), "needs a display");
    this.saved_x_pixels = Coords.x_pixels;
    this.saved_y_pixels = Coords.y_pixels;
    this.saved_x_pixelso2 = Coords.x_pixelso2;
    this.saved_y_pixelso2 = Coords.y_pixelso2;
    this.saved_shift_constant_x = Coords.shift_constant_x;
    this.saved_shift_constant_y = Coords.shift_constant_y;
    this.saved_shift_constant_z = Coords.shift_constant_z;
    Coords.x_pixels = SIZE;
    Coords.y_pixels = SIZE;
    Coords.x_pixelso2 = SIZE / 2;
    Coords.y_pixelso2 = SIZE / 2;
    Coords.shift_constant_x = 0;
    Coords.shift_constant_y = 0;
    Coords.shift_constant_z = 192;

    this.saved_divisor = RendererTileManager.divisor;
    this.saved_show_tiles = RendererTileManager.show_tiles;
    this.saved_show_active_tiles = RendererTileManager.show_active_tiles;
    RendererTileManager.divisor = DIVISOR;
    RendererTileManager.show_tiles = false;
    RendererTileManager.show_active_tiles = false;

    this.saved_render_nodes = FrEnd.render_nodes;
    this.saved_render_links = FrEnd.render_links;
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_scenic = RendererDelegator.scenic_background;
    this.saved_antialiasing = RendererDelegator.antialiasing;
    this.saved_pixellation = RendererDelegator.pixellation;
    FrEnd.render_nodes = true;
    FrEnd.render_links = true;
    RendererDelegator.shadows = false;
    RendererDelegator.scenic_background = false;
    RendererDelegator.antialiasing = 1;
    RendererDelegator.pixellation = 1;

    this.manager = new NodeManager();
    this.renderer = new ModularRendererRaytraced();
    this.renderer.buildTiles(SIZE, SIZE, false);
    this.saved_renderer = RendererDelegator.renderer;
    RendererDelegator.renderer = this.renderer;
  }

  @AfterEach
  public void tearDown() {
    RendererDelegator.renderer = this.saved_renderer;
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = this.saved_x_pixelso2;
    Coords.y_pixelso2 = this.saved_y_pixelso2;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
    RendererTileManager.divisor = this.saved_divisor;
    RendererTileManager.show_tiles = this.saved_show_tiles;
    RendererTileManager.show_active_tiles = this.saved_show_active_tiles;
    FrEnd.render_nodes = this.saved_render_nodes;
    FrEnd.render_links = this.saved_render_links;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.scenic_background = this.saved_scenic;
    RendererDelegator.antialiasing = this.saved_antialiasing;
    RendererDelegator.pixellation = this.saved_pixellation;
  }

  private void waitForDone() throws Exception {
    final Field done =
        ModularRendererRaytraced.class.getDeclaredField("frame_done");
    done.setAccessible(true);
    final long deadline = System.currentTimeMillis() + 30000;
    while (!((Boolean) done.get(this.renderer)).booleanValue()) {
      if (System.currentTimeMillis() > deadline) {
        throw new AssertionError("frame never completed");
      }
      Thread.sleep(10);
    }
  }

  private void repaintAndWait(BufferedImage dest) throws Exception {
    this.renderer.repaint(dest.getGraphics(), this.manager);
    waitForDone();
  }

  private static void fill(BufferedImage image, int rgb) {
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        image.setRGB(x, y, rgb);
      }
    }
  }

  /**
   * An empty scene settles into all-skipped frames. A repaint with
   * nothing staged must not touch the destination at all -- and the
   * all-skipped frame must still complete, or the renderer would never
   * start another frame.
   */
  @Test
  public void emptySceneBlitsNothingOnceSettled() throws Exception {
    final BufferedImage dest =
        new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
    repaintAndWait(dest);
    // Frame 2 is the first all-skipped frame; it must complete (not
    // freeze the renderer waiting for tasks that were never submitted).
    repaintAndWait(dest);
    fill(dest, SENTINEL);
    this.renderer.repaint(dest.getGraphics(), this.manager);
    for (int y = 0; y < SIZE; y++) {
      for (int x = 0; x < SIZE; x++) {
        assertEquals(SENTINEL, dest.getRGB(x, y),
            "idle repaint touched pixel " + x + "," + y);
      }
    }
    waitForDone();
  }

  /**
   * With a static sparse model, a settled repaint blits only the
   * content tiles' rectangles; empty tiles' screen regions are left
   * alone.
   */
  @Test
  public void partialFrameBlitsOnlyContentTiles() throws Exception {
    final Node a = nodeAt(100, 100);
    final Node b = nodeAt(150, 120);
    final Node c = nodeAt(320, 320);
    this.manager.element.add(a);
    this.manager.element.add(b);
    this.manager.element.add(c);
    this.manager.element.add(nodeAt(110, 300));
    this.manager.getLinkManager().element.add(new Link(a, b,
        new LinkTypeFactory().getNew(100 << Coords.shift, 50),
        new Clazz(0x654321)));

    final BufferedImage dest =
        new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
    repaintAndWait(dest);
    repaintAndWait(dest);

    final RectangleInt[] rects =
        this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    int content_tile = -1;
    int empty_tile = -1;
    for (int i = 0; i < rects.length; i++) {
      if (!rects[i].isEmpty() && content_tile < 0) {
        content_tile = i;
      }
      if (rects[i].isEmpty() && empty_tile < 0) {
        empty_tile = i;
      }
    }
    assertTrue(content_tile >= 0, "model must touch at least one tile");
    assertTrue(empty_tile >= 0, "model must leave at least one tile empty");

    fill(dest, SENTINEL);
    this.renderer.repaint(dest.getGraphics(), this.manager);

    // Only the content tile's dirty rectangle is blitted: every pixel
    // in it was ray-traced (geometry or background), so none can still
    // be the sentinel. The empty tile's screen region is left alone.
    final RectangleInt dirty = rects[content_tile];
    final int ccx = (dirty.min_x + dirty.max_x) / 2;
    final int ccy = (dirty.min_y + dirty.max_y) / 2;
    assertNotEquals(SENTINEL, dest.getRGB(ccx, ccy),
        "content tile's dirty rectangle was not blitted");
    final int nx = SIZE / DIVISOR;
    final int ecx = (empty_tile % nx) * DIVISOR + DIVISOR / 2;
    final int ecy = (empty_tile / nx) * DIVISOR + DIVISOR / 2;
    assertEquals(SENTINEL, dest.getRGB(ecx, ecy),
        "empty tile's screen region was repainted");
    waitForDone();
  }
}
