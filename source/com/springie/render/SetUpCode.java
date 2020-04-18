package com.springie.render;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.nodes.NodeManager;
import com.springie.explosions.fragments.LineFragmentManager;
import com.springie.explosions.particles.ParticleManager;
import com.springie.messages.ArgumentList;
import com.springie.world.WorldManager;
import com.tifsoft.Forget;

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

  public static void initialise(int resolutionx, int resolutiony) {
    Forget.about(resolutionx);
    Forget.about(resolutiony);
    
    ContextMananger.setNodeManager(new NodeManager());
    ParticleManager.initial();

    LineFragmentManager.initial();
    WorldManager.initial();
  }
  
  public static void clear() {
    ContextMananger.getNodeManager().initialSetUp();
  }

  public static void clearAndThenAddInitialObjects() {
    ContextMananger.getNodeManager().initial();
    informModulesOfReset();
  }

  public static void clearAndThenAddProceduralObjects(ArgumentList al) {
    ContextMananger.getNodeManager().initialWithPreset(al);
    informModulesOfReset();
  }

  private static void informModulesOfReset() {
    WorldManager.initial();
    if (FrEnd.module) {
      FrEnd.extension.calledOnReset();
    }
  }

  public static void addBoids() {
    ContextMananger.getNodeManager().add();
  }
}
