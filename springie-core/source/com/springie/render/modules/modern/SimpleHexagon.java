// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

/**
 * A flat, viewer-facing hexagon for node rendering. All six corners sit on
 * the unit circle in the z = 0 plane, wound like SimpleSquare, so the
 * backface test keeps it: nodes render as billboard hexagons.
 */
public class SimpleHexagon extends ObjectBase {
  public SimpleHexagon() {
    final double c = Math.sqrt(3) / 2;
    Double3D[] po = {
        new Double3D(0, 1, 0),
        new Double3D(-c, 0.5, 0),
        new Double3D(-c, -0.5, 0),
        new Double3D(0, -1, 0),
        new Double3D(c, -0.5, 0),
        new Double3D(c, 0.5, 0),
        };
    this.points = po;

    int[][] ia = {
        {0, 1, 2, 3, 4, 5 },
        };

    this.faces = ia;
  }
}
