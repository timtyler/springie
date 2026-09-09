package com.tifsoft.xml.writer;

public class XMLWriterUtilities {
  public static void indent(StringBuilder sb, int indent) {
    sb.append(repeat(' ', indent));
  }

  public static String repeat(char character, int count) {
    final StringBuilder stringBuffer = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      stringBuffer.append(character);
    }
    return stringBuffer.toString();
  }

}