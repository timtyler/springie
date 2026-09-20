// This program has been placed into the public domain by its author.

package com.springie.preferences;

import java.util.HashMap;

import com.springie.FrEnd;

public class Preferences {
  public static final String key_output_pov_ground = "output.pov.ground";

  public static final String key_output_pov_sky = "output.pov.sky";

  public static final String key_output_pov_compression = "output.pov.compression";

  public static final String key_update_animation_when_pointer_over = "update.animation.when.pointer.over";

  public HashMap<String, Object> map = new HashMap<>();

  public Preferences() {
    this.map.put(Preferences.key_output_pov_ground, "none");
    this.map.put(Preferences.key_output_pov_sky, "white");
    this.map.put(Preferences.key_output_pov_compression, "bulge");
    this.map.put(Preferences.key_update_animation_when_pointer_over, Boolean.valueOf(FrEnd.viewer));
  }
}
