package com.springie.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.springie.context.ContextManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.geometry.Point3D;
import com.springie.io.in.readers.spr.ReaderSPR;
import com.springie.io.out.writers.spr.WriterSpr;
import com.springie.render.Coords;

/**
 * Link phase must survive an SPR save/load round trip:
 * WriterSpr emits a phase attribute on the link tag, ReaderSPR turns it
 * into a PH instruction, and ReaderTens applies it to the link.
 */
class LinkPhaseSprRoundTripTest {

  private NodeManager manager;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    ContextManager.setNodeManager(this.manager);
    this.manager.initialSetUp();
  }

  private Link addLinkWithPhase(int phase) {
    final Node n1 = new Node();
    n1.pos = new Point3D(0, 0, 0);
    final Node n2 = new Node();
    n2.pos = new Point3D(100 << Coords.shift, 0, 0);
    final LinkManager link_manager = this.manager.getLinkManager();
    final int before = link_manager.element.size();
    final Link link = new Link(n1, n2,
        link_manager.link_type_factory.getNew(100 << Coords.shift, 50),
        this.manager.clazz_factory.getNew(0));
    link.phase = phase;
    link_manager.element.add(link);
    return (Link) link_manager.element.get(before);
  }

  @Test
  void writerEmitsPhaseAttributeWhenNonZero() {
    addLinkWithPhase(7);
    final String xml = new WriterSpr(this.manager).generateString();
    assertTrue(xml.contains("phase=\"7\""),
        "writer must emit phase attribute for a phased link");
  }

  @Test
  void writerOmitsPhaseAttributeWhenZero() {
    addLinkWithPhase(0);
    final String xml = new WriterSpr(this.manager).generateString();
    assertFalse(xml.contains("phase="),
        "writer must omit phase attribute for a zero-phase link");
  }

  @Test
  void readerConvertsPhaseAttributeToInstruction() throws Exception {
    final String xml = "<universe><links><type><class>"
        + "<link nodes=\"0 1\" phase=\"12\"/>"
        + "</class></type></links></universe>";
    final String out = new ReaderSPR().translateString(xml);
    assertTrue(out.contains("PH:12"),
        "reader must convert phase attribute to PH instruction, got: " + out);
  }
}
