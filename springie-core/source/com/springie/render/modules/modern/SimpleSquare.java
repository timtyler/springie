// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

/**
 * A flat, viewer-facing square for node rendering. All four corners sit in
 * the z = 0 plane, wound like the viewer-facing (-z) face of SimpleCube,
 * so the backface test keeps it: nodes render as billboard squares.
 */
public class SimpleSquare extends ObjectBase {
  public SimpleSquare() {
    Double3D[] po = {
        new Double3D(-1, 1, 0),
        new Double3D(1, 1, 0),
        new Double3D(1, -1, 0),
        new Double3D(-1, -1, 0),
        };
    this.points = po;

    int[][] ia = {
        {0, 3, 2, 1 },
        };

    this.faces = ia;
  }
}
