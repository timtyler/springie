package com.springie.modification.translation;

import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.metrics.BoundingBox;
import com.springie.render.Coords;

public final class CentreOnScreen {
  private CentreOnScreen() {
  //...
  }
  
  public static void centre(NodeManager node_manager) {
    final BoundingBox bb = new BoundingBox();
    bb.find(node_manager);

    final Point3D max = bb.min;
    final Point3D min = bb.max;

    final int max_screen_size_x = Coords
      .getInternalFromPixelCoords(Coords.x_pixels);
    final int max_screen_size_y = Coords
      .getInternalFromPixelCoords(Coords.y_pixels);
    final int max_screen_size_z = Coords
      .getInternalFromPixelCoords(Coords.z_pixels);

    final Vector3D offset_actual = new Vector3D(0, 0, 0);

    offset_actual.x = (max_screen_size_x - max.x - min.x) >> 1;
    offset_actual.y = (max_screen_size_y - max.y - min.y) >> 1;
    offset_actual.z = (max_screen_size_z - max.z - min.z) >> 1;

    final int number_of_nodes = node_manager.element.size();
    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) node_manager.element.get(counter);
      candidate.pos.addTuple3D(offset_actual);
    }
  }

  /**
   * Translates every node so the model's bounding box sits central on
   * each selected axis -- a full snap, not a creep. Position only:
   * velocities are left untouched. Returns the translation applied, so
   * world-locked decoration (the Olympics location markers) can ride the
   * same shift.
   */
  public static Vector3D centreOnAxes(NodeManager node_manager,
      boolean centre_x, boolean centre_y, boolean centre_z) {
    final Vector3D offset = new Vector3D(0, 0, 0);
    final int number_of_nodes = node_manager.element.size();
    if (number_of_nodes == 0) {
      return offset;
    }

    int min_x = Integer.MAX_VALUE;
    int min_y = Integer.MAX_VALUE;
    int min_z = Integer.MAX_VALUE;
    int max_x = Integer.MIN_VALUE;
    int max_y = Integer.MIN_VALUE;
    int max_z = Integer.MIN_VALUE;

    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) node_manager.element.get(counter);
      if (candidate.pos.x < min_x) {
        min_x = candidate.pos.x;
      }
      if (candidate.pos.y < min_y) {
        min_y = candidate.pos.y;
      }
      if (candidate.pos.z < min_z) {
        min_z = candidate.pos.z;
      }
      if (candidate.pos.x > max_x) {
        max_x = candidate.pos.x;
      }
      if (candidate.pos.y > max_y) {
        max_y = candidate.pos.y;
      }
      if (candidate.pos.z > max_z) {
        max_z = candidate.pos.z;
      }
    }

    if (centre_x) {
      final int screen_x = Coords
        .getInternalFromPixelCoords(Coords.x_pixels);
      offset.x = (screen_x - min_x - max_x) >> 1;
    }
    if (centre_y) {
      final int screen_y = Coords
        .getInternalFromPixelCoords(Coords.y_pixels);
      offset.y = (screen_y - min_y - max_y) >> 1;
    }
    if (centre_z) {
      final int screen_z = Coords
        .getInternalFromPixelCoords(Coords.z_pixels);
      offset.z = (screen_z - min_z - max_z) >> 1;
    }

    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) node_manager.element.get(counter);
      candidate.pos.addTuple3D(offset);
    }

    return offset;
  }
}
