// This program has been placed into the public domain by its author.

package com.springie.gui.panels.preferences;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.gui.GUIStrings;
import com.springie.gui.components.TTChoice;
import com.springie.gui.components.TabbedPanel;
import com.springie.messages.NewMessageManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ElementRendererLink;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.modern.RendererTileManager;
import com.springie.render.modules.modern.SimpleC60;
import com.springie.render.modules.modern.SimpleCube;
import com.springie.render.modules.modern.SimpleDodecahedron;
import com.springie.render.modules.modern.SimpleHexagon;
import com.springie.render.modules.modern.SimpleIcosahedron;
import com.springie.render.modules.modern.SimpleOctahedron;
import com.springie.render.modules.modern.SimpleSquare;
import com.tifsoft.Forget;

public class PanelPreferencesRendererModern {
  public Panel panel = FrEnd.setUpPanelForFrame2();

  public Panel panel_tiles = FrEnd.setUpPanelForFrame2();

  /**
   * The "Show labels on:" row. It lives on the shared Renderer tab
   * (added there by PanelPreferencesDisplay), shown only while the
   * modern renderer is active -- labels are a modern-renderer feature.
   */
  public Panel panel_labels_row;

  public Panel panel_misc = FrEnd.setUpPanelForFrame2();

  /**
   * The ray-traced-only option rows, shown at the bottom of the
   * single-layout Renderer tab while the ray-traced renderer is
   * active. Added and removed by {@link #setRaytracedRowsVisible}.
   */
  Panel[] raytraced_rows;

  /**
   * The rows that do not apply to the ray-traced renderer ("Node
   * polyhedron", "Cable divisions", "Strut divisions", "Strut/cable
   * sides", "Face lines"): rasterizer-only settings, removed from the
   * Renderer tab's Main sub-tab while ray-tracing and restored after.
   */
  Panel[] raytraced_hidden_rows;

  NewMessageManager new_message_manager;

  private Checkbox checkbox_show_tiles;

  private Checkbox checkbox_show_active_tiles;

  private TTChoice choose_polyhedron;

  private Label label_tile_size_number;

  private Scrollbar scroll_bar_tile_size;

  private Label label_strut_divisions;

  private Scrollbar scroll_bar_strut_divisions;

  private Label label_cable_divisions;

  private Scrollbar scroll_bar_cable_divisions;

  private TTChoice choose_link_sides;

  TTChoice choose_label_when;

  public static int render_label_when = 2;

  public PanelPreferencesRendererModern(NewMessageManager new_message_manager) {
    this.new_message_manager = new_message_manager;
    makePanel();
  }

  void makePanel() {
    final TabbedPanel tab = new TabbedPanel();
    // The shared renderer options live here now, combined with the
    // renderer-specific tabs to save the space of a second tab bar.
    // The top-level tabs are "Renderer" (the Main options), "Colours",
    // "Tiles" and "Fog": the ray-traced-only options (Glossiness,
    // Shadows, Specular, Fresnel, Fill light) are added and removed
    // at the bottom of the Renderer tab when the renderer is
    // switched -- a GridLayout gives invisible components space, so
    // setVisible cannot hide them.
    this.raytraced_rows = FrEnd.panel_preferences_renderer_raytraced.takeEffectRows();
    tab.add("Renderer", FrEnd.panel_preferences_shared_show.panel);

    tab.add("Colours", FrEnd.panel_preferences_renderer_modern_colours.panel);

    tab.add("Tiles", FrEnd.panel_preferences_shared_show.panel_tiles);

    tab.add("Fog", FrEnd.panel_preferences_shared_show.panel_fog);

    this.panel.add(tab);

    final Panel panel_node_polyhedron = panelNodePolyhedron();

    getPanelTiles();

    final Panel panel_cable_divisions = getPanelCableDivisions();
    final Panel panel_strut_divisions = getPanelStrutDivisions();
    final Panel panel_link_sides = panelLinkSides();

    this.panel_misc.add(panel_node_polyhedron);

    this.panel_misc.add(panel_cable_divisions);
    this.panel_misc.add(panel_strut_divisions);
    this.panel_misc.add(panel_link_sides);

    this.raytraced_hidden_rows = new Panel[] {
        panel_node_polyhedron, panel_cable_divisions,
        panel_strut_divisions, panel_link_sides,
        FrEnd.panel_preferences_shared_misc.panel_face_lines };

    this.panel_labels_row = getPanelLabelsWhen();

    // The Tiles rows move to the "Tiles" tab, the modern Misc rows
    // (node/tube tessellation) to the "Renderer" tab, and the shared
    // Misc rows join them there -- except the fog rows, which get the
    // "Fog" tab. ("Render deepest objects first" lives on the
    // Renderer tab instead.) PanelPreferencesDisplay inserts the top
    // rows at fixed indices afterwards, so the final order is stable.
    final PanelPreferencesRendererSharedShow shared_show =
        FrEnd.panel_preferences_shared_show;
    moveRowsInto(shared_show.panel_tiles, this.panel_tiles);
    moveRowsInto(shared_show.panel_main, this.panel_misc);
    FrEnd.panel_preferences_shared_misc.moveRowsInto(
        shared_show.panel_main, shared_show.panel_fog);
  }

  /**
   * Moves every row from one panel into another, leaving the source
   * empty afterwards.
   */
  private static void moveRowsInto(Panel target, Panel source) {
    final Component[] rows = source.getComponents();
    source.removeAll();
    for (final Component row : rows) {
      target.add(row);
    }
  }

  /**
   * Shows or hides the ray-traced-only option rows at the bottom of
   * the Renderer tab. The rows are added and removed (rather than
   * shown/hidden) because the tab's GridLayout gives invisible
   * components space. Idempotent: any rows already present are
   * removed first, so a repeated call cannot duplicate them.
   */
  void setRaytracedRowsVisible(boolean visible) {
    final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
    for (final Panel row : this.raytraced_rows) {
      tab.remove(row);
    }
    if (visible) {
      for (final Panel row : this.raytraced_rows) {
        tab.add(row);
      }
    }
    tab.validate();
  }

  /**
   * Shows or hides the "Render deepest objects first" row on the
   * Renderer tab's Main sub-tab. The depth sort is meaningless for
   * the ray-traced
   * renderer (occlusion is resolved per ray by the BVH), so the row is
   * removed while ray-tracing is active and restored for the
   * rasterizer renderers. Like the ray-traced rows, it is added and
   * removed (never just hidden) because the tab's GridLayout gives
   * invisible components space. Idempotent: the row is removed first,
   * so a repeated call cannot duplicate it.
   */
  void setDeepestFirstRowVisible(boolean visible) {
    final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
    final Panel row =
      FrEnd.panel_preferences_shared_misc.panel_redraw_deepest_first;
    tab.remove(row);
    if (visible) {
      // Keep the row in its usual slot, right after Pixellation.
      tab.add(row, Math.min(4, tab.getComponentCount()));
    }
    tab.validate();
  }

  /**
   * Shows or hides the rows that do not apply to the ray-traced
   * renderer ("Node polyhedron", "Cable divisions", "Strut divisions",
   * "Strut/cable sides", "Face lines") on the Renderer tab's Main
   * sub-tab. They configure rasterizer-only concepts the ray-traced
   * renderer ignores, so they are removed while ray-tracing is active
   * and restored to their usual slot (just above the explosions row)
   * for the rasterizers. Like the other conditional rows they are
   * added and removed (never just hidden) because the tab's GridLayout
   * gives invisible components space. Idempotent: the rows are removed
   * first, so a repeated call cannot duplicate them. The restore slot
   * is found dynamically from the explosions row, which never moves,
   * so interleaved changes to the rows above cannot misplace them.
   */
  void setRaytracedHiddenRowsVisible(boolean visible) {
    final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
    for (final Panel row : this.raytraced_hidden_rows) {
      tab.remove(row);
    }
    if (visible) {
      final Component anchor = FrEnd.panel_preferences_shared_misc.checkbox_explosions
          .getParent();
      int index = tab.getComponentCount();
      final Component[] components = tab.getComponents();
      for (int i = 0; i < components.length; i++) {
        if (components[i] == anchor) {
          index = i;
          break;
        }
      }
      for (final Panel row : this.raytraced_hidden_rows) {
        tab.add(row, index++);
      }
    }
    tab.validate();
  }

  /**
   * Shows or hides the "Show labels on:" row on the Renderer tab's
   * Main sub-tab. Labels are a modern-renderer feature, so the row is removed while
   * another renderer is active. Added and removed (never just hidden)
   * because the tab's GridLayout gives invisible components space.
   * Idempotent: the row is removed first, so a repeated call cannot
   * duplicate it.
   */
  void setLabelsRowVisible(boolean visible) {
    final Panel tab = FrEnd.panel_preferences_shared_show.panel_main;
    tab.remove(this.panel_labels_row);
    if (visible) {
      // Keep the row in its usual slot, right after the
      // "Render deepest objects first" row.
      tab.add(this.panel_labels_row, Math.min(5, tab.getComponentCount()));
    }
    tab.validate();
  }

  private void getPanelTiles() {
    final Panel panel_tile_size = getTileSizePanel();

    final Panel panel_show_tiles = new Panel();
    this.checkbox_show_tiles = new Checkbox(GUIStrings.SHOW_TILES, RendererTileManager.show_tiles);
    this.checkbox_show_tiles.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        RendererTileManager.show_tiles = ((Checkbox) e.getSource()).getState();
        FrEnd.main_canvas.forceResize();
      }
    });
    panel_show_tiles.add(this.checkbox_show_tiles);

    final Panel panel_show_active_tiles = new Panel();
    this.checkbox_show_active_tiles = new Checkbox(GUIStrings.SHOW_ACTIVE_TILES,
        RendererTileManager.show_active_tiles);
    this.checkbox_show_active_tiles.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        RendererTileManager.show_active_tiles = ((Checkbox) e.getSource()).getState();
        FrEnd.main_canvas.forceResize();
      }
    });
    panel_show_active_tiles.add(this.checkbox_show_active_tiles);

    this.panel_tiles.add(panel_show_tiles);

    this.panel_tiles.add(panel_show_active_tiles);

    this.panel_tiles.add(panel_tile_size);
  }

  private Panel getPanelLabelsWhen() {
    final Panel panel = new Panel();
    panel.add(new Label("Show labels on:", Label.RIGHT));

    this.choose_label_when = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererModern.this.choose_label_when.str_to_num(scs);
        PanelPreferencesRendererModern.render_label_when = val;
        RendererDelegator.repaintAll();
      }
    });

    this.choose_label_when.add("always", 1);
    this.choose_label_when.add("never", 2);
    this.choose_label_when.add("selected", 3);
    this.choose_label_when.choice.select(this.choose_label_when.num_to_str(render_label_when));
    panel.add(this.choose_label_when.choice);

    return panel;
  }

  private Panel panelLinkSides() {
    final Panel panel = new Panel();
    final Label label = new Label("Strut/cable sides:");
    panel.add(label);

    this.choose_link_sides = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Forget.about(e);
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererModern.this.choose_link_sides.str_to_num(scs);
        RendererDelegator.link_sides = val;
        FrEnd.main_canvas.forceResize();
      }
    });

    this.choose_link_sides.add("2", 2);
    this.choose_link_sides.add("3", 3);
    this.choose_link_sides.add("4", 4);
    this.choose_link_sides.add("6", 6);
    this.choose_link_sides.add("8", 8);
    this.choose_link_sides.choice.select(this.choose_link_sides.num_to_str(4));
    panel.add(this.choose_link_sides.choice);

    return panel;
  }

  private Panel panelNodePolyhedron() {
    final Panel panel = new Panel();
    panel.add(new Label("Node polyhedron:", Label.RIGHT));

    this.choose_polyhedron = new TTChoice(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        final String scs = (String) e.getItem();
        final int val = PanelPreferencesRendererModern.this.choose_polyhedron.str_to_num(scs);
        if (val == 1) {
          ModularRendererNew.sphere_object = new SimpleC60();
        } else if (val == 2) {
          ModularRendererNew.sphere_object = new SimpleDodecahedron();
        } else if (val == 3) {
          ModularRendererNew.sphere_object = new SimpleOctahedron();
        } else if (val == 4) {
          ModularRendererNew.sphere_object = new SimpleCube();
        } else if (val == 5) {
          ModularRendererNew.sphere_object = new SimpleIcosahedron();
        } else if (val == 6) {
          ModularRendererNew.sphere_object = new SimpleSquare();
        } else if (val == 7) {
          ModularRendererNew.sphere_object = new SimpleHexagon();
        }
        RendererDelegator.repaintAll();
      }
    });

    this.choose_polyhedron.add("C60", 1);
    this.choose_polyhedron.add("Dodecahedron", 2);
    this.choose_polyhedron.add("Octahedron", 3);
    this.choose_polyhedron.add("Cube", 4);
    this.choose_polyhedron.add("Icosahedron", 5);
    this.choose_polyhedron.add("Square", 6);
    this.choose_polyhedron.add("Hexagon", 7);
    this.choose_polyhedron.choice.select(this.choose_polyhedron.num_to_str(1));
    panel.add(this.choose_polyhedron.choice);

    return panel;
  }

  private Panel getTileSizePanel() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Tile size:", Label.RIGHT));

    final Scrollbar scroll_bar = new Scrollbar(Scrollbar.HORIZONTAL, RendererTileManager.divisor, 50, 50, 550);
    this.scroll_bar_tile_size = scroll_bar;
    scroll_bar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        final int temp = e.getValue();
        RendererTileManager.divisor = temp;
        reflectTileSizeNumber();
        FrEnd.main_canvas.forceResize();
      }
    });

    panel.add("Center", scroll_bar);

    this.label_tile_size_number = new Label("", Label.LEFT);
    panel.add("East", this.label_tile_size_number);
    reflectTileSizeNumber();

    return panel;
  }

  private Panel getPanelStrutDivisions() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Strut divisions:", Label.RIGHT));

    final Scrollbar scroll_bar = new Scrollbar(Scrollbar.HORIZONTAL, ElementRendererLink.strut_divisions, 1, 1, 8);
    this.scroll_bar_strut_divisions = scroll_bar;
    scroll_bar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        final int temp = e.getValue();
        ElementRendererLink.strut_divisions = temp;
        reflectLabelStrutDivisions();
        FrEnd.main_canvas.forceResize();
      }
    });

    panel.add("Center", scroll_bar);

    this.label_strut_divisions = new Label("", Label.LEFT);
    panel.add("East", this.label_strut_divisions);
    reflectLabelStrutDivisions();

    return panel;
  }

  private Panel getPanelCableDivisions() {
    final Panel panel = new Panel();
    panel.setLayout(new BorderLayout(0, 8));
    panel.add("West", new Label("Cable divisions:", Label.RIGHT));

    final Scrollbar scroll_bar = new Scrollbar(Scrollbar.HORIZONTAL, ElementRendererLink.cable_divisions, 1, 1, 8);
    this.scroll_bar_cable_divisions = scroll_bar;
    scroll_bar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        final int temp = e.getValue();
        ElementRendererLink.cable_divisions = temp;
        reflectLabelCableDivisions();
        FrEnd.main_canvas.forceResize();
      }
    });

    panel.add("Center", scroll_bar);

    this.label_cable_divisions = new Label("", Label.LEFT);
    panel.add("East", this.label_cable_divisions);
    reflectLabelCableDivisions();

    return panel;
  }

  private void reflectLabelStrutDivisions() {
    getLabelStrutDivisions().setText("" + ElementRendererLink.strut_divisions);
  }

  private void reflectLabelCableDivisions() {
    getLabelCableDivisions().setText("" + ElementRendererLink.cable_divisions);
  }

  private Label getLabelStrutDivisions() {
    return this.label_strut_divisions;
  }

  private Label getLabelCableDivisions() {
    return this.label_cable_divisions;
  }

  private void reflectTileSizeNumber() {
    getLabelTileSizeNumber().setText("" + RendererTileManager.divisor);
  }

  public Label getLabelTileSizeNumber() {
    return this.label_tile_size_number;
  }

  /**
   * Restores the default modern-renderer preferences.
   */
  public void resetToDefaults() {
    // Show tiles.
    RendererTileManager.show_tiles = false;
    this.checkbox_show_tiles.setState(false);

    // Show active tiles.
    RendererTileManager.show_active_tiles = false;
    this.checkbox_show_active_tiles.setState(false);

    // Tile size.
    RendererTileManager.divisor = 340;
    this.scroll_bar_tile_size.setValue(RendererTileManager.divisor);
    reflectTileSizeNumber();

    // Strut and cable divisions.
    ElementRendererLink.strut_divisions = 5;
    this.scroll_bar_strut_divisions
        .setValue(ElementRendererLink.strut_divisions);
    reflectLabelStrutDivisions();

    ElementRendererLink.cable_divisions = 1;
    this.scroll_bar_cable_divisions
        .setValue(ElementRendererLink.cable_divisions);
    reflectLabelCableDivisions();

    // Strut/cable sides (2).
    RendererDelegator.link_sides = 4;
    this.choose_link_sides.choice.select(this.choose_link_sides
        .num_to_str(2));

    // Node polyhedron (C60).
    ModularRendererNew.sphere_object = new SimpleC60();
    this.choose_polyhedron.choice.select(this.choose_polyhedron
        .num_to_str(1));

    // Show labels: never by default.
    render_label_when = 2;
    this.choose_label_when.choice.select(this.choose_label_when
        .num_to_str(render_label_when));

    FrEnd.main_canvas.forceResize();
  }

  public NewMessageManager getNewMessageManager() {
    return this.new_message_manager;
  }
}
