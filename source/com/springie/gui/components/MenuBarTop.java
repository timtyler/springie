// This program has been placed into the public domain by its author.

package com.springie.gui.components;

import java.awt.CheckboxMenuItem;
import java.awt.FileDialog;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.context.ModelManager;
import com.springie.context.ModelSlot;
import com.springie.messages.commands.SnakeDemoMessage;
import com.springie.messages.commands.CrawlerDemoMessage;
import com.springie.gui.frames.FrameMain;
import com.springie.io.out.writers.eig.WriterEIG;
import com.springie.io.out.writers.fdl.WriterFDL;
import com.springie.io.out.writers.off.WriterOFF;
import com.springie.io.out.writers.pov.WriterPOV;
import com.springie.io.out.writers.spr.WriterSpr;
import com.springie.io.out.writers.wrl.WriterWRL;
import com.springie.presets.AddXMLModelIndexLeaves;
import com.springie.utilities.FilePath;
import com.tifsoft.Forget;

public class MenuBarTop extends MenuBar {
  static final long serialVersionUID = 1250; 

  private static final Logger logger = LoggerFactory.getLogger(MenuBarTop.class);
  static final String QUIT = "Quit";

  private static final String CONTROLS = "Controls...";

  //private static final String PREFERENCES = "Preferences...";

  static final String HELP = "Help...";

  static final String ABOUT = "About...";

  static final String LOAD_DATA = "Load objects...";

  static final String CLOSE_MODEL = "Close current model";

  static final String SAVE_AS_SPR = "Save .SPR file as...";

  static final String SAVE_AS_FDL = "Save .FDL file as...";

  static final String SAVE_AS_EIG = "Save .EIG file as...";

  static final String SAVE_AS_OFF = "Save .OFF file as...";

  static final String SAVE_AS_POV = "Save .POV file as...";

  static final String SAVE_AS_WRL = "Save .WRL file as...";

  FrameMain frame;

  public MenuBarTop(FrameMain frame) {
    this.frame = frame;

    this.add(makeLoadMenu());

    this.add(makeSaveMenu());

    this.add(makeModelsMenu());

    this.add(makeWindowMenu());

    this.add(makeHelpMenu());

    this.add(makeQuitMenu());
  }

  private Menu makeLoadMenu() {
    final Menu file = new Menu("Load", true);

    file.add(MenuBarTop.LOAD_DATA);

    file.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String arg = e.getActionCommand();

        if (LOAD_DATA.equals(arg)) {
          chooseLoad();
        } else if (QUIT.equals(arg)) {
          chooseQuit();
        }
      }
    });
    return file;
  }

  private Menu models_menu;

  private Menu makeModelsMenu() {
    this.models_menu = new Menu("Models", true);
    rebuildModelsMenu();
    ModelManager.addChangeListener(new Runnable() {
      public void run() {
        // Slot changes can originate off the menu (e.g. presets), so
        // hop onto the event thread before touching AWT.
        java.awt.EventQueue.invokeLater(new Runnable() {
          public void run() {
            rebuildModelsMenu();
          }
        });
      }
    });
    return this.models_menu;
  }

  private void rebuildModelsMenu() {
    final Menu menu = this.models_menu;
    menu.removeAll();

    if (this.presets_menu == null) {
      this.presets_menu = makePresetsMenu();
    }
    menu.add(this.presets_menu);
    menu.add(makeDemosMenu());
    menu.addSeparator();

    final java.util.List<ModelSlot> slots = ModelManager.getSlots();
    final int active = ModelManager.getActiveIndex();
    for (int i = 0; i < slots.size(); i++) {
      final ModelSlot slot = slots.get(i);
      final CheckboxMenuItem item = new CheckboxMenuItem(slot.name,
          i == active);
      final int index = i;
      item.addItemListener(new java.awt.event.ItemListener() {
        public void itemStateChanged(java.awt.event.ItemEvent e) {
          ModelManager.switchTo(index);
        }
      });
      menu.add(item);
    }
    menu.addSeparator();

    final MenuItem close = new MenuItem(CLOSE_MODEL);
    close.setEnabled(slots.size() > 1);
    close.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ModelManager.closeActiveSlot();
      }
    });
    menu.add(close);
  }

  private Menu presets_menu;

  /**
   * The preset models, mirroring the bottom-bar preset dropdowns: one
   * submenu per preset index, one item per model. Choosing one loads it
   * exactly as the restart button does. Built once and reused across menu
   * rebuilds, so the index files are not re-parsed on every slot change.
   */
  private Menu makePresetsMenu() {
    final Menu presets = new Menu("Presets");
    try {
      final LinkedHashMap<String, String> indexes =
          AddXMLModelIndexLeaves.getLeaves(FrEnd.model_index);
      for (Map.Entry<String, String> index : indexes.entrySet()) {
        final Menu index_menu = new Menu(index.getKey());
        final LinkedHashMap<String, String> leaves =
            AddXMLModelIndexLeaves.getLeaves(index.getValue());
        for (Map.Entry<String, String> leaf : leaves.entrySet()) {
          final MenuItem item = new MenuItem(leaf.getKey());
          final String index_name = index.getKey();
          final String leaf_name = leaf.getKey();
          final String path = leaf.getValue();
          item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Forget.about(e);
              // Keep the bottom-bar preset dropdowns showing the same
              // preset that was just chosen here.
              FrEnd.panel_fundamental.selectPreset(index_name, leaf_name);
              FrEnd.next_file_path = path;
              FrEnd.new_message_manager
                  .add(FrEnd.system_messages.getRestartMessage());
            }
          });
          index_menu.add(item);
        }
        presets.add(index_menu);
      }
    } catch (IOException | SAXException e) {
      logger.error("Could not build the Presets menu", e);
    }
    return presets;
  }

  /**
   * Procedural demos, built in code rather than loaded from files.
   */
  private Menu makeDemosMenu() {
    final Menu demos = new Menu("Demos");
    final MenuItem snake = new MenuItem("Snake");
    snake.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        FrEnd.new_message_manager.add(new SnakeDemoMessage());
      }
    });
    demos.add(snake);
    final MenuItem crawler = new MenuItem("Crawler");
    crawler.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Forget.about(e);
        FrEnd.new_message_manager.add(new CrawlerDemoMessage());
      }
    });
    demos.add(crawler);
    return demos;
  }

  private Menu makeWindowMenu() {
    final Menu file = new Menu("Window", true);

    file.add(MenuBarTop.CONTROLS);
    //file.add(MenuBarTop.PREFERENCES);

    file.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String arg = e.getActionCommand();

        if (MenuBarTop.CONTROLS.equals(arg)) {
          if (FrEnd.frame_controls != null) {
            FrEnd.frame_controls.setVisible(true);
          }
        //} else if (MenuBarTop.PREFERENCES.equals(arg)) {
          //FrEnd.frame_preferences.setVisible(true);
        }
      }
    });
    return file;
  }

  private Menu makeSaveMenu() {
    final Menu export = new Menu("Save", true);
    export.add(MenuBarTop.SAVE_AS_SPR);
    export.add(MenuBarTop.SAVE_AS_POV);
    export.add(MenuBarTop.SAVE_AS_WRL);
    export.add(MenuBarTop.SAVE_AS_EIG);
    export.add(MenuBarTop.SAVE_AS_OFF);
    if (FrEnd.development_version) {
      export.add(MenuBarTop.SAVE_AS_FDL);
    }

    export.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String arg = e.getActionCommand();

        if (SAVE_AS_FDL.equals(arg)) {
          chooseSaveAsFDL();
        } else if (SAVE_AS_SPR.equals(arg)) {
          chooseSaveAsSpr();
        } else if (SAVE_AS_OFF.equals(arg)) {
          chooseSaveAsOff();
        } else if (SAVE_AS_EIG.equals(arg)) {
          chooseSaveAsEig();
        } else if (SAVE_AS_WRL.equals(arg)) {
          chooseSaveAsWrl();
        } else if (SAVE_AS_POV.equals(arg)) {
          chooseSaveAsPov();
        }
      }
    });
    return export;
  }

  
  private Menu makeHelpMenu() {
    final Menu menu = new Menu("Help", true);
    menu.add(HELP);
    menu.add(ABOUT);
    menu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String arg = e.getActionCommand();
        if (HELP.equals(arg)) {
          FrEnd.frame_panel_help.setVisible(true);
        } else if (ABOUT.equals(arg)) {
          FrEnd.frame_panel_about.setVisible(true);
        }
      }
    });
    return menu;
  }

  private Menu makeQuitMenu() {
    final Menu menu = new Menu("Quit", true);
    menu.add(MenuBarTop.QUIT);
    menu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final String arg = e.getActionCommand();
        if (QUIT.equals(arg)) {
          chooseQuit();
        }
      }
    });
    return menu;
  }

  protected FrameMain getFrame() {
    return this.frame;
  }

  private void chooseLoad() {
    final FileDialog fd = new FileDialog(this.frame.getAppletFrame(),
      "Load objects", FileDialog.LOAD);
    fd.setVisible(true);
    final String returnedstring = fd.getFile();
    fd.setVisible(false);

    if (isAcceptableFileName(returnedstring)) {
      final FilePath fp = new FilePath(fd.getDirectory(), returnedstring);
      FrEnd.loadFile(fp);
    }
  }

  private void chooseSaveAsWrl() {
    final FileDialog fd = new FileDialog(this.frame.getAppletFrame(),
      "Save .WRL file as:", FileDialog.SAVE);

    final String no_ext = getLeaf(FrEnd.last_file_path);
    fd.setFile(ensureExtension(no_ext, "wrl"));

    fd.setFilenameFilter(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        Forget.about(dir);
        return !(name.endsWith(".wrl"));
      }
    });
    fd.setVisible(true);
    final String path = fd.getDirectory();
    final String leaf = fd.getFile();
    if (isAcceptableFileName(leaf)) {
      new WriterWRL(ContextManager.getNodeManager()).write(path + leaf);
    }
  }

  private void chooseSaveAsSpr() {
    final FileDialog fd = new FileDialog(this.frame.getAppletFrame(),
      "Save .SPR file as:", FileDialog.SAVE);

    final String leaf = getLeaf(FrEnd.last_file_path);
    fd.setFile(ensureExtension(leaf, "spr"));

    fd.setFilenameFilter(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        Forget.about(dir);
        return !(name.endsWith(".spr"));
      }
    });

    fd.setVisible(true);
    final String path = fd.getDirectory();
    final String leaf_name = fd.getFile();
    if (isAcceptableFileName(leaf_name)) {
      final String file_path = path + leaf_name;
      new WriterSpr(ContextManager.getNodeManager()).write(file_path);
      FrEnd.setFilePath("file://" + file_path);
    }
  }

  private void chooseSaveAsFDL() {
    final FileDialog fd = new FileDialog(this.frame.getAppletFrame(),
      "Save .FDL file as:", FileDialog.SAVE);

    final String leaf = getLeaf(FrEnd.last_file_path);
    fd.setFile(ensureExtension(leaf, "fdl"));

    fd.setFilenameFilter(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        Forget.about(dir);
        return !(name.endsWith(".fdl"));
      }
    });

    fd.setVisible(true);
    final String path = fd.getDirectory();
    final String leaf_name = fd.getFile();
    if (isAcceptableFileName(leaf_name)) {
      final String file_path = path + leaf_name;
      new WriterFDL(ContextManager.getNodeManager()).write(file_path);
      FrEnd.setFilePath("file://" + file_path);
    }
  }
  
  private void chooseSaveAsOff() {
    final FileDialog fd = new FileDialog(this.frame.getAppletFrame(),
      "Save .OFF file as:", FileDialog.SAVE);

    final String leaf = getLeaf(FrEnd.last_file_path);
    fd.setFile(ensureExtension(leaf, "off"));

    fd.setFilenameFilter(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        Forget.about(dir);
        return !(name.endsWith("off"));
      }
    });

    fd.setVisible(true);
    final String path = fd.getDirectory();
    final String leaf_name = fd.getFile();
    if (isAcceptableFileName(leaf_name)) {
      final String file_path = path + leaf_name;
      new WriterOFF(ContextManager.getNodeManager()).write(file_path);
      FrEnd.setFilePath("file://" + file_path);
    }
  }

  private void chooseSaveAsEig() {
    final FileDialog fd = new FileDialog(this.frame.getAppletFrame(),
      "Save .EIG file as:", FileDialog.SAVE);

    final String leaf = getLeaf(FrEnd.last_file_path);
    fd.setFile(ensureExtension(leaf, "eig"));

    fd.setFilenameFilter(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        Forget.about(dir);
        return !(name.endsWith("eig"));
      }
    });

    fd.setVisible(true);
    final String path = fd.getDirectory();
    final String leaf_name = fd.getFile();
    if (isAcceptableFileName(leaf_name)) {
      final String file_path = path + leaf_name;
      new WriterEIG(ContextManager.getNodeManager()).write(file_path);
      FrEnd.setFilePath("file://" + file_path);
    }
  }

  private void chooseSaveAsPov() {
    final FileDialog fd = new FileDialog(this.frame.getAppletFrame(),
      "Save .POV file as:", FileDialog.SAVE);

    final String leaf = getLeaf(FrEnd.last_file_path);
    fd.setFile(ensureExtension(leaf, "pov"));

    fd.setFilenameFilter(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        Forget.about(dir);
        return !(name.endsWith("pov"));
      }
    });

    fd.setVisible(true);
    final String path = fd.getDirectory();
    final String leaf_name = fd.getFile();
    if (isAcceptableFileName(leaf_name)) {
      final String file_path = path + leaf_name;
      new WriterPOV(ContextManager.getNodeManager()).write(file_path);
    }
  }

  private String ensureExtension(String leaf, String extension) {
    final int len = leaf.length();
    final int idx = leaf.lastIndexOf(".");
    if (idx > len - 6) {
      return leaf.substring(0, idx) + "." + extension;
    }
    
    return leaf + "." + extension;
  }

  private boolean isAcceptableFileName(String name) {
    if (name == null) {
      return false;
    }
    if ("".equals(name)) {
      return false;
    }

    return true;
  }

  private void chooseQuit() {
    this.frame.getApplet().stop();
    System.exit(0);
  }

  private String getLeaf(String path) {
    final int i = path.lastIndexOf("/") + 1;
    return path.substring(i);
  }
}