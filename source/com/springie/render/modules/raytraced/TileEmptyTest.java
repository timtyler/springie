// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;

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
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.RendererBinManager;
import com.springie.render.modules.raytraced.ModularRendererRaytraced.Tile;

/**
 * The ray-traced renderer skips re-tracing tiles that were empty and are
 * still empty. These tests pin down computeEmptyTiles: which tiles a
 * scene's geometry touches.
 */
public class TileEmptyTest {
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

    this.saved_divisor = RendererBinManager.divisor;
    this.saved_show_bins = RendererBinManager.show_bins;
    RendererBinManager.divisor = DIVISOR;
    RendererBinManager.show_bins = false;

    this.saved_render_nodes = FrEnd.render_nodes;
    this.saved_render_links = FrEnd.render_links;
    FrEnd.render_nodes = true;
    FrEnd.render_links = true;

    this.manager = new NodeManager();
    this.renderer = new ModularRendererRaytraced();
    this.renderer.buildTiles(SIZE, SIZE, false);
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
  }

  private static int tileContaining(Tile[] grid, int sx, int sy) {
    for (int i = 0; i < grid.length; i++) {
      final Tile t = grid[i];
      if (sx >= t.x0 && sx < t.x0 + t.width
          && sy >= t.y0 && sy < t.y0 + t.height) {
        return i;
      }
    }
    return -1;
  }

  @Test
  public void emptySceneMarksEveryTileEmpty() {
    final boolean[] empty = this.renderer.computeEmptyTiles(this.manager);
    assertNotNull(empty);
    assertEquals(16, empty.length);
    for (int i = 0; i < empty.length; i++) {
      assertTrue(empty[i], "tile " + i + " should be empty");
    }
  }

  @Test
  public void nodeMarksOnlyItsTiles() {
    this.manager.element.add(nodeAt(100, 100));
    final boolean[] empty = this.renderer.computeEmptyTiles(this.manager);
    assertNotNull(empty);
    final Tile[] grid = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
    final int hit = tileContaining(grid, 100, 100);
    assertTrue(hit >= 0);
    assertFalse(empty[hit], "the node's own tile must be non-empty");
    final int far = tileContaining(grid, 350, 350);
    assertTrue(far >= 0);
    assertTrue(empty[far], "a far tile must stay empty");
  }

  @Test
  public void movedNodeFreesItsOldTiles() {
    final Node node = nodeAt(100, 100);
    this.manager.element.add(node);
    final Tile[] grid = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
    final int old_tile = tileContaining(grid, 100, 100);

    node.pos.x = internal(300);
    node.pos.y = internal(300);
    final boolean[] empty = this.renderer.computeEmptyTiles(this.manager);
    assertNotNull(empty);
    assertTrue(empty[old_tile],
        "the node's old tile must be empty again so it can be skipped");
    final int new_tile = tileContaining(grid, 300, 300);
    assertFalse(empty[new_tile],
        "the node's new tile must be non-empty");
  }

  @Test
  public void hiddenLinkMarksNothing() {
    final Node a = nodeAt(100, 100);
    final Node b = nodeAt(150, 120);
    this.manager.element.add(a);
    this.manager.element.add(b);
    FrEnd.render_nodes = false;
    final Link link = new Link(a, b,
        new LinkTypeFactory().getNew(100 << Coords.shift, 50),
        new Clazz(0x654321));
    link.type.hidden = true;
    this.manager.getLinkManager().element.add(link);
    final boolean[] empty = this.renderer.computeEmptyTiles(this.manager);
    assertNotNull(empty);
    for (int i = 0; i < empty.length; i++) {
      assertTrue(empty[i], "tile " + i + " should be empty");
    }
  }

  @Test
  public void degenerateProjectionMarksNothingDecided() {
    // Behind the camera: the projection is meaningless, so the caller
    // must re-trace everything (null).
    final Node node = nodeAt(100, 100);
    node.pos.z = -200000;
    this.manager.element.add(node);
    assertNull(this.renderer.computeEmptyTiles(this.manager));
  }

  @Test
  public void markTilesNotEmptyPacksRowsTightly() {
    // A canvas exactly two divisors wide still has only two tiles per
    // row: the degenerate zero-area column is dropped from the grid.
    final boolean[] empty = new boolean[4];
    for (int i = 0; i < empty.length; i++) {
      empty[i] = true;
    }
    ModularRendererRaytraced.markTilesNotEmpty(empty, 2, 100, 200, 200,
        150, 150, 160, 160);
    assertFalse(empty[3]);
    assertTrue(empty[0] && empty[1] && empty[2]);
  }
}
