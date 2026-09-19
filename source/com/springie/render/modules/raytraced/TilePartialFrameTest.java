// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

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
import com.springie.render.modules.modern.RendererBinManager;
import com.springie.render.modules.raytraced.ModularRendererRaytraced.Tile;

/**
 * Re-tracing only the non-skipped tiles must be pixel-identical to
 * re-tracing the whole canvas: a tile that was empty and is still empty
 * shows only background, so keeping its snapshot changes nothing.
 */
public class TilePartialFrameTest {
  private static final int SIZE = 400;

  private static final int DIVISOR = 100;

  private int saved_x_pixels;

  private int saved_y_pixels;

  private int saved_x_pixelso2;

  private int saved_y_pixelso2;

  private int saved_shift_constant_x;

  private int saved_shift_constant_y;

  private int saved_shift_constant_z;

  private int saved_divisor;

  private boolean saved_show_bins;

  private boolean saved_render_nodes;

  private boolean saved_render_links;

  private boolean saved_shadows;

  private boolean saved_scenic;

  private int saved_antialiasing;

  private int saved_pixellation;

  private NodeManager manager;

  private ModularRendererRaytraced renderer;

  private Tile[] tiles;

  private static int internalX(int sx) {
    return (200 << Coords.shift) + (sx - 200) * 192;
  }

  private static Node node(int sx, int sy) {
    final Node node = new Node(
        new Point3D(internalX(sx), internalX(sy), 0), 42,
        new NodeTypeFactory());
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

    this.saved_divisor = RendererBinManager.divisor;
    this.saved_show_bins = RendererBinManager.show_bins;
    RendererBinManager.divisor = DIVISOR;
    RendererBinManager.show_bins = false;

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
    final Node a = node(100, 100);
    final Node b = node(150, 120);
    final Node c = node(320, 320);
    this.manager.element.add(a);
    this.manager.element.add(b);
    this.manager.element.add(c);
    this.manager.getLinkManager().element.add(new Link(a, b,
        new LinkTypeFactory().getNew(100 << Coords.shift, 50),
        new Clazz(0x654321)));

    this.renderer = new ModularRendererRaytraced();
    this.renderer.buildTiles(SIZE, SIZE, false);
    // Same geometry the renderer built, for driving the tiles directly.
    this.tiles = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
  }

  @AfterEach
  public void tearDown() {
    Coords.x_pixels = this.saved_x_pixels;
    Coords.y_pixels = this.saved_y_pixels;
    Coords.x_pixelso2 = this.saved_x_pixelso2;
    Coords.y_pixelso2 = this.saved_y_pixelso2;
    Coords.shift_constant_x = this.saved_shift_constant_x;
    Coords.shift_constant_y = this.saved_shift_constant_y;
    Coords.shift_constant_z = this.saved_shift_constant_z;
    RendererBinManager.divisor = this.saved_divisor;
    RendererBinManager.show_bins = this.saved_show_bins;
    FrEnd.render_nodes = this.saved_render_nodes;
    FrEnd.render_links = this.saved_render_links;
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.scenic_background = this.saved_scenic;
    RendererDelegator.antialiasing = this.saved_antialiasing;
    RendererDelegator.pixellation = this.saved_pixellation;
  }

  /**
   * Renders tiles straight through the raytracer, the way the
   * renderer's worker does; a null skip set renders every tile. Each
   * tile traces its whole area here (staged rectangle = whole tile),
   * like a render-all frame.
   */
  private void renderTiles(boolean[] skip) {
    final RayCamera camera = new RayCamera();
    final Primitive[] primitives = RayScene.build(this.manager);
    final BVH bvh = new BVH(primitives);
    final RTRing[] rings = RayScene.selectionRings(this.manager,
        camera.getEyeX(), camera.getEyeY(), camera.getEyeZ());
    for (int i = 0; i < this.tiles.length; i++) {
      if (skip != null && skip[i]) {
        continue;
      }
      final Tile tile = this.tiles[i];
      tile.rx0 = tile.x0;
      tile.ry0 = tile.y0;
      tile.rx1 = tile.x0 + tile.width - 1;
      tile.ry1 = tile.y0 + tile.height - 1;
      final int[] pixels = new int[tile.width * tile.height];
      final Raytracer.HitStats stats = new Raytracer.HitStats();
      Raytracer.renderTile(tile.x0, tile.y0, tile.width, tile.height,
          camera, bvh, rings, pixels, stats);
      final BufferedImage image = new BufferedImage(tile.width,
          tile.height, BufferedImage.TYPE_INT_RGB);
      image.setRGB(0, 0, tile.width, tile.height, pixels, 0, tile.width);
      tile.image = image;
      tile.stats = stats;
      tile.done = true;
    }
    ModularRendererRaytraced.publishFrame(this.tiles);
  }

  private static boolean[] skipFromRects(RectangleInt[] rects,
      boolean[] last_empty) {
    final boolean[] skip = new boolean[rects.length];
    for (int i = 0; i < skip.length; i++) {
      skip[i] = last_empty[i] && rects[i].isEmpty();
    }
    return skip;
  }

  private static boolean[] emptiness(RectangleInt[] rects) {
    final boolean[] empty = new boolean[rects.length];
    for (int i = 0; i < empty.length; i++) {
      empty[i] = rects[i].isEmpty();
    }
    return empty;
  }

  @Test
  public void partialFrameMatchesFullFrame() {
    // Frame 1: everything renders.
    renderTiles(null);
    final RectangleInt[] first =
        this.renderer.computeDirtyRects(this.manager);
    assertNotNull(first);
    final boolean[] last_empty = emptiness(first);
    final BufferedImage before =
        ModularRendererRaytraced.compositeFrame(this.tiles, SIZE, SIZE);

    // Move one node a little; most tiles stay empty and are skipped.
    final Node moved = (Node) this.manager.element.get(0);
    moved.pos.x += 5 * 192;
    moved.pos.y += 3 * 192;
    final RectangleInt[] rects =
        this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    final boolean[] skip = skipFromRects(rects, last_empty);
    int skipped = 0;
    for (int i = 0; i < skip.length; i++) {
      if (skip[i]) {
        skipped++;
      }
    }
    assertTrue(skipped > 0 && skipped < this.tiles.length,
        "a small move must skip some tiles but not all: " + skipped
            + " of " + this.tiles.length);

    // Frame 2, partial: re-trace only non-skipped tiles over frame 1.
    renderTiles(skip);
    final BufferedImage partial =
        ModularRendererRaytraced.compositeFrame(this.tiles, SIZE, SIZE);

    // Frame 2, full: re-trace every tile from scratch.
    renderTiles(null);
    final BufferedImage full =
        ModularRendererRaytraced.compositeFrame(this.tiles, SIZE, SIZE);

    assertEquals(SIZE, partial.getWidth());
    for (int y = 0; y < SIZE; y++) {
      for (int x = 0; x < SIZE; x++) {
        assertEquals(full.getRGB(x, y), partial.getRGB(x, y),
            "pixel (" + x + ", " + y + ") differs");
      }
    }

    // And the move must actually have changed the picture.
    boolean changed = false;
    for (int y = 0; y < SIZE && !changed; y++) {
      for (int x = 0; x < SIZE && !changed; x++) {
        changed = before.getRGB(x, y) != partial.getRGB(x, y);
      }
    }
    assertTrue(changed, "the moved node must change some pixel");
  }
}
