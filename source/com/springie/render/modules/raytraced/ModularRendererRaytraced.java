// This code has been placed into the public domain by its author

package com.springie.render.modules.raytraced;

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
import com.springie.render.modules.ModularRendererBase;
import com.springie.render.modules.modern.RendererBinManager;

/**
 * A ray-traced renderer that lives alongside the default renderer.
 *
 * <p>It renders into the same bin tiles as the default renderer
 * (RendererBinManager.divisor blocks), one tile per worker thread, and each
 * finished tile is blitted to the screen as it completes -- resolution is
 * to the pixel, one primary ray per pixel.
 *
 * <p>The camera reproduces the default renderer's projection exactly (see
 * RayCamera), so the model appears at the same size and from the same
 * angle. Nodes are spheres, links are open cylinders, faces are opaque
 * triangle fans. Selection shows as a colour change only.
 *
 * <p>A frame renders a snapshot of the model: while a frame is in progress
 * repaints just display whatever tiles have finished, and a new frame
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

    volatile BufferedImage image;

    volatile boolean done;

    Tile(int x0, int y0, int width, int height) {
      this.x0 = x0;
      this.y0 = y0;
      this.width = width;
      this.height = height;
    }
  }

  private Tile[] tiles;

  private int canvas_width = -1;

  private int canvas_height = -1;

  private volatile long frame_id;

  private volatile long frame_signature = -1L;

  private volatile boolean frame_done = true;

  public void resize(int x, int y) {
    this.tiles = null;
  }

  public void reset() {
    this.tiles = null;
    this.frame_done = true;
  }

  public void repaint(Graphics graphics, NodeManager manager) {
    final int width = Coords.x_pixels;
    final int height = Coords.y_pixels;
    if (this.tiles == null || width != this.canvas_width
        || height != this.canvas_height) {
      buildTiles(width, height);
    }

    final long signature = signature(manager);
    if (this.frame_done && signature != this.frame_signature) {
      startFrame(manager, signature);
    }

    final Tile[] tiles = this.tiles;
    for (int i = 0; i < tiles.length; i++) {
      final BufferedImage image = tiles[i].image;
      if (image != null) {
        graphics.drawImage(image, tiles[i].x0, tiles[i].y0, null);
      }
    }
  }

  /**
   * Builds the tile grid: divisor-sized blocks covering the canvas, the
   * same bins the default renderer uses.
   */
  static Tile[] buildTileGrid(int width, int height) {
    final int divisor = RendererBinManager.divisor;
    final int nx = width / divisor + 1;
    final int ny = height / divisor + 1;
    final Tile[] tiles = new Tile[nx * ny];
    int i = 0;
    for (int ty = 0; ty < ny; ty++) {
      for (int tx = 0; tx < nx; tx++) {
        final int x0 = tx * divisor;
        final int y0 = ty * divisor;
        final int w = Math.min(divisor, width - x0);
        final int h = Math.min(divisor, height - y0);
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

  private void buildTiles(int width, int height) {
    this.tiles = buildTileGrid(width, height);
    this.canvas_width = width;
    this.canvas_height = height;
    this.frame_done = true;
    this.frame_signature = -1L;
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
    Raytracer.renderTile(tile.x0, tile.y0, tile.width, tile.height, camera,
        bvh, pixels);
    if (id != this.frame_id) {
      // Superseded by a newer frame; drop the work.
      return;
    }
    final BufferedImage image = new BufferedImage(tile.width, tile.height,
        BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, tile.width, tile.height, pixels, 0, tile.width);
    tile.image = image;
    tile.done = true;

    boolean all_done = true;
    for (int i = 0; i < tiles.length; i++) {
      if (!tiles[i].done) {
        all_done = false;
        break;
      }
    }
    if (all_done) {
      this.frame_done = true;
    }

    // Ask the main loop for another pass so the finished tile displays,
    // even when the model is not animating.
    RendererDelegator.repaint_some_objects = true;
    if (FrEnd.main_canvas != null && FrEnd.main_canvas.panel != null) {
      FrEnd.main_canvas.panel.repaint(tile.x0, tile.y0, tile.width,
          tile.height);
    }
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
    sig = sig * 31 + (FrEnd.render_nodes ? 1 : 0);
    sig = sig * 31 + (FrEnd.render_links ? 1 : 0);
    sig = sig * 31 + (FrEnd.render_faces ? 1 : 0);
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
