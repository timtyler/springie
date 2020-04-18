// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

public class SimpleOctahedron extends ObjectBase {
  public SimpleOctahedron() {
    Double3D[] po = {
        new Double3D(0, 0, 1),
        new Double3D(1, 1, 0),
        new Double3D(-1, 1, 0),
        new Double3D(-1, -1, 0),
        new Double3D(1, -1, 0),
        new Double3D(0, 0, -1) };
    this.points = po;

    int[][] ia = {{0, 2, 1 }, {0, 3, 2 }, {0, 4, 3 }, {0, 1, 4 },
        {5, 1, 2 }, {5, 2, 3 }, {5, 3, 4 }, {5, 4, 1 } };

    this.faces = ia;
  }
}
