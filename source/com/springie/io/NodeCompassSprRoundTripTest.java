package com.springie.io;

import com.springie.context.ContextManager;
import com.springie.demos.CompassPoint;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.io.in.readers.spr.ReaderSPR;
import com.springie.io.in.readers.tensegrity.ReaderTens;
import com.springie.io.out.writers.spr.WriterSpr;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A node's compass heading must survive an SPR save/load round trip:
 * WriterSpr emits a compass attribute on the node tag, ReaderSPR turns it
 * into a CD instruction, and ReaderTens applies it to the node.
 */
class NodeCompassSprRoundTripTest {

  private NodeManager manager;

  @BeforeEach
  void setUp() {
    assumeTrue(!GraphicsEnvironment.isHeadless(), "needs a display");
    this.manager = new NodeManager();
    ContextManager.setNodeManager(this.manager);
    this.manager.initialSetUp();
  }

  private Node addNode(final CompassPoint heading) {
    final NodeType type = this.manager.node_type_factory.getNew();
    final Clazz clazz = this.manager.clazz_factory.getNew(0xFFFFFFFF);
    final Node node = this.manager.addNewAgent(new Point3D(0, 0, 0), clazz,
        type);
    node.compass = heading;
    return node;
  }

  @Test
  void writerEmitsCompassAttributeWhenSet() {
    addNode(CompassPoint.N);
    final String xml = new WriterSpr(this.manager).generateString();
    assertTrue(xml.contains("compass=\"N\""),
        "writer must emit compass attribute for a headed node");
  }

  @Test
  void writerOmitsCompassAttributeWhenNull() {
    addNode(null);
    final String xml = new WriterSpr(this.manager).generateString();
    assertFalse(xml.contains("compass="),
        "writer must omit compass attribute for a heading-less node");
  }

  @Test
  void readerConvertsCompassAttributeToInstruction() throws Exception {
    final String xml = "<universe><nodes><type><class>"
        + "<node compass=\"S\"/>"
        + "</class></type></nodes></universe>";
    final String out = new ReaderSPR().translateString(xml);
    assertTrue(out.contains("CD:S"),
        "reader must convert compass attribute to CD instruction, got: "
            + out);
  }

  @Test
  void headingSurvivesFullRoundTrip() throws Exception {
    addNode(CompassPoint.E);
    addNode(null);
    final String xml = new WriterSpr(this.manager).generateString();
    final String intermediate = new ReaderSPR().translateString(xml);

    final NodeManager loaded = new NodeManager();
    ContextManager.setNodeManager(loaded);
    loaded.initialSetUp();
    final char[] buf = intermediate.toCharArray();
    ReaderTens.interpretBuffer(loaded, buf, 0, 0, 0, 256);

    assertEquals(2, loaded.element.size(), "both nodes must load");
    assertEquals(CompassPoint.E, ((Node) loaded.element.get(0)).compass,
        "headed node must keep its heading");
    assertNull(((Node) loaded.element.get(1)).compass,
        "heading-less node must stay heading-less");
  }
}
