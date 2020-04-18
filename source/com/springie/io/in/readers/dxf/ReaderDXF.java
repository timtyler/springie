// * Read in information from files...

package com.springie.io.in.readers.dxf;

import java.util.StringTokenizer;
import java.util.Vector;

import com.springie.presets.ColourFactory;
import com.springie.utilities.log.Log;

public class ReaderDXF {
  Vector nodes = new Vector();

  int scale_factor = 3000;

  int node_number;

  boolean first_node = true;

  public String translate(String in) {
    final byte[] ba = {10, 13 };
    final String c_r = new String(ba);

    final StringTokenizer st = new StringTokenizer(in, ", [](){}" + c_r);

    final StringBuffer out = parseTheFile(st);

    return out.toString();
  }

  private StringBuffer parseTheFile(final StringTokenizer st) {
    final StringBuffer out = new StringBuffer();
    extractNodes(st, out);

    Log.log("" + out);

    return out;
  }

  class Stage {
    // ...
  }

  private void extractNodes(final StringTokenizer st, final StringBuffer out) {
    String token = null;
    // final int colour = 0xFFFFB0FF;
    double radius = 0;
    int colour_this = 0xFFFFB0FF;

    final int[] colours = new ColourFactory(52746).getColourArray(16);

    out.append("CR NG R:0 C:0xFFFFB0FF ");

    token = getNextValidToken(st);

    boolean insert_found = false;
    //boolean circle_found = false;
    final boolean entities_found = true;

    Stage state = null;

    final Stage state_line = new Stage();
    final Stage state_circle = new Stage();
    final Stage state_block = new Stage();
    final Stage state_insert = new Stage();
    // final Stage state_block = new Stage();

    int count = 0;

    double xd = 0;
    double yd = 0;
    double zd = 0;

    double sf_x = 0;
    double sf_y = 0;
    double sf_z = 0;

    do {
      token = getNextValidToken(st);

      //if (token.charAt(0) >= 'A') {
        //if (token.length() > 3) {
          // Log.log(token);
        //}
      //}

      if ("EOF".equals(token)) {
        return;
      }

      if ("INSERT".equals(token)) {
        state = state_insert;
        insert_found = true;
      }

      if (insert_found) {
        if ("BLOCK".equals(token)) {
          state = state_block;
        }

        //if ("*U49".equals(token)) {
          //entities_found = true;
        //}

        if (entities_found) {
        if ("CIRCLE".equals(token)) {
          state = state_circle;
          //circle_found = true;
        }

          if ("LINE".equals(token)) {
            state = state_line;
          }
        }
      }

      if (state == state_line) {
        if (token.equals("62")) {
          final String temp = getNextValidToken(st);
          final int cv = Integer.parseInt(temp);
          colour_this = colours[cv] | 0x808080;
        } else if (token.equals("10") || token.equals("11")) {
          final String temp = getNextValidToken(st);
          xd = Double.valueOf(temp).doubleValue();
        } else if (token.equals("20") || token.equals("21")) {
          final String temp = getNextValidToken(st);
          yd = Double.valueOf(temp).doubleValue();
        } else if (token.equals("30") || token.equals("31")) {
          final String temp = getNextValidToken(st);
          zd = Double.valueOf(temp).doubleValue();
          final int x = (int) (xd * this.scale_factor);
          final int y = (int) (yd * this.scale_factor);
          final int z = (int) (zd * this.scale_factor);
          out.append("NG R:" + 0 + " ");
          out
              .append("C:0xFF" + Long.toString(colour_this & 0xFFFFFF, 16)
                  + " ");
          out.append("N X:" + x + " Y:" + y + " Z:" + z + " ");
          count++;
          if (token.equals("31")) {

            out.append("LG R:" + 0 + " ");
            out.append("C:0xFF" + Long.toString(colour_this & 0xFFFFFF, 16)
                + " ");

            out.append("LK V:" + (count - 2) + " V:" + (count - 1) + " ");
            state = null;
          }
        }
      }

      if (state == state_circle) {
        if (token.equals("62")) {
          final String temp = getNextValidToken(st);
          final int cv = Integer.parseInt(temp);
          if (cv == 2) {
            state = null;
          }
          colour_this = colours[cv] | 0x808080;
        } else if (token.equals("10")) {
          final String temp = getNextValidToken(st);
          xd = Double.valueOf(temp).doubleValue();
        } else if (token.equals("20")) {
          final String temp = getNextValidToken(st);
          yd = Double.valueOf(temp).doubleValue();
        } else if (token.equals("30")) {
          final String temp = getNextValidToken(st);
          zd = Double.valueOf(temp).doubleValue();
        } else if (token.equals("40")) {
          final String temp = getNextValidToken(st);
          radius = Double.valueOf(temp).doubleValue();
        } else if (token.equals("210")) {
          final String temp = getNextValidToken(st);
          sf_x = Double.valueOf(temp).doubleValue();
        } else if (token.equals("220")) {
          final String temp = getNextValidToken(st);
          sf_y = Double.valueOf(temp).doubleValue();
        } else if (token.equals("230")) {
          final String temp = getNextValidToken(st);
          sf_z = Double.valueOf(temp).doubleValue();
          final ReaderDXFPoint xtruDir = new ReaderDXFPoint(sf_x, sf_y, sf_z);
          final ReaderDXFPoint Ax = new ReaderDXFPoint();
          final ReaderDXFPoint Ay = new ReaderDXFPoint();
          final ReaderDXFPoint Az = new ReaderDXFPoint();
          Az.set(xtruDir);
          Az.normalize();
          ReaderDXFPoint.calcAAA(Ax, Ay, Az);

          final ReaderDXFMatrix M_circle = new ReaderDXFMatrix();

          M_circle.mtxRotateAxes_World_to_Local(Ax, Ay, Az);

          final ReaderDXFPoint input = new ReaderDXFPoint(xd, yd, zd);
          final ReaderDXFPoint result = new ReaderDXFPoint();
          M_circle.mtxTransformPoint(input, result);

          final int x = (int) (result.x * this.scale_factor);
          final int y = (int) (result.y * this.scale_factor);
          final int z = (int) (result.z * this.scale_factor);
          final int r = (int) (radius * this.scale_factor);

          //final int x = (int) (xd  * this.scale_factor);
          //final int y = (int) (yd * this.scale_factor);
          //final int z = (int) (zd * this.scale_factor);

          out.append("NG R:" + r + " ");
          out
              .append("C:0xFF" + Long.toString(colour_this & 0xFFFFFF, 16)
                  + " ");
          out.append("N X:" + x + " Y:" + y + " Z:" + z + " ");
          count++;
          state = null;
          //Log.log("X:" + x + "  Y:" + y + "  Z:" + z);
        }
      }
    } while (true);
  }

  static String getNextValidToken(StringTokenizer st) {
    boolean found;
    String tok;
    do {
      tok = st.nextToken();

      while ((tok.length() > 1) && (tok.charAt(0) < 33)) {
        tok = tok.substring(1);
      }

      found = true;
      if (tok.length() < 1) {
        found = false;
      }
      if ("".equals(tok)) {
        found = false;
      }
    } while (!found);

    final int i = tok.indexOf("*^");

    if (i > 0) {
      tok = tok.substring(0, i) + "E" + tok.substring(i + 2);
    }

    return tok;
  }
}