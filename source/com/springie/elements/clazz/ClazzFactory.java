package com.springie.elements.clazz;

import java.util.ArrayList;
import java.util.List;

public class ClazzFactory {
  public List<Clazz> array = new ArrayList<>();

  public Clazz getNew(int colour) {
    final Clazz clazz = new Clazz(colour);
    this.array.add(clazz);
    return clazz;
  }
}
