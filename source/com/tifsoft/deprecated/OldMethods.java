package com.tifsoft.deprecated;

import java.awt.Component;

public class OldMethods {

  public int getWidth(Component c) {
    return c.getSize().width;
  }

  public int getHeight(Component c) {
    return c.getSize().height;
  }

  public static boolean isInsidePolygon(int x, int y, final java.awt.Polygon polygon) {
    return polygon.contains(x, y);
  }
}