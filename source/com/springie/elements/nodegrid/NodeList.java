//This code has been placed into the public domain by its author

package com.springie.elements.nodegrid;

import com.springie.elements.nodes.Node;

public class NodeList {
  public Node[] node;

  int max_number_in_list;
  public int current_number;
  int last_updated;

  static int agent_counter;

  static int MAX_ENTITY_NUMBER = 40; // per

  // bin...

  public NodeList() {
    this.max_number_in_list = 0;
    this.current_number = 0;
    this.node = new Node[this.max_number_in_list]; // nulls
    // ATM...
  }

  //static final void set_up_grid() {
  //}

  public void add(Node e) {
    if (this.current_number >= this.max_number_in_list) {
      Node[] new_node = new Node[++this.max_number_in_list];

      if (this.max_number_in_list > 1) {
        for (int temp = 0; temp < (this.max_number_in_list - 1); temp++) {
          new_node[temp] = this.node[temp];
        }
      }

      this.node = new_node;
    }

    this.node[this.current_number++] = e;
  }

  int getNumberInList(Node e) {
    for (int temp = 0; temp < this.current_number; temp++) {
      if (this.node[temp] == e) {
        return temp;
      }
    }

    return -1;
  }

  // 3D version needs following method updated... ***
  public void remove(Node e) {
    final int temp = getNumberInList(e);
    //if (current_number > 0) {
    // temp = 0;
    //while ((node[temp] != e) && (temp < (current_number - 1))) {
    //temp++;
    //}

    // if (temp < current_number) {
    if (temp >= 0) {
      this.node[temp] = this.node[--this.current_number];
    }
    //}
    //else
    //{
    // error!
    // debug("ERROR 123");
    //}
  }

//  public void draw() {
//    for (int temp = 0; temp < this.max_number_in_list; temp++) {
//      this.node[temp].draw();
//    }
//  }

//  public void processAndRender() {
//    for (int counter = 0; counter < this.max_number_in_list; counter++) {
//      BinGrid.colourZero();
//      this.node[counter].scrub();
//
//      if (!FrEnd.paused) {
//        this.node[counter].travel();
//      }
//
//      this.node[counter].draw();
//    }
//  }
}
