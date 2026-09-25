// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.render.RectangleInt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.ArrayList;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.render.Coords;
import com.springie.render.DepthSort;
import com.springie.render.RendererDelegator;
import com.springie.render.ScenicBackground;

public class RendererTileManager {
  // Deliberately prime: no pixellation factor (2, 3, 4, 5) divides it
  // evenly, so every pixellation mode exercises the inexact
  // render/upscale path instead of only 3x3.
  public static int divisor = 337;

  int number_of_tiles_x;

  int number_of_tiles_y;

  private RendererTile[][] array;

  // The frame's global depth sort: an ascending-z permutation of the
  // frame's composite list, filled by distribute(). Reused across frames.
  private int[] node_depth_index = new int[0];

  // Scratch buffers for the global depth sort, reused across frames.
  private int[] sort_keys = new int[0];

  private int[] sort_scratch = new int[0];

  static Random rnd = new Random();

  public static boolean show_tiles;

  public static boolean show_active_tiles;

  /**
   * Tim: when true, use a single tile cropped to the model's bounds
   * instead of the grid. Works for polygon and ray-traced renderers.
   */
  public static boolean one_big_tile;

  public static int colour_modifier_filled = ColourModifier.natural;

  public static int colour_modifier_wireframe = ColourModifier.darker;

  /**
   * Monotonic frame counter, bumped once per render() call. Polygons
   * cache their modifier-adjusted colours against it, so the adjustment
   * is computed once per polygon per frame however many tiles the
   * polygon lands in.
   */
  static int render_frame;

  // Tiled rendering: each tile paints into an offscreen tile that is then
  // blitted to the screen. Every tile holding content is re-rendered every
  // frame; tiles are never reused across frames on the assumption their
  // content is unchanged.

  // Anti-aliasing factor in force when the tiles were rendered. A
  // change means the tiles are the wrong resolution and must be dropped.
  private int last_antialiasing;

  // Pixellation factor in force when the tiles were rendered. A change
  // means the tiles are the wrong resolution and must be dropped.
  private int last_pixellation;

  // The tile size in force when the tiles were created. show_tiles changes
  // the tile size, so the tiles must be dropped when it changes.
  private int last_block_size = -1;

  ArrayList<PolygonComposite> getVector(int x, int y) {
    return this.array[x][y].vector;
  }

  void putVector(int x, int y, ArrayList<PolygonComposite> vector) {
    this.array[x][y].vector = vector;
  }

  void clear() {
    // Log.log("BinManager.clear");
    for (int i = 0; i < this.number_of_tiles_x; i++) {
      for (int j = 0; j < this.number_of_tiles_y; j++) {
        this.array[i][j].vector.clear();
      }
    }
  }

  void resize(int number_of_pixels_x, int number_of_pixels_y) {
    int x;
    int y;
    if (one_big_tile) {
      // Tim: one big tile instead of the grid.
      x = 1;
      y = 1;
    } else {
      x = calcTileX(number_of_pixels_x) + 1;
      y = calcTileY(number_of_pixels_y) + 1;
    }

    // Log.log("BinManager.resize");
    // Log.log("number_of_pixels_x:" + number_of_pixels_x);
    // Log.log("number_of_pixels_Y:" + number_of_pixels_y);
    // Log.log("X:" + x);
    // Log.log("Y:" + y);

    this.number_of_tiles_x = x;
    this.number_of_tiles_y = y;

    reset();
  }

  public void reset() {
    final int x = this.number_of_tiles_x;
    final int y = this.number_of_tiles_y;

    this.array = new RendererTile[x][y];

    for (int i = 0; i < x; i++) {
      for (int j = 0; j < y; j++) {
        this.array[i][j] = new RendererTile();
      }
    }

    // The tiles are gone, so the tiled path rebuilds them on the next
    // render; the direct path repaints everything every frame anyway.
  }

  void add(int x, int y, PolygonComposite triangle) {
    final ArrayList<PolygonComposite> v = getVector(x, y);
    v.add(triangle);
  }

  void add(PolygonComposite composite) {
    final RectangleInt bb = composite.getBoundingBox();
    // final int min_x = getBMinimum(triangle.x);
    // final int max_x = getMaximum(triangle.x) + 1;
    // final int min_y = getMinimum(triangle.y);
    // final int max_y = getMaximum(triangle.y) + 1;

    final int min_tile_x = getTileX(bb.min_x);
    final int max_tile_x = getTileX(bb.max_x + 1);

    final int min_tile_y = getTileY(bb.min_y);
    final int max_tile_y = getTileY(bb.max_y + 1);

    for (int i = min_tile_x; i <= max_tile_x; i++) {
      for (int j = min_tile_y; j <= max_tile_y; j++) {
        add(i, j, composite);
      }
    }
  }

  /**
   * Sorts the frame's composites once, globally -- ascending by z,
   * stable -- and distributes them to the tiles in that order. Every
   * tile's vector is therefore pre-sorted, so render() needs no per-tile
   * sort: with deepest-first on, each tile holds ascending-z order; with
   * it off, the creation order, at zero sort cost (the old identity
   * index did the same). Either way the render loops walk each tile's
   * vector from the end backwards, exactly the draw order the old
   * per-tile sorts produced: a stable sort of a subsequence equals the
   * subsequence of the stable sort.
   *
   * The sort buffers are reused across frames; the only per-frame work
   * besides the sort itself is the distribution, which the old code did
   * anyway.
   */
  void distribute(ArrayList<PolygonComposite> all, boolean deepest_first) {
    final int size = all.size();
    if (deepest_first) {
      if (this.node_depth_index.length < size) {
        this.node_depth_index = new int[size];
      }
      if (this.sort_keys.length < size) {
        this.sort_keys = new int[size];
        this.sort_scratch = new int[size];
      }
      final int[] index = this.node_depth_index;
      final int[] keys = this.sort_keys;
      for (int i = size; --i >= 0;) {
        index[i] = i;
        keys[i] = all.get(i).z;
      }
      DepthSort.sort(index, keys, size, this.sort_scratch);
      for (int i = 0; i < size; i++) {
        add(all.get(index[i]));
      }
    } else {
      for (int i = 0; i < size; i++) {
        add(all.get(i));
      }
    }
  }

  public void render(RendererTileManager tiles_last, Graphics graphics) {
    ContextManager.getNodeManager().depth_range = null;

    render_frame++;

    final int block_size;
    if (one_big_tile) {
      // Tim: one big tile has unlimited size -- it covers the whole
      // canvas, not limited by the divisor.
      final java.awt.Rectangle clip = graphics.getClipBounds();
      if (clip != null) {
        block_size = Math.max(clip.width, clip.height);
      } else {
        // Fallback: very large, effectively unlimited.
        block_size = 10000;
      }
    } else {
      block_size = divisor - getMargin();
    }

    renderTiled(tiles_last, graphics, block_size);
  }

  /**
   * "Show active tiles": red outline around the content rectangle of every
   * tile holding content this frame -- the same min/max rect the scrubs and
   * blits use, so the outline hugs what the tile really touched. Drawn on
   * the screen graphics after the tile pixels (not baked into the cached
   * tiles), so toggling the option needs no tile invalidation; and because
   * the outline lies inside the tile's paint union, the normal scrub/blit
   * erases it when content moves or the option is turned off.
   */
  private void drawActiveTileOutlines(Graphics graphics) {
    if (!show_active_tiles) {
      return;
    }
    graphics.setClip(0, 0, 9999, 9999);
    graphics.setColor(Color.RED);
    for (int j = 0; j < this.number_of_tiles_y; j++) {
      for (int i = 0; i < this.number_of_tiles_x; i++) {
        final RendererTile tile = this.array[i][j];
        if (tile.vector.size() > 0) {
          final RectangleInt actual = tile.actual;
          // The bbox max is exclusive, but drawRect's far corner is
          // inclusive: shrink by one so the outline stays inside the
          // scrub/blit clip and is erased with everything else.
          graphics.drawRect(actual.min_x, actual.min_y,
              actual.max_x - actual.min_x - 1, actual.max_y - actual.min_y - 1);
        }
      }
    }
  }

  private void renderTiled(RendererTileManager tiles_last, Graphics graphics,
      int block_size) {

    final int aa = RendererDelegator.antialiasing;
    final int px = RendererDelegator.pixellation;

    // The tile size changed (show_tiles toggled), or the anti-aliasing or
    // pixellation factor changed: the tiles are the wrong size, so drop
    // them. They are rebuilt below.
    if (block_size != this.last_block_size || aa != this.last_antialiasing
        || px != this.last_pixellation) {
      for (int j = 0; j < this.number_of_tiles_y; j++) {
        for (int i = 0; i < this.number_of_tiles_x; i++) {
          this.array[i][j].image = null;
          this.array[i][j].image_aa = null;
        }
      }
      this.last_block_size = block_size;
      this.last_antialiasing = aa;
      this.last_pixellation = px;
    }

    // Pixellated tiles are rendered at 1/px resolution: the coarse tile
    // covers the tile with ceil(block_size / px) pixels per side, then
    // gets nearest-neighbour upsampled to the full tile size on blit.
    final int coarse_w = (block_size + px - 1) / px;
    final int coarse_h = (block_size + px - 1) / px;
    // The render tile: anti-aliasing supersamples the coarse tile.
    final int render_w = coarse_w * aa;
    final int render_h = coarse_h * aa;

    final RectangleInt potential = new RectangleInt(0, 0, 0, 0);

    // Every tile holding content is re-rendered into its tile; every tile
    // (and every vacated tile's repaired screen area) is blitted below, so
    // exposure damage self-heals on the next frame without any explicit
    // invalidation.
    for (int j = 0; j < this.number_of_tiles_y; j++) {
      for (int i = 0; i < this.number_of_tiles_x; i++) {
        final RendererTile tile = this.array[i][j];
        final ArrayList<PolygonComposite> v_this = tile.vector;
        final int size = v_this.size();

        final RendererTile last_tile = tiles_last.array[i][j];
        final int size_last = last_tile.vector.size();

        if (size == 0 && size_last == 0) {
          // Empty tile: normally skipped.
          continue;
        }

        potential.min_x = getPixelsFromTileX(i);
        potential.min_y = getPixelsFromTileY(j);
        potential.max_x = potential.min_x + block_size;
        potential.max_y = potential.min_y + block_size;

        tile.setUpActual(potential);
        tile.union.setToUnion(tile.actual, last_tile.actual);
        if (px > 1) {
          // Cover the pixellation bleed (see expandByBleed). In 4x4 and
          // 5x5 modes the coarse rasterizer can spill a pixel past the
          // bleed, leaving node-coloured trails peeking out at tile
          // edges; grow by an extra pixel there.
          expandByBleed(tile.union, px + ((px == 4 || px == 5) ? 1 : 0));
        }

        if (size > 0) {
            if (tile.image == null) {
              FrEnd.main_canvas.panel
                  .setBackground(RendererDelegator.color_background);
              if (aa > 1 || px > 1) {
                // Rendered at aa / px resolution, then box-filtered
                // (anti-aliasing) and/or nearest-neighbour upsampled
                // (pixellation) on blit. A BufferedImage guarantees
                // readable pixels for the resampling.
                tile.image = new BufferedImage(render_w, render_h,
                    BufferedImage.TYPE_INT_RGB);
              } else {
                tile.image = FrEnd.main_canvas.createImage(block_size,
                    block_size);
              }
            }
            final Graphics graphics_paint = tile.image.getGraphics();
            if (aa > 1 || px > 1) {
              // Render in screen coordinates scaled to the coarse tile:
              // translate first, then scale, so a screen point p lands on
              // tile pixel (coarse_w * aa / block_size) * (p - min). The
              // scale is the exact inverse of the blit's upscale (which
              // maps the coarse tile back onto the full tile), not aa /
              // px: with a prime tile size no px divides block_size
              // evenly, so aa / px always leaves the last coarse
              // row/column only fractionally covered, the rasterizer
              // skips the sliver, and the upscale samples the unpainted
              // pixels as a dark seam along the tile's bottom and right
              // edges. The clip and scrub below are in the same user
              // space, so they scale along untouched.
              final Graphics2D graphics_2d = (Graphics2D) graphics_paint;
              final double scale =
                  (double) (coarse_w * aa) / block_size;
              graphics_2d.translate(-potential.min_x * scale,
                  -potential.min_y * scale);
              graphics_2d.scale(scale, scale);
            } else {
              graphics_paint.translate(-potential.min_x, -potential.min_y);
            }
            // Scrub the union of last frame's and this frame's content, so
            // moved content leaves no trail. Always scrub, even for newly
            // created tiles (createImage content is undefined).
            doScrubbing(graphics_paint, potential, tile);

            // The tile's vector is already in draw order (see
            // distribute): ascending by z with deepest-first on, drawn
            // from the end backwards -- deepest first -- and creation
            // order with it off. No per-tile sort.
            graphics_paint.setClip(potential.min_x, potential.min_y,
                block_size, block_size);

            for (int c = size; --c >= 0;) {
              renderThePolygon(graphics_paint, v_this.get(c));
            }

            if (aa > 1) {
              // Box-filter the supersampled tile into the coarse tile
              // (the full tile size while pixellation is off).
              if (tile.image_aa == null) {
                tile.image_aa = new BufferedImage(coarse_w, coarse_h,
                    BufferedImage.TYPE_INT_RGB);
              }
              downsampleTile((BufferedImage) tile.image, tile.image_aa, aa);
            }
            graphics_paint.dispose();
          } else if (tile.image != null) {
            // Vacated tile: repair the screen directly and drop the tile.
            doScrubbing(graphics, potential, tile);
            tile.image = null;
            tile.image_aa = null;
          }
      }
    }

    // The blit loop below leaves the clip on the last tile's union
    // rectangle: restore the full canvas clip afterwards, so the
    // screen-space painters that run after the renderer (the
    // boundary-box dots, the info button) are not clipped to a stale
    // tile. (Toggling "Show active tiles" used to mask this: its
    // outline pass resets the clip as a side effect.)
    for (int j = 0; j < this.number_of_tiles_y; j++) {
      for (int i = 0; i < this.number_of_tiles_x; i++) {
        final RendererTile tile = this.array[i][j];
        if (tile.image != null) {
          final int tile_min_x = getPixelsFromTileX(i);
          final int tile_min_y = getPixelsFromTileY(j);
          final RectangleInt union = tile.union;
          graphics.setClip(union.min_x, union.min_y, union.max_x - union.min_x,
              union.max_y - union.min_y);
          if (px > 1) {
            // Pixellated tiles scale the coarse tile up to the full tile
            // with a nearest-neighbour blit: one flat colour per
            // px-by-px block, done natively.
            final BufferedImage coarse = aa > 1 ? tile.image_aa
                : (BufferedImage) tile.image;
            if (coarse != null) {
              paintPixellated(graphics, coarse, tile_min_x, tile_min_y,
                  block_size, block_size);
            }
          } else {
            // Anti-aliased tiles blit the box-filtered tile; the 1x path
            // blits the rendered tile directly, exactly as before.
            final Image blit =
                aa > 1 && tile.image_aa != null ? tile.image_aa : tile.image;
            graphics.drawImage(blit, tile_min_x, tile_min_y, null);
          }
        }
      }
    }

    graphics.setClip(0, 0, 9999, 9999);

    drawActiveTileOutlines(graphics);
  }

  /**
   * Box-filter downsample: averages each aa-by-aa block of the supersampled
   * tile into one destination pixel. Integer division truncates, so the
   * result can be at most one level darker per channel than the true mean.
   */
  static void downsampleTile(BufferedImage src, BufferedImage dst, int aa) {
    final int w = dst.getWidth();
    final int h = dst.getHeight();
    final int sw = w * aa;
    final int[] src_pixels = src.getRGB(0, 0, sw, h * aa, null, 0, sw);
    final int[] dst_pixels = new int[w * h];
    final int n = aa * aa;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        long r = 0;
        long g = 0;
        long b = 0;
        final int base = y * aa * sw + x * aa;
        for (int dy = 0; dy < aa; dy++) {
          final int row = base + dy * sw;
          for (int dx = 0; dx < aa; dx++) {
            final int rgb = src_pixels[row + dx];
            r += (rgb >> 16) & 0xFF;
            g += (rgb >> 8) & 0xFF;
            b += rgb & 0xFF;
          }
        }
        dst_pixels[y * w + x] = 0xFF000000 | (int) (r / n) << 16
            | (int) (g / n) << 8 | (int) (b / n);
      }
    }
    dst.setRGB(0, 0, w, h, dst_pixels, 0, w);
  }

  /**
   * Pixellated blit: scales the coarse (1/px resolution) tile up to the
   * given screen rect with nearest-neighbour interpolation, so each
   * coarse pixel becomes one flat px-by-px block. Done natively by the
   * blitter: a Java pixel loop over the full tile costs ~2.4ms per tile
   * per frame here, the scaled blit ~0.1ms. Package-visible for the
   * tests.
   */
  static void paintPixellated(Graphics graphics, BufferedImage coarse,
      int dst_x, int dst_y, int dst_w, int dst_h) {
    final Graphics2D g2d = (Graphics2D) graphics;
    final Object old_hint =
        g2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g2d.drawImage(coarse, dst_x, dst_y, dst_x + dst_w, dst_y + dst_h, 0, 0,
        coarse.getWidth(), coarse.getHeight(), null);
    // A null old hint means the default was in force, which is
    // nearest-neighbour; restoring it explicitly keeps setRenderingHint
    // from throwing on the null.
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old_hint != null
        ? old_hint
        : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
  }

  /**
   * Rotates per-tile frame state after rendering: tiles_last takes this
   * frame's vectors and rectangles for next frame's damage repair (the
   * union of last frame's and this frame's content is scrubbed before
   * repainting), while the tiles stay on this manager.
   */
  void rotateFrameState(RendererTileManager tiles_last) {
    for (int j = 0; j < this.number_of_tiles_y; j++) {
      for (int i = 0; i < this.number_of_tiles_x; i++) {
        final RendererTile tile = this.array[i][j];
        final RendererTile last_tile = tiles_last.array[i][j];

        final ArrayList<PolygonComposite> vector = tile.vector;
        tile.vector = last_tile.vector;
        last_tile.vector = vector;

        final RectangleInt actual = tile.actual;
        tile.actual = last_tile.actual;
        last_tile.actual = actual;

        final RectangleInt union = tile.union;
        tile.union = last_tile.union;
        last_tile.union = union;
      }
    }
  }

  private void renderThePolygon(Graphics graphics,
      final PolygonComposite composite) {
    final int size = composite.count;
    final int frame = render_frame;
    if (colour_modifier_filled != 0) {
      for (int i = size; --i >= 0;) {
        final PolygonObject2D polygon = composite.array[i];
        if (polygon.colour_cache_frame != frame) {
          cacheModifiedColours(polygon, frame);
        }
        polygon.fill(graphics, polygon.colour_cache_filled);
      }
    }

    if (colour_modifier_wireframe != 0) {
      // for (int i = 0; i < size; i++) {
      for (int i = size; --i >= 0;) {
        final PolygonObject2D polygon = composite.array[i];
        if (polygon.colour_cache_frame != frame) {
          cacheModifiedColours(polygon, frame);
        }
        polygon.draw(graphics, polygon.colour_cache_wireframe);
      }
    }
  }

  /**
   * Computes a polygon's modifier-adjusted fill/draw colours once per
   * frame. The modifiers are frame-constant, and the polygon's own
   * colour is rewritten before each frame's render, so caching against
   * the frame counter is exact.
   */
  private void cacheModifiedColours(PolygonObject2D polygon, int frame) {
    polygon.colour_cache_filled = getModifiedColour(polygon.colour,
        colour_modifier_filled);
    polygon.colour_cache_wireframe = getModifiedColour(polygon.colour,
        colour_modifier_wireframe);
    polygon.colour_cache_frame = frame;
  }

  int getModifiedColour(int colour, int modifier) {
    switch (modifier) {
      case ColourModifier.natural:
        return colour;
      case ColourModifier.darker:
        return (colour >>> 1) & 0xFF7F7F7F;
      case ColourModifier.lighter:
        return (colour >>> 1) | 0xFF808080;
      case ColourModifier.colour_a:
        return getScaledValue(colour, ColourModifier.colour_a_number);
      case ColourModifier.colour_b:
        return getScaledValue(colour, ColourModifier.colour_b_number);
      case ColourModifier.grey:
        return getGreyscaleValue(colour);
      default:
        return 0xFF808080;
    }
  }

  int getScaledValue(int colour1, int colour2) {
    final int r1 = (colour1 >> 16) & 0xFF;
    final int g1 = (colour1 >> 8) & 0xFF;
    final int b1 = colour1 & 0xFF;
    final int r2 = (colour2 >> 16) & 0xFF;
    final int g2 = (colour2 >> 8) & 0xFF;
    final int b2 = colour2 & 0xFF;
    final int r = r1 * r2 >> 8;
    final int g = g1 * g2 >> 8;
    final int b = b1 * b2 >> 8;
    final int rv = 0xFF000000 | b | (g << 8) | (r << 16);
    return rv;
  }

  private int getGreyscaleValue(int colour) {
    final int r = (colour >> 16) & 0xFF;
    final int g = (colour >> 8) & 0xFF;
    final int b = colour & 0xFF;
    final int t = (r + g + b) / 3;
    final int rv = 0xFF000000 | t | (t << 8) | (t << 16);
    return rv;
  }

  private void doScrubbing(Graphics graphics, RectangleInt potential,
      RendererTile tile) {
    final RectangleInt union = tile.union;
    final int px = RendererDelegator.pixellation;
    if (px > 1) {
      // The scrub runs through the 1/px tile transform: a union edge that
      // is not on a coarse-block boundary truncates in tile space, so the
      // edge coarse pixels are never scrubbed -- stale content that the
      // upsampler then streaks to the right and bottom. Snap the clip out
      // to whole coarse blocks (aligned to the tile origin, matching the
      // upsampler); the min edges already over-cover, which is harmless.
      snapScrubToCoarseBlocks(union, potential.min_x, potential.min_y, px,
          scrub_rect);
      graphics.setClip(scrub_rect.min_x, scrub_rect.min_y,
          scrub_rect.max_x - scrub_rect.min_x,
          scrub_rect.max_y - scrub_rect.min_y);
    } else {
      graphics.setClip(union.min_x, union.min_y, union.max_x - union.min_x,
          union.max_y - union.min_y);
    }
    // Tim: blank the minimal content box, not the whole tile/canvas.
    scrubTile(graphics, union);
  }

  // Scratch rect for the snapped scrub clip, reused across tiles and
  // frames to stay out of the render loop's allocations.
  private final RectangleInt scrub_rect = new RectangleInt(0, 0, 0, 0);

  /**
   * Expands a damage rect by the pixellation bleed margin. The coarse
   * tile rasterization plus nearest-neighbour upscale can colour a full
   * px-by-px block from a sub-block sliver of polygon, so rendered
   * pixels can land up to px outside the integer content bbox. The
   * scrub and the blit clip must cover that bleed or it survives as
   * trails. Package-visible for the tests.
   */
  static void expandByBleed(RectangleInt rect, int px) {
    if (rect.min_x < rect.max_x && rect.min_y < rect.max_y) {
      rect.min_x -= px;
      rect.min_y -= px;
      rect.max_x += px;
      rect.max_y += px;
    }
  }

  /**
   * Snaps a screen-space scrub rect out to whole coarse pixellation
   * blocks, aligned to the tile origin (the upsampler aligns its blocks
   * to the tile origin, i.e. the tile's screen origin). Min edges round
   * down, max edges round up, so every coarse pixel the content could
   * have touched is covered once the rect is run through the 1/px tile
   * transform. Package-visible for the tests.
   */
  static void snapScrubToCoarseBlocks(RectangleInt rect, int tile_min_x,
      int tile_min_y, int px, RectangleInt out) {
    out.min_x = tile_min_x + px * ((rect.min_x - tile_min_x) / px);
    out.min_y = tile_min_y + px * ((rect.min_y - tile_min_y) / px);
    out.max_x = tile_min_x + px * ((rect.max_x - tile_min_x + px - 1) / px);
    out.max_y = tile_min_y + px * ((rect.max_y - tile_min_y + px - 1) / px);
  }

  void scrubTile(Graphics graphics, RectangleInt union) {
    // Tim: blank the minimal content box (union), roughly the same size
    // as what was drawn -- not the whole tile or canvas.
    final int x = union.min_x;
    final int y = union.min_y;
    final int w = union.max_x - union.min_x;
    final int h = union.max_y - union.min_y;

    // graphics.setColor(new Color(rnd.nextInt() & 0x7F7F7F));
    if (RendererDelegator.scenic_background && Coords.x_pixels > 0
        && Coords.y_pixels > 0) {
      // Repaint the scenic background under the scrubbed area.
      final BufferedImage scenic = ScenicBackground.imageFor(
          Coords.x_pixels, Coords.y_pixels);
      graphics.drawImage(scenic, x, y, x + w, y + h, x, y, x + w, y + h, null);
    } else {
      graphics.setColor(RendererDelegator.color_background);
      graphics.fillRect(x, y, w, h);
    }
  }

  private int getMargin() {
    return RendererTileManager.show_tiles ? 4 : 0;
  }

  private static boolean rectsIntersect(RectangleInt a, RectangleInt b) {
    return a.min_x < b.max_x && a.max_x > b.min_x && a.min_y < b.max_y
        && a.max_y > b.min_y;
  }

  public int min4(int x1, int x2, int x3, int x4) {
    return min(min(x1, x2), min(x3, x4));
  }

  public int min3(int x1, int x2, int x3) {
    return min(min(x1, x2), x3);
  }

  private int min(int x1, int x2) {
    return (x1 < x2) ? x1 : x2;
  }

  public int max3(int x1, int x2, int x3) {
    return max(x1, max(x2, x3));
  }

  private int max(int x1, int x2) {
    return (x1 > x2) ? x1 : x2;
  }

  private int getTileX(int pixels) {
    if (pixels < 0) {
      return 0;
    }
    final int proposed = pixels / divisor;

    if (proposed >= this.number_of_tiles_x) {
      if (proposed > 0) {
        return this.number_of_tiles_x - 1;
      }
    }
    return proposed;
  }

  private int getTileY(int pixels) {
    if (pixels < 0) {
      return 0;
    }

    final int proposed = pixels / divisor;
    if (proposed >= this.number_of_tiles_y) {
      if (proposed > 0) {
        return this.number_of_tiles_y - 1;
      }
    }
    return proposed;
  }

  private int calcTileX(int pixels) {
    if (pixels < 0) {
      return 0;
    }
    final int proposed = pixels / divisor;
    return proposed;
  }

  private int calcTileY(int pixels) {
    if (pixels < 0) {
      return 0;
    }

    final int proposed = pixels / divisor;
    return proposed;
  }

  private int getPixelsFromTileX(int pixels) {
    return pixels * divisor;
  }

  private int getPixelsFromTileY(int pixels) {
    return pixels * divisor;
  }

  int getMaximum(int[] x) {
    final int length = x.length;
    int max = 0;
    for (int i = length; --i >= 0;) {
      if (max < x[i]) {
        max = x[i];
      }
    }
    return max;
  }

  int getMinimum(int[] x) {
    final int length = x.length;
    int min = Integer.MAX_VALUE;
    for (int i = length; --i >= 0;) {
      if (min > x[i]) {
        min = x[i];
      }
    }
    return min;
  }
}
