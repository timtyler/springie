//This code has been placed into the public domain by its author

package com.springie.elements.faces;

import java.util.Vector;

import com.springie.elements.base.BaseElement;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;

public class Face extends BaseElement {
  public Vector nodes;

  public FaceType type;

  public static int face_display_type = FaceRenderTypes.CONCENTRIC;

  public static int number_of_render_divisions = 4;

  public Face(Vector v, FaceType type, Clazz clazz) {
    this.nodes = v;
    this.type = type;
    this.clazz = clazz;
  }

  public Face(Vector v) {
    this.nodes = v;
  }

  public boolean hasExactlyTheseNodes(Vector node_list) {
    final int n_points = this.nodes.size();
    final int l_points = node_list.size();
    if (n_points != l_points) {
      return false;
    }

    int count = 0;

    for (int i = n_points; --i >= 0;) {
      final Node n1 = (Node) this.nodes.elementAt(i);

      for (int j = l_points; --j >= 0;) {
        final Node n2 = (Node) node_list.elementAt(j);
        if (n1 == n2) {
          count++;
        }
      }
    }

    if (count == n_points) {
      return true;
    }

    return false;
  }

  public boolean containsNodes(Node e) {
    final int n_points = this.nodes.size();

    for (int i = n_points; --i >= 0;) {
      final Node n1 = (Node) this.nodes.elementAt(i);

      if (n1 == e) {
        return true;
      }
    }

    return false;
  }

  public Point3D getCoordinatessOfCentre() {
    final Point3D centre = new Point3D(0, 0, 0);

    final int npoints = this.nodes.size();

    for (int i = npoints; --i >= 0;) {
      final Node n = (Node) this.nodes.elementAt(i);

      centre.addTuple3D(n.pos);
    }

    centre.divideBy(npoints);
    return centre;
  }
  
  public boolean isHidden() {
    return this.type.hidden;
  }
  
  public boolean isSelected() {
    return this.type.selected;
  }
  
  public void setSelected(boolean selected) {
    this.type.selected = selected;
  }

  public void setSelectedFiltered(boolean selected) {
    this.type.selected = selected;    
  }

  public Point3D getCoordinatesOfCentrePoint() {
    final Node n = (Node) this.nodes.elementAt(0);
    return  new Point3D(n.pos);
  }
}
