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
import com.springie.gui.GUIStrings;
import com.springie.render.RendererDelegator;
import com.springie.render.Coords;

public class PanelPreferencesViewpoint {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  /**
   * Viewport overlay: draws the boundary box as a sequence of white dots,
   * one dot per frame. Not part of the universe -- it is a view aid, so it
   * lives here rather than on the Universe tab.
   */
  public Checkbox checkbox_show_boundary_box;

  private Scrollbar scroll_bar_translate_x;

  private Scrollbar scroll_bar_translate_y;

  private Scrollbar scroll_bar_translate_z;

  private Label label_translate_x;

  private Label label_translate_y;

  private Label label_translate_z;

  public PanelPreferencesViewpoint() {
    makePanel();
  }

  void makePanel() {
    this.panel.add(getBoundaryBoxPanel());

    final Panel panel_translate_view_x = getTranslateViewXPanel();

    final Panel panel_translate_view_y = getTranslateViewYPanel();

    final Panel panel_translate_view_z = getTranslateViewZPanel();

    this.panel.add(panel_translate_view_x);
    this.panel.add(panel_translate_view_y);
    this.panel.add(panel_translate_view_z);
  }

  private Panel getBoundaryBoxPanel() {
    final Panel panel = new Panel();
    this.checkbox_show_boundary_box =
        new Checkbox(GUIStrings.SHOW_BOUNDARY_BOX);
    this.checkbox_show_boundary_box.setState(FrEnd.show_boundary_box);
    this.checkbox_show_boundary_box.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        FrEnd.show_boundary_box =
            getCheckboxShowBoundaryBox().getState();
        // A full redraw: the dots are painted in screen space, so turning
        // the box off must clear them from the whole canvas, and the flag
        // alone does not schedule a paint when the animation is paused.
        RendererDelegator.repaint_all_objects = true;
        FrEnd.main_canvas.panel.repaint();
      }
    });
    panel.add(this.checkbox_show_boundary_box);
    return panel;
  }

  public Checkbox getCheckboxShowBoundaryBox() {
    return this.checkbox_show_boundary_box;
  }

  private Panel getTranslateViewXPanel() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Translate X:", Label.RIGHT));
    this.scroll_bar_translate_x = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10,
      -110, 110);
    this.scroll_bar_translate_x.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        RendererDelegator.repaintAll();
        Coords.shift_constant_x = e.getValue() << 11;
        reflectTranslateX();
      }
    });
    panel.add("Center", this.scroll_bar_translate_x);

    this.label_translate_x = new Label("0", Label.LEFT);
    panel.add("East", this.label_translate_x);
    reflectTranslateX();

    return panel;
  }

  private Panel getTranslateViewYPanel() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Translate Y:", Label.RIGHT));
    this.scroll_bar_translate_y = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10,
      -110, 110);
    this.scroll_bar_translate_y.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        Coords.shift_constant_y = e.getValue() << 11;
        RendererDelegator.repaint_some_objects = true;
        reflectTranslateY();
      }
    });
    panel.add("Center", this.scroll_bar_translate_y);

    this.label_translate_y = new Label("0", Label.LEFT);
    panel.add("East", this.label_translate_y);
    RendererDelegator.repaintAll();
    reflectTranslateY();

    return panel;
  }

  private Panel getTranslateViewZPanel() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Translate Z:", Label.RIGHT));
    this.scroll_bar_translate_z = new Scrollbar(Scrollbar.HORIZONTAL, 10, 10,
      0, 110);
    this.scroll_bar_translate_z.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        Coords.shift_constant_z = e.getValue() << 3;
        RendererDelegator.repaintAll();
        reflectTranslateZ();
      }
    });
    panel.add("Center", this.scroll_bar_translate_z);

    this.label_translate_z = new Label("0", Label.LEFT);
    panel.add("East", this.label_translate_z);
    reflectTranslateZ();

    return panel;
  }

  public void reflectTranslateX() {
    final int tmp = Coords.shift_constant_x >> 11;

    this.scroll_bar_translate_x.setValue(tmp);

    this.label_translate_x.setText("" + tmp);
  }

  public void reflectTranslateY() {
    final int tmp = Coords.shift_constant_y >> 11;

    this.scroll_bar_translate_y.setValue(tmp);

    this.label_translate_y.setText("" + tmp);
  }

  public void reflectTranslateZ() {
    final int tmp = Coords.shift_constant_z >> 3;

    this.scroll_bar_translate_z.setValue(tmp);

    this.label_translate_z.setText("" + tmp);
  }

  /**
   * Restores the default viewpoint: no translation shift.
   */
  public void resetToDefaults() {
    // The boundary-box dots are on by default.
    FrEnd.show_boundary_box = true;
    this.checkbox_show_boundary_box.setState(true);
    Coords.shift_constant_x = 0;
    Coords.shift_constant_y = 0;
    Coords.shift_constant_z = Coords.shift_shifted - (Coords.shift_shifted >> 2);
    reflectTranslateX();
    reflectTranslateY();
    reflectTranslateZ();
  }

}
