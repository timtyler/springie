package com.springie.render.modules.modern;

public class RectangleInt {
  int min_x;
  int min_y;
  int max_x;
  int max_y;
  
  public RectangleInt(int min_x, int min_y, int max_x, int max_y) {
    super();
    this.min_x = min_x;
    this.min_y = min_y;
    this.max_x = max_x;
    this.max_y = max_y;
  }

  public void setToUnion(RectangleInt r1, RectangleInt r2) {
    this.min_x = Math.min(r1.min_x, r2.min_x);
    this.max_x = Math.max(r1.max_x, r2.max_x);
    this.min_y = Math.min(r1.min_y, r2.min_y);
    this.max_y = Math.max(r1.max_y, r2.max_y);
  }
}
