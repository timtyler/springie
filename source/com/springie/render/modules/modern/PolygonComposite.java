// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

public class PolygonComposite {
  PolygonObject2D[] array;

  int z;

  RectangleInt bounding_box;

  public PolygonComposite(PolygonObject2D[] array, int z) {
    super();
    this.array = array;
    this.z = z;
  }

  public RectangleInt getBoundingBox() {
    if (this.bounding_box == null) {
      this.bounding_box = new RectangleInt(Integer.MAX_VALUE,
          Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
      final int size = this.array.length;
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
