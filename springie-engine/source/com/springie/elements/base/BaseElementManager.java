package com.springie.elements.base;

import java.util.ArrayList;
import java.util.List;

public class BaseElementManager<T> {
  public List<T> element = new ArrayList<T>();

  public boolean each_has_its_own_type;

  public boolean each_has_its_own_clazz;

  public void reset() {
    this.element.clear();
    this.each_has_its_own_type = false;
    this.each_has_its_own_clazz = false;
  }
}
