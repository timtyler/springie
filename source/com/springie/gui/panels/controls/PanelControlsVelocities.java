// This program has been placed into the public domain by its author.

package com.springie.gui.panels.controls;

import java.awt.Button;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.gui.GUIStrings;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessage;
import com.springie.messages.NewMessageManager;
import com.springie.modification.velocity.MotionlessMaker;
import com.tifsoft.Forget;

public class PanelControlsVelocities {
	public Panel panel = FrEnd.setUpPanelForFrame2();

	MessageManager message_manager;
	NewMessageManager new_message_manager;

	public Button button_edit_make_motionless;

	public Button button_edit_freeze;

	public Button button_edit_reverse;

	public PanelControlsVelocities(MessageManager message_manager, NewMessageManager new_message_manager) {
		this.message_manager = message_manager;
		this.new_message_manager = new_message_manager;
		makePanel();
	}

	void makePanel() {
		this.panel.add(makePanelVelocityReduce());
		this.panel.add(makePanelVelocityIncrease());
		this.panel.add(makePanelVelocityFreeze());
		this.panel.add(makePanelVelocityReverse());
	}

	private Panel makePanelVelocityReduce() {
		this.button_edit_make_motionless = new Button(GUIStrings.VELOCITY_REDUCE);
		this.button_edit_make_motionless.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Forget.about(e);
				getNewMessageManager().add(new NewMessage(null) {
					public Object execute() {
						new MotionlessMaker().reduce(ContextMananger.getNodeManager(), 0.5f);
						return null;
					}
				});
			}
		});
		final Panel panel = new Panel();
		panel.add(this.button_edit_make_motionless);
		return panel;
	}

	private Panel makePanelVelocityIncrease() {
		this.button_edit_make_motionless = new Button(GUIStrings.VELOCITY_INCREASE);
		this.button_edit_make_motionless.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Forget.about(e);
				getNewMessageManager().add(new NewMessage(null) {
					public Object execute() {
						new MotionlessMaker().reduce(ContextMananger.getNodeManager(), 2.0f);
						return null;
					}
				});
			}
		});
		final Panel panel = new Panel();
		panel.add(this.button_edit_make_motionless);
		return panel;
	}

	private Panel makePanelVelocityFreeze() {
		this.button_edit_freeze = new Button(GUIStrings.VELOCITY_FREEZE);
		this.button_edit_freeze.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Forget.about(e);
				getNewMessageManager().add(new NewMessage(null) {
					public Object execute() {
						new MotionlessMaker().reduce(ContextMananger.getNodeManager(), 0.0f);
						return null;
					}
				});
			}
		});
		final Panel panel = new Panel();
		panel.add(this.button_edit_freeze);
		return panel;
	}

	private Panel makePanelVelocityReverse() {
		// MAKE_MOTIONLESS
		this.button_edit_reverse = new Button(GUIStrings.VELOCITY_REVERSE);
		this.button_edit_reverse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Forget.about(e);
				getNewMessageManager().add(new NewMessage(null) {
					public Object execute() {
						new MotionlessMaker().reduce(ContextMananger.getNodeManager(), -1.0f);
						return null;
					}
				});
			}
		});
		final Panel panel = new Panel();
		panel.add(this.button_edit_reverse);
		return panel;
	}

	public MessageManager getMessageManager() {
		return this.message_manager;
	}

	public NewMessageManager getNewMessageManager() {
		return this.new_message_manager;
	}
}
