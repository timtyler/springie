package com.tifsoft.xml.driver;
public class SomeStringUtilities {
  static boolean isWhiteSpace(final char[] ca, final int n) {
    for (int i = 0; i < n; i++) {
      if (ca[i] > ' ') {
        return false;
      }
    }

    return true;
  }

  public static boolean isWhiteSpace(final String string) {
    if (string == null) {
      return true;
    }
    
    final char[] ca = string.toCharArray();

    return isWhiteSpace(ca, ca.length);
  }
  
  static boolean isXMLElementPart(final char c) {
    if (c == '-') {
      return true;
    }
    if (c == ':') {
      return true;
    }

    return Character.isJavaIdentifierPart(c);
  }

}
