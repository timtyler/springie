// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.gui.GUIStrings;
import com.springie.messages.NewMessageManager;
import com.springie.messages.commands.ContinuouslyCentreMessage;
import com.springie.muscles.Muscles;
import com.springie.render.RendererDelegator;
import com.springie.world.World;
import com.tifsoft.Forget;

public class PanelControlsUniverse {
	public Panel panel = FrEnd.setUpPanelForFrame2();

	NewMessageManager new_message_manager;

	public Checkbox checkbox_3D;

	public Checkbox checkbox_show_boundary_box;

	public Checkbox checkbox_gravity_switch;

	public Checkbox checkbox_charge_switch;

	public Checkbox checkbox_continuously_centre;

	public Checkbox checkbox_collision_check;


	public Checkbox checkbox_muscles;

	Label label_gravity;
	Label label_temperature;

	Label label_viscocity;

	Label label_number;

	Label label_bias;

	Label label_noc;

	Label label_impact;

	Label label_limit;

	Label label_friction;

	Label label_muscles_amplitude;

	Label label_muscles_period;

	Scrollbar scroll_bar_noc;

	Scrollbar scroll_bar_n;

	Scrollbar scroll_bar_gravity;
	Scrollbar scroll_bar_temperature;

	Scrollbar scroll_bar_stiffness;

	Scrollbar scroll_bar_elasticity;

	Scrollbar scroll_bar_viscocity;

	Scrollbar scroll_bar_bias;

	Scrollbar scroll_bar_limit;

	Scrollbar scroll_bar_muscles_amplitude;

	Scrollbar scroll_bar_muscles_period;

	Scrollbar scroll_bar_impact;

	Scrollbar scroll_bar_friction;

	public PanelControlsUniverse(NewMessageManager new_message_manager) {
		this.new_message_manager = new_message_manager;
		makeEditMiscPanel();
	}

	void makeEditMiscPanel() {
		final Panel panel3D = new Panel();
		this.checkbox_3D = new Checkbox("3D");
		this.checkbox_3D.setState(FrEnd.three_d);

		this.checkbox_3D.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				FrEnd.three_d = getCheckbox3D().getState();

				RendererDelegator.repaintAll();
			}
		});

		panel3D.add(this.checkbox_3D);

		this.checkbox_show_boundary_box =
			new Checkbox(GUIStrings.SHOW_BOUNDARY_BOX);
		this.checkbox_show_boundary_box.setState(FrEnd.show_boundary_box);
		this.checkbox_show_boundary_box.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				FrEnd.show_boundary_box =
					getCheckboxShowBoundaryBox().getState();

				RendererDelegator.repaintAll();
			}
		});
		panel3D.add(this.checkbox_show_boundary_box);

		// START GRAVITY
		final Panel panel_gravity = new Panel();
		panel_gravity.setLayout(new BorderLayout(0, 8));
		this.checkbox_gravity_switch = new Checkbox(GUIStrings.GRAVITY);
		this.checkbox_gravity_switch.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				final boolean active = ((Checkbox) e.getSource()).getState();
				World.gravity_active = active;
				scroll_bar_gravity.setEnabled(active);
			}
		});
		panel_gravity.add("West", this.checkbox_gravity_switch);

		this.scroll_bar_gravity = new Scrollbar(Scrollbar.HORIZONTAL, 10, 80, 0, 580);
		this.scroll_bar_gravity.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				final int temp = e.getValue();
				World.gravity_strength = temp;
				getLabelGravity().setText("" + temp);
			}
		});
		this.scroll_bar_gravity.setEnabled(World.gravity_active);

		panel_gravity.add("Center", this.scroll_bar_gravity);

		this.label_gravity = new Label("" + World.gravity_strength, Label.LEFT);
		panel_gravity.add("East", this.label_gravity);

		// START FRICTION
		final Panel panel_friction = new Panel();
		panel_friction.setLayout(new BorderLayout(0, 8));
		panel_friction.add("West", new Label("Friction:", Label.RIGHT));

		this.scroll_bar_friction = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0, 110);
		this.scroll_bar_friction.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				final int temp = e.getValue();
				World.ground_friction = temp;
				getLabelFriction().setText("" + temp);
			}
		});

		panel_friction.add("Center", this.scroll_bar_friction);

		this.label_friction = new Label("" + World.ground_friction, Label.LEFT);
		panel_friction.add("East", this.label_friction);

		// END FRICTION (added below, under gravity)

		// START TEMPERATURE
		final Panel panel_temperature = new Panel();
		panel_temperature.setLayout(new BorderLayout(0, 8));
		panel_temperature.add("West", new Label("Temperature:", Label.RIGHT));

		this.scroll_bar_temperature = new Scrollbar(Scrollbar.HORIZONTAL, 10, 80, 0, 1080);
		this.scroll_bar_temperature.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				final int temp = e.getValue();
				World.global_temperature = temp;
				getLabelTemperature().setText("" + temp);
			}
		});

		panel_temperature.add("Center", this.scroll_bar_temperature);

		this.label_temperature = new Label("" + World.global_temperature, Label.LEFT);
		panel_temperature.add("East", this.label_temperature);

		final Panel panel_charge_switch = new Panel();
		this.checkbox_charge_switch = new Checkbox(GUIStrings.CHARGE, true);
		this.checkbox_charge_switch.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				ContextManager.getNodeManager().electrostatic.charge_active = ((Checkbox) e.getSource()).getState();
			}
		});
		panel_charge_switch.add(this.checkbox_charge_switch);

		// START MUSCLES SWITCH
		final Panel panel_muscles_switch = new Panel();
		this.checkbox_muscles = new Checkbox(GUIStrings.MUSCLES);
		this.checkbox_muscles.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				Muscles.enabled = ((Checkbox) e.getSource()).getState();
			}
		});
		panel_muscles_switch.add(this.checkbox_muscles);

		final Panel panel_muscles_amplitude = new Panel();
		panel_muscles_amplitude.setLayout(new BorderLayout(0, 8));
		panel_muscles_amplitude.add("West", new Label("Muscle amplitude %:", Label.RIGHT));

		this.scroll_bar_muscles_amplitude = new Scrollbar(Scrollbar.HORIZONTAL, 85, 1, 0, 201);
		this.scroll_bar_muscles_amplitude.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Muscles.activeOscillator().setAmplitude(e.getValue() * Muscles.UNITY / 100);
				reflectMuscles();
			}
		});

		panel_muscles_amplitude.add("Center", this.scroll_bar_muscles_amplitude);

		this.label_muscles_amplitude = new Label("85", Label.LEFT);
		panel_muscles_amplitude.add("East", this.label_muscles_amplitude);

		final Panel panel_muscles_period = new Panel();
		panel_muscles_period.setLayout(new BorderLayout(0, 8));
		panel_muscles_period.add("West", new Label("Muscle period (ticks):", Label.RIGHT));

		this.scroll_bar_muscles_period = new Scrollbar(Scrollbar.HORIZONTAL, 12, 10, 2, 610);
		this.scroll_bar_muscles_period.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Muscles.activeOscillator().setPeriodTicks(e.getValue());
				reflectMuscles();
			}
		});

		panel_muscles_period.add("Center", this.scroll_bar_muscles_period);

		this.label_muscles_period = new Label("12", Label.LEFT);
		panel_muscles_period.add("East", this.label_muscles_period);

		final Panel panel_continuously_centre = new Panel();
		this.checkbox_continuously_centre = new Checkbox(GUIStrings.CONTINUOUSLY_CENTRE);
		this.checkbox_continuously_centre.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				getNewMessageManager().add(new ContinuouslyCentreMessage());
			}
		});
		panel_continuously_centre.add(this.checkbox_continuously_centre);

		// elasticity..
		final Panel panel_viscocity = new Panel();
		panel_viscocity.setLayout(new BorderLayout(0, 8));
		panel_viscocity.add("West", new Label("Viscosity:", Label.RIGHT));

		this.scroll_bar_viscocity = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0, 110);
		this.scroll_bar_viscocity.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Node.viscocity = e.getValue();
				reflectViscocity();
			}
		});

		panel_viscocity.add("Center", this.scroll_bar_viscocity);

		this.label_viscocity = new Label("XXX", Label.LEFT);
		panel_viscocity.add("East", this.label_viscocity);

		final Panel panel_collision_check = new Panel();
		this.checkbox_collision_check = new Checkbox(GUIStrings.collision_check);
		this.checkbox_collision_check.setState(true);
		this.checkbox_collision_check.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Forget.about(e);
				FrEnd.check_collisions = !FrEnd.check_collisions;
			}
		});
		panel_collision_check.add(this.checkbox_collision_check);


		final Panel panel_bias = new Panel();
		panel_bias.setLayout(new BorderLayout(0, 8));
		panel_bias.add("West", new Label("Bias:", Label.RIGHT));

		this.scroll_bar_bias = new Scrollbar(Scrollbar.HORIZONTAL, 0, 90, -180, 270);
		this.scroll_bar_bias.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				final int temp = e.getValue();
				// Node.direction_bias = (byte) ((temp * 16) / 180);
				getLabelBias().setText("" + temp);
			}
		});

		panel_bias.add("Center", this.scroll_bar_bias);

		this.label_bias = new Label("  0", Label.LEFT);
		panel_bias.add("East", this.label_bias);

		final Panel panel_speed = new Panel();
		panel_speed.setLayout(new BorderLayout(0, 8));
		panel_speed.add("West", new Label("Speed:", Label.RIGHT));

		this.scroll_bar_limit = new Scrollbar(Scrollbar.HORIZONTAL, Node.max_speed >> 5, 50, 0, 450);
		this.scroll_bar_limit.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				Node.max_speed = e.getValue() << 5;
				reflectMaxSpeed();
			}
		});
		panel_speed.add("Center", this.scroll_bar_limit);

		this.label_limit = new Label("" + (Node.max_speed >> 5), Label.LEFT);
		panel_speed.add("East", this.label_limit);

		// rest energy
		final Panel panel_excite = new Panel();
		panel_excite.setLayout(new BorderLayout(0, 8));
		panel_excite.add("West", new Label("Excite:", Label.RIGHT));

		this.scroll_bar_impact = new Scrollbar(Scrollbar.HORIZONTAL, World.minimum_magnitude >> 6, 10, 0, 109);
		this.scroll_bar_impact.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				final int temp = e.getValue();
				World.minimum_magnitude = temp << 6;
				getLabelImpact().setText("" + temp);
			}
		});

		panel_excite.add("Center", this.scroll_bar_impact);

		this.label_impact = new Label("" + (World.minimum_magnitude >> 6), Label.LEFT);
		panel_excite.add("East", this.label_impact);

		// universe...
		this.panel.add(panel3D);
		this.panel.add(panel_viscocity);
		this.panel.add(panel_temperature);
		this.panel.add(panel_gravity);
		this.panel.add(panel_friction);

		this.panel.add(panel_muscles_switch);
		this.panel.add(panel_muscles_amplitude);
		this.panel.add(panel_muscles_period);

		this.panel.add(panel_continuously_centre);
		this.panel.add(panel_collision_check);
		this.panel.add(panel_charge_switch);

		if (FrEnd.development_version) {
			this.panel.add(panel_bias); // bias...

			this.panel.add(panel_speed);

			this.panel.add(panel_excite);
		}

		this.panel.add(getResetUniversePanel());
	}

	private Panel getResetUniversePanel() {
		final Button button_reset_universe = new Button(GUIStrings.RESET_UNIVERSE);
		button_reset_universe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetUniverse();
			}
		});
		final Panel panel_reset_universe = new Panel();
		panel_reset_universe.add(button_reset_universe);
		return panel_reset_universe;
	}

	/**
	 * Restores the universe settings belonging to the current model and
	 * updates the controls to match. The model's settings were captured by
	 * {@link com.springie.world.UniverseDefaults#snapshot()} when the model
	 * loaded; before any model loads, the factory defaults apply.
	 */
	public void resetUniverse() {
		com.springie.world.UniverseDefaults.restore();

		// Reflect the restored statics in the controls.
		this.checkbox_3D.setState(FrEnd.three_d);
		reflectGravity();
		reflectFriction();
		reflectTemperature();
		reflectViscocity();
		reflectUniverseToggles();
		setCheckboxSilently(this.checkbox_muscles, Muscles.enabled);
		reflectMuscles();

		if (FrEnd.development_version) {
			reflectMaxSpeed();
			reflectImpact();

			// Bias (display only).
			this.scroll_bar_bias.setValue(0);
			this.label_bias.setText("  0");
		}

		RendererDelegator.repaintAll();
	}

	/**
	 * Sets a checkbox without firing its item listeners (several Universe
	 * checkboxes queue toggle messages when clicked, which must not happen
	 * during a reset).
	 */
	private static void setCheckboxSilently(Checkbox checkbox, boolean state) {
		final ItemListener[] listeners = checkbox.getItemListeners();
		for (final ItemListener listener : listeners) {
			checkbox.removeItemListener(listener);
		}
		checkbox.setState(state);
		for (final ItemListener listener : listeners) {
			checkbox.addItemListener(listener);
		}
	}

	public void reflectMaxSpeed() {
		this.scroll_bar_limit.setValue(Node.max_speed >> 5);

		this.label_limit.setText("" + (Node.max_speed >> 5));
	}

	public void reflectViscocity() {
		this.scroll_bar_viscocity.setValue(Node.viscocity);

		this.label_viscocity.setText("" + Node.viscocity);
	}

	public void reflectGravity() {
		this.scroll_bar_gravity.setValue(World.gravity_strength);

		this.label_gravity.setText("" + World.gravity_strength);
		this.checkbox_gravity_switch.setState(World.gravity_active);
		this.scroll_bar_gravity.setEnabled(World.gravity_active);
	}

	public void reflectFriction() {
		this.scroll_bar_friction.setValue(World.ground_friction);

		this.label_friction.setText("" + World.ground_friction);
	}

	public void reflectTemperature() {
		this.scroll_bar_temperature.setValue(World.global_temperature);

		this.label_temperature.setText("" + World.global_temperature);
	}

	public void reflect3D() {
		this.checkbox_3D.setState(FrEnd.three_d);
	}

	/**
	 * Reflects the universe toggle checkboxes from the current simulation
	 * statics. Model files carry their own universe settings, so a load (or
	 * a model switch) can change the statics from under the UI; without this
	 * the checkboxes keep showing the previous model's values (e.g. Moscow
	 * ships collision_check=false while the box stayed on). The toggle
	 * listeners must not fire here: several of them invert the static.
	 */
	public void reflectUniverseToggles() {
		setCheckboxSilently(this.checkbox_collision_check, FrEnd.check_collisions);
		setCheckboxSilently(this.checkbox_continuously_centre, FrEnd.continuously_centre);
		final NodeManager manager = ContextManager.getNodeManager();
		setCheckboxSilently(this.checkbox_charge_switch,
				manager != null && manager.electrostatic.charge_active);
	}

	public void reflectImpact() {
		this.scroll_bar_impact.setValue(World.minimum_magnitude >> 6);

		this.label_impact.setText("" + (World.minimum_magnitude >> 6));
	}

	public void reflectMuscles() {
		// Round to nearest: the scrollbar moves in whole percents, but the
		// amplitude is stored in fixed point, so a truncating readback would
		// snap single-step arrow clicks straight back (and make the left
		// arrow skip values).
		final int amplitude_percent = (Muscles.activeOscillator().getAmplitude() * 100 + Muscles.UNITY / 2)
				/ Muscles.UNITY;
		this.scroll_bar_muscles_amplitude.setValue(amplitude_percent);
		this.label_muscles_amplitude.setText("" + amplitude_percent);

		this.scroll_bar_muscles_period.setValue(Muscles.activeOscillator().getPeriodTicks());
		this.label_muscles_period.setText("" + Muscles.activeOscillator().getPeriodTicks());
	}

	public Label getLabelBias() {
		return this.label_bias;
	}

	public Label getLabelFriction() {
		return this.label_friction;
	}

	public Label getLabelGravity() {
		return this.label_gravity;
	}

	public Label getLabelTemperature() {
		return this.label_temperature;
	}

	public Label getLabelImpact() {
		return this.label_impact;
	}

	public Label getLabelLimit() {
		return this.label_limit;
	}

	public Label getLabelNOC() {
		return this.label_noc;
	}

	public Label getLabelNumber() {
		return this.label_number;
	}

	public Label getLabelViscocity() {
		return this.label_viscocity;
	}

	public NewMessageManager getNewMessageManager() {
		return this.new_message_manager;
	}

	public Checkbox getCheckbox3D() {
		return this.checkbox_3D;
	}

	public Checkbox getCheckboxShowBoundaryBox() {
		return this.checkbox_show_boundary_box;
	}

	public Checkbox getCheckboxCollisionCheck() {
		return this.checkbox_collision_check;
	}

	public Checkbox getCheckboxContinuouslyCentre() {
		return this.checkbox_continuously_centre;
	}

	public Checkbox getCheckboxGravitySwitch() {
		return this.checkbox_gravity_switch;
	}
}