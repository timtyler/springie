// * Read in information from files...

package com.springie.io.in.readers.dat;

import java.util.StringTokenizer;

import com.springie.presets.ColourFactory;
import com.springie.utilities.log.Log;

public final class ReaderDAT {

  private ReaderDAT() {
    // ...
  }

  public static String translate(String in) {
    final StringTokenizer st = new StringTokenizer(in);

    final StringBuffer out = parseTheFile(st);

    return out.toString();
  }

  private static StringBuffer parseTheFile(final StringTokenizer st) {
    final int sf = 32000;
    final int[] colours = new ColourFactory(65387).getColourArray(64);
    final StringBuffer out = new StringBuffer();
    String tok;
    out.append("CR NG R:0 C:0x0 ");
    do {
      tok = st.nextToken();
      switch (tok.charAt(0)) {
        case 'v':
          if (tok.length() < 3) {
            final String st_x = st.nextToken();
            if (isANumber(st_x)) {
              final String st_y = st.nextToken();
              if (isANumber(st_y)) {
                final String st_z = st.nextToken();
                if (isANumber(st_z)) {
                  final int x = (int) (Double.valueOf(st_x).doubleValue() * sf);
                  final int y = -(int) (Double.valueOf(st_y).doubleValue() * sf);
                  final int z = (int) (Double.valueOf(st_z).doubleValue() * sf);

                  out.append("N X:" + x + " Y:" + y + " Z:" + z + " ");
                  Log.log("N X:" + x + " Y:" + y + " Z:" + z + " ");
                }
              }
            }
          }
          break;

        case 'l':
          if (tok.length() < 3) {
            final String st_a = st.nextToken();
            if (isANumber(st_a)) {
              final String st_b = st.nextToken();
              if (isANumber(st_b)) {
                final String st_c = st.nextToken();
                if (isANumber(st_c)) {
                  final int i_a = Integer.parseInt(st_a);
                  final int i_b = Integer.parseInt(st_b);
                  final int i_c = Integer.parseInt(st_c);
                  final boolean link = (i_c & 7) != 1;
                  out.append("LG L:500 ");
                  out.append("E:80 C:0xFF");
                  final int colour = (colours[i_c % 7] + i_c) | 0x808080;
                  out.append("" + Integer.toString(colour, 16) + " ");
                  if (link) {
                    out.append("CP:0 ");
                  }

                  out.append("LK V:" + i_a + " V:" + i_b + " ");
                  Log.log("LK V:" + i_a + " V:" + i_b + " ");
                }
              }
            }
          }
          break;

        default:
          // do nothing..
          break;
      }
    } while (st.hasMoreTokens());

    return out;
  }

  static boolean isANumber(String s) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (!couldBeInANumber(c)) {
        return false;
      }
    }
    return true;
  }

  static boolean couldBeInANumber(char c) {
    if (c == '-') {
      return true;
    }
    if (c == '.') {
      return true;
    }
    if (c > '9') {
      return false;
    }
    if (c < '0') {
      return false;
    }
    return true;
  }
}