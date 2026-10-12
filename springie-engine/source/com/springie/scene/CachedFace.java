// This code has been placed into the public domain by its author

package com.springie.scene;
import com.springie.geometry.Coords;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.faces.Face;
import com.springie.elements.nodes.Node;
import java.awt.Point;
import java.util.ArrayList;

public class CachedFace {

  public ArrayList<Point> render = new ArrayList<>();

  public Point render_centre = new Point(0, 0);

  public int colour;

  public int selected_colour;

  public boolean selected;


  public void cache(final Face face, final int mask) {
    final Node n1 = (Node) face.nodes.get(0);

    this.colour = DeepObjectColourCalculator.getColourOfDeepObject(
      face.clazz.colour, n1.pos.z)
      & mask;

    this.selected_colour = DeepObjectColourCalculator.getColourOfDeepObject(
      0xFF4040, n1.pos.z)
      & mask;

    final int npoints = face.nodes.size();
    final int rpoints = this.render.size();
    if (rpoints != npoints) {
      setUpRenderingScratchSpace(face);
    }

    getCoordsOfCentre(face);

    this.selected = face.type.selected;

    //final Node n1 = (Node) face.node.get(0);
    this.selected_colour = DeepObjectColourCalculator.getColourOfDeepObject(
      0xFF2020, n1.pos.z)
      & mask;
  }

  private void getCoordsOfCentre(final Face face) {
    final int npoints = face.nodes.size();

    int sum_x = 0;
    int sum_y = 0;

    for (int i = npoints; --i >= 0;) {
      final Node n = (Node) face.nodes.get(i);
      final Point scratch = (Point) this.render.get(i);

      scratch.x = Coords.getXCoords(n.pos.x, n.pos.z);
      scratch.y = Coords.getYCoords(n.pos.y, n.pos.z);
      sum_x += scratch.x;
      sum_y += scratch.y;
    }

    this.render_centre.x = sum_x / npoints;
    this.render_centre.y = sum_y / npoints;
  }


  public void setUpRenderingScratchSpace(final Face face) {
    if (face.nodes != null) {
      this.render = new ArrayList<>();

      final int npoints = face.nodes.size();
      for (int i = npoints; --i >= 0;) {
        this.render.add(new Point(0, 0));
      }
    }
  }

}
