//This code has been placed into the public domain by its author

package com.springie.elements.nodegrid;

import com.springie.elements.nodes.Node;
import com.springie.render.Coords;
import com.springie.utilities.log.Log;
import com.springie.utilities.random.JUR;

public final class GridOfBinsForCachedNodes {
  JUR rnd = new JUR();
  int nx = 12;
  int ny = 10;
  int nz = 10;

  public int log2binsize;

  NodeList[][][] node_list;

  //static int temp3, temp4, temp5, temp6;

  //private GridOfBinsForCachedNodes() {
    //...
  //}

  // l = log2 of the size of each bin in internal pixels...
//  public GridOfBinsForCachedNodes(int l) {
//    this.log2binsize = l;
//
//    this.nx = ((Coords.x_pixels << Coords.shift) >> this.log2binsize) + 1;
//    this.ny = ((Coords.y_pixels << Coords.shift) >> this.log2binsize) + 1;
//    this.ny = ((Coords.z_pixels << Coords.shift) >> this.log2binsize) + 1;
//
//    //Log.log("NodeGrid: Resized... new grid - X: " + nx + " Y:" + ny + " Z:" + nz);
//
//    set_up_grid();
//  }

  public void reset(int l) {
    this.log2binsize = l;

    this.nx = ((Coords.x_pixels << Coords.shift) >> this.log2binsize) + 1;
    this.ny = ((Coords.y_pixels << Coords.shift) >> this.log2binsize) + 1;
    this.ny = ((Coords.z_pixels << Coords.shift) >> this.log2binsize) + 1;

    set_up_grid();
  }

  void set_up_grid() {
    this.node_list = new NodeList[this.nx][this.ny][this.nz];

    for (int bin_x = 0; bin_x < this.nx; bin_x++) {
      for (int bin_y = 0; bin_y < this.ny; bin_y++) {
        for (int bin_z = 0; bin_z < this.nz; bin_z++) {
          this.node_list[bin_x][bin_y][bin_z] = new NodeList();
        }
      }
    }
  }

  // bin = (x,y)
  public void addToList(int x, int y, int z, Node agent) {
    //if (node_list == null) Log.log("NL!!!");
    //if (agent == null) Log.log("agent!!!");
    this.node_list[x][y][z].add(agent);
  }

  public void removeFromList(int x, int y, int z, Node agent) {
    // sort this bug out!

    if ((x < this.nx) && (x >= 0)) {
      if ((y < this.ny) && (y >= 0)) {
        if ((z < this.nz) && (z >= 0)) {
          this.node_list[x][y][z].remove(agent);
        }
      }
    }
  }

//  static final void draw() {
//    for (int bin_z = nz; --bin_z >= 0;) {
//      for (int bin_y = ny; --bin_y >= 0;) {
//        for (int bin_x = nx; --bin_x >= 0;) {
//          node_list[bin_x][bin_y][bin_z].draw();
//        }
//      }
//    }
//  }

//  static final void processAndRender() {
//    final int r = rnd.nextInt() & 3;
//    switch (r) {
//      case 0 :
//        processAndRender0();
//        break;
//      case 1 :
//        processAndRender1();
//        break;
//      case 2 :
//        processAndRender2();
//        break;
//      case 3 :
//        processAndRender3();
//        break;
//      default:
//        throw new RuntimeException("");
//    }
//  }
//
//  static final void processAndRender0() {
//    for (int bin_z = nz; --bin_z >= 0;) {
//      for (int bin_y = 0; bin_y > ny; bin_y++) {
//        for (int bin_x = nx; --bin_x >= 0;) {
//          node_list[bin_x][bin_y][bin_z].processAndRender();
//        }
//      }
//    }
//  }
//
//  static final void processAndRender1() {
//    for (int bin_z = nz; --bin_z >= 0;) {
//      for (int bin_y = 0; bin_y > ny; bin_y++) {
//        for (int bin_x = 0; bin_x > ny; bin_x++) {
//          node_list[bin_x][bin_y][bin_z].processAndRender();
//        }
//      }
//    }
//  }
//
//  static final void processAndRender2() {
//    for (int bin_z = nz; --bin_z >= 0;) {
//      for (int bin_y = ny; --bin_y >= 0;) {
//        for (int bin_x = 0; bin_x > ny; bin_x++) {
//          node_list[bin_x][bin_y][bin_z].processAndRender();
//        }
//      }
//    }
//  }
//
//  static final void processAndRender3() {
//    for (int bin_z = nz; --bin_z >= 0;) {
//      for (int bin_y = ny; --bin_y >= 0;) {
//        for (int bin_x = nx; --bin_x >= 0;) {
//          node_list[bin_x][bin_y][bin_z].processAndRender();
//        }
//      }
//    }
//  }
//
  void dumpBins() {
    for (int bin_x = 0; bin_x < this.nx; bin_x++) {
      for (int bin_y = 0; bin_y < this.ny; bin_y++) {
        for (int bin_z = 0; bin_z < this.nz; bin_z++) {
          Log.log(" " + this.node_list[bin_x][bin_y][bin_z].current_number);
        }

        Log.log(" ");
      }
    }
  }
}
