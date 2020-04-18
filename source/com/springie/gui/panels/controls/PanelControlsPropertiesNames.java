// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.elements.base.BaseElement;
import com.springie.elements.selection.SelectionManager;
import com.springie.gui.components.TTChoice;
import com.springie.gui.components.TextFieldWrapper;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.springie.utilities.general.Executor;
import com.springie.utilities.general.StringMatcher;
import com.springie.utilities.general.StringPair;
import com.tifsoft.Forget;

public class PanelControlsPropertiesNames {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  public TextFieldWrapper textfield = new TextFieldWrapper(8);

  public Button button_set_prefix = new Button("Set");

  public Button button_select_prefix = new Button("Act");

  private NewMessageManager new_message_manager;

  private TTChoice choose_matcher;

  private TTChoice choose_action;

  public StringMatcher matcher_equals = new StringMatcher() {
    public boolean matches(String a, String b) {
      if (a == null) {
        return false;
      }
      return a.equals(b);
    }

    public String combine(String a, String b) {
      if (a == null) {
        return b;
      }
      if (a.equals(b)) {
        return a;
      }

      return null;
    }

    public String set(String from, String to, String current) {
      Forget.about(current);
      Forget.about(from);
      return to;
    }
  };

  public StringMatcher matcher_starts_with = new StringMatcher() {
    public boolean matches(String a, String b) {
      if (a == null) {
        return false;
      }
      return a.startsWith(b);
    }

    public String combine(String current, String name) {
      return extractSharedPrefix(current, name);
    }

    public String set(String from, String to, String current) {
      if (current == null) {
        return to;
      } else if (matches(current, from)) {
        return current.substring(from.length());
      }
      return to + current;
    }
  };

  public StringMatcher matcher_ends_with = new StringMatcher() {
    public boolean matches(String a, String b) {
      if (a == null) {
        return false;
      }
      return a.endsWith(b);
    }

    public String combine(String current, String name) {
      return extractSharedSuffix(current, name);
    }

    public String set(String from, String to, String current) {
      Forget.about(current);
      Forget.about(from);
      return to;
    }
  };

  public StringMatcher matcher = this.matcher_equals;

  public Executor action_select = new Executor() {
    public Object execute(Object parameter) {
      final BaseElement be = (BaseElement) parameter;
      be.setSelected(true);
      return null;
    }
  };

  public Executor action_deselect = new Executor() {
    public Object execute(Object parameter) {
      final BaseElement be = (BaseElement) parameter;
      be.setSelected(false);
      return null;
    }
  };

  public Executor action_invert = new Executor() {
    public Object execute(Object parameter) {
      final BaseElement be = (BaseElement) parameter;
      be.setSelected(!be.isSelected());
      return null;
    }
  };

  public Executor action = this.action_select;

  public PanelControlsPropertiesNames(MessageManager message_manager,
      NewMessageManager new_message_manager) {
    this.message_manager = message_manager;
    this.new_message_manager = new_message_manager;
    makePanel();
  }

  public void makePanel() {
    final Panel prefix = new Panel();

    prefix.add(new Label("Name"));

    this.choose_matcher = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        final int value = PanelControlsPropertiesNames.this.choose_matcher
            .str_to_num(scs);
        if (value == 1) {
          PanelControlsPropertiesNames.this.matcher = PanelControlsPropertiesNames.this.matcher_equals;
        } else if (value == 2) {
          PanelControlsPropertiesNames.this.matcher = PanelControlsPropertiesNames.this.matcher_starts_with;
        } else if (value == 3) {
          PanelControlsPropertiesNames.this.matcher = PanelControlsPropertiesNames.this.matcher_ends_with;
        }
      }
    });

    this.choose_matcher.add("matches", 1);
    this.choose_matcher.add("starts with   ", 2);
    this.choose_matcher.add("ends with", 3);
    this.choose_matcher.choice.select(this.choose_matcher.num_to_str(1));

    prefix.add(this.choose_matcher.choice);

    prefix.add(this.textfield);

    this.button_set_prefix.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        final String proposed = PanelControlsPropertiesNames.this.textfield.getText();
        final String sanitised = sanitise(proposed);
        getNewMessageManager().add(new NewMessage(sanitised) {
          public Object execute() {
            final String to = (String) this.context;
            final String from = SelectionManager
                .combineSelection(PanelControlsPropertiesNames.this.matcher);
            final StringPair pair = new StringPair(from, to);
            final Executor ex = new Executor(pair) {
              public Object execute(Object parameter) {
                final StringPair pair = (StringPair) this.input;
                final String from = pair.a;
                final String to = pair.b;
                final BaseElement be = (BaseElement) parameter;
                String result = PanelControlsPropertiesNames.this.matcher.set(
                    from, to, be.name);
                be.name = result;
                return null;
              }
            };
            SelectionManager.applyToEachSelectedObject(ex);
            updatePrefix();
            return null;
          }
        });
      }

    });

    updatePrefix();

    this.panel.add(prefix);

    final Panel bottom = new Panel();

    bottom.add(this.button_set_prefix);

    this.choose_action = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        final int value = PanelControlsPropertiesNames.this.choose_action
            .str_to_num(scs);
        if (value == 1) {
          PanelControlsPropertiesNames.this.action = PanelControlsPropertiesNames.this.action_select;
        } else if (value == 2) {
          PanelControlsPropertiesNames.this.action = PanelControlsPropertiesNames.this.action_deselect;
        } else if (value == 3) {
          PanelControlsPropertiesNames.this.action = PanelControlsPropertiesNames.this.action_invert;
        }
      }
    });

    this.choose_action.add("Select", 1);
    this.choose_action.add("Deselect  ", 2);
    this.choose_action.add("Invert", 3);
    this.choose_action.choice.select(this.choose_action.num_to_str(1));

    bottom.add(this.choose_action.choice);

    this.button_select_prefix.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(
            new NewMessage(PanelControlsPropertiesNames.this.textfield
                .getText()) {
              public Object execute() {
                final String pfx = (String) this.context;
                // final String from = SelectionManager.getPrefixOfSelection();
                // final StringPair pair = new StringPair(from, to);
                final Executor ex = new Executor(pfx) {
                  public Object execute(Object parameter) {
                    final String pfx = (String) this.input;
                    final BaseElement be = (BaseElement) parameter;
                    if (be.name != null) {
                      if (be.name.startsWith(pfx)) {
                        PanelControlsPropertiesNames.this.action.execute(be);
                        //
                      }
                    }
                    return null;
                  }
                };
                SelectionManager.applyToEachObject(ex);
                FrEnd.updateGUIToReflectSelectionChange();
                return null;
              }
            });
      }
    });

    bottom.add(this.button_select_prefix);

    this.panel.add(bottom);
  }

  public void updatePrefix() {
    final String combined = SelectionManager.combineSelection(this.matcher);
    this.textfield.setText(combined);
  }

  private String extractSharedPrefix(String current, String name) {
    if (current == null) {
      return name;
    }

    if (name.equals(current)) {
      return name;
    }

    int same_up_to = 0;
    final int min_len = Math.min(name.length(), current.length());
    for (int i = 0; i < min_len; i++) {
      final char c1 = name.charAt(i);
      final char c2 = current.charAt(i);
      if (c1 != c2) {
        break;
      }
      same_up_to++;
    }

    return name.substring(0, same_up_to);
  }

  private String extractSharedSuffix(String current, String name) {
    if (current == null) {
      return name;
    }

    if (name.equals(current)) {
      return name;
    }

    int same_up_to = 0;
    final int min_len = Math.min(name.length(), current.length());
    for (int i = 0; i < min_len; i++) {
      final char c1 = name.charAt(name.length() - i - 1);
      final char c2 = current.charAt(current.length() - i - 1);
      if (c1 != c2) {
        break;
      }
      same_up_to++;
    }

    return name.substring(name.length() - same_up_to);
  }

  public String sanitise(String name) {
    if ("".equals(name)) {
      return null;
    }

    String proposed = name;
    final char c0 = name.charAt(0);
    if ((c0 >= '0') && (c0 <= '9')) {
      proposed = '_' + proposed;
    }

    final StringBuffer result = new StringBuffer();
    for (int i = 0; i < proposed.length(); i++) {
      char c = proposed.charAt(i);
      if (c <= 32) {
        c = '_';
      }
      if (Character.isJavaIdentifierPart(c)) {
        result.append(c);
      }
    }
    return result.toString();
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }

  public void updateSuffix() {
    // TODO Auto-generated method stub

  }
}
