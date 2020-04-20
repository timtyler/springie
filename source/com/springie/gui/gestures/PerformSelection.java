// This program has been placed into the public domain by its author.

package com.springie.gui.gestures;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
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

public class PerformSelection {
	int INITIAL_INFECTION = 32;
	
	public void performSelection(int x, int y, boolean drag_is_possible) {
		if (RendererDelegator.renderer instanceof ModularRendererNew) {
			ContextMananger.getNodeManager().nodeAndLinkRenderDummy();
		}

		final BaseElement dragged_element = FrEnd.dragged_element;

		if ((dragged_element == null) || (dragged_element instanceof Node)) {
			if (FrEnd.panel_edit_select_main.checkbox_select_nodes.getState()) {
				final boolean change_or_selected = doSelectNodes(x, y, drag_is_possible);
				if (change_or_selected) {
					if (FrEnd.dragged_element == null) {
						return;
					}
				}
			}
		}

		if (FrEnd.panel_edit_select_main.checkbox_select_links.getState()) {
			final boolean change_or_selected = doSelectLinks(x, y, drag_is_possible);
			if (change_or_selected) {
				if (FrEnd.dragged_element == null) {
					return;
				}
			}
		}

		if (FrEnd.panel_edit_select_main.checkbox_select_faces.getState()) {
			final boolean change_or_selected = doSelectPolygons(x, y);
			if (change_or_selected) {
				if (FrEnd.dragged_element == null) {
					return;
				}
			}
		}

		if (FrEnd.dragged_element == null) {
			conditionallyDeselectAll();
			FrEnd.perform_actions.drag_box_manager.drag(x, y);
		}
	}

	// performInfection
	public void performInfection(int x, int y) {
		final Node node = ContextMananger.getNodeManager().isThereOne(x, y);
		if (node != null) {
			node.type.counter = INITIAL_INFECTION;
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
		final Node selected_node = ContextMananger.getNodeManager().isThereOne(x, y);

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
		final Link selected_link = ContextMananger.getLinkManager().isThereOne(x, y);
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
		final Face selected_face = ContextMananger.getFaceManager().isThereOne(x, y);
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
		ContextMananger.getNodeManager().deselectAll();
	}

	private void selectAllNodes() {
		ContextMananger.getNodeManager().selectAll();
	}

	private void selectAllLinks() {
		ContextMananger.getLinkManager().selectAll();
	}

	private void deselectAllLinks() {
		ContextMananger.getLinkManager().deselectAll();
	}

	private void deselectAllPolygons() {
		ContextMananger.getFaceManager().deselectAll();
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