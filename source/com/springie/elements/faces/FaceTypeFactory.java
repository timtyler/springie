package com.springie.elements.faces;

import java.util.ArrayList;

public class FaceTypeFactory {
  public ArrayList array = new ArrayList<>();

  public FaceType getNew() {
    final FaceType type = new FaceType();
    this.array.add(type);
    return type;
  }
}
