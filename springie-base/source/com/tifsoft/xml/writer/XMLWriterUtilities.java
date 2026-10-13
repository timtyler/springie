package com.tifsoft.xml.writer;

public class XMLWriterUtilities {
  public static void indent(final StringBuilder sb, int indent) {
    sb.append(repeat(' ', indent));
  }

  public static String repeat(final char character, final int count) {
    final StringBuilder stringBuffer = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      stringBuffer.append(character);
    }
    return stringBuffer.toString();
  }

}
