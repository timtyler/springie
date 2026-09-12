// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.springie.FrEnd;
import com.springie.elements.faces.Face;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.ScenicBackground;
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.RendererBinManager;

/**
 * A ray-traced renderer that lives alongside the default renderer.
 *
 * <p>It renders into the same bin tiles as the default renderer
 * (RendererBinManager.divisor blocks), one tile per worker thread.
 * Finished tiles are stored, not displayed: only when every tile of a
 * frame is done is the whole frame blitted to the screen at once, so
 * the user never sees a half-rendered frame.
 *
 * <p>The camera reproduces the default renderer's projection exactly (see
 * RayCamera), so the model appears at the same size and from the same
 * angle. Nodes are spheres, links are stretched spheres (ellipsoids), faces are opaque
 * triangle fans. Selection shows as a colour change only.
 *
 * <p>A frame renders a snapshot of the model: while a frame is in progress
 * repaints keep displaying the last complete frame, and a new frame
 * starts once the previous one completes and the scene (or view) changed.
 */
public class ModularRendererRaytraced implements ModularRendererBase {
  private static final ExecutorService POOL = Executors.newFixedThreadPool(
      Math.max(2, Runtime.getRuntime().availableProcessors()),
      new ThreadFactory() {
        public Thread newThread(Runnable job) {
          final Thread thread = new Thread(job, "raytraced-tile");
          thread.setDaemon(true);
          return thread;
        }
      });

  static final class Tile {
    final int x0, y0, width, height;

    // The tile currently being rendered (staging): replaced by the worker
    // when its tile finishes.
    volatile BufferedImage image;

    volatile boolean done;

    // Hit statistics for the staged image, written by the worker before
    // done is set. Read only by the thread that observes every tile done.
    Raytracer.HitStats stats;

    // The last complete frame: the only thing repaint() ever draws. A
    // single volatile write publishes the whole snapshot atomically.
    volatile ShownTile shown;

    Tile(int x0, int y0, int width, int height) {
      this.x0 = x0;
      this.y0 = y0;
      this.width = width;
      this.height = height;
    }
  }

  /**
   * One tile of a complete frame: its image plus, for the "show active
   * bins" overlay, whether any ray hit geometry and the content rectangle
   * (screen coordinates, inclusive) those hits covered.
   */
  static final class ShownTile {
    final BufferedImage image;

    final boolean active;

    final int min_x, min_y, max_x, max_y;

    ShownTile(BufferedImage image, boolean active, int min_x, int min_y,
        int max_x, int max_y) {
      this.image = image;
      this.active = active;
      this.min_x = min_x;
      this.min_y = min_y;
      this.max_x = max_x;
      this.max_y = max_y;
    }
  }

  private Tile[] tiles;

  private int canvas_width = -1;

  private int canvas_height = -1;

  private boolean last_show_bins;

  private volatile long frame_id;

  private volatile long frame_signature = -1L;

  private volatile boolean frame_done = true;

  // The composed frame: background plus every published tile, blitted to
  // the screen in one drawImage. Repaints never touch the screen with a
  // clear or a half-drawn tile set, so there is no black flash between
  // frames -- new tiles are painted over the old frame offscreen, and
  // the result appears whole.
  private BufferedImage frame_image;

  // Set by the worker that publishes a frame; the next repaint
  // re-composites frame_image and clears it.
  private volatile boolean frame_staged;

  public void resize(int x, int y) {
    this.tiles = null;
  }

  public void reset() {
    this.tiles = null;
    this.frame_done = true;
  }

  /**
   * Holds the model while a frame is rendering: every model state is
   * rendered exactly once, in order, and the animation runs at render
   * speed instead of skipping states to stay real-time. Released when
   * the renderer is no longer current, so a mid-frame renderer switch
   * cannot freeze the model.
   */
  @Override
  public boolean holdModelForFrame() {
    return this.frame_done == false
        && RendererDelegator.renderer == this;
  }

  public void repaint(Graphics graphics, NodeManager manager) {
    final int width = Coords.x_pixels;
    final int height = Coords.y_pixels;
    final boolean show_bins = RendererBinManager.show_bins;
    if (this.tiles == null || width != this.canvas_width
        || height != this.canvas_height || show_bins != this.last_show_bins) {
      buildTiles(width, height, show_bins);
    }

    final long signature = signature(manager);
    if (this.frame_done && signature != this.frame_signature) {
      startFrame(manager, signature);
    }

    // Blit the composed frame in a single drawImage: the screen never
    // sees the background clear or a partially drawn tile set, so there
    // is no flicker. New frames are painted over the old one offscreen.
    if (this.frame_image == null) {
      this.frame_image =
          new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      this.frame_staged = true;
    }
    if (this.frame_staged) {
      this.frame_image = compositeFrame(this.tiles, width, height);
      this.frame_staged = false;
    }
    graphics.drawImage(this.frame_image, 0, 0, null);

    final boolean show_active = RendererBinManager.show_active_bins;
    final Tile[] tiles = this.tiles;
    for (int i = 0; i < tiles.length; i++) {
      final ShownTile shown = tiles[i].shown;
      // "Show active bins": red outline around the content rectangle of
      // every tile holding geometry, drawn on the screen graphics after
      // the tile pixels (not baked into the tiles), so toggling the
      // option needs no re-render.
      if (show_active && shown != null && shown.active) {
        graphics.setColor(Color.RED);
        graphics.drawRect(shown.min_x, shown.min_y,
            shown.max_x - shown.min_x, shown.max_y - shown.min_y);
      }
    }
  }

  /**
   * Composes one whole frame offscreen: the background first (with "show
   * bins" the tiles are shrunk by a margin, so the gutters between them
   * show the background as black grid lines, exactly like the default
   * renderer), then every published tile painted over it. Tiles with no
   * published snapshot yet contribute nothing, so a frame in progress
   * never shows half-rendered tiles.
   */
  static BufferedImage compositeFrame(Tile[] tiles, int width, int height) {
    final BufferedImage frame =
        new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    final Graphics g = frame.getGraphics();
    try {
      if (RendererDelegator.scenic_background) {
        g.drawImage(ScenicBackground.imageFor(width, height), 0, 0, null);
      } else {
        g.setColor(RendererDelegator.color_background);
        g.fillRect(0, 0, width, height);
      }
      for (int i = 0; i < tiles.length; i++) {
        final ShownTile shown = tiles[i].shown;
        if (shown == null) {
          continue;
        }
        g.drawImage(shown.image, tiles[i].x0, tiles[i].y0, null);
      }
    } finally {
      g.dispose();
    }
    return frame;
  }

  /**
   * Builds the tile grid: divisor-sized blocks covering the canvas, the
   * same bins the default renderer uses. With "show bins" each tile is
   * shrunk by the same margin the default renderer leaves, so the
   * background shows through as black grid lines between the tiles.
   */
  static Tile[] buildTileGrid(int width, int height) {
    final int divisor = RendererBinManager.divisor;
    // Same margin as the default renderer's getMargin().
    final int margin = RendererBinManager.show_bins ? 4 : 0;
    final int block = divisor - margin;
    final int nx = width / divisor + 1;
    final int ny = height / divisor + 1;
    final Tile[] tiles = new Tile[nx * ny];
    int i = 0;
    for (int ty = 0; ty < ny; ty++) {
      for (int tx = 0; tx < nx; tx++) {
        final int x0 = tx * divisor;
        final int y0 = ty * divisor;
        final int w = Math.min(block, width - x0);
        final int h = Math.min(block, height - y0);
        if (w > 0 && h > 0) {
          tiles[i++] = new Tile(x0, y0, w, h);
        }
      }
    }
    // An exact multiple of the divisor leaves a degenerate zero-area bin;
    // it covers no pixels, so it is dropped.
    final Tile[] result = new Tile[i];
    System.arraycopy(tiles, 0, result, 0, i);
    return result;
  }

  private void buildTiles(int width, int height, boolean show_bins) {
    this.tiles = buildTileGrid(width, height);
    this.canvas_width = width;
    this.canvas_height = height;
    this.last_show_bins = show_bins;
    this.frame_done = true;
    this.frame_signature = -1L;
    this.frame_image = null;
    this.frame_staged = false;
  }

  private void startFrame(NodeManager manager, long signature) {
    final long id = ++this.frame_id;
    this.frame_done = false;
    this.frame_signature = signature;

    final RayCamera camera = new RayCamera();
    final Primitive[] primitives = RayScene.build(manager);
    final BVH bvh = new BVH(primitives);

    final Tile[] tiles = this.tiles;
    for (int i = 0; i < tiles.length; i++) {
      final Tile tile = tiles[i];
      tile.done = false;
      POOL.execute(new Runnable() {
        public void run() {
          renderTile(id, tiles, tile, camera, bvh);
        }
      });
    }
  }

  private void renderTile(long id, Tile[] tiles, Tile tile, RayCamera camera,
      BVH bvh) {
    if (RendererDelegator.renderer != this) {
      // The user switched to another renderer mid-frame: drop the tile
      // instead of burning CPU on an image nobody will display. Best
      // effort (the field is not volatile); a stale read just means the
      // tile renders as it does today.
      return;
    }
    final int[] pixels = new int[tile.width * tile.height];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(tile.x0, tile.y0, tile.width, tile.height, camera,
        bvh, pixels, stats);
    if (id != this.frame_id) {
      // Superseded by a newer frame; drop the work.
      return;
    }
    final BufferedImage image = new BufferedImage(tile.width, tile.height,
        BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, tile.width, tile.height, pixels, 0, tile.width);
    tile.image = image;
    tile.stats = stats;
    tile.done = true;

    boolean all_done = true;
    for (int i = 0; i < tiles.length; i++) {
      if (!tiles[i].done) {
        all_done = false;
        break;
      }
    }
    if (!all_done) {
      // Not the last tile: store the work, but display nothing yet. The
      // frame is blitted all at once when every tile is done.
      return;
    }

    // Last tile of the frame: publish every tile's snapshot at once, so
    // repaints never show a half-rendered frame.
    publishFrame(tiles);
    this.frame_done = true;
    // The next repaint re-composites the offscreen frame and blits it
    // whole; the screen never shows the frame being assembled.
    this.frame_staged = true;

    // Ask the main loop for another pass so the finished frame displays,
    // even when the model is not animating.
    RendererDelegator.repaint_some_objects = true;
    if (FrEnd.main_canvas != null && FrEnd.main_canvas.panel != null) {
      FrEnd.main_canvas.panel.repaint();
    }
  }

  /**
   * Publishes every tile's finished snapshot at once. Repaints only ever
   * draw the published snapshots, so the frame appears on screen whole --
   * never tile by tile. The caller must have observed every tile done;
   * each worker wrote its tile's image and stats before its (volatile)
   * done flag, so they are visible here.
   */
  static void publishFrame(Tile[] tiles) {
    for (int i = 0; i < tiles.length; i++) {
      final Tile t = tiles[i];
      final Raytracer.HitStats s = t.stats;
      final boolean active = s != null && s.hits > 0;
      t.shown = new ShownTile(t.image, active,
          active ? t.x0 + s.min_x : 0, active ? t.y0 + s.min_y : 0,
          active ? t.x0 + s.max_x : 0, active ? t.y0 + s.max_y : 0);
    }
    // One completed frame, however many AWT paints it takes to display.
    RendererDelegator.countRenderedFrame();
  }

  /**
   * Cheap change detector: view parameters, render flags and a hash over
   * the elements. Decides when a new frame must start.
   */
  private static long signature(NodeManager manager) {
    long sig = 17L;
    sig = sig * 31 + Coords.x_pixels;
    sig = sig * 31 + Coords.y_pixels;
    sig = sig * 31 + Coords.shift_constant_x;
    sig = sig * 31 + Coords.shift_constant_y;
    sig = sig * 31 + Coords.shift_constant_z;
    sig = sig * 31 + RendererBinManager.divisor;
    // The tile geometry (and the background gutters) follow show_bins;
    // show_active_bins needs no new frame, its outlines are drawn over
    // the finished frame on the screen graphics.
    sig = sig * 31 + (RendererBinManager.show_bins ? 1 : 0);
    sig = sig * 31 + (FrEnd.render_nodes ? 1 : 0);
    sig = sig * 31 + (FrEnd.render_links ? 1 : 0);
    sig = sig * 31 + (FrEnd.render_faces ? 1 : 0);
    sig = sig * 31 + RendererDelegator.glossiness;
    sig = sig * 31 + (RendererDelegator.shadows ? 1 : 0);
    sig = sig * 31 + RendererDelegator.specular;
    sig = sig * 31 + RendererDelegator.fresnel;
    sig = sig * 31 + RendererDelegator.fill_light;
    sig = sig * 31 + RendererDelegator.antialiasing;
    sig = sig * 31 + RendererDelegator.generation;

    final List<?> nodes = manager.element;
    final int node_count = nodes.size();
    sig = sig * 31 + node_count;
    for (int i = 0; i < node_count; i++) {
      final Node node = (Node) nodes.get(i);
      sig = sig * 31 + node.pos.x;
      sig = sig * 31 + node.pos.y;
      sig = sig * 31 + node.pos.z;
      sig = sig * 31 + (node.type.selected ? 1 : 0);
      sig = sig * 31 + node.clazz.colour;
    }

    final LinkManager link_manager = manager.getLinkManager();
    final List<?> links = link_manager.element;
    final int link_count = links.size();
    sig = sig * 31 + link_count;
    for (int i = 0; i < link_count; i++) {
      final Link link = (Link) links.get(i);
      sig = sig * 31 + (link.type.selected ? 1 : 0);
      sig = sig * 31 + link.clazz.colour;
    }

    final List<?> faces = manager.getFaceManager().element;
    final int face_count = faces.size();
    sig = sig * 31 + face_count;
    for (int i = 0; i < face_count; i++) {
      final Face face = (Face) faces.get(i);
      sig = sig * 31 + (face.type.selected ? 1 : 0);
      sig = sig * 31 + face.clazz.colour;
    }

    return sig;
  }
}
