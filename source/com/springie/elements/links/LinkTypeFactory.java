package com.springie.elements.links;

import java.util.Vector;

public class LinkTypeFactory {
  public Vector array = new Vector();

  public LinkType getNew() {
    final LinkType type = new LinkType();
    this.array.addElement(type);
    return type;
  }

  public LinkType getNew(int length, int elasticity) {
    final LinkType type = new LinkType(length, elasticity);
    this.array.addElement(type);
    return type;
  }

  //  public static LinkType getNew(int l, int e, int c) {
  //    final LinkType type = new LinkType(l, e, c);
  //    array.addElement(type);
  //    return type;
  //  }
}
