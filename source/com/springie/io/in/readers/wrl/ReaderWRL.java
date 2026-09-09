// * Read in information from files...

package com.springie.io.in.readers.wrl;

import java.awt.Point;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.ArrayList;

import com.springie.render.modules.modern.Double3D;

public final class ReaderWRL {
  static ArrayList<ReaderWRLGroup> groups = new ArrayList<>();

  static ReaderWRLGroup current_group;

  static int scale_factor = 3000;

  private ReaderWRL() {
    // ...
  }

  public static String translate(String in) {
    groups.clear();

    final byte[] ba = {10, 13 };
    final String c_r = new String(ba);

    final StringTokenizer st = new StringTokenizer(in, ", [](){}" + c_r);

    final StringBuilder out = parseTheFile(st);

    return out.toString();
  }

  private static StringBuilder parseTheFile(final StringTokenizer st) {
    final StringBuilder out = new StringBuilder();
    extractNodes(st, out);

    //Log.log("" + out);

    return out;
  }

  class Stage {
    // ...
  }

  private static void extractNodes(final StringTokenizer st,
      final StringBuilder out) {
    String token = null;
    // final int colour = 0xFFFFB0FF;
    // final double radius = 0;
    // final int colour_this = 0xFFFFB0FF;

    // final int[] colours = new ColourFactory(52746).getColourArray(16);

    out.append("CR ");

    // token = getNextValidTok//en(st);

    // final int count = 0;
    //
    // final double xd = 0;
    // final double yd = 0;
    // final double zd = 0;
    //
    // final double sf_x = 0;
    // final double sf_y = 0;
    // final double sf_z = 0;

    do {
      token = getNextValidToken(st);
      if ("diffuseColor".equals(token)) {
        String temp = getNextValidToken(st);
        final double red = getDouble(temp);
        temp = getNextValidToken(st);
        final double gre = getDouble(temp);
        temp = getNextValidToken(st);
        final double blu = getDouble(temp);
        final int r = (int) (red * 255);
        final int g = (int) (gre * 255);
        final int b = (int) (blu * 255);

        current_group = new ReaderWRLGroup();
        current_group.colour = r << 16 | g << 8 | b;

        groups.add(current_group);
      }

      if ("point".equals(token)) {
        boolean finished = false;
        do {
          final String temp_x = getNextValidToken(st);
          if (temp_x.charAt(0) < '@') {
            final String temp_y = getNextValidToken(st);
            final String temp_z = getNextValidToken(st);
            final double x = getDouble(temp_x);
            final double y = getDouble(temp_y);
            final double z = getDouble(temp_z);

            current_group.points.add(new Double3D(x, y, z));
          } else {
            finished = true;
          }
        } while (!finished);
      }

      if ("coordIndex".equals(token)) {
        ArrayList<Integer> current_face = new ArrayList<>();
        current_group.faces.add(current_face);
        boolean finished = false;
        do {
          boolean finished2 = false;
          do {
            final String temp = getNextValidToken(st);
            if (temp.charAt(0) < '@') {
              final int v = Integer.parseInt(temp);
              if (v == -1) {
                finished2 = true;
                current_face = new ArrayList<>();
                current_group.faces.add(current_face);
              } else {
                current_face.add(new Integer(v));
              }
            } else {
              finished = true;
              finished2 = true;
            }
          } while (!finished2);
        } while (!finished);
      }
    } while (st.hasMoreElements());

    // // output points...

    // for (int i = 0; i < main_size; i++) {
    // final ReaderWRLGroup group = (ReaderWRLGroup) groups.get(i);
    // Log.log("c: " + group.colour);
    // Log.log("Points");
    //
    // final int point_size = group.points.size();
    // for (int j = 0; j < point_size; j++) {
    // final Double3D point = (Double3D) group.points.get(j);
    // Log.log("x: " + point.x + " - y: " + point.y + " - z: " + point.z);
    // }
    // Log.log("Faces");
    //
    // final int faces_size = group.faces.size();
    // for (int j = 0; j < faces_size; j++) {
    // String out_str = "";
    // final ArrayList face = (ArrayList) group.faces.get(j);
    // final int face_size = face.size();
    // for (int k = 0; k < face_size; k++) {
    // final Integer v = (Integer) face.get(k);
    // out_str += " " + v;
    // }
    // Log.log("face:" + out_str);
    // }
    // }

    // make node number lists...
    final int main_size = groups.size();

    final ArrayList<Double3D> final_node_list = new ArrayList<>();

    for (int i = 0; i < main_size; i++) {
      final ReaderWRLGroup group = groups.get(i);
      if (group.colour == 0xFFFFFF) {
        // list of point numbers for each node
        final ArrayList<ArrayList<Integer>> lists = new ArrayList<>();
        final int faces_size = group.faces.size();
        for (int j = 0; j < faces_size; j++) {
          final ArrayList<Integer> face = group.faces.get(j);
          final int face_size = face.size();
          ArrayList<Integer> found_list = null;
          for (int k = 0; k < face_size; k++) {
            final Integer v = face.get(k);
            if (found_list == null) {
              found_list = foundAlready(v, lists);
              break;
            }
          }

          // no points found? make a new list
          if (found_list == null) {
            found_list = new ArrayList<>();
            lists.add(found_list);
          }

          // add all needed points to relevant list
          for (int k = 0; k < face_size; k++) {
            final Integer v = face.get(k);
            if (!found_list.contains(v)) {
              found_list.add(v);
            }
          }
        }

        final ArrayList<ReaderWRLNode> node_list = new ArrayList<>();
        // make the nodes...
        final int lists_size = lists.size();
        for (int idx = 0; idx < lists_size; idx++) {
          final ArrayList<Integer> list = lists.get(idx);
          final int list_size = list.size();
          if (list_size > 0) {
            final Double3D min = new Double3D(9999, 9999, 9999);
            final Double3D max = new Double3D(-9999, -9999, -9999);
            final Double3D average = new Double3D(0, 0, 0);
            for (int jdx = 0; jdx < list_size; jdx++) {
              final Integer integer = list.get(jdx);
              final int index = integer.intValue();
              final Double3D d3d = group.points.get(index);
              min.x = Math.min(d3d.x, min.x);
              min.y = Math.min(d3d.y, min.y);
              min.z = Math.min(d3d.z, min.z);

              max.x = Math.max(d3d.x, max.x);
              max.y = Math.max(d3d.y, max.y);
              max.z = Math.max(d3d.z, max.z);
              average.add(d3d);
            }
            average.divideBy(list_size);
            double max_r = Math.max(max.x - min.x, max.z - min.z);
            max_r = Math.max(max.y - min.y, max_r);
            if (max_r > 0) {
              final ReaderWRLNode tn = new ReaderWRLNode(min, max, average);
              node_list.add(tn);
            }
          }
        }

        final int node_list_size = node_list.size();
        for (int idx = 0; idx < node_list_size; idx++) {
          final ReaderWRLNode node = node_list.get(idx);
          if (noLargerNodesIntersect(node, node_list, idx + 1)) {
            final Double3D node_min = node.min;
            final Double3D node_max = node.max;
            final Double3D node_average = node.average;

            final int x = (int) (node_average.x * scale_factor);
            final int y = (int) (node_average.y * scale_factor);
            final int z = (int) (node_average.z * scale_factor);
            double max_r = Math.max(node_max.x - node_min.x, node_max.y
                - node_min.y);
            max_r = Math.max(max_r, node_max.z - node_min.z);
            final int r = (int) (max_r * scale_factor);
            out.append("NG R:" + r + " C:0xFFFFFFFF ");
            out.append("N X:" + x + " Y:" + y + " Z:" + z + " ");

            final_node_list.add(node.min);
          }
        }

        // Log.log("Found " + lists.size() + " points");
      }
    }

    // links...
    final HashMap<Point, Point> already = new HashMap<>();
    for (int i = 0; i < main_size; i++) {
      final ReaderWRLGroup group = groups.get(i);
      if (group.colour != 0xFFFFFF) {
        final int faces_size = group.faces.size();
        for (int j = 0; j < faces_size; j++) {
          final ArrayList<Integer> face = group.faces.get(j);
          final int face_size = face.size();
          for (int k = 0; k < face_size - 1; k++) {
            final Integer v1 = face.get(k);
            final Integer v2 = face.get(k + 1);

            // Log.log("Link idx: " + v1 + " - " + v2);

            final Double3D pt_1 = group.points.get(v1
                .intValue());
            final Double3D pt_2 = group.points.get(v2
                .intValue());

            // create link
            int n1 = getIndexOfNearest(pt_1, final_node_list);
            int n2 = getIndexOfNearest(pt_2, final_node_list);

            if (n1 != n2) {
              if (n1 > n2) {
                final int temp = n1;
                n1 = n2;
                n2 = temp;
              }
              final Point p = new Point(n1, n2);
              if (!already.containsKey(p)) {
                already.put(p, p);
                out.append("LG L:500 E:80 C:0x");
                out.append(getHexColour(group.colour) + " ");

                out.append("LK V:" + n1 + " V:" + n2 + " ");
              }
            }
          }
        }
      }
    }
  }

  private static double getDouble(String temp) {
    return Double.valueOf(temp).doubleValue();
  }

  static String getHexColour(int colour) {
    final int v = 0x1000000 | colour;
    final String rv = Integer.toString(v, 16);

    return "FF" + rv.substring(1);
  }

  private static int getIndexOfNearest(Double3D point, ArrayList<Double3D> final_node_list) {
    final int final_node_list_size = final_node_list.size();
    int nearest_index = 0;
    double min_distance = 99999;

    for (int k = 0; k < final_node_list_size; k++) {
      final Double3D pt = final_node_list.get(k);
      final Double3D copy = pt.subtract(point);
      double len = copy.length();
      if (len < min_distance) {
        min_distance = len;
        nearest_index = k;
      }
    }

    return nearest_index;
  }

  private static boolean noLargerNodesIntersect(ReaderWRLNode target,
      ArrayList<ReaderWRLNode> node_list, int start) {
    final int node_list_size = node_list.size();
    for (int idx = start; idx < node_list_size; idx++) {
      final ReaderWRLNode node = node_list.get(idx);
      if (node.intersects(target)) {
        return false;
      }
    }

    return true;
  }

  private static ArrayList<Integer> foundAlready(Integer value, ArrayList<ArrayList<Integer>> lists) {
    final int lists_size = lists.size();
    for (int i = 0; i < lists_size; i++) {
      final ArrayList<Integer> list = lists.get(i);
      final int list_size = list.size();
      for (int j = 0; j < list_size; j++) {
        final Integer integer = list.get(j);
        if (integer.equals(value)) {
          return list;
        }
      }
    }
    return null;
  }

  // private static String removeCommas(String temp) {
  // int io = temp.indexOf(',');
  // if (io >= 0) {
  // String start = temp.substring(0, io);
  // String end = temp.substring(io + 1);
  // return removeCommas(start + end);
  // }
  //
  // return temp;
  // }
  //
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

    // final int i = tok.indexOf("*^");

    // if (i > 0) {
    // tok = tok.substring(0, i) + "E" + tok.substring(i + 2);
    // }

    return tok;
  }
}