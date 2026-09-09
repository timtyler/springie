package com.tifsoft.xml.writer;

import java.util.ArrayList;

public class XMLWriterTagPair implements XMLWriterInterface {
  String name;

  ArrayList<XMLWriterAttribute> attributes;

  ArrayList<XMLWriterInterface> children;
  
  public boolean newlines = true;

  public XMLWriterTagPair(String name, ArrayList<XMLWriterAttribute> arguments, ArrayList<XMLWriterInterface> children) {
    this.name = name;
    this.attributes = arguments;
    this.children = children;
  }

  public XMLWriterTagPair(String name, ArrayList<XMLWriterAttribute> arguments) {
    this.name = name;
    this.attributes = arguments;
  }

  public XMLWriterTagPair(String name) {
    this.name = name;
  }

  public String makeString() {
    return makeString(0);
  }

  public String makeString(int indent) {
    final StringBuilder sb = new StringBuilder();

    outputStartTagAndAttributes(indent, sb);

    outputChildren(indent, sb);

    outputEndTag(indent, sb);

    return sb.toString();
  }

  private void outputStartTagAndAttributes(final int indent,
    final StringBuilder sb) {
    if (this.name != null) {
      XMLWriterUtilities.indent(sb, indent);
      sb.append("<");
      sb.append(this.name);

      if (this.attributes != null) {
        final int arguments_size = this.attributes.size();
        for (int i = 0; i < arguments_size; i++) {
          final XMLWriterAttribute arg = this.attributes.get(i);
          sb.append(" " + arg.toString());
        }
      }

      sb.append(">");
      if (this.newlines) {
        sb.append("\n");
      }
    } else {
      sb.append("[Error: Null tag]\n");
    }
  }

  private void outputChildren(int indent, final StringBuilder sb) {
    if (this.children != null) {
      final int children_size = this.children.size();
      for (int i = 0; i < children_size; i++) {
        final XMLWriterInterface node = this.children.get(i);
        sb.append(node.makeString(indent + XMLWriterIndent.level));
      }
    }
  }

  private void outputEndTag(int indent, final StringBuilder sb) {
    if (this.name != null) {
      XMLWriterUtilities.indent(sb, indent);
      sb.append("</");
      sb.append(this.name);
      sb.append(">\n");
    }
  }

  public ArrayList<XMLWriterAttribute> getAttributes() {
    return this.attributes;
  }

  public void setAttributes(ArrayList<XMLWriterAttribute> arguments) {
    this.attributes = arguments;
  }

  public ArrayList<XMLWriterInterface> getChildren() {
    return this.children;
  }

  public void setChildren(ArrayList<XMLWriterInterface> children) {
    this.children = children;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void add(XMLWriterAttribute attribute) {
    ensureAttributesExist();

    this.attributes.add(attribute);
  }

  public void add(XMLWriterInterface child) {
    ensureChildrenExist();

    this.children.add(child);
  }

  private void ensureAttributesExist() {
    if (this.attributes == null) {
      this.attributes = new ArrayList<>();
    }
  }

  private void ensureChildrenExist() {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
  }

  public void addContentsOf(XMLWriterTagPair child) {
    ensureChildrenExist();
    
    for (int i = 0; i < child.children.size(); i++) {
      final XMLWriterInterface tag = child.children.get(i);
      this.children.add(tag);
    }
  }
}