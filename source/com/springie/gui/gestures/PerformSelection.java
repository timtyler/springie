// This program has been placed into the public domain by its author.

package com.springie.gui.gestures;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.elements.base.BaseElement;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceType;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.geometry.Point3D;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;
import com.springie.render.modules.modern.ModularRendererNew;
import com.springie.render.modules.raytraced.ModularRendererRaytraced;

public class PerformSelection {
	public static int INFECTION_START = 64;
	public static int VIRUS_RELEASE1 = 60;
	public static int VIRUS_RELEASE2 = 55;
	public static int VIRUS_RELEASE3 = 50;
	public static int IMMUNITY_START = 48;

	// Single-click pick arbitration: when candidates from several element
	// types match the click, only one of them is kept.
	private static final int PICK_NONE = 0;
	private static final int PICK_NODE = 1;
	private static final int PICK_LINK = 2;
	private static final int PICK_FACE = 3;

	public void performSelection(int x, int y, boolean drag_is_possible) {
		if (RendererDelegator.renderer instanceof ModularRendererNew
				|| RendererDelegator.renderer instanceof ModularRendererRaytraced) {
			// Link/face picking reads the projected caches this builds; the
			// tiled renderers do not maintain them while drawing.
			ContextManager.getNodeManager().nodeAndLinkRenderDummy();
		}

		final BaseElement dragged_element = FrEnd.dragged_element;

		if (dragged_element != null) {
			// Mid-drag (this also covers the click event that follows a
			// press): keep driving the dragged element, but never pick up
			// additional items while a drag is in progress.
			if (dragged_element instanceof Node) {
				if (FrEnd.panel_edit_select_main.checkbox_select_nodes.getState()) {
					doSelectNodes(x, y, drag_is_possible);
				}
			}
			if (FrEnd.panel_edit_select_main.checkbox_select_links.getState()) {
				doSelectLinks(x, y, drag_is_possible);
			}
			return;
		}

		// Fresh click: take the nearest candidate from every enabled element
		// type, then select only the one nearest to the viewer (smallest z).
		// A click overlapping e.g. a node and a face used to select both.
		final Node node;
		if (FrEnd.panel_edit_select_main.checkbox_select_nodes.getState()) {
			node = ContextManager.getNodeManager().isThereOne(x, y);
		} else {
			node = null;
		}

		final Link link;
		if (FrEnd.panel_edit_select_main.checkbox_select_links.getState()) {
			link = ContextManager.getLinkManager().isThereOne(x, y);
		} else {
			link = null;
		}

		final Face face;
		if (FrEnd.panel_edit_select_main.checkbox_select_faces.getState()) {
			face = ContextManager.getFaceManager().isThereOne(x, y);
		} else {
			face = null;
		}

		final int pick = nearestPick(node, link, face);

		final boolean hit;
		if (pick == PICK_NODE) {
			hit = doSelectNodes(x, y, drag_is_possible);
		} else if (pick == PICK_LINK) {
			hit = doSelectLinks(x, y, drag_is_possible);
		} else if (pick == PICK_FACE) {
			hit = doSelectPolygons(x, y);
		} else {
			hit = false;
		}

		if (hit) {
			// With the no-drag tool there is nothing more to do; otherwise
			// a drag has started and dragged_element is now set.
			if (FrEnd.dragged_element == null) {
				return;
			}
		} else {
			// Nothing under the cursor: as before, the GUI is refreshed,
			// anything selected is dropped, and this is where a drag-box
			// gesture starts.
			FrEnd.updateGUIToReflectSelectionChange();
			conditionallyDeselectAll();
			FrEnd.perform_actions.drag_box_manager.drag(x, y);
		}
	}

	// Chooses the click candidate nearest to the viewer. Every picker
	// already defines "nearest" as the smallest z, so the same depth is
	// used here; ties keep the historical node > link > face precedence.
	private static int nearestPick(Node node, Link link, Face face) {
		int pick = PICK_NONE;
		int best_z = Integer.MAX_VALUE;

		if (node != null) {
			best_z = node.pos.z;
			pick = PICK_NODE;
		}

		if (link != null && link.nodes[0].pos.z < best_z) {
			best_z = link.nodes[0].pos.z;
			pick = PICK_LINK;
		}

		if (face != null && averageZ(face) < best_z) {
			pick = PICK_FACE;
		}

		return pick;
	}

	private static int averageZ(Face face) {
		final int npoints = face.nodes.size();
		if (npoints == 0) {
			return Integer.MAX_VALUE;
		}
		int sum_z = 0;
		for (int i = 0; i < npoints; i++) {
			sum_z += ((Node) face.nodes.get(i)).pos.z;
		}
		return sum_z / npoints;
	}

	// performInfection
	public void performInfection(int x, int y) {
		final Node node = ContextManager.getNodeManager().isThereOne(x, y);
		if (node != null) {
			node.type.counter = INFECTION_START;
		}
	}

	private void dealWithDrag(int x, int y, final BaseElement selected_element) {
		if (FrEnd.button_virginity) {
			if (selected_element != null) {
				if (selected_element.isSelected()) {
					FrEnd.dragged_element = selected_element;
					final Point3D centre = FrEnd.dragged_element.getCoordinatesOfCentrePoint();

					FrEnd.dragged_x_offset = x - centre.x;
					FrEnd.dragged_y_offset = y - centre.y;
					FrEnd.currently_dragging = true;
				}
			}
		} else {
			if (FrEnd.currently_dragging) {
				FrEnd.dragCurrentObject(x, y);
			}
		}
	}

	public boolean doSelectNodes(int x, int y, boolean drag_is_possible) {
		// Log.log("GETS");
		final Node selected_node = ContextManager.getNodeManager().isThereOne(x, y);

		// check for selected node...
		boolean selection_changed = false;
		if (selected_node != null) {
			if (!FrEnd.currently_dragging) {
				if (!selected_node.isSelected()) {
					conditionallyDeselectAll();
					selection_changed = true;
				}

				FrEnd.selectNewNodeIfAppropriate(selected_node);

				FrEnd.panel_edit_properties_flags.checkbox_pinned.setState(selected_node.type.pinned);
				FrEnd.panel_edit_properties_flags.checkbox_hidden.setState(selected_node.type.hidden);
				FrEnd.panel_edit_color.color_picker_controller.setColour(selected_node.clazz.colour);

				FrEnd.panel_edit_properties_scalars.scroll_bar_radius.setValue(selected_node.type.radius);
				FrEnd.panel_edit_properties_scalars.setRadiusLabel(selected_node.type.radius);
				selection_changed = true;
			}

			if (drag_is_possible) {
				dealWithDrag(x, y, selected_node);
			}
		}

		if (selection_changed) {
			FrEnd.updateGUIToReflectSelectionChange();
		}

		return selected_node != null;
	}

	public boolean doSelectLinks(int x, int y, boolean drag_is_possible) {
		final Link selected_link = ContextManager.getLinkManager().isThereOne(x, y);
		boolean selection_changed = false;

		final BaseElement dragged_element = FrEnd.dragged_element;

		if (dragged_element == null) {
			if (selected_link != null) {
				if (!selected_link.isSelected()) {
					conditionallyDeselectAll();
					selection_changed = true;
				}
			}

			if (selected_link != null) {
				if (selected_link != FrEnd.dragged_element) {
					final LinkType type = selected_link.type;
					if ((FrEnd.main_canvas.modifiers & 2) != 0) {
						if (FrEnd.button_virginity) {
							FrEnd.prepareToModifyLinkTypes();
							selected_link.setSelectedFiltered(!type.selected);
							selection_changed = true;
						}
					} else {
						FrEnd.prepareToModifyLinkTypes();
						selected_link.setSelectedFiltered(true);
						selection_changed = true;
					}

					updateFlagsForOneLink(type);

					updateScalarsForOneLink(type);

					FrEnd.panel_edit_color.color_picker_controller.setColour(selected_link.clazz.colour);
				}
			}
		}

		if (drag_is_possible) {
			dealWithDrag(x, y, selected_link);
		}

		if (selection_changed) {
			FrEnd.updateGUIToReflectSelectionChange();
		}

		return selected_link != null;
	}

	private void updateScalarsForOneLink(final LinkType type) {
		FrEnd.panel_edit_properties_scalars.scroll_bar_elasticity.setValue(type.elasticity);
		FrEnd.panel_edit_properties_scalars.setElasticityLabel(type.elasticity);
		FrEnd.panel_edit_properties_scalars.scroll_bar_length.setValue(type.length);
		FrEnd.panel_edit_properties_scalars.setLengthLabel(type.length >> Coords.shift);
		FrEnd.panel_edit_properties_scalars.scroll_bar_radius.setValue(type.radius);
		FrEnd.panel_edit_properties_scalars.setRadiusLabel(type.radius);
	}

	private void updateFlagsForOneLink(final LinkType type) {
		FrEnd.panel_edit_properties_flags.checkbox_hidden.setState(type.hidden);
		FrEnd.panel_edit_properties_flags.checkbox_disabled.setState(type.disabled);
		FrEnd.panel_edit_properties_flags.checkbox_compression.setState(type.compression);
		FrEnd.panel_edit_properties_flags.checkbox_tension.setState(type.tension);
	}

	private void conditionallyDeselectAll() {
		if ((FrEnd.main_canvas.modifiers & 2) == 0) {
			deselectAllNodesInitially();
			deselectAllLinksInitially();
			deselectAllPolygonsInitially();
		}
	}

	public boolean doSelectPolygons(int x, int y) {
		final Face selected_face = ContextManager.getFaceManager().isThereOne(x, y);
		if (selected_face != null) {
			final FaceType type = selected_face.type;
			if ((FrEnd.main_canvas.modifiers & 2) != 0) {
				if (FrEnd.button_virginity) {
					FrEnd.prepareToModifyFaceTypes();
					type.selected = !type.selected;
				}
			} else {
				FrEnd.prepareToModifyFaceTypes();
				type.selected = true;
			}

			FrEnd.panel_edit_properties_flags.checkbox_hidden.setState(type.hidden);

			FrEnd.panel_edit_color.color_picker_controller.setColour(selected_face.clazz.colour);
		}

		FrEnd.updateGUIToReflectSelectionChange();

		return selected_face != null;
	}

	public void selectAll() {
		selectAllNodes();
		selectAllLinks();
	}

	public void deselectAll() {
		deselectAllNodes();
		deselectAllLinks();
	}

	private void deselectAllNodes() {
		ContextManager.getNodeManager().deselectAll();
	}

	private void selectAllNodes() {
		ContextManager.getNodeManager().selectAll();
	}

	private void selectAllLinks() {
		ContextManager.getLinkManager().selectAll();
	}

	private void deselectAllLinks() {
		ContextManager.getLinkManager().deselectAll();
	}

	private void deselectAllPolygons() {
		ContextManager.getFaceManager().deselectAll();
	}

	private void deselectAllNodesInitially() {
		deselectAllNodes();
	}

	private void deselectAllLinksInitially() {
		deselectAllLinks();
	}

	private void deselectAllPolygonsInitially() {
		deselectAllPolygons();
	}
}