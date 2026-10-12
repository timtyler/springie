package com.springie.io.out;

public final class WriteFloatingPoint {
  private WriteFloatingPoint() {
    //...
  }

  public static String emit(final float f) {
    return emit(f, 5, false);
  }

  public static String emit(final double f) {
    return emit(f, 5, false);
  }

  public static String emit(final double f, final int sf, final boolean fixed_dp) {
    String o = "" + f;
    if (o.indexOf("E") >= 0) {
      return "0";
    }

    if (o.indexOf(".") < 0) {
      o += ".";
    }

    o += "000000000000";

    final int actual_sf = (o.charAt(0) == '-') ? sf + 1 : sf;
    o = o.substring(0, actual_sf);

    if (fixed_dp) {
      return o;
    }

    while (o.endsWith("0")) {
      o = o.substring(0, o.length() - 1);
    }

    while (o.endsWith(".")) {
      o = o.substring(0, o.length() - 1);
    }

    return o;
  }
}
