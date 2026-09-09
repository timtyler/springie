package com.springie.elements.links;

import java.util.ArrayList;

public class LinkTypeFactory {
  public ArrayList array = new ArrayList<>();

  public LinkType getNew() {
    final LinkType type = new LinkType();
    this.array.add(type);
    return type;
  }

  public LinkType getNew(int length, int elasticity) {
    final LinkType type = new LinkType(length, elasticity);
    this.array.add(type);
    return type;
  }

  //  public static LinkType getNew(int l, int e, int c) {
  //    final LinkType type = new LinkType(l, e, c);
  //    array.add(type);
  //    return type;
  //  }
}
