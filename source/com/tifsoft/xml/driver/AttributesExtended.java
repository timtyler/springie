package com.tifsoft.xml.driver;

import java.util.ArrayList;

import org.xml.sax.Attributes;

import com.tifsoft.Forget;

public class AttributesExtended implements Attributes {
  static String CDATA = "CDATA";
  private AttributesBasic aw;

  AttributesExtended(AttributesBasic aw) {
    this.aw = aw;
  }

  public int getLength() {
    return this.aw.attribute_values.size();
  }

  public String getURI(final int i) {
    return (String) this.aw.attribute_uris.get(i);
  }

  public String getLocalName(final int i) {
    return (String) this.aw.attribute_local_names.get(i);
  }

  public String getQName(final int i) {
    return (String) this.aw.attribute_qnames.get(i);
  }

  public String getType(final int i) {
    Forget.about(i);
    return AttributesExtended.CDATA;
  }

  public String getValue(final int i) {
    return (String) this.aw.attribute_values.get(i);
  }

  public int getIndex(final String uri, final String local_part) {
    int i = -1;

    while (true) {
      i = indexOfFrom(this.aw.attribute_local_names, local_part, i + 1);

      if (i == -1 || uri.equals(this.aw.attribute_uris.get(i))) {
        return i;
      }
    }
  }

  // ArrayList has no indexOf(element, from_index); Vector did.
  private static int indexOfFrom(ArrayList<String> list, String element, int from_index) {
    for (int i = from_index, n = list.size(); i < n; i++) {
      if (element.equals(list.get(i))) {
        return i;
      }
    }
    return -1;
  }

  public int getIndex(final String q_name) {
    return this.aw.attribute_qnames.indexOf(q_name);
  }

  public String getType(final String uri, final String local_name) {
    Forget.about(uri);
    Forget.about(local_name);
    return AttributesExtended.CDATA;
  }

  public String getType(final String q_name) {
    Forget.about(q_name);
    return AttributesExtended.CDATA;
  }

  public String getValue(final String uri, final String local_name) {
    final int index = this.getIndex(uri, local_name);

    return (index == -1) ? null : (String) this.aw.attribute_values.get(index);
  }

  public String getValue(final String q_name) {
    final int index = this.aw.attribute_qnames.indexOf(q_name);

    return (index == -1) ? null : (String) this.aw.attribute_values.get(index);
  }

  public void setAw(AttributesBasic aw) {
    this.aw = aw;
  }

  public AttributesBasic getAw() {
    return this.aw;
  }
}