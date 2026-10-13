// This code has been placed into the public domain by its author.
package com.springie.io;

/**
 * Configuration for the I/O module (file readers/writers).
 * The GUI/application layer sets these values; the io package only reads them.
 * This decouples io from FrEnd and the GUI panels.
 */
public class IoConfig {
  // Output formatting (was FrEnd.output_linefeeds).
  public static boolean output_linefeeds = true;

  // Merge mode for model loading (was FrEnd.merge).
  public static boolean merge = false;

  // POV export settings (was PanelPreferencesPOVExport statics).
  public static int pov_view_height = 50;
  public static int pov_immersion_depth = 0;
  public static String pov_ground = "1";
  public static String pov_sky = "1";
  public static String pov_compression = "1";

  // Import scale (was PanelPreferencesImport.import_scale).
  public static int import_scale = 94;

  // Callbacks the application wires up (replaces FrEnd.reflect* calls).
  public static Runnable after_property_editing = () -> {};
  public static Runnable after_serious_editing = () -> {};
  public static Runnable select_all = () -> {};
  public static Runnable deselect_all = () -> {};
  public static Runnable on_reset = () -> {};
  public static java.util.function.Consumer<String> set_file_path = (s) -> {};
}
