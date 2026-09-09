package com.tifsoft.xml.driver;

import java.util.ArrayList;

public class AttributesBasic {
  final ArrayList attribute_uris = new ArrayList<>();
  final ArrayList attribute_local_names = new ArrayList<>();
  public final ArrayList attribute_qnames = new ArrayList<>();
  final ArrayList attribute_values = new ArrayList<>();

  
  void removeAllElements() {
    this.attribute_uris.clear();
    this.attribute_local_names.clear();
    this.attribute_qnames.clear();
    this.attribute_values.clear();
  }
}
