// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class RendererBin {
  ArrayList<PolygonComposite> vector = new ArrayList<>();

  Image image;

  // Anti-aliased blit tile: the box-filtered 1x version of image, kept only
  // while anti-aliasing is above 1x1. While pixellation is above 1x1 this
  // holds the coarse (1/n resolution) tile instead.
  BufferedImage image_aa;

  // Pixellated blit tile: image_aa (or image, when anti-aliasing is off)
  // nearest-neighbour upsampled to the full bin size, kept only while
  // pixellation is above 1x1.
  BufferedImage image_px;

  RectangleInt actual = new RectangleInt(0, 0, 0, 0);

  RectangleInt union = new RectangleInt(0, 0, 0, 0);

  void setUpActual(RectangleInt potential) {
    final RectangleInt actual = this.actual;
    actual.min_x = Integer.MAX_VALUE;
    actual.min_y = Integer.MAX_VALUE;
    actual.max_x = Integer.MIN_VALUE;
    actual.max_y = Integer.MIN_VALUE;
    final ArrayList<PolygonComposite> vector = this.vector;
    final int size = vector.size();
    for (int c = size; --c >= 0;) {
      final PolygonComposite polygon = vector.get(c);
      final RectangleInt bb = polygon.getBoundingBox();
      if (bb.min_x < actual.min_x) {
        actual.min_x = bb.min_x;
      }
      if (bb.max_x > actual.max_x) {
        actual.max_x = bb.max_x;
      }
      if (bb.min_y < actual.min_y) {
        actual.min_y = bb.min_y;
      }
      if (bb.max_y > actual.max_y) {
        actual.max_y = bb.max_y;
      }

      if (actual.min_x <= potential.min_x) {
        if (actual.min_y <= potential.min_y) {
          if (actual.max_x >= potential.max_x) {
            if (actual.max_y >= potential.max_y) {
              break;
            }
          }
        }
      }
    }

    if (actual.min_x < potential.min_x) {
      actual.min_x = potential.min_x;
    }

    if (actual.min_y < potential.min_y) {
      actual.min_y = potential.min_y;
    }

    if (actual.max_x > potential.max_x) {
      actual.max_x = potential.max_x;
    }

    if (actual.max_y > potential.max_y) {
      actual.max_y = potential.max_y;
    }
  }
}
