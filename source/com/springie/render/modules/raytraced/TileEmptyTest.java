// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkTypeFactory;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.geometry.Point3D;
import com.springie.gui.gestures.PerformActions;
import com.springie.render.Coords;
import com.springie.render.RectangleInt;
import com.springie.render.RendererDragBox;
import com.springie.render.modules.modern.RendererBinManager;
import com.springie.render.modules.raytraced.ModularRendererRaytraced.Tile;

/**
 * The ray-traced renderer re-traces only each tile's dirty rectangle --
 * the union of the geometry's screen boxes clipped to the tile -- and
 * skips tiles that were empty and are still empty. These tests pin down
 * computeDirtyRects: which tiles a scene's geometry touches, and how
 * tight each tile's rectangle is.
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
    final RectangleInt[] rects = this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    assertEquals(16, rects.length);
    for (int i = 0; i < rects.length; i++) {
      assertTrue(rects[i].isEmpty(), "tile " + i + " should be empty");
    }
  }

  @Test
  public void nodeMarksOnlyItsTiles() {
    this.manager.element.add(nodeAt(100, 100));
    final RectangleInt[] rects = this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    final Tile[] grid = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
    final int hit = tileContaining(grid, 100, 100);
    assertTrue(hit >= 0);
    assertFalse(rects[hit].isEmpty(),
        "the node's own tile must be non-empty");
    final int far = tileContaining(grid, 350, 350);
    assertTrue(far >= 0);
    assertTrue(rects[far].isEmpty(), "a far tile must stay empty");
  }

  @Test
  public void nodeDirtyRectIsTight() {
    // Radius 512 at 192 world units per pixel: ceil(512/192) + 2 = 5
    // pixels each way, so the node's box is (95, 95)..(105, 105), and
    // the tile at (100, 100)..(199, 199) must see only its clipped
    // share -- not the whole tile.
    this.manager.element.add(nodeAt(100, 100));
    final RectangleInt[] rects = this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    final Tile[] grid = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
    final int hit = tileContaining(grid, 100, 100);
    assertTrue(hit >= 0);
    final RectangleInt rect = rects[hit];
    assertEquals(100, rect.min_x);
    assertEquals(100, rect.min_y);
    assertEquals(105, rect.max_x);
    assertEquals(105, rect.max_y);
  }

  @Test
  public void movedNodeFreesItsOldTiles() {
    final Node node = nodeAt(100, 100);
    this.manager.element.add(node);
    final Tile[] grid = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
    final int old_tile = tileContaining(grid, 100, 100);

    node.pos.x = internal(300);
    node.pos.y = internal(300);
    final RectangleInt[] rects = this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    assertTrue(rects[old_tile].isEmpty(),
        "the node's old tile must be empty again so it can be skipped");
    final int new_tile = tileContaining(grid, 300, 300);
    assertFalse(rects[new_tile].isEmpty(),
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
    final RectangleInt[] rects = this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    for (int i = 0; i < rects.length; i++) {
      assertTrue(rects[i].isEmpty(), "tile " + i + " should be empty");
    }
  }

  @Test
  public void degenerateProjectionMarksNothingDecided() {
    // Behind the camera: the projection is meaningless, so the caller
    // must re-trace everything (null).
    final Node node = nodeAt(100, 100);
    node.pos.z = -200000;
    this.manager.element.add(node);
    assertNull(this.renderer.computeDirtyRects(this.manager));
  }

  @Test
  public void markTilesDirtyPacksRowsTightly() {
    // A canvas exactly two divisors wide still has only two tiles per
    // row: the degenerate zero-area column is dropped from the grid.
    final Tile[] grid = ModularRendererRaytraced.buildTileGrid(200, 200);
    final RectangleInt[] rects = new RectangleInt[grid.length];
    for (int i = 0; i < rects.length; i++) {
      rects[i] = new RectangleInt(Integer.MAX_VALUE, Integer.MAX_VALUE,
          Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
    ModularRendererRaytraced.markTilesDirty(rects, grid, 2, 100, 200, 200,
        150, 150, 160, 160);
    assertEquals(150, rects[3].min_x);
    assertEquals(150, rects[3].min_y);
    assertEquals(160, rects[3].max_x);
    assertEquals(160, rects[3].max_y);
    assertTrue(rects[0].isEmpty() && rects[1].isEmpty()
        && rects[2].isEmpty());
  }

  @Test
  public void diagonalLinkMarksOnlyItsDiagonal() {
    final Node a = nodeAt(50, 50);
    final Node b = nodeAt(350, 350);
    this.manager.element.add(a);
    this.manager.element.add(b);
    FrEnd.render_nodes = false;
    final Link link = new Link(a, b,
        new LinkTypeFactory().getNew(100 << Coords.shift, 50),
        new Clazz(0x654321));
    link.type.radius = 192;
    this.manager.getLinkManager().element.add(link);
    final RectangleInt[] rects = this.renderer.computeDirtyRects(this.manager);
    assertNotNull(rects);
    final Tile[] grid = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
    final int on_diagonal = tileContaining(grid, 200, 200);
    assertFalse(rects[on_diagonal].isEmpty(),
        "a tile on the link's diagonal must be non-empty");
    // The old whole-span AABB marked the full 300x300 square: every
    // tile. The tight per-piece marking must leave the off-diagonal
    // corners of that square alone.
    final int off_diagonal = tileContaining(grid, 350, 50);
    assertTrue(rects[off_diagonal].isEmpty(),
        "a tile inside the span's bounding square but off the link must"
            + " stay empty");
    int marked = 0;
    for (int i = 0; i < rects.length; i++) {
      if (!rects[i].isEmpty()) {
        marked++;
      }
    }
    assertTrue(marked <= 10,
        "a thin diagonal should touch far fewer than all 16 tiles, got "
            + marked);
    // (10 is the true minimum here: the diagonal runs exactly through
    // three tile corners, and the link's few-pixel capsule genuinely
    // touches the two extra tiles meeting at each corner.)
  }

  @Test
  public void dragBoxDamageMarksItsTiles() {
    // An empty scene with an active drag-box selection: the box draws
    // directly on the screen after the blit, so its rectangle must join
    // the dirty region -- otherwise the old rectangle's pixels are never
    // repainted and the red box leaves a trail.
    final NodeManager old_manager = ContextManager.getNodeManager();
    final PerformActions old_actions = FrEnd.perform_actions;
    try {
      ContextManager.setNodeManager(this.manager);
      final PerformActions actions = new PerformActions();
      FrEnd.perform_actions = actions;
      actions.drag_box_manager.drag_box_end = new Point(0, 0);
      // A drawn box: cached coordinates, in internal units (shift 8).
      final RendererDragBox box =
          this.manager.renderer.renderer_drag_box;
      box.min = new Point(50 << 8, 50 << 8);
      box.max = new Point(150 << 8, 150 << 8);
      box.last_min = new Point(50 << 8, 50 << 8);
      box.last_max = new Point(150 << 8, 150 << 8);
      box.cache_valid = true;
      final RectangleInt[] rects =
          this.renderer.computeDirtyRects(this.manager);
      assertNotNull(rects);
      final Tile[] grid = ModularRendererRaytraced.buildTileGrid(SIZE, SIZE);
      final int hit = tileContaining(grid, 100, 100);
      assertTrue(hit >= 0);
      assertFalse(rects[hit].isEmpty(),
          "the drag box's tile must be dirty");
      final int far = tileContaining(grid, 350, 350);
      assertTrue(far >= 0);
      assertTrue(rects[far].isEmpty(), "a far tile must stay empty");
    } finally {
      ContextManager.setNodeManager(old_manager);
      FrEnd.perform_actions = old_actions;
    }
  }
}
