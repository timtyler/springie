// This program has been placed into the public domain by its author.

package com.springie.gui.components;

import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;

public class ChoiceWithDescription {
  public LinkedHashMap<String, String> hashtable;

  public Choice choice;

  public ChoiceWithDescription(ItemListener il) {
    this.choice = new FixedWidthChoice();
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

  /**
   * A Choice whose preferred size freezes at its first paint. Repopulating
   * the dropdown (e.g. switching the preset index) can add much longer item
   * names; without the freeze the next re-layout grows the Choice, and in
   * the bottom button bar's FlowLayout the wider dropdown wraps onto a
   * clipped second row and looks like it vanished. The freeze restores the
   * long-standing behaviour: the dropdown keeps its initial width and
   * longer names are clipped, as before.
   */
  private static class FixedWidthChoice extends Choice {
    static final long serialVersionUID = 1L;

    private Dimension frozen;

    @Override
    public void paint(Graphics g) {
      if (this.frozen == null) {
        final Dimension d = super.getPreferredSize();
        if (d.width > 0 && d.height > 0) {
          this.frozen = d;
        }
      }
      super.paint(g);
    }

    @Override
    public Dimension getPreferredSize() {
      return this.frozen != null ? new Dimension(this.frozen)
          : super.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }
  }
}
