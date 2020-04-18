package com.tifsoft.fdl.old;

import java.util.Vector;

import uk.org.fdl.writer.FDLWriterInterface;
import uk.org.fdl.writer.FDLWriterStringUtilities;


public class ZFDLWriterSinglet implements FDLWriterInterface {
  String name;

  Vector attributes;

  public ZFDLWriterSinglet(String name, Vector arguments) {
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
    final StringBuffer sb = new StringBuffer();

    if (this.name != null) {
      FDLWriterStringUtilities.indent(sb, indent);
      //sb.append("<");
      sb.append(this.name);

      if (this.attributes != null) {
        final int arguments_size = this.attributes.size();
        for (int i = 0; i < arguments_size; i++) {
          final ZFDLWriterAttribute arg = (ZFDLWriterAttribute) this.attributes.elementAt(i);
          sb.append(":" + arg.toString());
        }
      }

      sb.append("\n");
    }

    return sb.toString();
  }

  public Vector getAttributes() {
    return this.attributes;
  }

  public void setAttributes(Vector arguments) {
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

    this.attributes.addElement(attribute);
  }

  private void ensureAttributesExist() {
    if (this.attributes == null) {
      this.attributes = new Vector();
    }
  }
}