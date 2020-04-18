// * Read in information from files...

package com.springie.io.in.readers.crudeclay;

import java.awt.Color;
import java.util.StringTokenizer;

import com.springie.utilities.log.Log;

public class ReaderCrudeClay {
  int current_r;

  int current_g;

  int current_b;

  int current_a;

  float current_radius = 1;

  float current_length = 1;

  int current_elasticity = -1;

  int current_damping = -1;

  int current_charge = -1;

  int current_colour = -1;

  boolean current_hidden;

  boolean current_disabled;

  boolean current_fixed;

  boolean current_cable;

  float last_r;

  float last_g;

  float last_b;

  float last_a;

  float last_radius = 1;

  float last_length = 1;

  int last_elasticity = -1;

  int last_damping = -1;

  int last_charge = -1;

  int last_colour = -1;

  boolean last_hidden;

  boolean last_disabled;

  boolean last_fixed;

  boolean last_cable;

  OutputType type;

  OutputType type_node = new OutputType();

  OutputType type_link = new OutputType();

  OutputType type_face = new OutputType();

  class OutputType {
    // ...
  }

  final int scale_factor = 10000;

  public String translate(String in) {
    final byte[] ba = {10 };
    final String c_r = new String(ba);

    final StringTokenizer st = new StringTokenizer(in, c_r);

    final StringBuffer out = parseTheFile(st);

    final String o = out.toString();

    // Log.log(o);
    return o;
  }

  private StringBuffer parseTheFile(final StringTokenizer st) {
    final StringBuffer out = new StringBuffer();
    out.append("CR ");

    while (st.hasMoreTokens()) {
      final String line = getNextValidLine(st);
      final StringTokenizer st2 = new StringTokenizer(line);

      while (st2.hasMoreTokens()) {
        String command = st2.nextToken();
        command = command.toLowerCase();
        execute(st2, command, out);
      }
    }

    return out;
  }

  private void execute(final StringTokenizer st2, String command,
      StringBuffer out) {
    if ("node".equals(command)) {
      commandNode(st2, out);
    } else if ("link".equals(command)) {
      commandLink(st2, out);
    } else if ("select".equals(command)) {
      commandSelect(st2, out);
    } else if ("deselect".equals(command)) {
      commandDeselect(st2, out);
    } else if ("reset".equals(command)) {
      commandReset(st2, out);
    } else if ("face".equals(command)) {
      commandFace(st2, out);
    } else if ("radius".equals(command)) {
      commandRadius(st2);
    } else if ("length".equals(command)) {
      commandLength(st2);
    } else if ("red".equals(command)) {
      commandRed(st2);
    } else if ("green".equals(command)) {
      commandGreen(st2);
    } else if ("blue".equals(command)) {
      commandBlue(st2);
    } else if ("hue".equals(command)) {
      commandHue(st2);
    } else if ("saturation".equals(command)) {
      commandSaturation(st2);
    } else if ("brightness".equals(command)) {
      commandBrightness(st2);
    } else if ("opacity".equals(command)) {
      commandOpacity(st2);
    } else if ("elasticity".equals(command)) {
      commandElasticity(st2);
    } else if ("damping".equals(command)) {
      commandDamping(st2);
    } else if ("charge".equals(command)) {
      commandCharge(st2);
    } else if ("hidden".equals(command)) {
      commandHidden(st2);
    } else if ("cable".equals(command)) {
      commandCable(st2);
    } else if ("disabled".equals(command)) {
      commandDisabled(st2);
    } else if ("fixed".equals(command)) {
      commandFixed(st2);
    } else {
      Log.log("Unknown command: " + command);
    }
  }

  private void commandReset(StringTokenizer s_t, StringBuffer out) {
    final String t = getNextCommand(s_t);
    if (t.equals("link")) {
      final String t2 = getNextCommand(s_t);
      if (t2.equals("lengths")) {
        out.append("RST_LNK_LEN ");
      }
    }
  }

  private void commandSelect(StringTokenizer s_t, StringBuffer out) {
    final String t = getNextCommand(s_t);
    if (t.equals("all")) {
      final String t2 = getNextCommand(s_t);
      if (t2.equals("links")) {
        out.append("SEL_ALL_LNK ");
      }
    }
  }

  private void commandDeselect(StringTokenizer s_t, StringBuffer out) {
    final String t = getNextCommand(s_t);
    if (t.equals("all")) {
      final String t2 = getNextCommand(s_t);
      if (t2.equals("links")) {
        out.append("DES_ALL_LNK ");
      }
    }
  }

  private String getNextCommand(StringTokenizer s_t) {
    return s_t.nextToken().toLowerCase();
  }

  private void commandNode(StringTokenizer s_t, StringBuffer out) {
    boolean need_group = this.type != this.type_node;
    need_group |= needsNewGroup();
    need_group |= this.current_radius != this.last_radius;
    need_group |= this.current_fixed != this.last_fixed;
    need_group |= this.current_charge != this.last_charge;

    if (need_group) {
      out.append("NG ");
      outputNewRadius(out);
      outputNewColour(out);
      outputNewHidden(out);
      outputNewFixed(out);
      outputNewCharge(out);

      nodeGroupNotNeeded();
    }

    out.append("N ");
    final String argument_name = s_t.nextToken();
    String argument_x = argument_name;

    final char c = argument_name.charAt(0);

    if (c > '9') {
      out.append("NA:" + argument_name + " ");
      argument_x = s_t.nextToken();
    }

    final int x = (int) (Double.valueOf(argument_x).doubleValue() * this.scale_factor);
    out.append("X:" + x + " ");
    final String argument_y = s_t.nextToken();
    final int y = (int) (Double.valueOf(argument_y).doubleValue() * this.scale_factor);
    out.append("Y:" + y + " ");
    final String argument_z = s_t.nextToken();
    final int z = (int) (Double.valueOf(argument_z).doubleValue() * this.scale_factor);
    out.append("Z:" + z + " ");
  }

  private void commandLink(StringTokenizer s_t, StringBuffer out) {
    boolean need_group = this.type != this.type_link;
    need_group |= needsNewGroup();
    need_group |= this.current_radius != this.last_radius;
    need_group |= this.current_length != this.last_length;
    need_group |= this.current_cable != this.last_cable;
    need_group |= this.current_disabled != this.last_disabled;
    need_group |= this.current_damping != this.last_damping;
    need_group |= this.current_elasticity != this.last_elasticity;

    if (need_group) {
      out.append("LG ");
      outputNewRadius(out);
      outputNewLength(out);
      outputNewColour(out);
      outputNewHidden(out);
      outputNewDisabled(out);
      outputNewCable(out);
      outputNewDamping(out);
      outputNewElasticity(out);
      linkGroupNotNeeded();
    }

    out.append("LK ");

    while (s_t.hasMoreTokens()) {
      final String t = s_t.nextToken();
      out.append("V:" + t + " ");
    }
  }

  private void commandFace(StringTokenizer s_t, StringBuffer out) {
    boolean need_group = this.type != this.type_face;
    need_group |= needsNewGroup();

    if (need_group) {
      out.append("PG ");
      outputNewColour(out);
      outputNewHidden(out);
      faceGroupNotNeeded();
    }

    out.append("P ");

    while (s_t.hasMoreTokens()) {
      final String t = s_t.nextToken();
      out.append("V:" + t + " ");
    }
  }

  private void commandBrightness(StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    final float value = Float.valueOf(argument).floatValue();
    final float[] hsb = new float[3];
    Color.RGBtoHSB(this.current_r, this.current_g, this.current_b, hsb);
    final int n_col = Color.HSBtoRGB(hsb[0], hsb[1], value);
    setCurrentColour(n_col);
  }

  private void commandSaturation(StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    final float value = Float.valueOf(argument).floatValue();
    final float[] hsb = new float[3];
    Color.RGBtoHSB(this.current_r, this.current_g, this.current_b, hsb);
    final int n_col = Color.HSBtoRGB(hsb[0], value, hsb[2]);
    setCurrentColour(n_col);
  }

  private void commandHue(StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    final float value = Float.valueOf(argument).floatValue();
    final float[] hsb = new float[3];
    Color.RGBtoHSB(this.current_r, this.current_g, this.current_b, hsb);
    final int n_col = Color.HSBtoRGB(value, hsb[1], hsb[2]);
    setCurrentColour(n_col);
  }

  private void setCurrentColour(int n_col) {
    this.current_r = (n_col >> 16) & 0xFF;
    this.current_g = (n_col >> 8) & 0xFF;
    this.current_b = (n_col >> 0) & 0xFF;
  }

  private void commandRed(final StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_r = (int) (Double.valueOf(argument).doubleValue() * 255);
  }

  private void commandGreen(final StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_g = (int) (Double.valueOf(argument).doubleValue() * 255);
  }

  private void commandBlue(final StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_b = (int) (Double.valueOf(argument).doubleValue() * 255);
  }

  private void commandOpacity(final StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_a = (int) (Double.valueOf(argument).doubleValue() * 255);
  }

  private void commandLength(final StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_length = (int) (Double.valueOf(argument).doubleValue() * this.scale_factor);
  }

  private void commandRadius(final StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_radius = (int) (Double.valueOf(argument).doubleValue() * this.scale_factor);
  }

  private void commandCable(StringTokenizer s_t) {
    final String argument = (s_t.nextToken()).toLowerCase();
    this.current_cable = "true".equals(argument);
  }

  private void commandFixed(StringTokenizer s_t) {
    final String argument = (s_t.nextToken()).toLowerCase();
    this.current_fixed = "true".equals(argument);
  }

  private void commandDisabled(StringTokenizer s_t) {
    final String argument = (s_t.nextToken()).toLowerCase();
    this.current_disabled = "true".equals(argument);
  }

  private void commandHidden(StringTokenizer s_t) {
    final String argument = (s_t.nextToken()).toLowerCase();
    this.current_hidden = "true".equals(argument);
  }

  private void commandDamping(StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_damping = Integer.parseInt(argument);
  }

  private void commandElasticity(StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_elasticity = Integer.parseInt(argument);
  }

  private void commandCharge(StringTokenizer s_t) {
    final String argument = s_t.nextToken();
    this.current_charge = Integer.parseInt(argument);
  }

  private boolean needsNewGroup() {
    boolean need_group = false;
    need_group |= this.current_r != this.last_r;
    need_group |= this.current_g != this.last_g;
    need_group |= this.current_b != this.last_b;
    need_group |= this.current_a != this.last_a;
    need_group |= this.current_hidden != this.last_hidden;
    need_group |= this.current_radius != this.last_radius;

    return need_group;
  }

  private void faceGroupNotNeeded() {
    generalGroupNotNeeded();
    this.type = this.type_face;
  }

  private void linkGroupNotNeeded() {
    generalGroupNotNeeded();
    this.last_length = this.current_length;
    this.last_cable = this.current_cable;
    this.last_disabled = this.current_disabled;
    this.last_elasticity = this.current_elasticity;
    this.last_damping = this.current_damping;
    this.type = this.type_link;
  }

  private void nodeGroupNotNeeded() {
    generalGroupNotNeeded();
    this.last_fixed = this.current_fixed;
    this.last_charge = this.current_charge;
    this.type = this.type_node;
  }

  private void generalGroupNotNeeded() {
    this.last_r = this.current_r;
    this.last_g = this.current_g;
    this.last_b = this.current_b;
    this.last_a = this.current_a;
    this.last_hidden = this.current_hidden;
    this.last_radius = this.current_radius;
  }

  private void outputNewRadius(StringBuffer out) {
    out.append("R:" + (int) this.current_radius + " ");
    this.last_radius = this.current_radius;
  }

  private void outputNewHidden(StringBuffer out) {
    if (this.current_hidden) {
      out.append("H:1 ");
      this.last_hidden = this.current_hidden;
    }
  }

  private void outputNewFixed(StringBuffer out) {
    if (this.current_fixed) {
      out.append("FX:1 ");
      this.last_fixed = this.current_fixed;
    }
  }

  private void outputNewCable(StringBuffer out) {
    if (this.current_cable) {
      out.append("CA:1 ");
      this.last_cable = this.current_cable;
    }
  }

  private void outputNewDisabled(StringBuffer out) {
    if (this.current_disabled) {
      out.append("D:1 ");
      this.last_disabled = this.current_disabled;
    }
  }

  private void outputNewLength(StringBuffer out) {
    // if (this.current_length != this.last_length) {
    out.append("L:" + (int) this.current_length + " ");
    this.last_length = this.current_length;
    // }
  }

  private void outputNewElasticity(StringBuffer out) {
    // if (this.current_elasticity != this.last_elasticity) {
    out.append("E:" + this.current_elasticity + " ");
    this.last_elasticity = this.current_elasticity;
    // }
  }

  private void outputNewDamping(StringBuffer out) {
    if (this.current_damping != this.last_damping) {
      out.append("DA:" + this.current_damping + " ");
      this.last_damping = this.current_damping;
    }
  }

  private void outputNewCharge(StringBuffer out) {
    if (this.current_charge != this.last_charge) {
      out.append("CH:" + this.current_charge + " ");
      this.last_charge = this.current_charge;
    }
  }

  private void outputNewColour(StringBuffer out) {
    this.current_colour = getCurrentColour();
    if (this.current_colour != this.last_colour) {
      final long c = this.current_colour & 0xFFFFFFFFL;
      out.append("C:0x" + Long.toString(c, 16) + " ");
      this.last_colour = this.current_colour;
    }
  }

  private int getCurrentColour() {
    return (this.current_a << 24) | (this.current_r << 16)
        | (this.current_g << 8) | this.current_b;
  }

  String getNextValidLine(StringTokenizer st) {
    boolean found;
    String tok;
    do {
      tok = st.nextToken();

      while ((tok.length() > 1) && (tok.charAt(0) == ' ')) {
        tok = tok.substring(1);
      }

      found = true;
      if (tok.length() < 1) {
        found = false;
      } else if (tok.charAt(0) == '/') {
        found = false;
      }
    } while (!found);

    return tok;
  }
}