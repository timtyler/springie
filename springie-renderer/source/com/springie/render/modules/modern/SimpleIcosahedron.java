// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

public class SimpleIcosahedron extends ObjectBase {
  public SimpleIcosahedron() {
    Double3D[] po = {
        new Double3D(-0.68345, 0, 1.10585),
        new Double3D(0.68345, 0, 1.10585),
        new Double3D(-0.68345, 0, -1.10585),
        new Double3D(0.68345, 0, -1.10585),
        new Double3D(0, 1.10585, 0.68345),
        new Double3D(0, 1.10585, -0.68345),
        new Double3D(0, -1.10585, 0.68345),
        new Double3D(0, -1.10585, -0.68345),
        new Double3D(1.10585, 0.68345, 0),
        new Double3D(-1.10585, 0.68345, 0),
        new Double3D(1.10585, -0.68345, 0),
        new Double3D(-1.10585, -0.68345, 0), };
    this.points = po;

    int[][] ia = {{0, 4, 1 }, {0, 9, 4 }, {4, 9, 5 }, {4, 5, 8 },
        {1, 4, 8 }, {1, 8, 10 }, {3, 10, 8 }, {3, 8, 5 }, {2, 3, 5 },
        {2, 7, 3 }, {3, 7, 10 }, {6, 10, 7 }, {6, 7, 11 }, {0, 6, 11 },
        {0, 1, 6 }, {1, 10, 6 }, {0, 11, 9 }, {2, 9, 11 }, {2, 5, 9 },
        {2, 11, 7 }, };

    this.faces = ia;
  }
}
