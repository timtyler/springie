package com.tifsoft.fdl.old;

import java.util.Vector;

import uk.org.fdl.writer.FDLWriter;
import uk.org.fdl.writer.FDLWriterInterface;
import uk.org.fdl.writer.FDLWriterStringUtilities;


public class ZFDLWriterNamedBraceList implements FDLWriterInterface {
  String name;

  Vector attributes;

  Vector children;

  public boolean newlines = true;

  String open = "{";

  String close = "}";

  public ZFDLWriterNamedBraceList(String name, Vector arguments, Vector children) {
    this.name = name;
    this.attributes = arguments;
    this.children = children;
  }

  public ZFDLWriterNamedBraceList(String name, Vector arguments) {
    this.name = name;
    this.attributes = arguments;
  }

  public ZFDLWriterNamedBraceList(String name) {
    this.name = name;
  }

  public ZFDLWriterNamedBraceList() {
    //...
  }

  public String makeString() {
    return makeString(0);
  }

  public String makeString(int indent) {
    final StringBuffer sb = new StringBuffer();

    outputStartTagAndAttributes(indent, sb);

    outputChildren(indent, sb);

    outputEndTag(indent, sb);

    return sb.toString();
  }

  private void outputStartTagAndAttributes(final int indent,
      final StringBuffer sb) {
    if (this.name != null) {
      FDLWriterStringUtilities.indent(sb, indent);
      sb.append(this.name);
      // sb.append(":");

      if (this.attributes != null) {
        final int arguments_size = this.attributes.size();
        for (int i = 0; i < arguments_size; i++) {
          final ZFDLWriterAttribute arg = (ZFDLWriterAttribute) this.attributes
              .elementAt(i);
          sb.append(":" + arg.toString());
        }
      }

      sb.append(":");
      sb.append(this.open);
      if (this.newlines) {
        sb.append("\n");
      }
    } else {
      sb.append("[Error: Null tag]\n");
    }
  }

  private void outputChildren(int indent, final StringBuffer sb) {
    if (this.children != null) {
      final int children_size = this.children.size();
      for (int i = 0; i < children_size; i++) {
        final FDLWriterInterface node = (FDLWriterInterface) this.children
            .elementAt(i);
        sb.append(node.makeString(indent + FDLWriter.level));
      }
    }
  }

  private void outputEndTag(int indent, final StringBuffer sb) {
    if (this.name != null) {
      FDLWriterStringUtilities.indent(sb, indent);
      sb.append(this.close);
      if (this.newlines) {
        sb.append("\n");
      }
    }
  }

  public Vector getAttributes() {
    return this.attributes;
  }

  public void setAttributes(Vector arguments) {
    this.attributes = arguments;
  }

  public Vector getChildren() {
    return this.children;
  }

  public void setChildren(Vector children) {
    this.children = children;
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

  public void add(FDLWriterInterface child) {
    ensureChildrenExist();

    this.children.addElement(child);
  }

  private void ensureAttributesExist() {
    if (this.attributes == null) {
      this.attributes = new Vector();
    }
  }

  private void ensureChildrenExist() {
    if (this.children == null) {
      this.children = new Vector();
    }
  }

  public void addContentsOf(ZFDLWriterNamedBraceList child) {
    ensureChildrenExist();

    for (int i = 0; i < child.children.size(); i++) {
      final Object tag = child.children.elementAt(i);
      this.children.addElement(tag);
    }
  }
}