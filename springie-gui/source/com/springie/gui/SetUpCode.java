package com.springie.gui;
import com.springie.world.World;

import com.springie.FrEnd;
import com.springie.context.ContextManager;
import com.springie.context.ModelManager;
import com.springie.elements.nodes.NodeManager;
import com.springie.explosions.fragments.LineFragmentManager;
import com.springie.explosions.particles.ParticleManager;
import com.springie.io.in.DataInput;
import com.springie.presets.PresetObjects;
import com.springie.world.ArgumentList;
import com.springie.world.WorldManager;

/*
 * To do: ======
 *
 * Add a "low number of bits" live/dead array - to postpone cache thrashing...?
 *
 */

public final class SetUpCode {
  private SetUpCode() {
    //...
  }

  public static void initialise(final int resolutionx, final int resolutiony) {

    // Fresh boot: drop the previous boot's model slots (they are
    // JVM-static), so the new model loads into the new manager.
    ModelManager.reset();
    ContextManager.setNodeManager(new NodeManager());
    ParticleManager.initial();

    LineFragmentManager.initial();
    World.explosion_spawner = new World.ExplosionSpawner() {
      public void spawnFragment(final int ix, final int iy, final int jx,
          final int jy, final int idx, final int idy, final int rotspeed) {
        LineFragmentManager.add(ix, iy, jx, jy, idx, idy, rotspeed);
      }

      public void spawnParticle(final int ix, final int iy, final int idx,
          final int idy, final int type) {
        ParticleManager.add(ix, iy, idx, idy, type);
      }
    };
    World.preset_provider = new World.PresetProvider() {
      public String getCreatureDescription(final ArgumentList arguments) {
        return PresetObjects.getCreatureDescription(arguments);
      }

      public String getCreatureDescription(final String location)
          throws Exception {
        return PresetObjects.getCreatureDescription(location);
      }
    };
    World.model_loader = new World.ModelLoader() {
      public void load(final NodeManager manager, final String path) {
        new DataInput(manager).loadFile(path);
      }

      public void loadFromString(final NodeManager manager,
          final String data) {
        new DataInput(null).addFromString(data, manager);
      }
    };
    World.reset_render_lights = new Runnable() {
      public void run() {
        com.springie.render.modules.modern.LightSource.resetToDefaults();
        com.springie.render.modules.modern.LightSource.light_moved = true;
        // Reset colored lighting state (Tim, 2026-10-14).
        com.springie.render.modules.modern.LightSource.colored_lighting = true;
        com.springie.render.RendererDelegator.ambient_light_pct = 0;
        com.springie.render.RendererDelegator.ambient_colored_pct = 0;
        com.springie.render.RendererDelegator.ambient_white_pct = 80;
        // Sync the UI (Tim, 2026-10-14).
        if (com.springie.FrEnd.panel_universe != null) {
          com.springie.FrEnd.panel_universe.reflectLights();
        }
      }
    };
    WorldManager.initial();
  }
  
  public static void clear() {
    ContextManager.getNodeManager().initialSetUp();
  }

  public static void clearAndThenAddInitialObjects() {
    ContextManager.getNodeManager().initial();
    informModulesOfReset();
  }

  public static void clearAndThenAddProceduralObjects(final ArgumentList al) {
    ContextManager.getNodeManager().initialWithPreset(al);
    informModulesOfReset();
  }

  private static void informModulesOfReset() {
    WorldManager.initial();
    if (World.module) {
      FrEnd.extension.calledOnReset();
    }
  }

  public static void addBoids() {
    ContextManager.getNodeManager().add();
  }
}
