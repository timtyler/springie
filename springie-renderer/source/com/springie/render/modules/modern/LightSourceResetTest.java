// This code has been placed into the public domain by its author

package com.springie.render.modules.modern;

import com.springie.render.RendererDelegator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A model load resets the lights (ReaderTens calls resetToDefaults).
 * With colored lighting off, the positioned lights are ignored, so the
 * ambient white is the only light there is -- resetting it to 0 left
 * the fresh model with no lighting at all (Tim's report, 2026-11-07).
 */
class LightSourceResetTest {
  private boolean saved_colored;
  private int saved_ambient;
  private int saved_ambient_white;
  private Runnable saved_hook;

  @BeforeEach
  void save() {
    this.saved_colored = LightSource.colored_lighting;
    this.saved_ambient = RendererDelegator.ambient_light_pct;
    this.saved_ambient_white = RendererDelegator.ambient_white_pct;
    this.saved_hook = LightSource.onLightsReset;
    LightSource.onLightsReset = null;
  }

  @AfterEach
  void restore() {
    LightSource.colored_lighting = this.saved_colored;
    RendererDelegator.ambient_light_pct = this.saved_ambient;
    RendererDelegator.ambient_white_pct = this.saved_ambient_white;
    LightSource.onLightsReset = this.saved_hook;
  }

  @Test
  void resetWithColoredLightingOffKeepsAmbientWhite() {
    LightSource.colored_lighting = false;
    RendererDelegator.ambient_white_pct = 80;
    LightSource.resetToDefaults();
    assertEquals(80, RendererDelegator.ambient_light_pct);
  }

  @Test
  void resetWithColoredLightingOnZeroesAmbient() {
    LightSource.colored_lighting = true;
    LightSource.resetToDefaults();
    assertEquals(0, RendererDelegator.ambient_light_pct);
    assertEquals(6, LightSource.lights.size());
  }
}
