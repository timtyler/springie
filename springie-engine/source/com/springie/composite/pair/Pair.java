package com.springie.composite.pair;
import com.springie.elements.nodes.Node;

class Pair {
  Node node1;
  Node node2;

  Pair(final Node e1, final Node e2) {
    this.node1 = e1;
    this.node2 = e2;
  }

  void set(final Node e1, final Node e2) {
    this.node1 = e1;
    this.node2 = e2;
  }

  // DANGER
  void set(final Pair l) {
    this.node1 = l.node1;
    this.node2 = l.node2;
  }

  final Node theOtherMember(final Node e) {
    if (this.node1 == e) {
      return this.node2;
    }

    return this.node1;
  }

  static final void debug(final String o) {
    System.out.println(o);
  }

}
