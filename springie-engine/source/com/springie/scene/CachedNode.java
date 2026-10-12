// This code has been placed into the public domain by its author

package com.springie.scene;
import com.springie.geometry.Coords;
import com.springie.world.World;

import com.springie.elements.DeepObjectColourCalculator;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;

public class CachedNode {
  public Point3D pos = new Point3D(0, 0, 0);

  public int radius;

  public int colour;

  public int selected_colour;

  public boolean selected;


  public void cache(final Node node, final int mask) {
    if (!World.render_hidden_nodes && node.type.hidden) {
      return;
    }

    this.pos.x = Coords.getXCoords(node.pos.x, node.pos.z);
    this.pos.y = Coords.getYCoords(node.pos.y, node.pos.z);
    this.radius = Coords.getRadius(node.type.radius, node.pos.z);
    this.selected = node.type.selected;
    //this.charge = node.type.charge;

    this.colour = DeepObjectColourCalculator.getColourOfDeepObject(
      node.clazz.colour, node.pos.z)
      & mask;

    if (this.selected) {
      this.selected_colour = DeepObjectColourCalculator.getColourOfDeepObject(
        0xFF4040, this.pos.z)
        & mask;
    }
  }


  
}
