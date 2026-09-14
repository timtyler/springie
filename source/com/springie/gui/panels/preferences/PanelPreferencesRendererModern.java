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
import com.springie.messages.commands.DoubleBufferNewMessage;
import com.springie.preferences.Preferences;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ElementRendererLink;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.modern.RendererBinManager;
import com.springie.render.modules.modern.SimpleCube;
import com.springie.render.modules.modern.SimpleDodecahedron;
import com.springie.render.modules.modern.SimpleHexagon;
import com.springie.render.modules.modern.SimpleIcosahedron;
import com.springie.render.modules.modern.SimpleOctahedron;
import com.springie.render.modules.modern.SimpleSquare;
import com.tifsoft.Forget;

public class PanelPreferencesRendererModern {
	public Panel panel = FrEnd.setUpPanelForFrame2();

	public Panel panel_bins = FrEnd.setUpPanelForFrame2();

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

	NewMessageManager new_message_manager;

	public Checkbox checkbox_db_new;

	private Checkbox checkbox_show_bins;

	private Checkbox checkbox_show_active_bins;

	private TTChoice choose_polyhedron;

	private Label label_bin_size_number;

	private Scrollbar scroll_bar_bin_size;

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
		// The whole Renderer tab is one GridLayout (the shared panel
		// itself): the ray-traced-only options (Glossiness, Shadows,
		// Specular, Fresnel, Fill light) are added and removed at the
		// bottom when the renderer is switched -- a GridLayout gives
		// invisible components space, so setVisible cannot hide them.
		this.raytraced_rows =
				FrEnd.panel_preferences_renderer_raytraced.takeEffectRows();
		tab.add("Renderer", FrEnd.panel_preferences_shared_show.panel);

		tab.add("Colours", FrEnd.panel_preferences_renderer_modern_colours.panel);

		this.panel.add(tab);

		final Panel panel_node_polyhedron = panelNodePolyhedron();

		getPanelBins();

		this.panel_misc.add(panel_node_polyhedron);

		this.panel_misc.add(getPanelCableDivisions());
		this.panel_misc.add(getPanelStrutDivisions());
		this.panel_misc.add(panelLinkSides());

		// The shared Misc rows (explosions, fog, face lines) join the modern
		// Misc rows. ("Render deepest objects first" lives on the Renderer
		// tab instead.)
		FrEnd.panel_preferences_shared_misc.moveRowsInto(this.panel_misc);

		this.panel_labels_row = getPanelLabelsWhen();

		// The old Options tab is flattened into the Renderer tab: the
		// Bins and Misc rows move under the shared rows there, so there
		// is just the one layout. (PanelPreferencesDisplay inserts the
		// top rows at fixed indices afterwards, so the final order is
		// stable.)
		final Panel renderer_tab = FrEnd.panel_preferences_shared_show.panel;
		moveRowsInto(renderer_tab, this.panel_bins);
		moveRowsInto(renderer_tab, this.panel_misc);
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
		final Panel tab = FrEnd.panel_preferences_shared_show.panel;
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
	 * Renderer tab. The depth sort is meaningless for the ray-traced
	 * renderer (occlusion is resolved per ray by the BVH), so the row is
	 * removed while ray-tracing is active and restored for the
	 * rasterizer renderers. Like the ray-traced rows, it is added and
	 * removed (never just hidden) because the tab's GridLayout gives
	 * invisible components space. Idempotent: the row is removed first,
	 * so a repeated call cannot duplicate it.
	 */
	void setDeepestFirstRowVisible(boolean visible) {
		final Panel tab = FrEnd.panel_preferences_shared_show.panel;
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
	 * Shows or hides the "Show labels on:" row on the Renderer tab.
	 * Labels are a modern-renderer feature, so the row is removed while
	 * another renderer is active. Added and removed (never just hidden)
	 * because the tab's GridLayout gives invisible components space.
	 * Idempotent: the row is removed first, so a repeated call cannot
	 * duplicate it.
	 */
	void setLabelsRowVisible(boolean visible) {
		final Panel tab = FrEnd.panel_preferences_shared_show.panel;
		tab.remove(this.panel_labels_row);
		if (visible) {
			// Keep the row in its usual slot, right after the
			// "Render deepest objects first" row.
			tab.add(this.panel_labels_row, Math.min(5, tab.getComponentCount()));
		}
		tab.validate();
	}

	private void getPanelBins() {
		final Panel panel_bin_size = getBinSizePanel();

		final Panel panel_new_double_buffering = new Panel();
		this.checkbox_db_new = new Checkbox(GUIStrings.DB_NEW, RendererDelegator.isNewDoubleBuffer());
		panel_new_double_buffering.add(this.checkbox_db_new);

		this.checkbox_db_new.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				getNewMessageManager().add(new DoubleBufferNewMessage());
			}
		});

		final Panel panel_show_bins = new Panel();
		this.checkbox_show_bins = new Checkbox(GUIStrings.SHOW_BINS, RendererBinManager.show_bins);
		this.checkbox_show_bins.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				RendererBinManager.show_bins = ((Checkbox) e.getSource()).getState();
				FrEnd.main_canvas.forceResize();
			}
		});
		panel_show_bins.add(this.checkbox_show_bins);

		final Panel panel_show_active_bins = new Panel();
		this.checkbox_show_active_bins = new Checkbox(GUIStrings.SHOW_ACTIVE_BINS,
				RendererBinManager.show_active_bins);
		this.checkbox_show_active_bins.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				RendererBinManager.show_active_bins = ((Checkbox) e.getSource()).getState();
				FrEnd.main_canvas.forceResize();
			}
		});
		panel_show_active_bins.add(this.checkbox_show_active_bins);

		this.panel_bins.add(panel_new_double_buffering);

		this.panel_bins.add(panel_show_bins);

		this.panel_bins.add(panel_show_active_bins);

		this.panel_bins.add(panel_bin_size);
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
		this.choose_link_sides.choice.select(this.choose_link_sides.num_to_str(2));
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
					ModularRendererNew.sphere_object = new SimpleDodecahedron();
				} else if (val == 2) {
					ModularRendererNew.sphere_object = new SimpleOctahedron();
				} else if (val == 3) {
					ModularRendererNew.sphere_object = new SimpleCube();
				} else if (val == 4) {
					ModularRendererNew.sphere_object = new SimpleIcosahedron();
				} else if (val == 5) {
					ModularRendererNew.sphere_object = new SimpleSquare();
				} else if (val == 6) {
					ModularRendererNew.sphere_object = new SimpleHexagon();
				}
				RendererDelegator.repaintAll();
			}
		});

		this.choose_polyhedron.add("Dodecahedron", 1);
		this.choose_polyhedron.add("Octahedron", 2);
		this.choose_polyhedron.add("Cube", 3);
		this.choose_polyhedron.add("Icosahedron", 4);
		this.choose_polyhedron.add("Square", 5);
		this.choose_polyhedron.add("Hexagon", 6);
		this.choose_polyhedron.choice.select(this.choose_polyhedron.num_to_str(1));
		panel.add(this.choose_polyhedron.choice);

		return panel;
	}

	private Panel getBinSizePanel() {
		final Panel panel = new Panel();
		panel.setLayout(new BorderLayout(0, 8));
		panel.add("West", new Label("Bin size:", Label.RIGHT));

		final Scrollbar scroll_bar = new Scrollbar(Scrollbar.HORIZONTAL, RendererBinManager.divisor, 50, 50, 550);
		this.scroll_bar_bin_size = scroll_bar;
		scroll_bar.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				final int temp = e.getValue();
				RendererBinManager.divisor = temp;
				reflectBinSizeNumber();
				FrEnd.main_canvas.forceResize();
			}
		});

		panel.add("Center", scroll_bar);

		this.label_bin_size_number = new Label("", Label.LEFT);
		panel.add("East", this.label_bin_size_number);
		reflectBinSizeNumber();

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

	private void reflectBinSizeNumber() {
		getLabelBinSizeNumber().setText("" + RendererBinManager.divisor);
	}

	public Label getLabelBinSizeNumber() {
		return this.label_bin_size_number;
	}

	/**
	 * Restores the default modern-renderer preferences. The double-buffer
	 * default comes from the (freshly reset) Preferences map.
	 */
	public void resetToDefaults() {
		// Double buffering (the message handler reads the checkbox state).
		this.checkbox_db_new.setState(((Boolean) FrEnd.preferences.map
				.get(Preferences.renderer_new_double_buffer)).booleanValue());

		// Show bins.
		RendererBinManager.show_bins = false;
		this.checkbox_show_bins.setState(false);

		// Show active bins.
		RendererBinManager.show_active_bins = false;
		this.checkbox_show_active_bins.setState(false);

		// Bin size.
		RendererBinManager.divisor = 340;
		this.scroll_bar_bin_size.setValue(RendererBinManager.divisor);
		reflectBinSizeNumber();

		// Strut and cable divisions.
		ElementRendererLink.strut_divisions = 3;
		this.scroll_bar_strut_divisions
				.setValue(ElementRendererLink.strut_divisions);
		reflectLabelStrutDivisions();

		ElementRendererLink.cable_divisions = 1;
		this.scroll_bar_cable_divisions
				.setValue(ElementRendererLink.cable_divisions);
		reflectLabelCableDivisions();

		// Strut/cable sides (2).
		RendererDelegator.link_sides = 2;
		this.choose_link_sides.choice.select(this.choose_link_sides
				.num_to_str(2));

		// Node polyhedron (Dodecahedron).
		ModularRendererNew.sphere_object = new SimpleDodecahedron();
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