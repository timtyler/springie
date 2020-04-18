// This program has been placed into the public domain by its author.

package com.springie.gui.panels;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.gui.colourpicker.ColourPicker;
import com.springie.gui.components.ComponentAccess;
import com.springie.gui.panels.controls.PanelControlsDelete;
import com.springie.gui.panels.controls.PanelControlsMisc;
import com.springie.gui.panels.controls.PanelControlsPropertiesFlags;
import com.springie.gui.panels.controls.PanelControlsPropertiesScalars;
import com.springie.gui.panels.controls.PanelControlsSelectAdvanced;
import com.springie.gui.panels.controls.PanelControlsSelectMain;

public final class UpdateEnabledComponents {
  static boolean needs_update;

  static boolean buttons_need_update;

  static int hash_old = 99;

  private UpdateEnabledComponents() {
    // ...
  }

  public static void actuallyUpdate() {
    if (buttons_need_update) {
      actuallyUpdateButtonsAfterSelectionChange();
      needs_update = false;
    }
    if (needs_update) {
      actuallyUpdateGUIAfterSelectionChange();
      needs_update = false;
    }
  }

  public static void actuallyUpdateButtonsAfterSelectionChange() {
    final PanelControlsSelectMain panel_edit_select_main = FrEnd.panel_edit_select_main;

    final boolean nodes = panel_edit_select_main.checkbox_select_nodes
        .getState();
    final boolean links = panel_edit_select_main.checkbox_select_links
        .getState();
    final boolean faces = panel_edit_select_main.checkbox_select_faces
        .getState();
    final boolean any = links || nodes || faces;

    ComponentAccess.setAccess(panel_edit_select_main.button_select_all_type,
        any);
    ComponentAccess.setAccess(panel_edit_select_main.button_deselect_all_type,
        any);
    
  }

  public static void actuallyUpdateGUIAfterSelectionChange() {
    final boolean nodes = ContextMananger.getNodeManager().isSelection();
    final boolean links = ContextMananger.getLinkManager().isSelection();
    final boolean faces = ContextMananger.getFaceManager().isSelection();

    int hash = (nodes ? 1 : 0) | (links ? 2 : 0) | (faces ? 4 : 0);

    if (hash == hash_old) {
      return;
    }

    hash_old = hash;

    final boolean links_and_nodes = links || nodes;
    final boolean any = links || nodes || faces;

    final PanelControlsPropertiesScalars panel_edit_properties_scalars = FrEnd.panel_edit_properties_scalars;

    final PanelControlsPropertiesFlags panel_edit_properties_flags = FrEnd.panel_edit_properties_flags;

    ComponentAccess
        .setAccess(panel_edit_properties_flags.checkbox_pinned, nodes);
    ComponentAccess.setAccess(panel_edit_properties_flags.checkbox_compression,
        links);
    ComponentAccess.setAccess(panel_edit_properties_flags.checkbox_tension,
        links);
    ComponentAccess.setAccess(panel_edit_properties_flags.checkbox_disabled,
        links);

    ComponentAccess.setAccess(panel_edit_properties_scalars.scroll_bar_radius,
        links_and_nodes);
    ComponentAccess.setAccess(panel_edit_properties_scalars.scroll_bar_length,
        links);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.scroll_bar_elasticity, links);
    ComponentAccess.setAccess(panel_edit_properties_scalars.scroll_bar_damping,
        links);
    ComponentAccess.setAccess(panel_edit_properties_scalars.scroll_bar_charge,
        nodes);

    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_radius_up, links_and_nodes);
    ComponentAccess
        .setAccess(panel_edit_properties_scalars.button_scale_radius_down,
            links_and_nodes);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_lengths_up, links);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_lengths_down, links);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_elasticity_up, links);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_elasticity_down, links);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_damping_up, links);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_damping_down, links);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_charge_up, nodes);
    ComponentAccess.setAccess(
        panel_edit_properties_scalars.button_scale_charge_down, nodes);

    final PanelControlsSelectMain panel_edit_select_main = FrEnd.panel_edit_select_main;

    ComponentAccess.setAccess(panel_edit_properties_flags.checkbox_hidden, any);

    ComponentAccess.setAccess(panel_edit_select_main.button_select_clazz, any);
    ComponentAccess.setAccess(panel_edit_select_main.button_select_type, any);

    final boolean some_nodes_exist = FrEnd.panel_controls_statistics.number_of_nodes != 0;
    final boolean some_links_exist = FrEnd.panel_controls_statistics.number_of_links != 0;
    final boolean some_faces_exist = FrEnd.panel_controls_statistics.number_of_faces != 0;

    ComponentAccess.setAccess(panel_edit_select_main.button_select_nodes,
        some_nodes_exist);
    ComponentAccess.setAccess(panel_edit_select_main.button_select_links,
        some_links_exist);
    ComponentAccess.setAccess(panel_edit_select_main.button_select_faces,
        some_faces_exist);

    ComponentAccess.setAccess(panel_edit_select_main.checkbox_select_nodes,
        some_nodes_exist);
    ComponentAccess.setAccess(panel_edit_select_main.checkbox_select_links,
        some_links_exist);
    ComponentAccess.setAccess(panel_edit_select_main.checkbox_select_faces,
        some_faces_exist);

    ComponentAccess.setAccess(panel_edit_select_main.button_deselect_all_type,
        any);
    ComponentAccess.setAccess(panel_edit_select_main.button_invert_all_type,
        any);

    final PanelControlsMisc panel_edit_misc = FrEnd.panel_edit_misc;

    ComponentAccess.setAccess(panel_edit_misc.button_automatic_node_radius,
        nodes);
    ComponentAccess.setAccess(panel_edit_misc.button_automatic_link_radius,
        links);

    ComponentAccess.setAccess(panel_edit_misc.button_add_faces, nodes);
    ComponentAccess.setAccess(
        panel_edit_misc.button_connect_nodes_to_nearest_nodes, nodes);
    ComponentAccess.setAccess(panel_edit_misc.button_add_central_hub, nodes);
    ComponentAccess.setAccess(panel_edit_misc.button_add_stellations, faces);
    ComponentAccess.setAccess(panel_edit_misc.button_dimple, nodes);
    ComponentAccess.setAccess(panel_edit_misc.button_cartesian_colourer, links);
    ComponentAccess.setAccess(panel_edit_misc.button_extend_links, links);
    ComponentAccess.setAccess(panel_edit_misc.button_split_links, links);
    ComponentAccess.setAccess(panel_edit_misc.button_edit_reset_link_lengths,
        links);
    ComponentAccess.setAccess(
        panel_edit_misc.button_edit_equalise_link_lengths, links);
    ComponentAccess.setAccess(panel_edit_misc.button_face_reverse, faces);

    final PanelControlsDelete panel_edit_delete = FrEnd.panel_edit_delete;

    ComponentAccess
        .setAccess(panel_edit_delete.button_edit_remove_links, nodes);
    ComponentAccess.setAccess(panel_edit_delete.button_edit_remove_polygons,
        nodes);
    ComponentAccess.setAccess(panel_edit_delete.button_delete_selection, any);
    ComponentAccess.setAccess(panel_edit_delete.button_delete_all, true);

    ComponentAccess.setAccess(
        panel_edit_delete.button_edit_fuse_nodes, nodes);
    ComponentAccess.setAccess(
        panel_edit_delete.button_edit_remove_link_and_fuse_ends, links);

    final PanelFundamental panel_fundamental = FrEnd.panel_fundamental;
    
    ComponentAccess.setAccess(panel_fundamental.button_delete, any);
    ComponentAccess.setAccess(panel_fundamental.button_select_all_of_class, any);
        
    final PanelControlsSelectAdvanced panel_sel_adv = FrEnd.panel_edit_select_advanced;

    ComponentAccess.setAccess(
        panel_sel_adv.button_edit_spread_selection_via_links, any);

    final ColourPicker panel_controls_colour = FrEnd.panel_edit_colour;

    panel_controls_colour.colour_picker_controller.greyGetAndSetColourButtons();
    ComponentAccess.setAccess(
        panel_controls_colour.colour_picker_controller.cp_preview.button_set,
        any);

    panel_edit_properties_flags.resetPanel(nodes, links, faces);
    panel_edit_properties_scalars.resetPanel(nodes, links, faces);
  }

  public static void greySelectButtonsDependingOnSelection() {
    buttons_need_update = true;
  }

  public static void updateGUIGreyItemsDependingOnSelection() {
    needs_update = true;
  }
}
