// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.render.RectangleInt;

public class PolygonComposite {
  PolygonObject2D[] array;

  /**
   * How many leading entries of array are live this frame. The link
   * renderer reuses its quad arrays across frames and compacts the
   * front-facing quads in place, so array.length is the capacity while
   * count is the live prefix. Freshly built composites have
   * count == array.length.
   */
  int count;

  int z;

  RectangleInt bounding_box;

  public PolygonComposite(PolygonObject2D[] array, int z) {
    super();
    this.array = array;
    this.count = array.length;
    this.z = z;
  }

  public RectangleInt getBoundingBox() {
    if (this.bounding_box == null) {
      this.bounding_box = new RectangleInt(Integer.MAX_VALUE,
          Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
      final int size = this.count;
      for (int i = 0; i < size; i++) {
        final PolygonObject2D polygon = this.array[i];
        final RectangleInt bounding = polygon.getBoundingBox();
        if (bounding.min_x < this.bounding_box.min_x) {
          this.bounding_box.min_x = bounding.min_x;
        }
        if (bounding.max_x > this.bounding_box.max_x) {
          this.bounding_box.max_x = bounding.max_x;
        }
        if (bounding.min_y < this.bounding_box.min_y) {
          this.bounding_box.min_y = bounding.min_y;
        }
        if (bounding.max_y > this.bounding_box.max_y) {
          this.bounding_box.max_y = bounding.max_y;
        }
      }
    }
    return this.bounding_box;
  }
}
