package com.springie.presets;

import com.springie.world.ArgumentList;
import com.springie.world.World;

/**
 * The presets module's World.PresetProvider: hands out preset model
 * descriptions from this module's bundled model files. Registered in
 * META-INF/services so the engine can discover it via
 * World.presetProvider() without depending on this module; SetUpCode
 * may still overwrite the field during GUI boot.
 */
public class PresetObjectsProvider implements World.PresetProvider {

  public PresetObjectsProvider() {
    // ServiceLoader instantiation
  }

  @Override
  public String getCreatureDescription(final ArgumentList arguments) {
    return PresetObjects.getCreatureDescription(arguments);
  }

  @Override
  public String getCreatureDescription(final String location)
      throws Exception {
    return PresetObjects.getCreatureDescription(location);
  }
}
