// This code has been placed into the public domain by its author

package com.springie.gui.components;

import java.awt.ItemSelectable;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import com.springie.utilities.ImageLoader;
import com.springie.utilities.ImageWrapper;
import com.tifsoft.Forget;

public class ButtonBar extends Panel implements ItemSelectable {
  static final long serialVersionUID = 1L;

  boolean first = true;

  ImageButtonGroup image_button_group = new ImageButtonGroup();

  Vector buttons = new Vector();

  String name;

  String description;

  ItemListener listener;

  public void add(String name, String description) {
    this.name = name;
    this.description = description;
    final ImageButton ib = getImageButton(name, description);
    this.buttons.addElement(ib);
    this.add(ib);
  }

  private ImageButton getImageButton(String iname, String name) {
    final ImageWrapper image_o = ImageLoader.getImage(GraphicsDirectory.directory + iname
        + "_o.png");
    final ImageWrapper image_i = ImageLoader.getImage(GraphicsDirectory.directory + iname
        + "_i.png");
    final ImageButton ib = new ImageButton(image_o, image_i
        , this.image_button_group, name, this.first);
    final ActionListener action_listener = new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        Forget.about(event);
        if (ButtonBar.this.listener != null) {
          final String action_string = event.getActionCommand();
          //Log.log("Button found:" + action_string);
          
          final ItemEvent ie = new ItemEvent(ButtonBar.this, 0, action_string, 0);
          
          ButtonBar.this.listener.itemStateChanged(ie);
        }
      }
    };
    
    ib.setRadio(true);

    ib.addActionListener(action_listener);
    this.first = false;

    return ib;
  }

  public void addItemListener(ItemListener listener) {
    this.listener = listener;
  }

  public Object[] getSelectedObjects() {
    final Object[] rv = new Object[] {null };

    return rv;
  }

  public void removeItemListener(ItemListener listener) {
    Forget.about(listener);
    this.listener = null;
  }

  public void select(String action) {
    final int size = this.buttons.size();
    for (int i = 0; i < size; i++) {
      final ImageButton button = (ImageButton) this.buttons.elementAt(i);
      if (button.name == action) {
        button.setState(true);
      }
    }
  }
}
