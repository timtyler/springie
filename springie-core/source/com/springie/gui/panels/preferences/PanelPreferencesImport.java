// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import com.springie.FrEnd;
import com.springie.io.IoConfig;
import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class PanelPreferencesImport {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  private Scrollbar scroll_bar_import_scale;

  private Label label_import_scale;

  private Checkbox checkbox_merge;

  // IoConfig.import_scale now in IoConfig (io module).

  public PanelPreferencesImport() {
    makePanel();
  }

  void makePanel() {
    final Panel panel_merge = new Panel();
    this.checkbox_merge = new Checkbox(
        "Merge new structures with the scene");
    this.checkbox_merge.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        IoConfig.merge = ((Checkbox) e.getSource()).getState();
      }
    });
    panel_merge.add(this.checkbox_merge);

    this.panel.add(panel_merge);
    this.panel.add(setUpSliderImportScale());
  }

  private Panel setUpSliderImportScale() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Import scale:", Label.RIGHT));

    this.scroll_bar_import_scale = new Scrollbar(Scrollbar.HORIZONTAL,
        IoConfig.import_scale, 10, 0, 110);
    this.scroll_bar_import_scale
        .addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(final AdjustmentEvent e) {
            IoConfig.import_scale = e.getValue();
            reflectImportScale();
          }
        });
    panel.add("Center", this.scroll_bar_import_scale);
    this.label_import_scale = new Label("" + IoConfig.import_scale, Label.LEFT);

    panel.add("East", this.label_import_scale);

    return panel;
  }

  public void reflectImportScale() {
    this.scroll_bar_import_scale.setValue(IoConfig.import_scale);
    this.label_import_scale.setText("" + IoConfig.import_scale);
  }

  /**
   * Restores the default import preferences.
   */
  public void resetToDefaults() {
    IoConfig.merge = false;
    this.checkbox_merge.setState(false);

    IoConfig.import_scale = 94;
    reflectImportScale();
  }
}
