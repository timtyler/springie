// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.links.Link;
import com.springie.gui.components.TTChoice;
import com.springie.messages.MessageManager;
import com.tifsoft.Forget;

public class PanelControlsSelectLinks {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  TTChoice choice_link_definition;

  //private int select_type;

  static final int any_member = 0;

  static final int tension = 1;

  static final int compression = 2;

  static final int no_tension = 3;

  static final int no_compression = 4;

  public static FilterSelectLinks filter_any_member = new FilterSelectLinks() {
    public boolean qualifies(Link l) {
      Forget.about(l);
      return true;
    }
  };

  public static FilterSelectLinks filter_tension = new FilterSelectLinks() {
    public boolean qualifies(Link l) {
      return l.type.tension;
    }
  };

  public static FilterSelectLinks filter_compression = new FilterSelectLinks() {
    public boolean qualifies(Link l) {
      return l.type.compression;
    }
  };

  public static FilterSelectLinks filter_no_tension = new FilterSelectLinks() {
    public boolean qualifies(Link l) {
      return !l.type.tension;
    }
  };

  public static FilterSelectLinks filter_no_compression = new FilterSelectLinks() {
    public boolean qualifies(Link l) {
      return !l.type.compression;
    }
  };

  public static FilterSelectLinks comparator = filter_any_member;

  public PanelControlsSelectLinks(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    this.panel.add(getSelectAllPanel());
  }

  private Panel getSelectAllPanel() {
    this.choice_link_definition = new TTChoice(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }

        final String stateChangedString = (String) (e.getItem());

        if (PanelControlsSelectLinks.this.choice_link_definition
            .str_to_num(stateChangedString) != -99) {
          final int select_type = PanelControlsSelectLinks.this.choice_link_definition
              .str_to_num(stateChangedString);

          ContextMananger.getLinkManager().deselectAll();

          switch (select_type) {
            // case any_member:
            // comparator = filter_any_member;
            // break;
            case tension:
              comparator = filter_tension;
              break;
            case compression:
              comparator = filter_compression;
              break;
            case no_tension:
              comparator = filter_no_tension;
              break;
            case no_compression:
              comparator = filter_no_compression;
              break;
            default:
              comparator = filter_any_member;
              break;
          }
        }
      }
    });

    this.choice_link_definition.add("any member",
        PanelControlsSelectLinks.any_member);
    this.choice_link_definition
        .add("tension", PanelControlsSelectLinks.tension);
    this.choice_link_definition.add("compression",
        PanelControlsSelectLinks.compression);
    this.choice_link_definition.add("no tension",
        PanelControlsSelectLinks.no_tension);
    this.choice_link_definition.add("no compression",
        PanelControlsSelectLinks.no_compression);

    this.choice_link_definition.choice.select(this.choice_link_definition
        .num_to_str(any_member));

    // choice with invert here...

    final Panel panel_select_all = new Panel();
    panel_select_all.add(new Label("'Links' refers to:"));
    panel_select_all.add(this.choice_link_definition.choice);

    return panel_select_all;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}
