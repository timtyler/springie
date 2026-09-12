//This program has been placed into the public domain by its author.

package com.springie.gui.components;

import java.awt.Choice;
import java.awt.Color;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.ArrayList;


public class TTChoice {
  public ArrayList<TTNumStr> vector;

  public Choice choice;

  public TTChoice(ItemListener il) {
    this.choice = new Choice();
    this.choice.addItemListener(il);
    this.choice.setBackground(Color.white);
    this.choice.setForeground(Color.black);

    this.vector = new ArrayList<>();
  }

  public void add(String s, int n) {
    this.choice.addItem(s);

    this.vector.add(new TTNumStr(n, s)); //  = s;
  }

  void removeAll() {
    this.choice.removeAll();

    this.vector.clear(); //  = s;
  }

  public int str_to_num(String s) {
    // int i = 0;
    final Iterator<TTNumStr> enumeration = this.vector.iterator();

    while (enumeration.hasNext()) {
      final TTNumStr temp_pair = enumeration.next();

      if (temp_pair.string.equals(s)) {
        return temp_pair.number;
      }
    }

    return -99; // -99 = not found...
  }

  public String num_to_str(int j) {
    //int i = 0;
    final Iterator<TTNumStr> enumeration = this.vector.iterator();

    while (enumeration.hasNext()) {
      final TTNumStr temp_pair = enumeration.next();

      if (temp_pair.number == j) {
        return temp_pair.string;
      }
    }

    return ""; // null marker = not found...
  }

}