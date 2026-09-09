package com.tifsoft.fdl.old;

import java.util.ArrayList;

import uk.org.fdl.writer.FDLWriterInterface;
import uk.org.fdl.writer.FDLWriterStringUtilities;


public class ZFDLWriterSinglet implements FDLWriterInterface {
  String name;

  ArrayList<ZFDLWriterAttribute> attributes;

  public ZFDLWriterSinglet(String name, ArrayList<ZFDLWriterAttribute> arguments) {
    this.name = name;
    this.attributes = arguments;
  }

  public ZFDLWriterSinglet(String name) {
    this.name = name;
  }

  public String makeString() {
    return makeString(0);
  }

  public String makeString(int indent) {
    final StringBuilder sb = new StringBuilder();

    if (this.name != null) {
      FDLWriterStringUtilities.indent(sb, indent);
      //sb.append("<");
      sb.append(this.name);

      if (this.attributes != null) {
        final int arguments_size = this.attributes.size();
        for (int i = 0; i < arguments_size; i++) {
          final ZFDLWriterAttribute arg = this.attributes.get(i);
          sb.append(":" + arg.toString());
        }
      }

      sb.append("\n");
    }

    return sb.toString();
  }

  public ArrayList<ZFDLWriterAttribute> getAttributes() {
    return this.attributes;
  }

  public void setAttributes(ArrayList<ZFDLWriterAttribute> arguments) {
    this.attributes = arguments;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void add(ZFDLWriterAttribute attribute) {
    ensureAttributesExist();

    this.attributes.add(attribute);
  }

  private void ensureAttributesExist() {
    if (this.attributes == null) {
      this.attributes = new ArrayList<>();
    }
  }
}