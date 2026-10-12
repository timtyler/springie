package com.springie.elements.base;

import com.springie.elements.clazz.Clazz;
import com.springie.geometry.Point3D;

public abstract class BaseElement {
  public Clazz clazz = new Clazz(0xffff8000);
  public String name;
  public abstract boolean isHidden();
  public abstract boolean isSelected();
  public abstract void setSelected(boolean selected);
  public abstract void setSelectedFiltered(boolean selected);
  public abstract Point3D getCoordinatesOfCentrePoint();
}
