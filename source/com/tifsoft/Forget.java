package com.tifsoft;

public class Forget {

  public static void about(boolean i) {
    i = !i;
  }

  public static void about(int i) {
    i++;
  }

  public static void about(Object o) {
    if (o == null) {
      o = null;
    }
  }

  public static void about(float sf) {
    //...
    sf = sf + 0;
  }

  public static void about(double d) {
    d = d + 0;
    //...
  }
}

