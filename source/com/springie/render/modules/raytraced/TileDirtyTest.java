// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.elements.nodes.NodeTypeFactory;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.RendererBinManager;
import com.springie.render.modules.raytraced.ModularRendererRaytraced.Tile;
import com.springie.geometry.Point3D;

/**
 * The ray-traced renderer must re-render only the tiles whose content
 * changed -- not the whole canvas every frame. Each tile's signature
 * mixes the global visual state with just the elements projecting into
 * that tile.
 */
public class TileDirtyTest {
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

  private boolean saved_shadows;

  private int saved_background;

  private NodeManager manager;

  private ModularRendererRaytraced renderer;

  // Internal x for screen pixel sx: inverts getXCoords with the pinned
  // Coords above (shift_constant_x = 0, x_pixelso2 = 200, wpp = 192).
  private static int internalX(int sx) {
    return (200 << Coords.shift) + (sx - 200) * 192;
  }

  private static Node node(int sx, int sy) {
    final int s = internalX(sx);
    final int t = internalX(sy);
    final Node node = new Node(new Point3D(s, t, 0), 42,
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
    this.saved_shadows = RendererDelegator.shadows;
    this.saved_background = RendererDelegator.color_background_number;
    FrEnd.render_nodes = true;
    RendererDelegator.shadows = false;

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
    RendererDelegator.shadows = this.saved_shadows;
    RendererDelegator.color_background_number = this.saved_background;
  }

  // 16 tiles; index = ty * 4 + tx.
  private static int tileIndex(int tx, int ty) {
    return ty * 4 + tx;
  }

  private void prime() {
    final boolean[] dirty = this.renderer.findDirtyTiles(this.manager);
    assertNotNull(dirty, "first frame renders everything");
    for (int i = 0; i < dirty.length; i++) {
      assertTrue(dirty[i], "first frame renders everything");
    }
  }

  @Test
  public void staticSceneDirtiesNoTiles() {
    this.manager.element.add(node(50, 50));
    this.manager.element.add(node(350, 350));
    prime();
    assertNull(this.renderer.findDirtyTiles(this.manager),
        "nothing moved: no tile may re-render");
  }

  @Test
  public void movedNodeDirtiesOnlyItsOwnTiles() {
    final Node wanderer = node(50, 50);
    final Node settler = node(350, 350);
    this.manager.element.add(wanderer);
    this.manager.element.add(settler);
    prime();

    // Hop from tile (0, 0) to tile (1, 0): the far tile (3, 3) holding
    // the settler must stay clean.
    wanderer.pos.x = internalX(150);
    final boolean[] dirty =
        this.renderer.findDirtyTiles(this.manager);
    assertNotNull(dirty);
    assertTrue(dirty[tileIndex(0, 0)], "tile the node left");
    assertTrue(dirty[tileIndex(1, 0)], "tile the node entered");
    assertTrue(!dirty[tileIndex(3, 3)], "far tile stays clean");
    int count = 0;
    for (int i = 0; i < dirty.length; i++) {
      if (dirty[i]) {
        count++;
      }
    }
    assertTrue(count < 16, "only nearby tiles re-render, not the canvas");
  }

  @Test
  public void shadowsOnDirtiesEveryTileOnAnyChange() {
    RendererDelegator.shadows = true;
    this.manager.element.add(node(50, 50));
    prime();
    RendererDelegator.shadows = true;

    // No change: still nothing to do, even with shadows on.
    assertNull(this.renderer.findDirtyTiles(this.manager));

    // Any move: the shadow can fall anywhere, so every tile is dirty.
    ((Node) this.manager.element.get(0)).pos.x = internalX(150);
    final boolean[] dirty = this.renderer.findDirtyTiles(this.manager);
    assertNotNull(dirty);
    for (int i = 0; i < dirty.length; i++) {
      assertTrue(dirty[i], "shadows couple every tile");
    }
  }

  @Test
  public void backgroundRecolourDirtiesEveryTile() {
    this.manager.element.add(node(50, 50));
    prime();
    RendererDelegator.color_background_number =
        this.saved_background ^ 0xFFFFFF;
    final boolean[] dirty = this.renderer.findDirtyTiles(this.manager);
    assertNotNull(dirty);
    for (int i = 0; i < dirty.length; i++) {
      assertTrue(dirty[i], "background covers every tile");
    }
  }

  @Test
  public void selectionChangeDirtiesTheNodesTiles() {
    final Node node = node(50, 50);
    this.manager.element.add(node);
    prime();
    node.type.selected = true;
    final boolean[] dirty = this.renderer.findDirtyTiles(this.manager);
    assertNotNull(dirty);
    assertTrue(dirty[tileIndex(0, 0)], "selection ring paints the tile");
    assertTrue(!dirty[tileIndex(3, 3)], "far tile stays clean");
  }

  @Test
  public void mixIntoTilesPacksRowsTightly() {
    // Width an exact multiple of the divisor: the degenerate trailing
    // column is dropped, so rows pack 2 wide, not 3.
    final int width = 200;
    final int divisor = 100;
    final Tile[] tiles =
        ModularRendererRaytraced.buildTileGrid(width, 200);
    assertEquals(4, tiles.length);
    final long[] sigs = new long[tiles.length];
    // Rectangle in the second column, second row: tile index 3.
    ModularRendererRaytraced.mixIntoTiles(sigs, 2, divisor, width, 200,
        150, 150, 199, 199, 12345L);
    for (int i = 0; i < sigs.length; i++) {
      assertEquals(i == 3 ? 12345L : 0L, sigs[i], "tile " + i);
    }
  }
}
