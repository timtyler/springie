// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.gui.components.TTChoice;
import com.springie.messages.MessageManager;
import com.springie.preferences.Preferences;
import com.tifsoft.Forget;

public class PanelPreferencesIO {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  MessageManager message_manager;

  private Scrollbar scroll_bar_import_scale;

  private Scrollbar scroll_bar_pov_view_height;

  private Scrollbar scroll_bar_pov_immersion_depth;

  private Label label_pov_view_height;

  private Label label_pov_immersion_depth;

  private Label label_import_scale;

  public static int import_scale = 94;

  public static int pov_immersion_depth;

  public static int pov_view_height = 50;

  private TTChoice choose_pov_ground;

  private TTChoice choose_pov_compression;

  public PanelPreferencesIO(MessageManager message_manager) {
    this.message_manager = message_manager;
    makePanel();
  }

  void makePanel() {
    final Panel panel_merge = new Panel();
    final Checkbox checkbox_merge = new Checkbox(
        "Merge new structures with the scene");
    checkbox_merge.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        FrEnd.merge = ((Checkbox) e.getSource()).getState();
      }
    });
    panel_merge.add(checkbox_merge);

    this.panel.add(panel_merge);
    this.panel.add(setUpSliderImportScale());
    this.panel.add(setUpSliderPOVViewHeight());
    this.panel.add(setUpSliderPOVImmersionDepth());
    this.panel.add(getPOVSkyPanel());
    this.panel.add(getPOVGroundPanel());
    this.panel.add(getPOVCompressionPanel());
  }

  private Panel setUpSliderImportScale() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Import scale:", Label.RIGHT));

    this.scroll_bar_import_scale = new Scrollbar(Scrollbar.HORIZONTAL,
        import_scale, 10, 0, 110);
    this.scroll_bar_import_scale
        .addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e) {
            import_scale = e.getValue();
            reflectImportScale();
          }
        });
    panel.add("Center", this.scroll_bar_import_scale);
    this.label_import_scale = new Label("" + import_scale, Label.LEFT);

    panel.add("East", this.label_import_scale);

    return panel;
  }

  public void reflectImportScale() {
    this.scroll_bar_import_scale.setValue(import_scale);
    this.label_import_scale.setText("" + import_scale);
  }

  private Panel setUpSliderPOVImmersionDepth() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("POV: Immersion depth:", Label.RIGHT));

    this.scroll_bar_pov_immersion_depth = new Scrollbar(Scrollbar.HORIZONTAL,
        pov_immersion_depth, 10, 0, 110);
    this.scroll_bar_pov_immersion_depth
        .addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e) {
            pov_immersion_depth = e.getValue();
            reflectPOVImmersionDepth();
          }
        });
    panel.add("Center", this.scroll_bar_pov_immersion_depth);
    this.label_pov_immersion_depth = new Label("" + pov_immersion_depth,
        Label.LEFT);

    panel.add("East", this.label_pov_immersion_depth);

    return panel;
  }

  public void reflectPOVImmersionDepth() {
    this.scroll_bar_pov_immersion_depth.setValue(pov_immersion_depth);
    this.label_pov_immersion_depth.setText("" + pov_immersion_depth);
  }

  private Panel setUpSliderPOVViewHeight() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("POV: View height:", Label.RIGHT));

    this.scroll_bar_pov_view_height = new Scrollbar(Scrollbar.HORIZONTAL,
        pov_view_height, 10, 0, 110);
    this.scroll_bar_pov_view_height
        .addAdjustmentListener(new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e) {
            pov_view_height = e.getValue();
            reflectPOVViewHeight();
          }
        });
    panel.add("Center", this.scroll_bar_pov_view_height);
    this.label_pov_view_height = new Label("" + pov_view_height, Label.LEFT);

    panel.add("East", this.label_pov_view_height);

    return panel;
  }

  public void reflectPOVViewHeight() {
    this.scroll_bar_pov_view_height.setValue(pov_view_height);
    this.label_pov_view_height.setText("" + pov_view_height);
  }

  private Panel getPOVCompressionPanel() {
    final Panel panel = new Panel();
    panel.add(new Label("Render struts using:", Label.RIGHT));
    this.choose_pov_compression = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        FrEnd.preferences.map.put(Preferences.key_output_pov_compression, e
            .getItem());
      }
    });

    this.choose_pov_compression.add("bulge", 0);
    this.choose_pov_compression.add("straght", 1);
    this.choose_pov_compression.add("point", 2);

    this.choose_pov_compression.choice.select((String) FrEnd.preferences.map
        .get(Preferences.key_output_pov_compression));

    panel.add(this.choose_pov_compression.choice);

    return panel;
  }

  private Panel getPOVGroundPanel() {
    final Panel panel = new Panel();
    panel.add(new Label("POV: Ground:", Label.RIGHT));
    this.choose_pov_ground = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        FrEnd.preferences.map.put(Preferences.key_output_pov_ground, e
            .getItem());
      }
    });

    this.choose_pov_ground.add("none", 0);
    this.choose_pov_ground.add("water", 1);
    this.choose_pov_ground.add("rock", 2);
    this.choose_pov_ground.add("wood", 3);

    this.choose_pov_ground.choice.select((String) FrEnd.preferences.map
        .get(Preferences.key_output_pov_ground));

    panel.add(this.choose_pov_ground.choice);
    return panel;
  }

  private Panel getPOVSkyPanel() {
    final Panel panel = new Panel();
    panel.add(new Label("POV: Sky:", Label.RIGHT));
    final TTChoice choose_pov_sky = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        FrEnd.preferences.map.put(Preferences.key_output_pov_sky, e.getItem());
      }
    });

    choose_pov_sky.add("cirrus", 0);
    choose_pov_sky.add("cloud 1", 1);
    choose_pov_sky.add("cloud 2", 2);
    choose_pov_sky.add("wispy", 3);
    choose_pov_sky.add("white", 4);
    choose_pov_sky.add("black", 5);

    choose_pov_sky.choice.select((String) FrEnd.preferences.map
        .get(Preferences.key_output_pov_sky));

    panel.add(choose_pov_sky.choice);
    return panel;
  }

  protected TTChoice getChooseRightAction() {
    return this.choose_pov_ground;
  }

  public MessageManager getMessageManager() {
    return this.message_manager;
  }
}