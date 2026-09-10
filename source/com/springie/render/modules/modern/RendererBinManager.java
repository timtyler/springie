// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;
import java.util.ArrayList;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.render.RendererDelegator;

public class RendererBinManager {
  public static int divisor = 192;

  int number_of_bins_x;

  int number_of_bins_y;

  private RendererBin[][] array;

  private int[] node_depth_index = new int[0];

  // Scratch buffers for the depth sort, reused across bins and frames.
  private int[] sort_keys = new int[0];

  private int[] sort_scratch = new int[0];

  static Random rnd = new Random();

  public static boolean show_bins;

  public static boolean show_active_bins;

  public static int colour_modifier_filled = ColourModifier.natural;

  public static int colour_modifier_wireframe = ColourModifier.darker;

  // Dirty-bin skipping: only bins whose polygon content changed since last
  // frame are re-rendered; the rest blit their cached tile. The render
  // settings below also affect bin pixels, so any change in them marks every
  // bin dirty. Compared field-by-field (not hashed) so a missed change can
  // never silently corrupt the display.
  private int last_colour_modifier_filled;

  private int last_colour_modifier_wireframe;

  private int last_colour_a_number;

  private int last_colour_b_number;

  private boolean last_redraw_deepest_first;

  private boolean last_show_bins;

  private boolean last_double_buffered;

  private boolean render_settings_valid;

  // The tile size in force when the tiles were created. show_bins changes
  // the tile size, so the cached tiles must be dropped when it changes.
  private int last_block_size = -1;

  // Set by RendererDelegator.renderDragBox after each frame: the drag box
  // paints background over bin pixels after the bins have rendered, so the
  // next frame must re-render every bin to repair the damage.
  public static boolean drag_box_damaged_last_frame;

  // Adaptive comparison: when no bin has been skippable for a while, the
  // per-bin content comparison is pure overhead, so stop doing it for a
  // while, then probe again. Purely a heuristic; correctness never depends
  // on it. Instance state: only bins_current.render() is ever called.
  private int frames_without_skips;

  private int comparison_cooldown;

  private static final int COOLDOWN_AFTER_N_EMPTY_FRAMES = 30;

  private static final int COOLDOWN_LENGTH_FRAMES = 120;

  ArrayList<PolygonComposite> getVector(int x, int y) {
    return this.array[x][y].vector;
  }

  void putVector(int x, int y, ArrayList<PolygonComposite> vector) {
    this.array[x][y].vector = vector;
  }

  void clear() {
    // Log.log("BinManager.clear");
    for (int i = 0; i < this.number_of_bins_x; i++) {
      for (int j = 0; j < this.number_of_bins_y; j++) {
        this.array[i][j].vector.clear();
      }
    }
  }

  void resize(int number_of_pixels_x, int number_of_pixels_y) {
    int x = calcBinX(number_of_pixels_x) + 1;
    int y = calcBinY(number_of_pixels_y) + 1;

    // Log.log("BinManager.resize");
    // Log.log("number_of_pixels_x:" + number_of_pixels_x);
    // Log.log("number_of_pixels_Y:" + number_of_pixels_y);
    // Log.log("X:" + x);
    // Log.log("Y:" + y);

    this.number_of_bins_x = x;
    this.number_of_bins_y = y;

    reset();
  }

  public void reset() {
    final int x = this.number_of_bins_x;
    final int y = this.number_of_bins_y;

    this.array = new RendererBin[x][y];

    for (int i = 0; i < x; i++) {
      for (int j = 0; j < y; j++) {
        this.array[i][j] = new RendererBin();
      }
    }

    // The cached tiles are gone, so the next render must treat every bin
    // as dirty regardless of the settings comparison.
    this.render_settings_valid = false;
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

    final int min_bin_x = getBinX(bb.min_x);
    final int max_bin_x = getBinX(bb.max_x + 1);

    final int min_bin_y = getBinY(bb.min_y);
    final int max_bin_y = getBinY(bb.max_y + 1);

    for (int i = min_bin_x; i <= max_bin_x; i++) {
      for (int j = min_bin_y; j <= max_bin_y; j++) {
        add(i, j, composite);
      }
    }
  }

  public void render(RendererBinManager bins_last, Graphics graphics) {
    ContextManager.getNodeManager().depth_range = null;

    final int block_size = divisor - getMargin();

    // The user's double-buffer preference is honoured: dirty-bin skipping
    // only applies to the tiled (double-buffered) path, which is where it
    // wins. With double-buffering off, bins paint directly every frame,
    // exactly as before.
    final boolean double_buffered = RendererDelegator.isNewDoubleBuffer();

    if (double_buffered != this.last_double_buffered) {
      // The tiling mode changed: drop all cached tiles. The tiled path
      // rebuilds them below (every bin is forced dirty via the settings
      // check); the direct path repaints everything every frame anyway.
      for (int j = 0; j < this.number_of_bins_y; j++) {
        for (int i = 0; i < this.number_of_bins_x; i++) {
          this.array[i][j].image = null;
        }
      }
    }

    if (double_buffered) {
      renderTiled(bins_last, graphics, block_size);
    } else {
      renderDirect(bins_last, graphics, block_size);
    }

    rememberRenderSettings(double_buffered);
  }

  /**
   * Direct painting path for when the double-buffer preference is off:
   * no tiles, no skipping. Behaviour is the pre-dirty-bin behaviour.
   */
  private void renderDirect(RendererBinManager bins_last, Graphics graphics,
      int block_size) {
    final RectangleInt potential = new RectangleInt(0, 0, 0, 0);

    for (int j = 0; j < this.number_of_bins_y; j++) {
      for (int i = 0; i < this.number_of_bins_x; i++) {
        final RendererBin bin = this.array[i][j];
        final ArrayList<PolygonComposite> v_this = bin.vector;
        final int size = v_this.size();

        final RendererBin last_bin = bins_last.array[i][j];
        final int size_last = last_bin.vector.size();

        if (size_last > 0 || size > 0) {
          potential.min_x = getPixelsFromBinX(i);
          potential.min_y = getPixelsFromBinY(j);
          potential.max_x = potential.min_x + block_size;
          potential.max_y = potential.min_y + block_size;

          bin.setUpActual(potential);
          bin.union.setToUnion(bin.actual, last_bin.actual);

          // No tiles in the direct path (any stale ones were dropped in
          // render() when the mode changed).
          bin.image = null;

          if (size > 0) {
            if (size_last > 0) {
              doScrubbing(graphics, potential, bin);
            }

            getSortedNodeDepthIndex(v_this, FrEnd.redraw_deepest_first);

            graphics.setClip(potential.min_x, potential.min_y, block_size,
                block_size);

            for (int c = size; --c >= 0;) {
              final int index = this.node_depth_index[c];
              final PolygonComposite composite = v_this.get(index);

              renderThePolygon(graphics, composite);
            }
          }
        }
      }
    }

    drawActiveBinOutlines(graphics, block_size);
  }

  /**
   * "Show active bins": red outline around every bin holding content this
   * frame. Drawn on the screen graphics after the bin pixels (not baked
   * into the cached tiles), so toggling the option needs no tile
   * invalidation.
   */
  private void drawActiveBinOutlines(Graphics graphics, int block_size) {
    if (!show_active_bins) {
      return;
    }
    graphics.setClip(0, 0, 9999, 9999);
    graphics.setColor(Color.RED);
    for (int j = 0; j < this.number_of_bins_y; j++) {
      for (int i = 0; i < this.number_of_bins_x; i++) {
        if (this.array[i][j].vector.size() > 0) {
          graphics.drawRect(getPixelsFromBinX(i), getPixelsFromBinY(j),
              block_size - 1, block_size - 1);
        }
      }
    }
  }

  private void renderTiled(RendererBinManager bins_last, Graphics graphics,
      int block_size) {

    // The tile size changed (show_bins toggled): the cached tiles are the
    // wrong size, so drop them. They are rebuilt below as bins go dirty.
    if (block_size != this.last_block_size) {
      for (int j = 0; j < this.number_of_bins_y; j++) {
        for (int i = 0; i < this.number_of_bins_x; i++) {
          this.array[i][j].image = null;
        }
      }
      this.last_block_size = block_size;
    }

    final RectangleInt potential = new RectangleInt(0, 0, 0, 0);

    // Dirty-bin skipping: only bins whose polygon content changed since last
    // frame are re-rendered into their cached tile; every tile (dirty or
    // not) is blitted below, so exposure damage self-heals on the next
    // frame without any explicit invalidation.
    if (comparison_cooldown > 0) {
      comparison_cooldown--;
    }
    final boolean force_all_dirty = comparison_cooldown > 0
        || drag_box_damaged_last_frame || renderSettingsChanged(true);

    int skipped_this_frame = 0;

    for (int j = 0; j < this.number_of_bins_y; j++) {
      for (int i = 0; i < this.number_of_bins_x; i++) {
        final RendererBin bin = this.array[i][j];
        final ArrayList<PolygonComposite> v_this = bin.vector;
        final int size = v_this.size();

        final RendererBin last_bin = bins_last.array[i][j];
        final ArrayList<PolygonComposite> v_last = last_bin.vector;
        final int size_last = v_last.size();

        if (size == 0 && size_last == 0) {
          continue;
        }

        potential.min_x = getPixelsFromBinX(i);
        potential.min_y = getPixelsFromBinY(j);
        potential.max_x = potential.min_x + block_size;
        potential.max_y = potential.min_y + block_size;

        bin.setUpActual(potential);
        bin.union.setToUnion(bin.actual, last_bin.actual);

        final boolean dirty = force_all_dirty || !binsEqual(v_this, v_last);

        if (dirty) {
          if (size > 0) {
            if (bin.image == null) {
              FrEnd.main_canvas.panel
                  .setBackground(RendererDelegator.color_background);
              bin.image = FrEnd.main_canvas.createImage(block_size,
                  block_size);
            }
            final Graphics graphics_paint = bin.image.getGraphics();
            graphics_paint.translate(-potential.min_x, -potential.min_y);
            // Scrub the union of last frame's and this frame's content, so
            // moved content leaves no trail. Always scrub, even for newly
            // created tiles (createImage content is undefined).
            doScrubbing(graphics_paint, potential, bin);

            getSortedNodeDepthIndex(v_this, FrEnd.redraw_deepest_first);

            graphics_paint.setClip(potential.min_x, potential.min_y,
                block_size, block_size);

            for (int c = size; --c >= 0;) {
              final int index = this.node_depth_index[c];
              final PolygonComposite composite = v_this.get(index);

              renderThePolygon(graphics_paint, composite);
            }
            graphics_paint.dispose();
          } else if (bin.image != null) {
            // Vacated bin: repair the screen directly and drop the tile.
            doScrubbing(graphics, potential, bin);
            bin.image = null;
          }
        } else {
          // Clean bin: the cached tile already holds these pixels.
          skipped_this_frame++;
        }
      }
    }

    // Adaptive comparison: when a fair probe found nothing skippable for a
    // while, the comparison is pure overhead; cool down, then probe again.
    if (!force_all_dirty) {
      if (skipped_this_frame == 0) {
        if (++frames_without_skips >= COOLDOWN_AFTER_N_EMPTY_FRAMES) {
          comparison_cooldown = COOLDOWN_LENGTH_FRAMES;
          frames_without_skips = 0;
        }
      } else {
        frames_without_skips = 0;
      }
    }

    // graphics.setClip(0, 0, 9999, 9999);
    for (int j = 0; j < this.number_of_bins_y; j++) {
      for (int i = 0; i < this.number_of_bins_x; i++) {
        final RendererBin bin = this.array[i][j];
        if (bin.image != null) {
          final int bin_min_x = getPixelsFromBinX(i);
          final int bin_min_y = getPixelsFromBinY(j);
          final RectangleInt union = bin.union;
          graphics.setClip(union.min_x, union.min_y, union.max_x - union.min_x,
              union.max_y - union.min_y);
          graphics.drawImage(bin.image, bin_min_x, bin_min_y, null);
        }
      }
    }

    drawActiveBinOutlines(graphics, block_size);
  }

  /**
   * Rotates per-bin frame state after rendering: bins_last takes this
   * frame's vectors and rectangles for next frame's dirty comparison, while
   * the cached tiles stay on this manager for next frame's blit. (The tiles
   * must not rotate: a clean bin next frame blits this frame's tile.)
   */
  void rotateFrameState(RendererBinManager bins_last) {
    for (int j = 0; j < this.number_of_bins_y; j++) {
      for (int i = 0; i < this.number_of_bins_x; i++) {
        final RendererBin bin = this.array[i][j];
        final RendererBin last_bin = bins_last.array[i][j];

        final ArrayList<PolygonComposite> vector = bin.vector;
        bin.vector = last_bin.vector;
        last_bin.vector = vector;

        final RectangleInt actual = bin.actual;
        bin.actual = last_bin.actual;
        last_bin.actual = actual;

        final RectangleInt union = bin.union;
        bin.union = last_bin.union;
        last_bin.union = union;
      }
    }
  }

  /**
   * True when any render setting that affects bin pixels changed since last
   * frame. Everything else that affects pixels is either baked into the
   * polygon composites (caught by binsEqual) or goes through
   * repaint_all_objects (which resets the bins and invalidates the
   * settings).
   */
  private boolean renderSettingsChanged(boolean double_buffered) {
    return !this.render_settings_valid
        || this.last_double_buffered != double_buffered
        || this.last_colour_modifier_filled != colour_modifier_filled
        || this.last_colour_modifier_wireframe != colour_modifier_wireframe
        || this.last_colour_a_number != ColourModifier.colour_a_number
        || this.last_colour_b_number != ColourModifier.colour_b_number
        || this.last_redraw_deepest_first != FrEnd.redraw_deepest_first
        || this.last_show_bins != show_bins;
  }

  private void rememberRenderSettings(boolean double_buffered) {
    this.last_double_buffered = double_buffered;
    this.last_colour_modifier_filled = colour_modifier_filled;
    this.last_colour_modifier_wireframe = colour_modifier_wireframe;
    this.last_colour_a_number = ColourModifier.colour_a_number;
    this.last_colour_b_number = ColourModifier.colour_b_number;
    this.last_redraw_deepest_first = FrEnd.redraw_deepest_first;
    this.last_show_bins = show_bins;
    this.render_settings_valid = true;
  }

  /**
   * Exact per-bin content comparison against last frame. Early-out on the
   * first difference, so animating bins cost little; only truly static bins
   * pay the full walk. Insertion order is compared as-is: equivalent
   * content in a different order counts as dirty (a wasted redraw, never
   * a missed one).
   */
  static boolean binsEqual(ArrayList<PolygonComposite> v_this,
      ArrayList<PolygonComposite> v_last) {
    final int size = v_this.size();
    if (size != v_last.size()) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!compositesEqual(v_this.get(i), v_last.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean compositesEqual(PolygonComposite a,
      PolygonComposite b) {
    if (a.z != b.z) {
      return false;
    }
    final PolygonObject2D[] aa = a.array;
    final PolygonObject2D[] bb = b.array;
    final int n = aa.length;
    if (n != bb.length) {
      return false;
    }
    for (int i = 0; i < n; i++) {
      final PolygonObject2D pa = aa[i];
      final PolygonObject2D pb = bb[i];
      if (pa.colour != pb.colour) {
        return false;
      }
      final int[] ax = pa.x;
      final int[] ay = pa.y;
      final int[] bx = pb.x;
      final int[] by = pb.y;
      final int m = ax.length;
      if (m != bx.length || m != ay.length || m != by.length) {
        return false;
      }
      for (int k = 0; k < m; k++) {
        if (ax[k] != bx[k] || ay[k] != by[k]) {
          return false;
        }
      }
    }
    return true;
  }

  private void renderThePolygon(Graphics graphics,
      final PolygonComposite composite) {
    final int size = composite.array.length;
    if (colour_modifier_filled != 0) {
      for (int i = size; --i >= 0;) {
        final PolygonObject2D polygon = composite.array[i];
        final int colour = getModifiedColour(polygon.colour,
            colour_modifier_filled);
        polygon.fill(graphics, colour);
      }
    }

    if (colour_modifier_wireframe != 0) {
      // for (int i = 0; i < size; i++) {
      for (int i = size; --i >= 0;) {
        final PolygonObject2D polygon = composite.array[i];
        final int colour = getModifiedColour(polygon.colour,
            colour_modifier_wireframe);
        polygon.draw(graphics, colour);
      }
    }
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
      RendererBin bin) {
    final RectangleInt union = bin.union;
    graphics.setClip(union.min_x, union.min_y, union.max_x - union.min_x,
        union.max_y - union.min_y);
    scrubBin(graphics, potential.min_x, potential.min_y);
  }

  void scrubBin(Graphics graphics, int bin_min_x, int bin_min_y) {
    final int block_size = divisor - getMargin();

    // graphics.setColor(new Color(rnd.nextInt() & 0x7F7F7F));
    graphics.setColor(RendererDelegator.color_background);
    graphics.fillRect(bin_min_x, bin_min_y, block_size, block_size);
  }

  private int getMargin() {
    return RendererBinManager.show_bins ? 4 : 0;
  }

  private void getSortedNodeDepthIndex(final ArrayList<PolygonComposite> v_this,
      boolean deepest_first) {
    final int size = v_this.size();
    setUpNewNodeDepthIndex(size);

    if (deepest_first) {
      sort(v_this);
    }
  }

  private void setUpNewNodeDepthIndex(int number_of_nodes) {
    if (this.node_depth_index.length < number_of_nodes) {
      this.node_depth_index = new int[number_of_nodes];
    }
    final int[] index = this.node_depth_index;
    for (int temp = number_of_nodes; --temp >= 0;) {
      index[temp] = temp;
    }
  }

  // Stable bottom-up merge sort of the depth index, ascending by z.
  // (The caller draws the index from the end backwards, deepest first.)
  // Replaces the old O(n^2) bubble sort; same observable order.
  private void sort(ArrayList<PolygonComposite> vector) {
    final int size = vector.size();
    if (size < 2) {
      return;
    }
    if (this.sort_keys.length < size) {
      this.sort_keys = new int[size];
      this.sort_scratch = new int[size];
    }
    final int[] index = this.node_depth_index;
    final int[] keys = this.sort_keys;
    for (int i = size; --i >= 0;) {
      keys[i] = vector.get(i).z;
    }

    int[] src = index;
    int[] dst = this.sort_scratch;
    for (int width = 1; width < size; width <<= 1) {
      final int step = width << 1;
      for (int lo = 0; lo < size; lo += step) {
        int mid = lo + width;
        if (mid > size) {
          mid = size;
        }
        int hi = lo + step;
        if (hi > size) {
          hi = size;
        }
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
          // Strictly-less takes from the right run; ties take from the
          // left run, which keeps the sort stable.
          if (keys[src[j]] < keys[src[i]]) {
            dst[k++] = src[j++];
          } else {
            dst[k++] = src[i++];
          }
        }
        while (i < mid) {
          dst[k++] = src[i++];
        }
        while (j < hi) {
          dst[k++] = src[j++];
        }
      }
      final int[] temp = src;
      src = dst;
      dst = temp;
    }
    if (src != index) {
      System.arraycopy(src, 0, index, 0, size);
    }
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

  private int getBinX(int pixels) {
    if (pixels < 0) {
      return 0;
    }
    final int proposed = pixels / divisor;

    if (proposed >= this.number_of_bins_x) {
      if (proposed > 0) {
        return this.number_of_bins_x - 1;
      }
    }
    return proposed;
  }

  private int getBinY(int pixels) {
    if (pixels < 0) {
      return 0;
    }

    final int proposed = pixels / divisor;
    if (proposed >= this.number_of_bins_y) {
      if (proposed > 0) {
        return this.number_of_bins_y - 1;
      }
    }
    return proposed;
  }

  private int calcBinX(int pixels) {
    if (pixels < 0) {
      return 0;
    }
    final int proposed = pixels / divisor;
    return proposed;
  }

  private int calcBinY(int pixels) {
    if (pixels < 0) {
      return 0;
    }

    final int proposed = pixels / divisor;
    return proposed;
  }

  private int getPixelsFromBinX(int pixels) {
    return pixels * divisor;
  }

  private int getPixelsFromBinY(int pixels) {
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
