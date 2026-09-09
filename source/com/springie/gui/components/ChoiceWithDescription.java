// This program has been placed into the public domain by its author.

package com.springie.gui.components;

import java.awt.Choice;
import java.awt.Color;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;

public class ChoiceWithDescription {
  public LinkedHashMap<String, String> hashtable;

  public Choice choice;

  public ChoiceWithDescription(ItemListener il) {
    this.choice = new Choice();
    this.choice.addItemListener(il);
    this.choice.setBackground(Color.white);
    this.choice.setForeground(Color.black);

    this.hashtable = new LinkedHashMap<>();
  }

  public void add(String description, String name) {
    this.choice.addItem(description);

    this.hashtable.put(description, name);
  }

  public void removeAll() {
    this.choice.removeAll();

    this.hashtable.clear();
  }

  public String getName(String description) {
    return this.hashtable.get(description);
  }
}
