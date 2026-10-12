// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

public class SimpleCube extends ObjectBase {
  public SimpleCube() {
    Double3D[] po = {
        new Double3D(-1, 1, 1),
        new Double3D(1, 1, 1),
        new Double3D(1, -1, 1),
        new Double3D(-1, -1, 1),
        new Double3D(-1, 1, -1),
        new Double3D(1, 1, -1),
        new Double3D(1, -1, -1),
        new Double3D(-1, -1, -1),
        };
    this.points = po;

    int[][] ia = {
        {0, 1, 2, 3 },
        {1, 0, 4, 5 },
        {2, 1, 5, 6 },
        {3, 2, 6, 7 },
        {0, 3, 7, 4 },
        {7, 6, 5, 4 },
        };

    this.faces = ia;
  }
}
