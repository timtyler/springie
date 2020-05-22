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

  public void reset(int l) {
    this.log2binsize = l;

    this.nx = ((Coords.x_pixels << Coords.shift) >> this.log2binsize) + 1;
    this.ny = ((Coords.y_pixels << Coords.shift) >> this.log2binsize) + 1;
    this.ny = ((Coords.z_pixels << Coords.shift) >> this.log2binsize) + 1;

    setUp();
  }

  void setUp() {
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
    if ((x < this.nx) && (x >= 0)) {
      if ((y < this.ny) && (y >= 0)) {
        if ((z < this.nz) && (z >= 0)) {
          this.node_list[x][y][z].remove(agent);
        }
      }
    }
  }

  void dumpBins() {
    for (int bin_x = 0; bin_x < this.nx; bin_x++) {
      for (int bin_y = 0; bin_y < this.ny; bin_y++) {
        for (int bin_z = 0; bin_z < this.nz; bin_z++) {
          Log.log(" " + this.node_list[bin_x][bin_y][bin_z].size());
        }

        Log.log(" ");
      }
    }
  }
}
