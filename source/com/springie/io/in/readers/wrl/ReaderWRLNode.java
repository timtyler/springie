package com.springie.io.in.readers.wrl;

import com.springie.render.modules.modern.Double3D;

public class ReaderWRLNode {
  Double3D min;

  Double3D max;

  Double3D average;

  // double radius;

  //public ReaderWRLNode(Double3D min, Double3D max) {
  //  this.min = min;
  //  this.max = max;
 // }

  public ReaderWRLNode(Double3D min, Double3D max, Double3D average) {
    this.min = min;
    this.max = max;
    this.average = average;
  }

  public boolean intersects(ReaderWRLNode target) {
    final double this_delta_x = this.max.x - this.min.x;
    final double this_delta_y = this.max.y - this.min.y;
    final double this_delta_z = this.max.z - this.min.z;
    
    final double target_delta_x = target.max.x - target.min.x;
    final double target_delta_y = target.max.y - target.min.y;
    final double target_delta_z = target.max.z - target.min.z;

    final boolean intersect_x = intersect(this.min.x - this_delta_x, this.max.x
        + this_delta_x, target.min.x - target_delta_x, target.max.x + target_delta_x);
    final boolean intersect_y = intersect(this.min.y - this_delta_y, this.max.y
        + this_delta_y, target.min.y - target_delta_y, target.max.y + target_delta_y);
    final boolean intersect_z = intersect(this.min.z - this_delta_z, this.max.z
        + this_delta_z, target.min.z - target_delta_z, target.max.z + target_delta_z);

    return intersect_x && intersect_y && intersect_z;
  }

  private boolean intersect(double min_x_1, double max_x_1, double min_x_2,
      double max_x_2) {
    if (min_x_1 >= min_x_2) {
      if (min_x_1 <= max_x_2) {
        return true;
      }
    }
    if (min_x_2 >= min_x_1) {
      if (min_x_2 <= max_x_1) {
        return true;
      }
    }

    return false;
  }
}
