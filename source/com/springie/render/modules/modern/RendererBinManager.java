// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Graphics;
import java.util.Random;
import java.util.Vector;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.render.RendererDelegator;

public class RendererBinManager {
  public static int divisor = 192;

  int number_of_bins_x;

  int number_of_bins_y;

  private RendererBin[][] array;

  private int[] node_depth_index;

  static Random rnd = new Random();

  public static boolean show_bins;

  public static int colour_modifier_filled = ColourModifier.natural;

  public static int colour_modifier_wireframe = ColourModifier.darker;

  Vector getVector(int x, int y) {
    return this.array[x][y].vector;
  }

  void putVector(int x, int y, Vector vector) {
    this.array[x][y].vector = vector;
  }

  void clear() {
    // Log.log("BinManager.clear");
    for (int i = 0; i < this.number_of_bins_x; i++) {
      for (int j = 0; j < this.number_of_bins_y; j++) {
        this.array[i][j].vector.setSize(0);
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
  }

  void add(int x, int y, PolygonComposite triangle) {
    final Vector v = getVector(x, y);
    v.addElement(triangle);
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
    ContextMananger.getNodeManager().depth_range = null;
    
    final int block_size = divisor - getMargin();

    final RectangleInt potential = new RectangleInt(0, 0, 0, 0);

    for (int j = 0; j < this.number_of_bins_y; j++) {
      for (int i = 0; i < this.number_of_bins_x; i++) {
        final RendererBin bin = this.array[i][j];
        final Vector v_this = bin.vector;
        final int size = v_this.size();

        final RendererBin last_bin = bins_last.array[i][j];
        final Vector v_last = last_bin.vector;
        final int size_last = v_last.size();

        final boolean size_last_gt_0 = size_last > 0;
        final boolean size_gt_0 = size > 0;

        if (size_last_gt_0 || size_gt_0) {
          potential.min_x = getPixelsFromBinX(i);
          potential.min_y = getPixelsFromBinY(j);
          potential.max_x = potential.min_x + block_size;
          potential.max_y = potential.min_y + block_size;

          bin.setUpActual(potential);
          bin.union.setToUnion(bin.actual, last_bin.actual);

          Graphics graphics_paint = null;

          if (RendererDelegator.isNewDoubleBuffer()) {
            if (bin.image == null) {
              final int width = block_size;
              final int height = block_size;
              FrEnd.main_canvas.panel
                  .setBackground(RendererDelegator.color_background);
              bin.image = FrEnd.main_canvas.createImage(width, height);
            }
            graphics_paint = bin.image.getGraphics();
            graphics_paint.translate(-potential.min_x, -potential.min_y);
          } else {
            bin.image = null;
            graphics_paint = graphics;
          }

          if (graphics_paint != null) {
            if (size > 0) {
              if (size_last > 0) {
                doScrubbing(graphics_paint, potential, bin);
              }

              getSortedNodeDepthIndex(v_this);

              graphics_paint.setClip(potential.min_x, potential.min_y,
                  block_size, block_size);

              for (int c = size; --c >= 0;) {
                final int index = this.node_depth_index[c];
                final PolygonComposite composite = (PolygonComposite) v_this
                    .elementAt(index);

                renderThePolygon(graphics_paint, composite);
              }
            }
          }
        }
        if (size < 1) {
          if (bin.image != null) {
            doScrubbing(graphics, potential, bin);
            bin.image = null;
          }
        }
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

  private void getSortedNodeDepthIndex(final Vector v_this) {
    final int size = v_this.size();
    setUpNewNodeDepthIndex(size);

    if (FrEnd.redraw_deepest_first) {
      sort(v_this);
    }
  }

  private void setUpNewNodeDepthIndex(int number_of_nodes) {
    this.node_depth_index = new int[number_of_nodes];
    for (int temp = number_of_nodes; --temp >= 0;) {
      this.node_depth_index[temp] = temp;
    }
  }

  private void sort(Vector vector) {
    // perform a dimwitted bubble sort... TODO improve sort...
    final int number_of_nodes = vector.size();

    for (int i = number_of_nodes - 1; --i >= 0;) {
      boolean flipped = false;
      for (int j = 0; j <= i; j++) {
        final int k = j + 1;
        final int j1 = this.node_depth_index[j];
        final int k1 = this.node_depth_index[k];
        final PolygonComposite a = (PolygonComposite) vector.elementAt(j1);
        final PolygonComposite b = (PolygonComposite) vector.elementAt(k1);
        if (a.z > b.z) {
          int temp = this.node_depth_index[j];
          this.node_depth_index[j] = this.node_depth_index[k];
          this.node_depth_index[k] = temp;

          flipped = true;
        }
      }

      if (!flipped) {
        return;
      }
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
