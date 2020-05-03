package com.springie.elements.base;

import java.util.ArrayList;

public class BaseElementManager {
  public ArrayList element = new ArrayList();

  public boolean each_has_its_own_type;

  public boolean each_has_its_own_clazz;

  public void reset() {
    this.element.clear();
    this.each_has_its_own_type = false;
    this.each_has_its_own_clazz = false;
  }
}
