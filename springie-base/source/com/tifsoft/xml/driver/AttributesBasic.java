package com.tifsoft.xml.driver;

import java.util.ArrayList;

public class AttributesBasic {
  final ArrayList<String> attribute_uris = new ArrayList<>();
  final ArrayList<String> attribute_local_names = new ArrayList<>();
  public final ArrayList<String> attribute_qnames = new ArrayList<>();
  final ArrayList<String> attribute_values = new ArrayList<>();

  
  void removeAllElements() {
    this.attribute_uris.clear();
    this.attribute_local_names.clear();
    this.attribute_qnames.clear();
    this.attribute_values.clear();
  }
}
