// This program has been placed into the public domain by its author.

package com.springie.gui.gestures;

import com.springie.FrEnd;
import com.springie.constants.Actions;
import com.springie.constants.ToolTypes;
import com.springie.context.ContextMananger;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.modification.pre.PrepareToModifyNodeTypes;
import com.springie.utilities.math.SquareRoot;
import com.springie.utilities.random.Hortensius32Fast;

public class PerformActions {
	Hortensius32Fast rnd = new Hortensius32Fast();

	public DraggedLinkManager dragged_link_manager = new DraggedLinkManager();

	public DragBoxManager drag_box_manager = new DragBoxManager();

	public void actionSwitch(int x, int y, int type) {
		switch (type) {
		case Actions.VISIBLE:
			doSetHidden(x, y, false);

			break;

		case Actions.INVISIBLE:
			doSetHidden(x, y, true);

			break;

		case Actions.LINK:
			this.dragged_link_manager.doLink(x, y);

			break;

		case Actions.UNLINK:
			doUnlink(x, y);

			break;

		case Actions.UNLINKALL:
			doUnlinkAll(x, y);

			break;

		case Actions.KILL:
			doKill(x, y);

			break;

		case Actions.SELECT:
			if (this.drag_box_manager.drag_box_start == null) {
				FrEnd.perform_selection.performSelection(x, y, true);
			} else {
				this.drag_box_manager.drag(x, y);
			}

			break;

		case Actions.INFECT:
			FrEnd.perform_selection.performInfection(x, y);
			break;

		case Actions.SELECT_NO_DRAG:
			if (this.drag_box_manager.drag_box_start == null) {
				FrEnd.perform_selection.performSelection(x, y, false);
			} else {
				this.drag_box_manager.drag(x, y);
			}

			break;

		case Actions.DRAG_BOX:
			this.drag_box_manager.drag(x, y);

			break;

		case Actions.CLONE:
			doClone(x, y);

			break;

		case Actions.MAKE_CIRCLE:
			doMakeCircle(x, y);

			break;

		case Actions.ROTATE:
			doRotation(x, y);
			break;

		case Actions.ROTATE_CW_ACW:
			doRotation2(x, y);
			break;

		case Actions.TRANSLATE:
			doTranslation(x, y);
			break;

		case Actions.SCALE:
			doScale(x, y);
			break;

		case Actions.DIET:
			doDiet(x, y);
			break;

		default:
			break;
		}
	}

	void doRotation(int x, int y) {
		FrEnd.rotation_manager.initialise(x, y, false);
	}

	void doRotation2(int x, int y) {
		FrEnd.rotation_manager.initialise(x, y, true);
	}

	void doTranslation(int x, int y) {
		FrEnd.translation_manager.initialise(x, y);
	}

	void doScale(int x, int y) {
		FrEnd.scale_manager.initialise(ContextMananger.getNodeManager(), x, y);
	}

	void doDiet(int x, int y) {
		FrEnd.diet_manager.initialise(x, y);
	}

	void doFreeze(int x, int y) {
		final Node dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
		if (dragged_node != null) {
			dragged_node.type.pinned = true;
		}
	}

	void doMelt(int x, int y) {
		final Node dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
		if (dragged_node != null) {
			dragged_node.type.pinned = false;
		}
	}

	void doSetHidden(int x, int y, boolean hidden) {
		final Node dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
		if (dragged_node != null) {
			NodeType new_type = ContextMananger.getNodeManager().node_type_factory.getNew();
			new_type.makeEqualTo(dragged_node.type);
			new_type.hidden = hidden;

			dragged_node.type = new_type;
		}
	}

	void doUnlink(int x, int y) {
		Node dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
		dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
		if (dragged_node != null) {
			FrEnd.killLastLink(dragged_node);
		}

		FrEnd.postCleanup();
	}

	void doUnlinkAll(int x, int y) {
		Node dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
		dragged_node = ContextMananger.getNodeManager().isThereOne(x, y);
		if (dragged_node != null) {
			FrEnd.killAllLinks(dragged_node);
		}

		FrEnd.postCleanup();
	}

	void doClone(int x, int y) {
		Node selected_node;
		if (FrEnd.button_virginity) {
			selected_node = ContextMananger.getNodeManager().getSelectedNode();

			if (selected_node != null) {
				final NodeType type = ContextMananger.getNodeManager().node_type_factory.getNew();
				type.makeEqualTo(selected_node.type);
				type.selected = false;

				final Point3D pos = new Point3D(x - selected_node.type.radius, y - selected_node.type.radius, 0);

				ContextMananger.getNodeManager().addNewAgent(pos, selected_node.clazz, type);
			}
		}

		FrEnd.postCleanup();
	}

	void doMakeCircle(int x, int y) {
		int circle_radius = 0x4000;
		int nodeCount = 28;
		int radius = (int)(circle_radius * Math.PI / nodeCount);
		int color = 0xFFFFFFFF;
		Clazz clazz = ContextMananger.getNodeManager().clazz_factory.getNew(color);
		
		for (int n = 0; n < nodeCount; n++) {
			final NodeType type = ContextMananger.getNodeManager().node_type_factory.getNew();
			type.log_mass = 0;
			type.selected = false;
			type.hidden = false;
			type.disabled = false;
			type.radius = radius;
			type.counter = 0;
			type.charge = 0;
			type.pinned = true;
			int dx = (int)(circle_radius * Math.sin(2 * Math.PI * n/ nodeCount));
			int dy = (int)(circle_radius * Math.cos(2 * Math.PI * n / nodeCount));
			final Point3D pos = new Point3D(x + dx, y + dy, Node.DEFAULT_2D_DEPTH);
			ContextMananger.getNodeManager().addNewAgent(pos, clazz, type);
		}
		FrEnd.postCleanup();
	}

	public void doKill(int x, int y) {
		FrEnd.killtype = Actions.KILL;

		if (FrEnd.button_virginity) {
			single_killing(x, y);
		} else {
			kill_a_line(x, y, FrEnd.last_mousex, FrEnd.last_mousey);
		}
	}

	void single_killing(int x, int y) {
		switch (FrEnd.weapon_type) {
		case ToolTypes._PENCIL:
			kill_a_cell(x, y);
			break;

		case ToolTypes._BRUSH:
			circle_fill(x, y, 0x2000, 0);
			break;

		case ToolTypes._MACHINE:
			octagon_fill(x, y, 0x5000, 127);
			break;

		case ToolTypes._SPRAY:
			octagon_fill(x, y, 0x3000, 31);
			break;

		case ToolTypes._POTATO:
			circle_fill(x, y, 0x4000, 0);
			break;

		default:
			break;
		}
	}

	void octagon_fill(int x, int y, int r, int f) {
		for (int cx = -r; cx < r; cx = cx + 0x800) {
			for (int cy = -r; cy < r; cy = cy + 0x800) {
				if ((this.rnd.nextInt() & f) == 0) {
					if (((cx + cy) > (r * -1.5)) && ((cx + cy) < (r * 1.5)) && ((cx - cy) > (r * -1.5))
							&& ((cx - cy) < (r * 1.5))) {
						kill_a_cell(x + cx, y + cy);
					}
				}
			}
		}
	}

	void circle_fill(int x, int y, int r, int f) {
		for (int cx = -r; cx < r; cx = cx + 0x800) {
			for (int cy = -r; cy < r; cy = cy + 0x800) {
				if ((this.rnd.nextInt() & f) == 0) {
					if ((cx * cx + cy * cy) < r * r) {
						kill_a_cell(x + cx, y + cy);
					}
				}
			}
		}
	}

	private void kill_a_cell(int x, int y) {
		final Node temp_node = ContextMananger.getNodeManager().isThereOne(x, y);
		if (temp_node != null) {
			switch (FrEnd.killtype) {
			case Actions.KILL:
				final PrepareToModifyNodeTypes prepare = new PrepareToModifyNodeTypes(ContextMananger.getNodeManager());
				prepare.prepare();

				temp_node.simplyKill();

				FrEnd.postCleanup();

				break;

			default:
				break;
			}
		}
	}

	private void kill_a_line(int x, int y, int ox, int oy) {
		float _x = x;
		float _y = y;
		float dx;
		float dy;
		float dis;
		float num;
		int temp;

		switch (FrEnd.weapon_type) {
		case ToolTypes._MACHINE:
			temp = 12;
			break;

		case ToolTypes._BRUSH:
			temp = 3;
			break;

		case ToolTypes._SPRAY:
			temp = 4;
			break;

		case ToolTypes._POTATO:
			temp = 6;
			break;

		default:
			temp = 1;
		}

		final int dis_sq = ((x - ox) * (x - ox)) + ((y - oy) * (y - oy));
		if (dis_sq > 0) {
			dis = SquareRoot.fastSqrt(1 + dis_sq) >> 8;

			if (dis <= temp) {
				num = dis + 2;
			} else {
				num = dis / temp;
			}

			if (num < 2) {
				num = 2;
			}

			dx = (ox - x) / num;
			dy = (oy - y) / num;

			for (int i = 0; i < (int) num; i++) {
				single_killing((int) _x, (int) _y);
				_x += dx;
				_y += dy;
			}
		}
	}
}