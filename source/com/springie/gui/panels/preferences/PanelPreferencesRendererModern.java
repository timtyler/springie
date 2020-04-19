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
import com.springie.messages.Message;
import com.springie.messages.MessageManager;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ElementRendererLink;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.modern.RendererBinManager;
import com.springie.render.modules.modern.SimpleCube;
import com.springie.render.modules.modern.SimpleDodecahedron;
import com.springie.render.modules.modern.SimpleIcosahedron;
import com.springie.render.modules.modern.SimpleOctahedron;
import com.tifsoft.Forget;

public class PanelPreferencesRendererModern {
	public Panel panel = FrEnd.setUpPanelForFrame2();

	public Panel panel_main = FrEnd.setUpPanelForFrame2();

	// final TabbedPanel tab_colours_main = new TabbedPanel();

	public Panel panel_bins = FrEnd.setUpPanelForFrame2();

	public Panel panel_labels = FrEnd.setUpPanelForFrame2();

	public Panel panel_misc = FrEnd.setUpPanelForFrame2();

	MessageManager message_manager;

	public Checkbox checkbox_db_new;

	private TTChoice choose_polyhedron;

	private Label label_bin_size_number;

	private Label label_strut_divisions;

	private Label label_cable_divisions;

	private Checkbox checkbox_fat_struts;

	TTChoice choose_label_when;

	public static int render_label_when = 3;

	public PanelPreferencesRendererModern(MessageManager message_manager) {
		this.message_manager = message_manager;
		makePanel();
	}

	void makePanel() {
		final TabbedPanel tab = new TabbedPanel();
		tab.add("Options", this.panel_main);

		tab.add("Filtering", FrEnd.panel_preferences_renderer_modern_filters.panel);

		tab.add("Colours", FrEnd.panel_preferences_renderer_modern_colours.panel);

		this.panel.add(tab);

		final Panel panel_node_polyhedron = panelNodePolyhedron();

		getPanelBins();

		this.panel_misc.add(panel_node_polyhedron);

		this.panel_misc.add(getPanelCableDivisions());
		this.panel_misc.add(getPanelStrutDivisions());
		this.panel_misc.add(getPanelFatStrutTensegrities());

		this.panel_labels.add(getPanelLabelsWhen());

		final TabbedPanel tab_bins = new TabbedPanel();

		tab_bins.add("Bins", this.panel_bins);
		tab_bins.add("Labels", this.panel_labels);

		this.panel_main.add(tab_bins);

		final TabbedPanel tab_misc = new TabbedPanel();

		tab_misc.add("Misc", this.panel_misc);

		this.panel_main.add(tab_misc);
	}

	private void getPanelBins() {
		final Panel panel_bin_size = getBinSizePanel();

		final Panel panel_new_double_buffering = new Panel();
		this.checkbox_db_new = new Checkbox(GUIStrings.DB_NEW, RendererDelegator.isNewDoubleBuffer());
		panel_new_double_buffering.add(this.checkbox_db_new);

		this.checkbox_db_new.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				getMessageManager().sendMessage(Message.MSG_DB_NEW, 0, 0);
			}
		});

		final Panel panel_show_bins = new Panel();
		final Checkbox checkbox_show_bins = new Checkbox(GUIStrings.SHOW_BINS, RendererBinManager.show_bins);
		checkbox_show_bins.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				RendererBinManager.show_bins = ((Checkbox) e.getSource()).getState();
				FrEnd.main_canvas.forceResize();
			}
		});
		panel_show_bins.add(checkbox_show_bins);

		this.panel_bins.add(panel_new_double_buffering);

		this.panel_bins.add(panel_show_bins);

		this.panel_bins.add(panel_bin_size);
	}

	private Component getPanelLabelsWhen() {
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

	private Component getPanelFatStrutTensegrities() {
		final Panel panel_fat_struts = new Panel();
		this.checkbox_fat_struts = new Checkbox("Fat struts in tensegrities", RendererDelegator.fat_struts);

		this.checkbox_fat_struts.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				RendererDelegator.fat_struts = PanelPreferencesRendererModern.this.checkbox_fat_struts.getState();
				FrEnd.main_canvas.forceResize();
			}
		});

		panel_fat_struts.add(this.checkbox_fat_struts);
		return panel_fat_struts;
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
				}
				RendererDelegator.repaintAll();
			}
		});

		this.choose_polyhedron.add("Dodecahedron", 1);
		this.choose_polyhedron.add("Octahedron", 2);
		this.choose_polyhedron.add("Cube", 3);
		this.choose_polyhedron.add("Icosahedron", 4);
		this.choose_polyhedron.choice.select(this.choose_polyhedron.num_to_str(1));
		panel.add(this.choose_polyhedron.choice);

		return panel;
	}

	private Panel getBinSizePanel() {
		final Panel panel = new Panel();
		panel.setLayout(new BorderLayout(0, 8));
		panel.add("West", new Label("Bin size:", Label.RIGHT));

		final Scrollbar scroll_bar = new Scrollbar(Scrollbar.HORIZONTAL, RendererBinManager.divisor, 100, 50, 500);
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

	public MessageManager getMessageManager() {
		return this.message_manager;
	}
}