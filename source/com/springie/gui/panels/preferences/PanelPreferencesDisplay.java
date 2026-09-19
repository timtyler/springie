// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import com.springie.FrEnd;
import com.springie.constants.Quality;
import com.springie.gui.components.TTChoice;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.raytraced.ModularRendererRaytraced;
import com.tifsoft.Forget;

public class PanelPreferencesDisplay {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_renderer = FrEnd.setUpPanelForFrame();

  public Panel panel_frame = new Panel();

  public Panel panel_main = FrEnd.setUpPanelForFrame();

  // The Renderer: dropdown appears twice: once at the top of the
  // shared Renderer tab (modern and ray-traced renderers) and once in
  // the original renderer's own Renderer tab, so the renderer can
  // always be switched back. The copies stay in sync; Choice.select()
  // does not fire item events, so syncing never recurses.
  private final List<TTChoice> display_type_choices = new ArrayList<>();

  private TTChoice choose_antialiasing;

  private TTChoice choose_pixellation;

  public Label label_fps_value;

  public PanelPreferencesDisplay() {
    makePanel();
  }

  /**
   * Builds one Renderer: row: the three renderer options. Every copy
   * shares the same listener, which applies the newly chosen renderer
   * and keeps the copies in sync.
   */
  private Panel makeDisplayTypePanel() {
    final Panel panel = new Panel();
    panel.add(new Label("Renderer:"));

    final TTChoice choice = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        PanelPreferencesDisplay.this.onDisplayTypeChanged((String) e.getItem());
      }
    });

    choice.add("Polygon renderer  ", Quality.SOLID);
    choice.add("Ray-traced renderer", Quality.RAYTRACED);
    choice.add("Original renderer ", Quality.THICK_OUTLINE);
    choice.choice.select(choice.num_to_str(Quality.SOLID));

    panel.add(choice.choice);
    this.display_type_choices.add(choice);
    return panel;
  }

  /**
   * Applies the newly chosen renderer and keeps every Display type
   * copy showing the same choice. The first registered copy serves as
   * the string-to-value prototype (all copies hold identical items);
   * it always exists here because item events only fire on user
   * interaction, long after construction. Choice.select() does not
   * fire item events, so the sync cannot recurse.
   */
  private void onDisplayTypeChanged(String selected) {
    final TTChoice proto = this.display_type_choices.get(0);
    final int value = proto.str_to_num(selected);
    for (final TTChoice c : this.display_type_choices) {
      c.choice.select(c.num_to_str(value));
    }
    applyRendererType(value);
  }

  void makePanel() {
    // The Renderer: dropdown, the frames-per-second readout, the
    // anti-aliasing and pixellation choices, the "deepest first"
    // checkbox and the "Show labels on" choice live at the top of the
    // shared Renderer tab's Main sub-tab (modern and ray-traced
    // renderers), so the rendering settings sit together; one Renderer:
    // copy also goes
    // in the original renderer's own Renderer tab, so the renderer can
    // always be switched back whichever is showing. Anti-aliasing and
    // pixellation only apply to the modern and ray-traced renderers;
    // "Show labels on" only applies to the modern renderer and is
    // hidden otherwise (see applyRendererType).
    // All three panels exist already: they are built before this one.
    // The Renderer tab's "Main" sub-tab holds the top rows: the display
    // type, the frames-per-second readout, the anti-aliasing and
    // pixellation choices, the "deepest first" checkbox and the "Show
    // labels on" choice.
    final Panel renderer_main = FrEnd.panel_preferences_shared_show.panel_main;
    renderer_main.add(makeDisplayTypePanel(), 0);
    renderer_main.add(getFpsPanel(), 1);
    renderer_main.add(getAntiAliasingPanel(), 2);
    renderer_main.add(getPixellationPanel(), 3);
    renderer_main.add(
        FrEnd.panel_preferences_shared_misc.panel_redraw_deepest_first, 4);
    renderer_main.add(
        FrEnd.panel_preferences_renderer_modern.panel_labels_row, 5);
    FrEnd.panel_preferences_renderer_original.panel_renderer_tab
        .add(makeDisplayTypePanel(), 0);

    // ...

//    final TTChoice choose_display_struts = new TTChoice(new ItemListener() {
//      public void itemStateChanged(ItemEvent e) {
//        final String scs = (String) e.getItem();
//        Link.link_display_struts_type = FrEnd.choose_display_struts
//            .str_to_num(scs);
//        RendererDelegator.repaintAll();
//      }
//    });
//
//    FrEnd.choose_display_struts = choose_display_struts;

    // The Display panel is the renderer tab bar; the rendering
    // settings (Display type, anti-aliasing, frames-per-second) live
    // under its Renderer tab.
    this.panel.add(this.panel_renderer);

    this.panel_frame.setLayout(new BorderLayout());

    this.panel_frame.add(this.panel_main, "Center");

    this.panel_main.add(FrEnd.panel_preferences_renderer_modern.panel, "Center");

    this.panel_renderer.setLayout(new BorderLayout());

    this.panel_renderer.add(this.panel_frame, "Center");

    // Common preferences...

//    this.panel_shared.add(panel_render_normal);
//    this.panel_shared.add(panel_render_hidden);
//    this.panel_shared.add(panel_redraw_deepest_first);
//    this.panel_shared.add(panel_fog);
//    this.panel_shared.add(panel_visible_explosions);
//    if (FrEnd.development_version) {
//      this.panel_shared.add(panel_fps);
//    }
  }

  private Panel getAntiAliasingPanel() {
    final Panel panel = new Panel();
    panel.add(new Label("Anti-aliasing:", Label.RIGHT));

    this.choose_antialiasing = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        final String scs = (String) e.getItem();
        RendererDelegator.antialiasing =
            PanelPreferencesDisplay.this.choose_antialiasing.str_to_num(scs);
        // The cached tiles are the wrong resolution now.
        FrEnd.main_canvas.forceResize();
      }
    });

    this.choose_antialiasing.add("1x1", 1);
    this.choose_antialiasing.add("2x2", 2);
    this.choose_antialiasing.add("3x3", 3);
    this.choose_antialiasing.add("4x4", 4);
    this.choose_antialiasing.add("5x5", 5);
    this.choose_antialiasing.choice
        .select(this.choose_antialiasing.num_to_str(1));
    panel.add(this.choose_antialiasing.choice);

    return panel;
  }

  private Panel getPixellationPanel() {
    final Panel panel = new Panel();
    panel.add(new Label("Pixellated:", Label.RIGHT));

    this.choose_pixellation = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        final String scs = (String) e.getItem();
        RendererDelegator.pixellation =
            PanelPreferencesDisplay.this.choose_pixellation.str_to_num(scs);
        // The cached tiles are the wrong resolution now.
        FrEnd.main_canvas.forceResize();
      }
    });

    this.choose_pixellation.add("1x1", 1);
    this.choose_pixellation.add("2x2", 2);
    this.choose_pixellation.add("3x3", 3);
    this.choose_pixellation.add("4x4", 4);
    this.choose_pixellation.add("5x5", 5);
    this.choose_pixellation.choice
        .select(this.choose_pixellation.num_to_str(1));
    panel.add(this.choose_pixellation.choice);

    return panel;
  }

  private Panel getFpsPanel() {
    // Frames-per-second readout, kept next to the renderer options so it
    // is visible while tuning the rendering settings.
    final Panel panel_fps = new Panel();
    panel_fps.add(new Label("Frames per second:", Label.RIGHT));
    this.label_fps_value = new Label("X.XXXX", Label.LEFT);
    panel_fps.add(this.label_fps_value);
    return panel_fps;
  }

  private void applyRendererType(int value) {
    this.panel_main.removeAll();
    final boolean raytraced = value == Quality.RAYTRACED;
    FrEnd.panel_preferences_renderer_modern.setRaytracedRowsVisible(raytraced);
    // The depth sort does not apply to the ray-traced renderer, so its
    // option is hidden there (and the sort itself is skipped).
    FrEnd.panel_preferences_renderer_modern
        .setDeepestFirstRowVisible(!raytraced);
    // The rasterizer-only rows ("Node polyhedron", "Cable/Strut
    // divisions", "Strut/cable sides", "Face lines") configure concepts
    // the ray-traced renderer ignores, so those rows are hidden there.
    FrEnd.panel_preferences_renderer_modern
        .setRaytracedHiddenRowsVisible(!raytraced);
    // Labels are a modern-renderer feature, so the option is hidden for
    // the other renderers.
    FrEnd.panel_preferences_renderer_modern
        .setLabelsRowVisible(value == Quality.SOLID);
    if (value == Quality.THICK_OUTLINE) {
      RendererDelegator.renderer = new com.springie.render.modules.original.ModularRendererOld();
      this.panel_main.add(FrEnd.panel_preferences_renderer_original.panel,
          "Center");
    } else if (raytraced) {
      RendererDelegator.renderer = new ModularRendererRaytraced();
      // Shares the modern panel: the bin size lives there, and the
      // ray-traced renderer uses the same bins as the default renderer.
      this.panel_main.add(FrEnd.panel_preferences_renderer_modern.panel,
          "Center");
    } else {
      RendererDelegator.renderer = new ModularRendererNew();
      this.panel_main.add(FrEnd.panel_preferences_renderer_modern.panel,
          "Center");
    }
    this.panel_main.validate();
    FrEnd.main_canvas.forceResize();
  }

  /**
   * Restores the default renderer (Modern) and resets every renderer
   * preference panel.
   */
  public void resetToDefaults() {
    // In case it was already selected (no item event fires then),
    // select the default on every copy before applying.
    for (final TTChoice c : this.display_type_choices) {
      c.choice.select(c.num_to_str(Quality.SOLID));
    }
    applyRendererType(Quality.SOLID);

    // Anti-aliasing: 1x1 is off.
    RendererDelegator.antialiasing = 1;
    this.choose_antialiasing.choice
        .select(this.choose_antialiasing.num_to_str(1));

    // Pixellation: 1x1 is off.
    RendererDelegator.pixellation = 1;
    this.choose_pixellation.choice
        .select(this.choose_pixellation.num_to_str(1));

    FrEnd.panel_preferences_renderer_original.resetToDefaults();
    FrEnd.panel_preferences_renderer_modern.resetToDefaults();
    FrEnd.panel_preferences_renderer_raytraced.resetToDefaults();
    FrEnd.panel_preferences_renderer_modern_filters.resetToDefaults();
    FrEnd.panel_preferences_renderer_modern_colours.resetToDefaults();
    FrEnd.panel_preferences_shared_show.resetToDefaults();
    FrEnd.panel_preferences_shared_misc.resetToDefaults();
  }
}
