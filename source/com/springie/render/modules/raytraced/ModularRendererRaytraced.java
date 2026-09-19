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
import com.springie.context.ContextManager;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.gestures.DragBoxManager;
import com.springie.render.Coords;
import com.springie.render.RectangleInt;
import com.springie.render.RendererDelegator;
import com.springie.render.RendererDragBox;
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
 * starts once the previous one completes. Each tile is re-traced only
 * where it needs to be: before a frame starts the renderer walks the
 * model's nodes, links, and faces, projecting each element's screen
 * rectangle and unioning it into every tile it touches, producing one
 * dirty rectangle per tile (empty when the tile holds no geometry).
 * This is the same RectangleInt algebra the polygon renderer uses for
 * its bins (see RendererBin): the worker re-traces only the dirty
 * rectangle -- a sub-rectangle of the tile -- unioned with the tile's
 * dirty rectangle from the previous frame (the whole tile for one
 * frame after a global effect like shadows or a background recolour),
 * so pixels where geometry used to be are repainted too. A tile that
 * was empty last frame and is still empty is not re-traced at all: its
 * pixels are background, which cannot have changed, so a small model
 * on a big canvas fires no rays into the blank tiles, and no rays
 * into the blank areas of the tiles it does touch. Within a traced
 * rectangle every pixel still gets at least one ray -- there is no way
 * to know a pixel is blank without tracing it -- but a miss only
 * costs a handful of bounding-box tests against the BVH, never a
 * shading calculation. With shadows or the scenic background on, or
 * when the background colour changed, every tile re-renders in full.
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

    // The staged image's rectangle (screen coordinates, inclusive): the
    // worker re-traces the tile's dirty rectangle unioned with the
    // tile's dirty rectangle from the previous frame (see last_dirty),
    // so the image covers (rx0, ry0)..(rx1, ry1), a sub-rectangle of
    // the tile -- the whole tile on a render-all frame. Empty
    // (rx0 > rx1) until the tile's first frame is started. Written by
    // the thread starting the frame before any task is submitted --
    // hence visible to the workers -- and read by publishFrame after
    // observing done, like image and stats.
    int rx0, ry0, rx1, ry1;

    // The true dirty rectangle of the last frame this tile was staged
    // for -- or the whole tile when that frame was forced whole by a
    // global effect (shadows, scenic background, background recolour)
    // or a degenerate walk, which make every pixel suspect. The next
    // frame re-traces the union of its dirty rectangle and this one, so
    // pixels where geometry used to be are repainted too -- the same
    // idea as the polygon renderer's bin.union. Empty until the tile's
    // first staged frame; untouched while the tile is skipped.
    final RectangleInt last_dirty =
        new RectangleInt(Integer.MAX_VALUE, Integer.MAX_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE);

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
      this.rx0 = Integer.MAX_VALUE;
      this.ry0 = Integer.MAX_VALUE;
      this.rx1 = Integer.MIN_VALUE;
      this.ry1 = Integer.MIN_VALUE;
    }
  }

  /**
   * One tile of a complete frame: its image plus, for the "show active
   * bins" overlay, whether any ray hit geometry and the content rectangle
   * (screen coordinates, inclusive) those hits covered.
   */
  static final class ShownTile {
    final BufferedImage image;

    // Screen coordinates of the image's top-left corner: the dirty
    // rectangle the worker re-traced, a sub-rectangle of the tile.
    final int rx0, ry0;

    final boolean active;

    final int min_x, min_y, max_x, max_y;

    ShownTile(BufferedImage image, int rx0, int ry0, boolean active,
        int min_x, int min_y, int max_x, int max_y) {
      this.image = image;
      this.rx0 = rx0;
      this.ry0 = ry0;
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

  // Per-tile emptiness from the last started frame: true = the tile held
  // no geometry. A tile that was empty and is still empty is not
  // re-traced. Null until the first frame is started.
  private boolean[] tile_empty;

  // Background colour the last frame was rendered with: recolouring the
  // background changes even empty tiles' pixels, so it forces every tile
  // to re-trace.
  private int last_background_rgb;

  // Effective tile columns per row (degenerate zero-area bins dropped),
  // for mapping a screen rectangle to tile indexes.
  private int tile_nx;

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

  // The skip set of the frame currently being composited: null when that
  // frame re-traced every tile, otherwise true = the tile was skipped.
  // Written when a frame is started, read when it is composited, so the
  // composite paints only the tiles the frame actually re-traced.
  private volatile boolean[] staged_skip;

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

    // Compose the staged frame over the persistent frame image, then
    // blit only what changed to the screen. A frame that re-traced
    // every tile gets a fresh full-canvas composite and a full blit; a
    // partial frame paints its re-traced tiles' snapshots and blits just
    // those rectangles; when nothing was re-traced nothing is blitted at
    // all -- the screen already shows this frame, so screen-space
    // overlays (like the boundary-box dots) survive there, exactly like
    // the polygon renderer's dirty bins. The composite runs before the
    // next frame starts, so the staged skip set still describes the
    // frame being composited.
    if (this.frame_image == null) {
      this.frame_image =
          new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      this.frame_staged = true;
    }
    if (this.frame_staged) {
      if (this.staged_skip == null) {
        this.frame_image = compositeFrame(this.tiles, width, height);
        graphics.drawImage(this.frame_image, 0, 0, null);
      } else {
        final Graphics g2 = this.frame_image.getGraphics();
        try {
          final Tile[] ctiles = this.tiles;
          for (int i = 0; i < ctiles.length; i++) {
            if (!this.staged_skip[i]) {
              final ShownTile shown = ctiles[i].shown;
              if (shown != null) {
                // Paint and blit only the rectangle the tile re-traced:
                // a sub-rectangle of the tile.
                g2.drawImage(shown.image, shown.rx0, shown.ry0, null);
                final int rx1 = shown.rx0 + shown.image.getWidth();
                final int ry1 = shown.ry0 + shown.image.getHeight();
                graphics.drawImage(this.frame_image, shown.rx0, shown.ry0,
                    rx1, ry1, shown.rx0, shown.ry0, rx1, ry1, null);
              }
            }
          }
        } finally {
          g2.dispose();
        }
      }
      this.frame_staged = false;
    }

    if (this.frame_done) {
      // Each tile's dirty rectangle: the union of every element's
      // screen box clipped to the tile, the same RectangleInt algebra
      // the polygon renderer uses for its bins. A tile that was empty
      // last frame and is still empty keeps its published snapshot: its
      // pixels are background, which cannot have changed. Shadows can
      // fall into tiles no element touches, the scenic background pans
      // with the view, and a recolour changes every background pixel,
      // so any of those re-traces the whole canvas. Emptiness is
      // derived from the true dirty rectangles, before any expansion.
      final RectangleInt[] dirty = computeDirtyRects(manager);
      final int background_rgb =
          0xFF000000 | RendererDelegator.color_background_number;
      final boolean background_changed =
          background_rgb != this.last_background_rgb;
      this.last_background_rgb = background_rgb;
      final boolean[] last_empty = this.tile_empty;
      // A global effect makes every pixel suspect, so the frame covers
      // whole tiles; the per-tile memory of what was dirty then holds
      // the whole tile for exactly one frame, and tracing converges
      // back to the tight rectangles on the frame after.
      final boolean global = RendererDelegator.shadows
          || RendererDelegator.scenic_background || background_changed;
      final boolean render_all =
          dirty == null || last_empty == null || global;
      final Tile[] tiles = this.tiles;
      final boolean[] skip;
      final boolean[] now_empty;
      if (dirty == null) {
        now_empty = null;
      } else {
        now_empty = new boolean[dirty.length];
        for (int i = 0; i < dirty.length; i++) {
          now_empty[i] = dirty[i].isEmpty();
        }
      }
      if (render_all) {
        skip = null;
      } else {
        skip = new boolean[dirty.length];
        for (int i = 0; i < skip.length; i++) {
          skip[i] = last_empty[i] && now_empty[i];
        }
      }
      // Remember which tiles this frame re-traces so the composite
      // paints only those snapshots over the persistent frame image.
      this.staged_skip = skip;
      startFrame(manager, skip, dirty, global);
      if (now_empty != null) {
        this.tile_empty = now_empty;
      }
    }

    // The staged frame was blitted (wholly or by tile rectangle) in the
    // composite block above; when nothing was staged the screen already
    // shows this frame, so nothing is painted here.

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
        // Pan-aware, exactly like the tile miss samples, so the base
        // matches the tiles at every boundary and while a frame is
        // still rendering.
        final BufferedImage scenic =
            ScenicBackground.imageFor(width, height);
        final int horizon = height / 2;
        for (int y = 0; y < height; y++) {
          final boolean sky = y < horizon;
          for (int x = 0; x < width; x++) {
            frame.setRGB(x, y,
                ScenicBackground.sampleWithPan(scenic, x, y, sky));
          }
        }
      } else {
        g.setColor(RendererDelegator.color_background);
        g.fillRect(0, 0, width, height);
      }
      for (int i = 0; i < tiles.length; i++) {
        final ShownTile shown = tiles[i].shown;
        if (shown == null) {
          continue;
        }
        g.drawImage(shown.image, shown.rx0, shown.ry0, null);
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

  void buildTiles(int width, int height, boolean show_bins) {
    this.tiles = buildTileGrid(width, height);
    this.canvas_width = width;
    this.canvas_height = height;
    this.last_show_bins = show_bins;
    this.frame_done = true;
    this.tile_empty = null;
    this.staged_skip = null;
    final int divisor = RendererBinManager.divisor;
    this.tile_nx = (width + divisor - 1) / divisor;
    this.frame_image = null;
    this.frame_staged = false;
  }

  /**
   * Starts a frame, re-tracing every tile except the ones marked skip.
   * A skipped tile was empty and is still empty: it keeps the snapshot
   * it published last frame (and its done flag, still true from the
   * frame that rendered it), so the "every tile done" check below only
   * waits on re-traced tiles. Each re-traced tile traces only its dirty
   * rectangle, unioned with the rectangle it showed last frame -- like
   * the polygon renderer's bin.union, this repaints pixels where
   * geometry used to be as well as where it is now. On a render-all
   * frame (global true, a degenerate walk with dirty null, or the
   * first frame) every tile traces its whole area instead. The last
   * tile to finish publishes the frame, mixing fresh and retained
   * snapshots, exactly like a full frame.
   */
  private void startFrame(NodeManager manager, boolean[] skip,
      RectangleInt[] dirty, boolean global) {
    final long id = ++this.frame_id;
    this.frame_done = false;

    final RayCamera camera = new RayCamera();
    final Primitive[] primitives = RayScene.build(manager);
    final BVH bvh = new BVH(primitives);
    final RTRing[] rings = RayScene.selectionRings(manager, camera.getEyeX(),
        camera.getEyeY(), camera.getEyeZ());

    final Tile[] tiles = this.tiles;
    // Clear every tile's done flag before submitting any task: a fast
    // task's "all done" scan must never observe a stale true for a tile
    // this loop has not reached yet, or it would publish the frame
    // prematurely and let the next repaint start a new frame while this
    // one's tasks are still queued (their id check would then drop them).
    // The staged rectangle is written here too, before the tasks that
    // read it are submitted.
    //
    // Whole tiles are traced when every pixel is suspect -- a global
    // effect, a degenerate walk -- and on the first frame, where an
    // empty dirty rectangle would otherwise stage a zero-area image.
    // (tile_empty is still the previous frame's here: it is updated
    // after startFrame returns.)
    final boolean force_whole =
        dirty == null || global || this.tile_empty == null;
    int submitted = 0;
    for (int i = 0; i < tiles.length; i++) {
      if (skip == null || !skip[i]) {
        final Tile tile = tiles[i];
        final RectangleInt last = tile.last_dirty;
        if (force_whole) {
          tile.rx0 = tile.x0;
          tile.ry0 = tile.y0;
          tile.rx1 = tile.x0 + tile.width - 1;
          tile.ry1 = tile.y0 + tile.height - 1;
        } else {
          final RectangleInt rect = dirty[i];
          tile.rx0 = Math.min(rect.min_x, last.min_x);
          tile.ry0 = Math.min(rect.min_y, last.min_y);
          tile.rx1 = Math.max(rect.max_x, last.max_x);
          tile.ry1 = Math.max(rect.max_y, last.max_y);
        }
        // Remember what this frame's snapshot is authoritative for, so
        // the next frame's union repaints anything that was dirty here:
        // the whole tile when a global effect (or a degenerate walk)
        // made every pixel suspect, else the true dirty rectangle. A
        // first frame forced whole by nothing global converges back to
        // the tight rectangles on the very next frame.
        if (global || dirty == null) {
          last.min_x = tile.x0;
          last.min_y = tile.y0;
          last.max_x = tile.x0 + tile.width - 1;
          last.max_y = tile.y0 + tile.height - 1;
        } else {
          last.setTo(dirty[i]);
        }
        tile.done = false;
      }
    }
    for (int i = 0; i < tiles.length; i++) {
      if (skip != null && skip[i]) {
        continue;
      }
      final Tile tile = tiles[i];
      submitted++;
      POOL.execute(new Runnable() {
        public void run() {
          renderTile(id, tiles, tile, camera, bvh, rings);
        }
      });
    }
    if (submitted == 0) {
      // Every tile was skipped: there is no task to complete the frame,
      // so it is complete immediately. Without this, frame_done would
      // stay false forever and no further frame would ever start.
      // Nothing was re-traced, so there is nothing to stage or blit.
      this.frame_done = true;
    }
  }

  private void renderTile(long id, Tile[] tiles, Tile tile, RayCamera camera,
      BVH bvh, RTRing[] rings) {
    if (RendererDelegator.renderer != this) {
      // The user switched to another renderer mid-frame: drop the tile
      // instead of burning CPU on an image nobody will display. Best
      // effort (the field is not volatile); a stale read just means the
      // tile renders as it does today.
      return;
    }
    // The tile's staged rectangle: a sub-rectangle of the tile. Tracing
    // it renders exactly the pixels the corresponding region of a
    // whole-tile trace would: the anti-aliasing jitter is seeded from
    // absolute screen coordinates and pixellation blocks are
    // screen-aligned, so neither depends on the trace origin.
    final int rx0 = tile.rx0;
    final int ry0 = tile.ry0;
    final int rw = tile.rx1 - tile.rx0 + 1;
    final int rh = tile.ry1 - tile.ry0 + 1;
    final int[] pixels = new int[rw * rh];
    final Raytracer.HitStats stats = new Raytracer.HitStats();
    Raytracer.renderTile(rx0, ry0, rw, rh, camera, bvh, rings, pixels,
        stats);
    if (id != this.frame_id) {
      // Superseded by a newer frame; drop the work.
      return;
    }
    final BufferedImage image = new BufferedImage(rw, rh,
        BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, rw, rh, pixels, 0, rw);
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
    // The next repaint re-composites the offscreen frame and blits what
    // changed; the screen never shows the frame being assembled.
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
      // The hit statistics are relative to the staged rectangle's
      // origin; the content rectangle is translated to screen
      // coordinates here.
      t.shown = new ShownTile(t.image, t.rx0, t.ry0, active,
          active ? t.rx0 + s.min_x : 0, active ? t.ry0 + s.min_y : 0,
          active ? t.rx0 + s.max_x : 0, active ? t.ry0 + s.max_y : 0);
    }
    // One completed frame, however many AWT paints it takes to display.
    RendererDelegator.countRenderedFrame();
  }

  /**
   * Each tile's dirty rectangle for the coming frame: the union of every
   * element's screen box clipped to the tile, empty when the tile holds
   * no geometry. The element walk mirrors RayScene.build's filters
   * exactly, so every pixel the frame can paint belongs to some
   * element's bounds, and a tile left marked empty is truly
   * background-only. Returns null when an element's projection is
   * degenerate (behind the camera); the caller then re-traces
   * everything.
   */
  RectangleInt[] computeDirtyRects(NodeManager manager) {
    final Tile[] tiles = this.tiles;
    final int divisor = RendererBinManager.divisor;
    final int width = this.canvas_width;
    final int height = this.canvas_height;
    final int nx = this.tile_nx;
    final RectangleInt[] rects = new RectangleInt[tiles.length];
    for (int i = 0; i < rects.length; i++) {
      rects[i] = new RectangleInt(Integer.MAX_VALUE, Integer.MAX_VALUE,
          Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    if (FrEnd.render_nodes) {
      final List<?> nodes = manager.element;
      final int count = nodes.size();
      for (int i = 0; i < count; i++) {
        final Node node = (Node) nodes.get(i);
        final double radius = node.type.radius;
        if (radius <= 0.0) {
          continue;
        }
        final int z = node.pos.z;
        final int world_per_pixel =
            Coords.shift_constant_z + (z >> Coords.shift_z);
        if (world_per_pixel <= 0) {
          return null;
        }
        final long sx = Coords.getXCoords(node.pos.x, z);
        final long sy = Coords.getYCoords(node.pos.y, z);
        long r = (long) Math.ceil(radius / world_per_pixel) + 2;
        if (node.type.selected) {
          // The billboard selection ring reaches 4/3 the node radius
          // plus 8 pixels, exactly like RayScene.selectionRings.
          final long ring =
              (long) Math.ceil(radius * 4.0 / 3.0 / world_per_pixel) + 8 + 2;
          if (ring > r) {
            r = ring;
          }
        }
        markTilesDirty(rects, tiles, nx, divisor, width, height,
            sx - r, sy - r, sx + r, sy + r);
      }
    }

    if (FrEnd.render_links) {
      final LinkManager link_manager = manager.getLinkManager();
      final List<?> links = link_manager.element;
      final int count = links.size();
      for (int i = 0; i < count; i++) {
        final Link link = (Link) links.get(i);
        if (link.type.hidden) {
          continue;
        }
        final double radius = link.type.radius;
        if (radius <= 0.0) {
          continue;
        }
        final Node[] ends = link.nodes;
        if (ends.length == 0) {
          continue;
        }
        // Mark the link span by span, the way the polygon renderer bins
        // each segment by its own tight box: a single AABB over the whole
        // span marks length-squared tiles for a diagonal link instead of
        // length. (A one-node link renders nothing, so it marks nothing.)
        for (int s = 0; s + 1 < ends.length; s++) {
          if (markLinkSpanDirty(rects, tiles, nx, divisor, width, height,
              ends[s], ends[s + 1], radius)) {
            return null;
          }
        }
      }
    }

    if (FrEnd.render_faces) {
      final FaceManager face_manager = manager.getFaceManager();
      final List<?> faces = face_manager.element;
      final int count = faces.size();
      for (int i = 0; i < count; i++) {
        final Face face = (Face) faces.get(i);
        final java.util.ArrayList<Node> nodes = face.nodes;
        final int points = nodes.size();
        if (points < 3) {
          continue;
        }
        long x0 = Long.MAX_VALUE;
        long y0 = Long.MAX_VALUE;
        long x1 = Long.MIN_VALUE;
        long y1 = Long.MIN_VALUE;
        for (int s = 0; s < points; s++) {
          final Node node = nodes.get(s);
          final int z = node.pos.z;
          final int world_per_pixel =
              Coords.shift_constant_z + (z >> Coords.shift_z);
          if (world_per_pixel <= 0) {
            return null;
          }
          final long sx = Coords.getXCoords(node.pos.x, z);
          final long sy = Coords.getYCoords(node.pos.y, z);
          if (sx - 2 < x0) {
            x0 = sx - 2;
          }
          if (sy - 2 < y0) {
            y0 = sy - 2;
          }
          if (sx + 2 > x1) {
            x1 = sx + 2;
          }
          if (sy + 2 > y1) {
            y1 = sy + 2;
          }
        }
        markTilesDirty(rects, tiles, nx, divisor, width, height,
            x0, y0, x1, y1);
      }
    }

    // The drag-box selection draws directly on the screen, after the
    // blit -- it is not model geometry, so the walks above never cover
    // it. Its old and new rectangles join the dirty region here, or the
    // old rectangle's pixels would never be repainted and the red box
    // would leave a trail (the same damage the polygon renderer forces
    // dirty; see RendererBinManager.getDragBoxDamage).
    final RectangleInt drag_damage = getDragBoxDamage();
    if (drag_damage != null) {
      markTilesDirty(rects, tiles, nx, divisor, width, height,
          drag_damage.min_x, drag_damage.min_y, drag_damage.max_x,
          drag_damage.max_y);
    }

    return rects;
  }

  /**
   * The screen region damaged by a drag-box selection: the union of the
   * previous and current rectangles, expanded by the box's line
   * thickness. Null when no drag is active. Mirrors
   * RendererBinManager.getDragBoxDamage: the box caches its coordinates
   * on draw, so the last drawn rectangle is known even after release,
   * when the gesture's start point is already gone -- and the release
   * frame is the one that erases the box for good.
   */
  private static RectangleInt getDragBoxDamage() {
    if (FrEnd.perform_actions == null
        || FrEnd.perform_actions.drag_box_manager == null
        || FrEnd.perform_actions.drag_box_manager.drag_box_end == null) {
      return null;
    }
    final NodeManager node_manager = ContextManager.getNodeManager();
    if (node_manager == null) {
      return null;
    }
    final RendererDragBox box = node_manager.renderer.renderer_drag_box;
    final int min_x;
    final int min_y;
    final int max_x;
    final int max_y;
    if (box.cache_valid) {
      min_x = Math.min(box.min.x, box.last_min.x);
      min_y = Math.min(box.min.y, box.last_min.y);
      max_x = Math.max(box.max.x, box.last_max.x);
      max_y = Math.max(box.max.y, box.last_max.y);
    } else {
      // Not drawn yet (the very first frame): fall back to the
      // gesture's live points.
      final DragBoxManager drag_box_manager =
          FrEnd.perform_actions.drag_box_manager;
      final java.awt.Point one = drag_box_manager.drag_box_start;
      final java.awt.Point two = drag_box_manager.drag_box_end;
      if (one == null || two == null) {
        return null;
      }
      min_x = Math.min(one.x, two.x);
      max_x = Math.max(one.x, two.x);
      min_y = Math.min(one.y, two.y);
      max_y = Math.max(one.y, two.y);
    }
    final int pad = 4; // the drag-box lines are drawn 3px thick
    final RectangleInt damage = new RectangleInt(0, 0, 0, 0);
    damage.min_x = Coords.getPixelFromInternalCoords(min_x) - pad;
    damage.min_y = Coords.getPixelFromInternalCoords(min_y) - pad;
    damage.max_x = Coords.getPixelFromInternalCoords(max_x) + pad;
    damage.max_y = Coords.getPixelFromInternalCoords(max_y) + pad;
    return damage;
  }

  /**
   * Unions a link span's screen box into every tile's dirty rectangle.
   * The span is split into pieces of at most one tile, and each piece is
   * marked by its own tight box: the perspective projection of a straight
   * 3D span is a straight 2D segment, so linearly interpolating the
   * projected endpoints is exact for the centreline, and the pixel radius
   * only shrinks or grows smoothly with depth. Returns true when the span
   * sits on or behind the eye plane (a degenerate view: the caller
   * re-traces everything).
   */
  private static boolean markLinkSpanDirty(RectangleInt[] rects, Tile[] tiles,
      int nx, int divisor, int width, int height, Node a, Node b,
      double radius) {
    final int ax = a.pos.x;
    final int ay = a.pos.y;
    final int az = a.pos.z;
    final int bx = b.pos.x;
    final int by = b.pos.y;
    final int bz = b.pos.z;
    final long sax = Coords.getXCoords(ax, az);
    final long say = Coords.getYCoords(ay, az);
    final long sbx = Coords.getXCoords(bx, bz);
    final long sby = Coords.getYCoords(by, bz);
    final double dx = sbx - sax;
    final double dy = sby - say;
    int pieces = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / divisor);
    if (pieces < 1) {
      pieces = 1;
    }
    for (int p = 0; p < pieces; p++) {
      final double t0 = (double) p / pieces;
      final double t1 = (double) (p + 1) / pieces;
      // Covers the cable cylinder and the strut's mid-span bulge,
      // both of which stay within the link radius of the span.
      final long[] c0 = projectLinkPoint(ax, ay, az, bx, by, bz, t0,
          radius);
      if (c0 == null) {
        return true;
      }
      final long[] c1 = projectLinkPoint(ax, ay, az, bx, by, bz, t1,
          radius);
      if (c1 == null) {
        return true;
      }
      final long r = Math.max(c0[2], c1[2]);
      markTilesDirty(rects, tiles, nx, divisor, width, height,
          Math.min(c0[0], c1[0]) - r, Math.min(c0[1], c1[1]) - r,
          Math.max(c0[0], c1[0]) + r, Math.max(c0[1], c1[1]) + r);
    }
    return false;
  }

  /**
   * Projects the point a fraction t along the 3D span a-b to the screen,
   * with the link's pixel radius there. Returns {sx, sy, r}, or null
   * when the point sits on or behind the eye plane.
   */
  private static long[] projectLinkPoint(int ax, int ay, int az, int bx,
      int by, int bz, double t, double radius) {
    final int x = (int) (ax + (bx - ax) * t);
    final int y = (int) (ay + (by - ay) * t);
    final int z = (int) (az + (bz - az) * t);
    final int world_per_pixel =
        Coords.shift_constant_z + (z >> Coords.shift_z);
    if (world_per_pixel <= 0) {
      return null;
    }
    // The int rounding moves the point under a world unit: far below the
    // +2 pixel padding folded into r.
    return new long[] {Coords.getXCoords(x, z), Coords.getYCoords(y, z),
        (long) Math.ceil(radius / world_per_pixel) + 2};
  }

  /**
   * Unions an element's screen rectangle into every tile's dirty
   * rectangle it touches, clipped to each tile's own bounds. Fully
   * off-screen elements touch no tile. With "show bins" the tiles are
   * shrunk by a margin, so a box can cross a tile's bin without touching
   * the tile itself: the per-tile clip leaves such tiles' rectangles
   * empty.
   */
  static void markTilesDirty(RectangleInt[] rects, Tile[] tiles, int nx,
      int divisor, int width, int height,
      long x0, long y0, long x1, long y1) {
    if (x1 < 0 || y1 < 0 || x0 >= width || y0 >= height) {
      return;
    }
    if (x0 < 0) {
      x0 = 0;
    }
    if (y0 < 0) {
      y0 = 0;
    }
    if (x1 >= width) {
      x1 = width - 1;
    }
    if (y1 >= height) {
      y1 = height - 1;
    }
    // Clamped to the canvas, every (tx, ty) in range names a real tile:
    // only the trailing column/row past the canvas edge was dropped from
    // the grid, and index = ty * tile_nx + tx packs rows tightly.
    final int tx0 = (int) (x0 / divisor);
    final int tx1 = (int) (x1 / divisor);
    final int ty0 = (int) (y0 / divisor);
    final int ty1 = (int) (y1 / divisor);
    for (int ty = ty0; ty <= ty1; ty++) {
      final int row = ty * nx;
      for (int tx = tx0; tx <= tx1; tx++) {
        final Tile tile = tiles[row + tx];
        final long cx0 = Math.max(x0, tile.x0);
        final long cy0 = Math.max(y0, tile.y0);
        final long cx1 =
            Math.min(x1, (long) tile.x0 + tile.width - 1);
        final long cy1 =
            Math.min(y1, (long) tile.y0 + tile.height - 1);
        if (cx0 > cx1 || cy0 > cy1) {
          continue;
        }
        rects[row + tx].unionBox(cx0, cy0, cx1, cy1);
      }
    }
  }
}
