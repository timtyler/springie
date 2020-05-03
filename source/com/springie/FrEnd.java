// This program has been placed into the public domain by its author.

// How broken? Bins? Surely not.

package com.springie;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.InputEvent;
import java.util.Vector;

import com.springie.constants.Actions;
import com.springie.constants.Delay;
import com.springie.constants.FrameFrequency;
import com.springie.constants.Quality;
import com.springie.constants.ToolTypes;
import com.springie.context.ContextMananger;
import com.springie.elements.base.BaseElement;
import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.extension.Module;
import com.springie.geometry.Point3D;
import com.springie.gui.colourpicker.ColorPicker;
import com.springie.gui.colourpicker.ColorPickerInformer;
import com.springie.gui.components.ChoiceWithDescription;
import com.springie.gui.components.DropablePanel;
import com.springie.gui.components.ImageButton;
import com.springie.gui.components.TTChoice;
import com.springie.gui.components.TextFieldWrapper;
import com.springie.gui.frames.FrameMain;
import com.springie.gui.frames.FrameMaker;
import com.springie.gui.gestures.DietManager;
import com.springie.gui.gestures.PerformActions;
import com.springie.gui.gestures.PerformSelection;
import com.springie.gui.gestures.RotationManager;
import com.springie.gui.gestures.ScaleManager;
import com.springie.gui.gestures.TranslationManager;
import com.springie.gui.panels.PanelAbout;
import com.springie.gui.panels.PanelAllControls;
import com.springie.gui.panels.PanelFundamental;
import com.springie.gui.panels.PanelHelp;
import com.springie.gui.panels.UpdateEnabledComponents;
import com.springie.gui.panels.controls.PanelControls;
import com.springie.gui.panels.controls.PanelControlsDelete;
import com.springie.gui.panels.controls.PanelControlsEdit;
import com.springie.gui.panels.controls.PanelControlsGenerate;
import com.springie.gui.panels.controls.PanelControlsMisc;
import com.springie.gui.panels.controls.PanelControlsModify;
import com.springie.gui.panels.controls.PanelControlsProperties;
import com.springie.gui.panels.controls.PanelControlsPropertiesFlags;
import com.springie.gui.panels.controls.PanelControlsPropertiesNames;
import com.springie.gui.panels.controls.PanelControlsPropertiesScalars;
import com.springie.gui.panels.controls.PanelControlsPropertiesScaleFactor;
import com.springie.gui.panels.controls.PanelControlsSelect;
import com.springie.gui.panels.controls.PanelControlsSelectAdvanced;
import com.springie.gui.panels.controls.PanelControlsSelectLinks;
import com.springie.gui.panels.controls.PanelControlsSelectMain;
import com.springie.gui.panels.controls.PanelControlsStatistics;
import com.springie.gui.panels.controls.PanelControlsUniverse;
import com.springie.gui.panels.controls.PanelControlsVelocities;
import com.springie.gui.panels.preferences.PanelPreferences;
import com.springie.gui.panels.preferences.PanelPreferencesDisplay;
import com.springie.gui.panels.preferences.PanelPreferencesEdit;
import com.springie.gui.panels.preferences.PanelPreferencesIO;
import com.springie.gui.panels.preferences.PanelPreferencesRendererModern;
import com.springie.gui.panels.preferences.PanelPreferencesRendererModernColours;
import com.springie.gui.panels.preferences.PanelPreferencesRendererModernFilters;
import com.springie.gui.panels.preferences.PanelPreferencesRendererOriginal;
import com.springie.gui.panels.preferences.PanelPreferencesRendererShared;
import com.springie.gui.panels.preferences.PanelPreferencesRendererSharedMisc;
import com.springie.gui.panels.preferences.PanelPreferencesRendererSharedShow;
import com.springie.gui.panels.preferences.PanelPreferencesStereo3D;
import com.springie.gui.panels.preferences.PanelPreferencesUpdate;
import com.springie.gui.panels.preferences.PanelPreferencesViewpoint;
import com.springie.gui.panels.statistics.PanelStatistics;
import com.springie.io.in.DataInput;
import com.springie.messages.MessageManager;
import com.springie.messages.NewMessageManager;
import com.springie.messages.SystemMessages;
import com.springie.modification.colour.ColourChanger;
import com.springie.modification.post.PostModification;
import com.springie.modification.pre.PrepareToModifyFaceClazzes;
import com.springie.modification.pre.PrepareToModifyFaceTypes;
import com.springie.modification.pre.PrepareToModifyLinkClazzes;
import com.springie.modification.pre.PrepareToModifyLinkTypes;
import com.springie.modification.pre.PrepareToModifyNodeClazzes;
import com.springie.modification.pre.PrepareToModifyNodeTypes;
import com.springie.preferences.Preferences;
import com.springie.render.Coords;
import com.springie.render.MainCanvas;
import com.springie.render.RendererDelegator;
import com.springie.render.SetUpCode;
import com.springie.utilities.FilePath;
import com.springie.utilities.random.Hortensius32Fast;
import com.tifsoft.Forget;

public class FrEnd extends java.applet.Applet implements Runnable {
  static final long serialVersionUID = 1250;

  /**
   * true if development version of the app
   */
  public static final boolean development_version = false;

  static final boolean testing = false;

  /**
   * true if this is the cut-down viewer version of Springie
   */
  public static final boolean viewer = false;

  public static final String application_name = viewer ? "SprView" : "Springie";

  /*
   * <pre> true if 3rd party module is in use &lt;STRONG&gt;MAKE SURE THIS BIT
   * IS SET &lt;/STRONG&gt; if developing or using a 3rd party module... </pre>
   */
  public static final boolean module = false;

  /**
   * true if running in 3D space
   */
  public static boolean three_d = true;

  static final boolean chained = true;

  public static String index_subdirectory = "index";

  public static String model_index = "resource://" + index_subdirectory
      + "/index.xml";

  public static boolean node_growth;

  public static boolean continuously_centre;

  public static boolean render_nodes = true;

  public static boolean render_links = true;

  public static boolean render_anaglyph;

  public static boolean render_faces = true;

  public static boolean render_charges = true;

  public static boolean render_hidden_nodes;

  public static boolean render_hidden_links;

  public static boolean render_hidden_faces;

  public static boolean output_linefeeds = true;

  public static boolean check_collisions = true;

  public static boolean boundaries = true;

  public static boolean oscd = true;

  public static boolean controls_visible = true;

  public static boolean collide_self_only;

  public static int node_initial_size = 32;

  public static boolean explosions = true;

  public static boolean merge;

  static boolean draw_guide = true;

  public static boolean depth_cues;

  public static int weapon_type = ToolTypes._PENCIL;

  public static String initial_type;

  public static String initial_initial_type = "Moscow";

  public static int frame_frequency = FrameFrequency._FRAME_1;

  // 2 is too much for IE JVM!
  public static int delay = Delay._DELAY_2;

  public static int action_left_type = Actions.SELECT;

  public static int action_middle_type = Actions.TRANSLATE;

  public static int action_right_type = Actions.ROTATE;

  public static boolean currently_dragging;

  public static boolean pandemic_paradigm_direct_contact = false;

  public static Module extension;

  static int sample_rate = 64;

  static int use_native;

  static long very_old_time = -1;

  static long old_time = -1;

  static long last_fps_time = -1;

  static long current_time;

  static long one_frame_takes;

  static long lastime;

  static long newtime;

  static TTChoice choose_resolution;

  static TTChoice choose_display;

  static TTChoice choose_kill;

  static TTChoice choose_frequency;

  public static TTChoice choose_delay;

  static TTChoice choose_sample_rate;

  static TTChoice choose_sample_number;

  static TTChoice choose_density;

  public static TTChoice choose_quality;

  public static TTChoice choose_tool;

  public static TTChoice choose_display_cables;

  public static TTChoice choose_display_struts;

  public static ChoiceWithDescription choose_initial;

  public static ChoiceWithDescription choose_preset_index;

  public static boolean links_disabled;

  public static boolean thread_terminated;

  static int finding_period;

  public static int killtype;

  public static boolean mouse_pressed;

  public static boolean show_exhaust;

  public static int generation;

  public static boolean paused;

  public static int stepping;

  static int start_type;

  static final boolean crippled_version = true;

  public static SystemMessages system_messages = new SystemMessages();

  static final Color colour_grey1 = new Color(0xB0B0B0);

  public static boolean button_virginity = true;

  static boolean statusShown = true;

  private static volatile Thread runner;

  static final int xoff = 4;

  static final int yoff = 4;

  public static Font bold_font = new Font("Dialog", Font.BOLD, 12);

  static Hortensius32Fast rnd = new Hortensius32Fast();

  public static MessageManager message_manager = new MessageManager();

  public static NewMessageManager new_message_manager = new NewMessageManager();

  public static int last_mousex;

  public static int last_mousey;

  public static boolean application;

  public static int dragged_x_offset;

  public static int dragged_y_offset;

  static Node temp_node;

  static Link temp_link;

  //public static Link selected_link;

  //public static NodeManager node_manager;

  public static boolean xor;

  public static int quality;

  public static Frame frame_main;

  public static final String window_title_prefix = application_name;

  public static String last_file_path = "test.spr";

  public static String next_file_path = last_file_path;

  static Panel panel_resolution_selector;

  public static Panel panel_with_controls_at_bottom;

  public static Frame frame_controls;

  public static Frame frame_panel_about;

  public static Frame frame_panel_help;

  static Scrollbar scroll_bar_zoom;

  public static MainCanvas main_canvas;

  public static ImageButton button_restart;

  public static ImageButton button_step;

  // labels
  public static Label label_step_size;

  static Label label_show_time;

  static Label label_show_gen;

  static Label label_show_cnum;

  static Label label_size_x;

  static Label label_size_y;

  public static TextFieldWrapper textfield_step_size;

  public static PerformActions perform_actions;

  public static PerformSelection perform_selection;

  public static RotationManager rotation_manager;

  public static ScaleManager scale_manager;

  public static DietManager diet_manager;

  public static TranslationManager translation_manager;

  public static boolean redraw_deepest_first = true;

  public static Preferences preferences = new Preferences();

  public static PanelControlsMisc panel_edit_misc = new PanelControlsMisc(
      message_manager, new_message_manager);

  public static PanelControlsGenerate panel_edit_generate = new PanelControlsGenerate(
      message_manager);

  public static ColorPicker panel_edit_color = new ColorPicker(
      new ColorPickerInformer() {
        public void inform(int colour) {
          final ColourChanger cc = new ColourChanger(ContextMananger.getNodeManager());
          cc.setColour(colour);
        }
      });

  public static PanelControlsDelete panel_edit_delete = new PanelControlsDelete(
      message_manager, new_message_manager);

  public static PanelControlsVelocities panel_edit_velocities = new PanelControlsVelocities(
      message_manager, new_message_manager);

  public static PanelControlsEdit panel_edit_edit = new PanelControlsEdit(
      message_manager, new_message_manager);

  public static PanelControlsUniverse panel_edit_universe = new PanelControlsUniverse(
      message_manager);

  public static PanelControlsSelectAdvanced panel_edit_select_advanced = new PanelControlsSelectAdvanced(
      message_manager, new_message_manager);

  public static PanelControlsSelectLinks panel_edit_select_links = new PanelControlsSelectLinks(
      message_manager);

  public static PanelControlsSelectMain panel_edit_select_main = new PanelControlsSelectMain(
      message_manager, new_message_manager);

  public static PanelControlsSelect panel_edit_select = new PanelControlsSelect(
      message_manager);

  public static PanelControlsPropertiesFlags panel_edit_properties_flags = new PanelControlsPropertiesFlags(
      message_manager);

  public static PanelControlsPropertiesNames panel_edit_properties_names = new PanelControlsPropertiesNames(
      message_manager, new_message_manager);

  public static PanelControlsStatistics panel_controls_statistics = new PanelControlsStatistics(
      message_manager);

  public static PanelControlsPropertiesScalars panel_edit_properties_scalars = new PanelControlsPropertiesScalars(
      message_manager);

  public static PanelControlsPropertiesScaleFactor panel_edit_properties_misc = new PanelControlsPropertiesScaleFactor(
      message_manager);

  public static PanelControlsProperties panel_edit_properties = new PanelControlsProperties(
      message_manager);

  public static PanelControlsModify panel_edit_model = new PanelControlsModify(
      message_manager);

  public static PanelFundamental panel_fundamental = new PanelFundamental(
      message_manager, new_message_manager);

  public static PanelAbout panel_about = new PanelAbout();

  public static PanelHelp panel_help = new PanelHelp();

  public static PanelPreferencesViewpoint panel_preferences_viewpoint = new PanelPreferencesViewpoint(
      message_manager);

  public static PanelPreferencesStereo3D panel_preferences_stereo3d = new PanelPreferencesStereo3D(
      message_manager);

  public static PanelPreferencesRendererSharedShow panel_preferences_shared_show = new PanelPreferencesRendererSharedShow(
      message_manager);

  public static PanelPreferencesRendererSharedMisc panel_preferences_shared_misc = new PanelPreferencesRendererSharedMisc(
      message_manager);

  public static PanelPreferencesRendererShared panel_preferences_shared = new PanelPreferencesRendererShared(
      message_manager);

  public static PanelPreferencesEdit panel_preferences_edit = new PanelPreferencesEdit(
      message_manager);

  public static PanelPreferencesUpdate panel_preferences_update = new PanelPreferencesUpdate(
      message_manager);

  public static PanelPreferencesIO panel_preferences_io = new PanelPreferencesIO(
      message_manager);

  public static PanelPreferencesRendererOriginal panel_preferences_renderer_original = new PanelPreferencesRendererOriginal(
      message_manager);

  public static PanelPreferencesRendererModernFilters panel_preferences_renderer_modern_filters = new PanelPreferencesRendererModernFilters(
      message_manager);

  public static PanelPreferencesRendererModernColours panel_preferences_renderer_modern_colours = new PanelPreferencesRendererModernColours(
      message_manager);
  
  public static PanelPreferencesRendererModern panel_preferences_renderer_modern = new PanelPreferencesRendererModern(
      message_manager);

  public static PanelPreferencesDisplay panel_preferences_display = new PanelPreferencesDisplay(
      message_manager);

  public static PanelPreferences panel_preferences = new PanelPreferences(
      message_manager);

  public static PanelControls panel_controls = new PanelControls(
      message_manager);

  public static PanelStatistics panel_statistics = new PanelStatistics(
      message_manager);

  public static PanelAllControls panel_controls_all = new PanelAllControls();

  public static int resolutionx;

  public static int resolutiony;

  public static Applet applet;

  public static DataInput data_input = new DataInput(null);

  public static BaseElement dragged_element;

  public static boolean forces_disabled_during_gesture;

  public static boolean animation_inactive = true;

  public static String archive;

  static int last_known_good_x;

  static int last_known_good_y;

  public static boolean artificial_chemistry = true;

  public void start() {
    // Log.log("start() called");
    applet = this;
    resolutionx = 800;
    resolutiony = 600;

    if (module) {
      extension = new Module();
      extension.oneOffInitialisation();
    }

    SetUpCode.initialise(resolutionx, resolutiony);

    setUpGestureManagers();

    main_canvas = new MainCanvas(this);

    paused = false;

    show_exhaust = true;
    quality = Quality.MULTIPLE;

    panel_with_controls_at_bottom = setUpPanelForFrame2();

    this.setBackground(Color.black);

    final Panel panelFPS = new Panel();
    panelFPS.add(new Label("Frames per second:", Label.RIGHT));
    final String string_4_zeros = "0000";
    label_show_time = new Label(string_4_zeros, Label.LEFT);
    panelFPS.add(label_show_time);

    final Panel panel_cnum = new Panel();
    panel_cnum.add(new Label("Number of creatures:", Label.RIGHT));
    label_show_cnum = new Label(string_4_zeros, Label.LEFT);
    panel_cnum.add(label_show_cnum);

    final Panel panel_generation = new Panel();
    panel_generation.add(new Label("Generation:", Label.RIGHT));
    label_show_gen = new Label("0       ", Label.LEFT);
    panel_generation.add(label_show_gen);

    FrEnd.panel_edit_universe.reflectMaxSpeed();

    setLayout(new BorderLayout(0, 0));

    if (!viewer) {
      panel_with_controls_at_bottom.add(panel_fundamental.panel);
      // panel_funfidamental.panel.setVisible(false);
      // this.validate();
    }

    add("South", panel_with_controls_at_bottom);

    setUpFrames();

    add("Center", main_canvas.panel);

    greyStepIfNeeded();

    loadInitialModel();

    ContextMananger.getNodeManager().initialSetUp();

    reflectValuesInGUIAfterPropertyEditing();

    main_canvas.start_up();

    startThread();

    if (application) {
      resize(resolutionx, resolutiony);
    } else if (last_known_good_x > 0) {
      resize(last_known_good_x, last_known_good_y);
    }

    new_message_manager.add(FrEnd.system_messages.getRestartMessage());

    validate();
    repaint();
  }

  // Note: sequence is important here...
  private void loadInitialModel() {
    // FrEnd.archive = null;

    initial_type = initial_initial_type;

    // TO DO - get rid of this legacy code

    SetUpCode.clearAndThenAddInitialObjects();

    if (!application) {
      FrEnd.archive = getParameter("url");
      final boolean focus = "true".equals(getParameter("focus"));
      preferences.map.put(Preferences.key_update_animation_when_pointer_over,
          new Boolean(focus));
    }
  }

  private void setUpGestureManagers() {
    translation_manager = new TranslationManager();

    perform_actions = new PerformActions();

    perform_selection = new PerformSelection();

    rotation_manager = new RotationManager();

    scale_manager = new ScaleManager();

    diet_manager = new DietManager();
  }

  public static Panel setUpPanelForFrame() {
    final Panel p = new Panel();
    p.setLayout(new GridLayout(0, 1, 0, 0));
    p.setForeground(Color.black);
    p.setBackground(Color.lightGray);
    return p;
  }

  public static Panel setUpPanelForFrame2() {
    if (application && usingJava120()) {
      final DropablePanel p = new DropablePanel();
      p.setLayout(new GridLayout(0, 1, 0, 0));
      p.setForeground(Color.black);
      p.setBackground(Color.lightGray);
      return p;
    }

    return setUpPanelForFrame();
  }

  public static boolean usingJava120() {
    final String version = System.getProperty("java.version");
    return "1.1.9".compareTo(version) < 0;
  }

  private void setUpFrames() {
    final FrameMaker frame_maker = new FrameMaker();

    frame_panel_about = frame_maker.setUpFrameAbout();
    frame_panel_help = frame_maker.setUpFrameHelp();
    // frame_preferences = frame_maker.setUpFramePreferences();
    frame_controls = frame_maker.setUpFrameControls();
  }

  static void setUpResolutionSelector2() {
    panel_resolution_selector.removeAll();
    panel_resolution_selector.setBackground(colour_grey1);
    panel_resolution_selector.add(label_size_x);
    panel_resolution_selector.add(label_size_y);
    panel_resolution_selector.add(panel_edit_properties_scalars.button_setsize);
  }

  public static void postCleanup() {
    new PostModification(ContextMananger.getNodeManager()).cleanup();
    reflectAllValuesInGUIAfterSeriousEditing();
  }

  public static void reflectAllValuesInGUIAfterSeriousEditing() {
    reflectElementNumbersInGUI();
    reflectValuesInGUIAfterPropertyEditing();
  }

  public static void reflectValuesInGUIAfterPropertyEditing() {
    reflectStatusInGUI();
    updateGUIToReflectSelectionChange();
  }

  private static void reflectElementNumbersInGUI() {
    final int n_nodes = ContextMananger.getNodeManager().element.size();
    final int n_links = ContextMananger.getLinkManager().element.size();
    final int n_faces = ContextMananger.getFaceManager().element.size();

    panel_controls_statistics.updateGUIToReflectSelectionChange();
    
    FrEnd.panel_edit_select_main.resetPanelTypeSelector(n_nodes != 0,
        n_links != 0, n_faces != 0);
  }

  public static void reflectStatusInGUI() {
    panel_edit_properties_scalars.reflectRadius();
    panel_edit_properties_scalars.reflectCharge();
    panel_edit_properties_scalars.reflectLength();
    panel_edit_properties_scalars.reflectElasticity();
    panel_edit_properties_scalars.reflectStiffness();

    FrEnd.panel_edit_universe.reflectMaxSpeed();
    FrEnd.panel_edit_universe.reflectImpact();
    FrEnd.panel_edit_universe.reflectViscocity();
    FrEnd.panel_edit_universe.reflectGravity();
    FrEnd.panel_edit_universe.reflectTemperature();
    FrEnd.panel_edit_universe.reflect3D();
    
    FrEnd.panel_controls_statistics.updateGUIToReflectPropertiesChange();

    RendererDelegator.repaintAll();
  }

  public static void updateGUIToReflectSelectionChange() {
    RendererDelegator.repaint_some_objects = true;
    UpdateEnabledComponents.updateGUIGreyItemsDependingOnSelection();
    
    FrEnd.panel_edit_properties_scalars.updateGUIToReflectSelectionChange();
    FrEnd.panel_controls_statistics.updateGUIToReflectSelectionChange();

    FrEnd.panel_edit_properties_names.updatePrefix();
    FrEnd.panel_edit_properties_names.updateSuffix();
  }

  public static void selectNewNodeIfAppropriate(Node selected_node) {
    if ((main_canvas.modifiers & 2) != 0) {
      if (button_virginity) {
        prepareToModifyNodeTypes();
        selected_node.type.selected = !selected_node.type.selected;
      }
    } else {
      prepareToModifyNodeTypes();
      selected_node.type.selected = true;
    }

    updateGUIToReflectSelectionChange();
  }

  public static void prepareToModifyAllTypes() {
    FrEnd.prepareToModifyNodeTypes();
    FrEnd.prepareToModifyLinkTypes();
    FrEnd.prepareToModifyFaceTypes();
  }

  // .....................Run Method......................
  public final void run() {
    // Log.log("run() called");
    thread_terminated = false;

    while (!thread_terminated) {
      RendererDelegator.callUpdateMethods();
      if (!paused) {
        if (stepping > 0) {
          if (--stepping == 0) {
            endStepping();
          }
        }
      }

      if (RendererDelegator.isOldDoubleBuffer()) {
        if (message_manager.current_message != 0) {
          message_manager.process();
        }
        new_message_manager.process();

        RendererDelegator.redrawChanged(main_canvas.graphics_handle);
      }

      main_canvas.panel.repaint();

      if (delay > 0) {
        takeFortyWinks();
      }

      Thread.yield();
    }

    thread_terminated = true;

    Thread.yield();

    // Log.log("Exit via thread_terminated");
  }

  private void takeFortyWinks() {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Forget.about(e);
    }
  }

  public static final void endStepping() {
    stepping = 0;
    paused = true;
    greyPauseAndRestartIfNeeded();
    //button_step.setLabel(GUIStrings.STEP);
    button_step.setEnabled(true);
  }

  public void startThread() {
    // Log.log("startThread() called");

    main_canvas.forceResize();
    thread_terminated = false;

    runner = new Thread(this);
    runner.start();

    validate();
    repaint();
    // Log.log("startThread() exited");
  }

  public void stop() {
    // Log.log("Call to 'stop' method");

    last_known_good_x = Coords.x_pixels;
    last_known_good_y = Coords.y_pixels;
    thread_terminated = true;
    runner = null;

    removeAll();
  }

  public static final void performResize() {
    main_canvas.forceResize();
    // BinGrid.set_size(resolutionx, resolutiony);
    RendererDelegator.repaintAll();
  }

  public static void greyStepIfNeeded() {
    button_step.setEnabled(paused);
  }

  public static void greyPauseAndRestartIfNeeded() {
    panel_fundamental.button_paused.setEnabled(paused);
    button_restart.setEnabled(paused);
  }

  public static void processMouseClick(int x, int y) {
    if ((main_canvas.modifiers & InputEvent.BUTTON1_MASK) != 0) {
      perform_actions.actionSwitch(x, y, action_left_type);
    }

    if ((main_canvas.modifiers & InputEvent.BUTTON2_MASK) != 0) {
      perform_actions.actionSwitch(x, y, action_middle_type);
    }

    if ((main_canvas.modifiers & InputEvent.BUTTON3_MASK) != 0) {
      perform_actions.actionSwitch(x, y, action_right_type);
    }

    last_mousex = x;
    last_mousey = y;
  }

  public static void springieMouseClicked(int x, int y) {
    if (!button_virginity) {
      processMouseClick(x, y);
      button_virginity = false;
    }
  }

  public static void springieMousePressed(int x, int y) {
    processMouseClick(x, y);
    button_virginity = false;
  }

  public static void springieMouseDragged(int x, int y) {
    processMouseClick(x, y);
    button_virginity = false;
  }

  public static void springieMouseReleased(int x, int y) {
    perform_actions.dragged_link_manager.terminateLink(x, y);
    perform_actions.drag_box_manager.terminate(x, y);
    rotation_manager.terminate(x, y);
    translation_manager.terminate(x, y);
    scale_manager.terminate(x, y);
    diet_manager.terminate(x, y);

    perform_actions.dragged_link_manager.pointer_node = null;
    currently_dragging = false;
    button_virginity = true;
    dragged_element = null;
  }

  public static void springieMouseEntered(int x, int y) {
    last_mousex = x;
    last_mousey = y;

    animation_inactive = false;
    if (((Boolean) FrEnd.preferences.map
        .get(Preferences.key_update_animation_when_pointer_over))
        .booleanValue()) {
      RendererDelegator.repaintAll();
    }
  }

  public static void springieMouseExited(int x, int y) {
    last_mousex = x;
    last_mousey = y;

    animation_inactive = true;
  }

  public static void prepareToModifyNodeTypes() {
    final PrepareToModifyNodeTypes prepare = new PrepareToModifyNodeTypes(
        ContextMananger.getNodeManager());
    prepare.prepare();
    RendererDelegator.repaint_some_objects = true;
  }

  public static void prepareToModifyLinkTypes() {
    final PrepareToModifyLinkTypes prepare = new PrepareToModifyLinkTypes(
        ContextMananger.getLinkManager());
    prepare.prepare();
    RendererDelegator.repaint_some_objects = true;
  }

  public static void prepareToModifyFaceTypes() {
    final PrepareToModifyFaceTypes prepare = new PrepareToModifyFaceTypes(
        ContextMananger.getFaceManager());
    prepare.prepare();
    RendererDelegator.repaint_some_objects = true;
  }

  static void prepareToModifyNodeClazzes() {
    final PrepareToModifyNodeClazzes prepare = new PrepareToModifyNodeClazzes(
        ContextMananger.getNodeManager());
    prepare.prepare();
    RendererDelegator.repaint_some_objects = true;
  }

  static void prepareToModifyLinkClazzes() {
    final PrepareToModifyLinkClazzes prepare = new PrepareToModifyLinkClazzes(
        ContextMananger.getNodeManager());
    prepare.prepare();
    RendererDelegator.repaint_some_objects = true;
  }

  static void prepareToModifyPolygonClazzes() {
    final PrepareToModifyFaceClazzes prepare = new PrepareToModifyFaceClazzes(
        ContextMananger.getNodeManager());
    prepare.prepare();
    RendererDelegator.repaint_some_objects = true;
  }

  public static void killAllLinks(Node e) {
    ContextMananger.getLinkManager().killAllLinks(e);

    postCleanup();
  }

  public static void killLastLink(Node e) {
    ContextMananger.getLinkManager().killLastLink(e);

    postCleanup();
  }

  public static void dragCurrentObject(int x, int y) {
    if (FrEnd.dragged_element != null) {
      if (FrEnd.dragged_element.isSelected()) {
        // Node dragged_node = (Node)FrEnd.dragged_element;
        final Point3D centre = FrEnd.dragged_element
            .getCoordinatesOfCentrePoint();
        final int d_x = x - dragged_x_offset - centre.x;
        final int d_y = y - dragged_y_offset - centre.y;

        final Vector list_of_nodes = getallNodesInContactWithSelection();

        ContextMananger.getNodeManager().moveNodesInList(list_of_nodes, d_x, d_y);
        RendererDelegator.repaint_some_objects = true;
      }
    }
  }

  private static Vector getallNodesInContactWithSelection() {
    Vector list_of_nodes;
    Vector list_of_nodes_2;
    list_of_nodes = ContextMananger.getNodeManager().getVectorOfSelectedNodes();
    list_of_nodes_2 = ContextMananger.getLinkManager()
        .getVectorOfNodesOnSelectedLinks();

    addElementsOfList2ToList1(list_of_nodes, list_of_nodes_2);

    return list_of_nodes;
  }

  private static void addElementsOfList2ToList1(Vector list_1, Vector list_2) {
    final int size = list_2.size();
    for (int i = 0; i < size; i++) {
      final Object o = list_2.elementAt(i);
      if (!list_1.contains(o)) {
        list_1.addElement(o);
      }
    }
  }

  public static void loadFile(FilePath filepath) {
    final String path = "file://" + filepath;
    FrEnd.setFilePath(path);
    FrEnd.data_input.loadFile(path);
  }

  public String getAppletInfo() {
    return application_name + " - tensegrity simulator.";
  }

  public static boolean isAnimationInactive() {
    if (FrEnd.paused) {
      return true;
    }

    final Boolean edit_animation_with_pointer = (Boolean) FrEnd.preferences.map
        .get(Preferences.key_update_animation_when_pointer_over);

    return FrEnd.animation_inactive
        && edit_animation_with_pointer.booleanValue();
  }

  public static void setFilePath(String path) {
    FrEnd.last_file_path = path;
    FrEnd.next_file_path = path;

    if (FrEnd.frame_main == null) {
      return;
    }

    final String s = FrEnd.window_title_prefix + " - " + FrEnd.last_file_path;

    FrEnd.frame_main.setTitle(s);
  }

  public static void main(String[] args) {
    Forget.about(args);

    final FrEnd applet = new FrEnd();

    FrEnd.application = true;

    frame_main = new FrameMain(window_title_prefix, applet);

    frame_main.setVisible(true);
  }
}