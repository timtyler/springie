// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Iterator;

import org.xml.sax.SAXException;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.demos.DemoCatalog;
import com.springie.gui.components.ButtonBar;
import com.springie.gui.components.ChoiceWithDescription;
import com.springie.gui.components.ImageButton;
import com.springie.gui.components.TextFieldWrapper;
import com.springie.gui.components.WrapLayout;
import com.springie.gui.panels.preferences.ButtonMouseActionStrings;
import com.springie.io.out.writers.spr.WriterSpr;
import com.springie.messages.commands.DeleteSelectedMessage;
import com.springie.messages.commands.SelectClazzMessage;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.springie.presets.AddXMLModelIndexLeaves;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.utilities.FilePath;
import com.tifsoft.Forget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PanelFundamental {
  private static final Logger logger = LoggerFactory.getLogger(PanelFundamental.class);

  public Panel panel = FrEnd.setUpPanelForFrame2();

  NewMessageManager new_message_manager;

  public TextFieldWrapper textfield_generate_tube_length;

  public TextFieldWrapper textfield_generate_matrix_x;

  public TextFieldWrapper textfield_generate_matrix_y;

  public TextFieldWrapper textfield_generate_matrix_z;

  public TextFieldWrapper textfield_generate_tube_circumference;

  public TextFieldWrapper textfield_generate_free_nodes;

  public TextFieldWrapper textfield_generate_sphere_pack;

  public ImageButton button_paused;

  public ImageButton button_delete;

  public ImageButton button_select_all_of_class;

  /**
   * The green check-mark button. It only makes sense while the controls
   * live in their own frame; when docked they are already visible, so
   * FrEnd greys it out instead of popping up an empty frame.
   */
  public ImageButton button_controls;

  private static final String CARD_PRESETS = "presets";

  private static final String CARD_FILE = "file";

  private CardLayout card_layout;

  private Panel card_panel;

  Panel card_presets;

  Panel card_file;

  private Label label_file_name;

  public ImageButton button_file_presets;

  /**
   * The demos dropdown: the procedural demos have a menu of their own in
   * the presets card now, below the preset dropdowns, instead of sitting
   * mixed into the bottom of the preset leaf dropdown.
   */
  ChoiceWithDescription choose_demo;

  /**
   * Placeholder shown in the demos dropdown when no demo is current (a
   * model file is). Choosing it does nothing; it maps to no demo.
   */
  static final String DEMO_PLACEHOLDER = "Select...";

  public PanelFundamental(NewMessageManager new_message_manager) {
    this.new_message_manager = new_message_manager;
    makePanelGenerate();
    // Insets i = panel.insets();

    // panel.b
  }

  void makePanelGenerate() {
    initialSetup();
    // this.panel.setLayout(new GridLayout(1, 0, 0, 0));
    // A wrapping layout that reports its wrapped height, so the bar grows
    // to two rows when the window narrows instead of clipping the second
    // row (FlowLayout's preferred height ignores wrapping).
    this.panel.setLayout(new WrapLayout());

    makePanelMouseActions(this.panel);
    this.panel.add(makePanelControls());
    this.panel.add(makePanelPause());
    this.panel.add(makePanelStep());
    this.panel.add(makePanelDelete());
    this.panel.add(makePanelSelectAllOfClass());
    this.panel.add(makePanelZoomIn()); 
    this.panel.add(makePanelZoomOut()); 
    this.panel.add(makePanelFilePresetsToggle());
    this.panel.add(makePanelFileOrPresets());
    this.panel.add(makePanelRestart());
  }

  /**
   * The floppy-disc toggle: pressed in shows the two preset dropdowns
   * (the default); pressed out swaps them for the current filename plus
   * Load and Save buttons. The launch button sits outside the cards, so
   * it stays visible and active in both cases.
   */
  private Panel makePanelFilePresetsToggle() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 0));

    this.button_file_presets = new ImageButton("floppy", null, "File", true);
    this.button_file_presets.setRadio(true);
    this.button_file_presets
        .setTooltipText("Toggle between model presets and file load/save");

    this.button_file_presets.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        showPresetsCard(button_file_presets.getState());
      }
    });

    panel.add(this.button_file_presets);
    return panel;
  }

  private Panel makePanelFileOrPresets() {
    final Panel panel = new Panel();
    this.card_layout = new CardLayout();
    panel.setLayout(this.card_layout);

    this.card_presets = makePresetsCard();
    this.card_file = makeFileCard();
    panel.add(this.card_presets, CARD_PRESETS);
    panel.add(this.card_file, CARD_FILE);
    this.card_panel = panel;

    this.card_layout.show(panel, CARD_PRESETS);
    return panel;
  }

  private Panel makePresetsCard() {
    final Panel panel = new Panel();
    panel.setLayout(new WrapLayout());
    panel.add(makePanelPresetIndex());
    panel.add(makePanelInitialCvonfiguration());
    return panel;
  }

  private Panel makeFileCard() {
    final Panel panel = new Panel();
    panel.setLayout(new WrapLayout());

    this.label_file_name = new Label(leafOf(FrEnd.last_file_path));
    panel.add(this.label_file_name);

    final Button button_load = new Button("Load");
    button_load.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        chooseLoadFile();
      }
    });
    panel.add(button_load);

    final Button button_save = new Button("Save");
    button_save.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        chooseSaveFile();
      }
    });
    panel.add(button_save);

    return panel;
  }

  void showPresetsCard(boolean show_presets) {
    if (show_presets) {
      this.card_layout.show(this.card_panel, CARD_PRESETS);
    } else {
      reflectFileName();
      this.card_layout.show(this.card_panel, CARD_FILE);
    }
  }

  /** Shows the leaf of the file named by the load menu / save dialog. */
  void reflectFileName() {
    this.label_file_name.setText(leafOf(FrEnd.last_file_path));
  }

  /** The Load button: the same file dialog as the Load menu. */
  void chooseLoadFile() {
    final FileDialog fd = new FileDialog(FrEnd.frame_main, "Load objects",
        FileDialog.LOAD);
    fd.setVisible(true);
    final String file = fd.getFile();
    final String directory = fd.getDirectory();
    fd.dispose();

    if (file != null && !"".equals(file)) {
      FrEnd.loadFile(new FilePath(directory, file));
      reflectFileName();
    }
  }

  /**
   * The Save button: writes straight back to the current file when it is
   * a local file from the load menu, otherwise falls back to a save-as
   * dialog like the menu's "Save .SPR file as...".
   */
  void chooseSaveFile() {
    final String current = FrEnd.last_file_path;
    if (isLocalFile(current)) {
      writeSprFile(stripFilePrefix(current));
      return;
    }

    final FileDialog fd = new FileDialog(FrEnd.frame_main,
        "Save .SPR file as:", FileDialog.SAVE);
    fd.setFile(ensureExtension(leafOf(current), "spr"));
    fd.setVisible(true);
    final String directory = fd.getDirectory();
    final String file = fd.getFile();
    fd.dispose();

    if (file != null && !"".equals(file)) {
      final String file_path = directory + file;
      writeSprFile(file_path);
      FrEnd.setFilePath("file://" + file_path);
      reflectFileName();
    }
  }

  private static void writeSprFile(String file_path) {
    new WriterSpr(ContextManager.getNodeManager()).write(file_path);
  }

  private static boolean isLocalFile(String path) {
    return !(path.startsWith("http:") || path.startsWith("https:")
        || path.startsWith("ftp:"));
  }

  private static String stripFilePrefix(String path) {
    if (path.startsWith("file://")) {
      return path.substring("file://".length());
    }

    return path;
  }

  private static String leafOf(String path) {
    final int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return path.substring(slash + 1);
  }

  private static String ensureExtension(String leaf, String extension) {
    final int len = leaf.length();
    final int idx = leaf.lastIndexOf(".");
    if (idx > len - 6) {
      return leaf.substring(0, idx) + "." + extension;
    }

    return leaf + "." + extension;
  }

  private Panel makePanelControls() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 0));
    // panel.setLayout(new GridLayout(1, 1, 0, 0));

    this.button_controls = new ImageButton("controls", null,
        "Controls", false);
    this.button_controls.setTooltipText("Show/hide the controls");

    this.button_controls.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        if (FrEnd.controls_window_mode == FrEnd.CONTROLS_DOCKED) {
          // The controls live docked in the main window: toggle the
          // docked panel, so the button still operates -- hiding them
          // gives the canvas the full window width.
          final java.awt.Panel controls_panel =
              FrEnd.panel_controls_all.panel;
          controls_panel.setVisible(!controls_panel.isVisible());
          FrEnd.frame_main.validate();
          return;
        }
        FrEnd.frame_controls.setVisible(true);
      }
    });

    panel.add(this.button_controls);
    return panel;
  }

  // private Panel makePanelPreferences() {
  // final Panel panel_controls_preferences = new Panel();
  // final Button button_controls_preferences = new Button(
  // GUIStrings.CONTROLS_PREFERENCES);
  // button_controls_preferences.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // FrEnd.frame_preferences.setVisible(true);
  // }
  // });
  // panel_controls_preferences.add(button_controls_preferences);
  // return panel_controls_preferences;
  // }

  // private Panel makePanelControls() {
  // final Panel panel_controls_edit = new Panel();
  // final Button button_controls_edit = new Button(GUIStrings.CONTROLS_EDIT);
  // button_controls_edit.addActionListener(new ActionListener() {
  // public void actionPerformed(ActionEvent e) {
  // Forget.about(e);
  // FrEnd.frame_controls.setVisible(true);
  // }
  // });
  // panel_controls_edit.add(button_controls_edit);
  // return panel_controls_edit;
  // }

  private Panel makePanelPresetIndex() {
    final Panel panel_preset_index = new Panel();
    panel_preset_index.add(FrEnd.choose_preset_index.choice);
    return panel_preset_index;
  }

  private Panel makePanelInitialCvonfiguration() {
    final Panel panel_initial_configuration = new Panel();
    panel_initial_configuration.add(FrEnd.choose_initial.choice);
    return panel_initial_configuration;
  }

  /**
   * The demos dropdown is a menu, not a selector: choosing a demo launches
   * it at once, through the exact message the Models > Demos menu
   * enqueues. Programmatic select() fires no item event, so selectDemo
   * (the menu's sync path) never double-launches.
   */
  private void setUpDemosChoice() {
    this.choose_demo = new ChoiceWithDescription(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }
        // The placeholder maps to no demo and is ignored.
        final DemoCatalog.Demo demo =
            DemoCatalog.forName((String) e.getItem());
        if (demo != null) {
          getNewMessageManager().add(demo.newMessage());
        }
      }
    });
    this.choose_demo.choice.addItem(DEMO_PLACEHOLDER);
    for (final DemoCatalog.Demo demo : DemoCatalog.DEMOS) {
      this.choose_demo.choice.addItem(demo.name);
    }
  }

  private Panel makePanelPause() {
    // pause...
    final Panel panel = new Panel();
    panel.setLayout(new GridLayout(1, 1, 0, 0));

    this.button_paused = new ImageButton("pause", null, "Pause", false);
    this.button_paused.setRadio(true);
    this.button_paused.setTooltipText("Pause / resume the simulation");

    this.button_paused.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        getNewMessageManager().add(FrEnd.system_messages.getPauseMessage());
      }
    });
    panel.add(this.button_paused);

    return panel;
  }

  private Panel makePanelStep() {
    // pause...
    final Panel panel = new Panel();
    panel.setLayout(new GridLayout(1, 1, 0, 0));

    // Step...
    FrEnd.button_step = new ImageButton("step", null, "Step", false);
    FrEnd.button_step.setTooltipText("Advance the simulation a number of steps");
    // this.button_paused.setRadio(true);
    // FrEnd.button_step = new Button(GUIStrings.STEP);
    FrEnd.button_step.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        getNewMessageManager().add(new NewMessage(null) {
          public Object execute() {
            // FrEnd.new_message_manager.add(getRestartMessage());
            FrEnd.paused = false;

            try {
              FrEnd.stepping = Integer.parseInt(FrEnd.textfield_step_size
                  .getText());
            } catch (RuntimeException e) {
              FrEnd.stepping = 1;
            }

            FrEnd.greyPauseAndRestartIfNeeded();
            // FrEnd.button_step.setLabel(GUIStrings.CANCEL);
            FrEnd.button_step.setEnabled(false);
            return null;
          }
        });
      }
    });

    FrEnd.button_step.setFont(FrEnd.bold_font);
    panel.add(FrEnd.button_step);

    return panel;
  }

  private Panel makePanelRestart() {
    final Panel panel = new Panel();
    // panel.setLayout(new GridLayout(1, 1, 0, 0));

    FrEnd.button_restart = new ImageButton("restart", null, "Restart", false);
    FrEnd.button_restart
        .setTooltipText("Restart the simulation from the initial state");

    FrEnd.button_restart.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        launchSelected();
      }
    });

    panel.setLayout(new BorderLayout(0, 0));
    panel.add(FrEnd.button_restart);
    return panel;
  }

  private Panel makePanelZoomIn() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 0));

    final ImageButton button_zoom_in = new ImageButton("zoom_in", null,
        "Zoom in", false);

    button_zoom_in.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);

        Coords.shift_constant_z -= 20;
        if (Coords.shift_constant_z < 1) {
          Coords.shift_constant_z = 1;
        }
        // Zooming changes the projection of everything on screen, so the
        // incremental tile repaint is not enough: repaint the whole screen.
        RendererDelegator.repaint_all_objects = true;
        FrEnd.panel_preferences_viewpoint.reflectTranslateZ();
      }
    });

    panel.add(button_zoom_in);
    return panel;
  }

  
  private Panel makePanelZoomOut() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 0));

    final ImageButton button = new ImageButton("zoom_out", null, "Zoom out",
        false);

    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);

        Coords.shift_constant_z += 20;
        RendererDelegator.repaintAll();
        FrEnd.panel_preferences_viewpoint.reflectTranslateZ();
      }
    });

    panel.add(button);
    return panel;
  }

  private Panel makePanelMouseActions(Panel panel) {

    final ButtonBar bb = new ButtonBar();

    bb.add("select", ButtonMouseActionStrings.action_select);
    bb.add("rotate_xy", ButtonMouseActionStrings.action_rotate_xy);
    bb.add("rotate_z", ButtonMouseActionStrings.action_rotate_z);
    bb.add("translate", ButtonMouseActionStrings.action_translate);
    bb.add("link", ButtonMouseActionStrings.action_link);
    bb.add("kill", ButtonMouseActionStrings.action_kill);

    ItemListener il = new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String str = (String) (e.getItem());
        FrEnd.action_left_type = ButtonMouseActionStrings
            .stringToActionNumber(str);
      }
    };

    bb.addItemListener(il);

    bb.select(ButtonMouseActionStrings.action_select);

    panel.add(bb);
    return panel;
  }

  private Panel makePanelDelete() {
    final Panel panel = new Panel();
    panel.setLayout(new FlowLayout());

    this.button_delete = new ImageButton("delete", null, "Delete", false);
    this.button_delete.setTooltipText("Delete the selected elements");

    this.button_delete.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        getNewMessageManager().add(new DeleteSelectedMessage());
      }
    });

    panel.setLayout(new BorderLayout(0, 0));
    panel.add(this.button_delete);
    return panel;
  }

  private Panel makePanelSelectAllOfClass() {
    final Panel panel = new Panel();
    panel.setLayout(new FlowLayout());

    this.button_select_all_of_class = new ImageButton("select_all_of_class",
        null, "Select all of class", false);
    this.button_select_all_of_class
        .setTooltipText("Select all elements of the same class");

    this.button_select_all_of_class.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        Forget.about(arg0);
        getNewMessageManager().add(new SelectClazzMessage());
      }
    });

    panel.setLayout(new BorderLayout(0, 0));
    panel.add(this.button_select_all_of_class);
    return panel;
  }

  private void initialSetup() {
    setUpPresetIndex();

    try {
      new AddXMLModelIndexLeaves().addLeaves(FrEnd.choose_preset_index,
          FrEnd.model_index);
    } catch (IOException e1) {
      logger.error("Unexpected exception", e1);
    } catch (SAXException e1) {
      logger.error("Unexpected exception", e1);
    }

    setUpDemosChoice();
    setUpInitialChoice();
  }

  private void setUpInitialChoice() {
    FrEnd.choose_initial = new ChoiceWithDescription(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }

        final String string = (String) (e.getItem());

        final String path = (String) FrEnd.choose_initial.hashtable.get(string);
        if (path != null) {
          FrEnd.next_file_path = path;
          // A model file is current again: stand the demos dropdown back
          // down to its placeholder, so the launch button rebuilds the
          // file rather than the last demo.
          if (choose_demo != null) {
            choose_demo.choice.select(0);
          }
        }
      }
    });

    final String index_name = (String) FrEnd.choose_preset_index.hashtable
        .get("Presets");

    setUpLeafIndex(index_name);

    FrEnd.choose_initial.choice.addKeyListener(new KeyListener() {
      public void keyTyped(KeyEvent arg0) {
        Forget.about(arg0);
      }

      public void keyPressed(KeyEvent arg0) {
        if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
          launchSelected();
        }
      }

      public void keyReleased(KeyEvent arg0) {
        Forget.about(arg0);
      }
    });
  }

  private void setUpPresetIndex() {
    FrEnd.choose_preset_index = new ChoiceWithDescription(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e == null) {
          return;
        }

        final String string = (String) (e.getItem());
        final String path = (String) FrEnd.choose_preset_index.hashtable
            .get(string);

        setUpLeafIndex(path);
      }
    });
  }

  private void setUpLeafIndex(final String path) {
    FrEnd.choose_initial.removeAll();
    try {
      new AddXMLModelIndexLeaves().addLeaves(FrEnd.choose_initial, path);
    } catch (IOException e1) {
      logger.error("Unexpected exception", e1);
    } catch (SAXException e1) {
      logger.error("Unexpected exception", e1);
    }
    // The demos have their own dropdown now: switching the preset index
    // stands it back down to its placeholder, so the launch button follows
    // the visible file selection.
    if (this.choose_demo != null) {
      this.choose_demo.choice.select(0);
    }
    // Repopulating the leaf dropdown fires no item event, so next_file_path
    // would keep pointing at the previous index's model (and the restart
    // path, which loads next_file_path, would disagree with the preset
    // path, which reads the dropdown selection). Sync them here.
    final String selected = FrEnd.choose_initial.choice.getSelectedItem();
    FrEnd.next_file_path = (String) FrEnd.choose_initial.hashtable.get(selected);
  }

  /**
   * Shows the given preset in the bottom-bar dropdowns, as if the user had
   * picked it there: selects the index, repopulates the leaf dropdown for
   * that index, selects the leaf, and points next_file_path at it. Used by
   * the Models menu's Presets submenu so the two stay in agreement.
   */
  public void selectPreset(String index_name, String leaf_description) {
    if (FrEnd.choose_preset_index == null || FrEnd.choose_initial == null) {
      return;
    }
    if (!FrEnd.choose_preset_index.hashtable.containsKey(index_name)) {
      return;
    }
    // Programmatic select() fires no item event, so drive the same path
    // the index dropdown's own listener uses.
    FrEnd.choose_preset_index.choice.select(index_name);
    final String path =
        (String) FrEnd.choose_preset_index.hashtable.get(index_name);
    setUpLeafIndex(path);

    if (FrEnd.choose_initial.hashtable.containsKey(leaf_description)) {
      FrEnd.choose_initial.choice.select(leaf_description);
      FrEnd.next_file_path =
          (String) FrEnd.choose_initial.hashtable.get(leaf_description);
    }
  }

  /**
   * Shows the given demo in the bottom-bar demos dropdown, as if the user
   * had picked it there. Used by the Models menu's Demos submenu so the two
   * stay in agreement; Choice.select fires no item event, so the menu's own
   * launch message is the only one enqueued.
   */
  public void selectDemo(String name) {
    if (this.choose_demo == null) {
      return;
    }
    final java.awt.Choice choice = this.choose_demo.choice;
    for (int i = 0; i < choice.getItemCount(); i++) {
      if (choice.getItem(i).equals(name)) {
        choice.select(i);
        return;
      }
    }
  }

  /**
   * The launch button (and Enter in the leaf dropdown): when a demo is
   * chosen in the demos dropdown, enqueues the demo's own launch message --
   * the exact one the Models > Demos menu uses. Otherwise restarts from the
   * selected model file, as before.
   */
  void launchSelected() {
    final DemoCatalog.Demo demo = selectedDemo();
    if (demo != null) {
      getNewMessageManager().add(demo.newMessage());
    } else {
      getNewMessageManager().add(FrEnd.system_messages.getRestartMessage());
    }
  }

  /**
   * The demo currently chosen in the demos dropdown, or null when the
   * placeholder is showing (a model file is current).
   */
  DemoCatalog.Demo selectedDemo() {
    if (this.choose_demo == null) {
      return null;
    }
    // The placeholder maps to no demo.
    return DemoCatalog.forName(this.choose_demo.choice.getSelectedItem());
  }

  public static String getXMLIndexPath() {
    final Iterator<String> e = FrEnd.choose_preset_index.hashtable.keySet().iterator();
    final String initial = e.next();
    return initial;
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}
