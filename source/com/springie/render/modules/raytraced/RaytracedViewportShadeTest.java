package com.springie.render.modules.raytraced;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.springie.FrEnd;
import com.springie.gui.GuiTestSupport;
import com.springie.render.Coords;
import com.springie.render.RendererDelegator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The grey border outside the viewpoint box must survive ray-traced
 * rendering. Each completed ray-traced frame blits its whole composite
 * image onto the canvas, which erases anything painted underneath it --
 * so the shade has to be repainted on every paint, not just after a
 * full clear (the way BoundaryBoxDots already repaints every paint).
 *
 * <p>The test pans the viewpoint right on the EDT immediately before the
 * asserting paint, so the expected grey strip is a direct consequence of
 * that paint's own shading pass -- it does not depend on the pan
 * surviving the multi-second asynchronous frame render.
 */
public class RaytracedViewportShadeTest {
  static int saved_shift_x;

  @BeforeAll
  static void setUpOnce() throws Exception {
    GuiTestSupport.bootApp();
    saved_shift_x = Coords.shift_constant_x;
    SwingUtilities.invokeAndWait(() -> {
      RendererDelegator.renderer = new ModularRendererRaytraced();
      FrEnd.paused = true;
      FrEnd.main_canvas.forceResize();
      RendererDelegator.repaint_all_objects = true;
    });
    repaintAndWait();
    waitForFrameStart();
    waitForFrameDone();
  }

  @AfterAll
  static void tearDownOnce() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Coords.shift_constant_x = saved_shift_x;
      RendererDelegator.repaint_all_objects = true;
    });
    GuiTestSupport.disposeFrames();
  }

  private static void repaintAndWait() throws Exception {
    SwingUtilities.invokeAndWait(() -> FrEnd.main_canvas.panel.repaint());
    // Queue behind the paint event: when this returns, the paint ran.
    SwingUtilities.invokeAndWait(() -> {
    });
  }

  private static ModularRendererRaytraced renderer() {
    return (ModularRendererRaytraced) RendererDelegator.renderer;
  }

  private static void waitForFrameStart() {
    final long deadline = System.currentTimeMillis() + 10000;
    while (!renderer().holdModelForFrame()
        && System.currentTimeMillis() < deadline) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    if (!renderer().holdModelForFrame()) {
      fail("The ray-traced frame never started rendering.");
    }
  }

  private static void waitForFrameDone() {
    final long deadline = System.currentTimeMillis() + 30000;
    while (renderer().holdModelForFrame()
        && System.currentTimeMillis() < deadline) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    assertTrue(!renderer().holdModelForFrame(),
      "The ray-traced frame did not finish within 30 seconds.");
  }

  @Test
  void greyBorderSurvivesRaytracedFrameBlit() throws Exception {
    // Pan right by exactly 100 front-plane pixels: the box's left edge
    // lands near x=100 (a little less at the back corners, which see a
    // larger perspective divisor), leaving a grey strip on the left.
    // This is a plain paint, not a full clear -- the old once-per-clear
    // shading would leave the strip unpainted here. repaint_some_objects
    // simulates a normal animation paint (a bare repaint() with the
    // animation paused would return early without painting anything).
    SwingUtilities.invokeAndWait(() -> {
      Coords.shift_constant_x = 100 * Coords.shift_constant_z;
      RendererDelegator.repaint_some_objects = true;
    });
    repaintAndWait();

    final BufferedImage[] image = new BufferedImage[1];
    SwingUtilities.invokeAndWait(() -> {
      final Rectangle[] area = new Rectangle[1];
      FrEnd.frame_main.toFront();
      final Point p = FrEnd.main_canvas.panel.getLocationOnScreen();
      area[0] = new Rectangle(p, FrEnd.main_canvas.panel.getSize());
      try {
        image[0] = new Robot().createScreenCapture(area[0]);
      } catch (java.awt.AWTException e) {
        throw new RuntimeException(e);
      }
    });

    final BufferedImage img = image[0];
    final int h = img.getHeight();
    // Well inside the strip (box left edge >= ~68px even at the back
    // corners), clear of the model and the boundary-box dots.
    for (int x = 10; x <= 40; x += 10) {
      assertEquals(0xFF808080, img.getRGB(x, h / 2),
        "Grey border missing at x=" + x + " after a ray-traced frame.");
    }
  }
}
