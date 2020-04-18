// This code has been placed into the public domain by its author

package com.springie.gui.gestures;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

public class RotationManager {
  private int start_x;

  private int start_y;
  
  boolean cw_acw;

  Point3D[] pos;
  
  Point3D centre;

  public void initialise(int x, int y, boolean cw_acw) {
    this.cw_acw = cw_acw;
    if (FrEnd.button_virginity) {
      this.start_x = x;
      this.start_y = y;

      this.pos = new TransferUtilities().transferPositions();
      
      this.centre =  ContextMananger.getNodeManager().getCentre();

      FrEnd.forces_disabled_during_gesture = true;
    } else {
      performRotation(x, y);
    }
  }

  public void performRotation(int x, int y) {
    final int n = ContextMananger.getNodeManager().element.size();

    final float theta1 = (x - this.start_x) / (float) (160 * Coords.x_pixelso2);
    final float theta2 = (y - this.start_y) / (float) (160 * Coords.y_pixelso2);

    for (int i = n; --i >= 0;) {
      final Node node = (Node) ContextMananger.getNodeManager().element.elementAt(i);

      node.pos = this.pos[i];
      if (this.cw_acw) {
      rotateAboutZAxis(theta1, node);
      } else {
        rotateAboutYAxis(theta1, node);
        rotateAboutXAxis(theta2, node);
  }
    }
    
    RendererDelegator.repaint_some_objects = true;
  }

  private void rotateAboutYAxis(final float theta, final Node node) {
    final Point3D relative = (Point3D) node.pos.clone();
    relative.subtractTuple3D(this.centre);

    final int dx = (int) (relative.x * Math.cos(theta) - relative.z
      * Math.sin(theta));
    final int dy = relative.y;
    final int dz = (int) (relative.z * Math.cos(theta) + relative.x
      * Math.sin(theta));

    node.pos = new Point3D(dx, dy, dz);
    node.pos.addTuple3D(this.centre);
  }

  private void rotateAboutXAxis(final float theta, final Node node) {
    final Point3D relative = (Point3D) node.pos.clone();
    relative.subtractTuple3D(this.centre);

    final int dx = relative.x;
    final int dy = (int) (relative.y * Math.cos(theta) - relative.z
      * Math.sin(theta));
    final int dz = (int) (relative.z * Math.cos(theta) + relative.y
      * Math.sin(theta));

    node.pos = new Point3D(dx, dy, dz);
    node.pos.addTuple3D(this.centre);
  }
  
  private void rotateAboutZAxis(final float theta, final Node node) {
    final Point3D relative = (Point3D) node.pos.clone();
    relative.subtractTuple3D(this.centre);

    final int dx = (int) (relative.x * Math.cos(theta) + relative.y
        * Math.sin(theta));
    final int dy = (int) (relative.y * Math.cos(theta) - relative.x
      * Math.sin(theta));
    final int dz = relative.z;

    node.pos = new Point3D(dx, dy, dz);
    node.pos.addTuple3D(this.centre);
  }

  public void terminate(int x, int y) {
    if (this.pos != null) {
      performRotation(x, y);
      this.pos = null;
      FrEnd.forces_disabled_during_gesture = false;
    }
  }
}