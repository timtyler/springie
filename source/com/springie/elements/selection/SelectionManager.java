package com.springie.elements.selection;

import com.springie.context.ContextMananger;
import com.springie.elements.base.BaseElement;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.utilities.general.Executor;
import com.springie.utilities.general.StringMatcher;

public final class SelectionManager {
  private SelectionManager() {
    // ...
  }

  // public static String getPrefixOfSelection() {
  // final Executor ex = new Executor() {
  // public Object execute(Object parameter) {
  // final String name = ((BaseElement) parameter).name;
  // if (name != null) {
  // final String current = (String) this.output;
  // String prefix = SelectionManager.getSharedPrefix(current, name);
  // this.output = prefix;
  // }
  // return null;
  // }
  // };
  // SelectionManager.applyToEachSelectedObject(ex);
  //
  // return (String) ex.output;
  // }

  public static void applyToEachSelectedObject(Executor ex) {
    applyToEachSelectedNode(ex);
    applyToEachSelectedLink(ex);
    applyToEachSelectedFace(ex);
  }

  public static void applyToEachSelectedNode(Executor ex) {
    final Executor ex2 = getSelectionExectorWrapper(ex);

    applyToEachNode(ex2);
  }

  public static void applyToEachSelectedLink(Executor ex) {
    final Executor ex2 = getSelectionExectorWrapper(ex);

    applyToEachLink(ex2);
  }

  public static void applyToEachSelectedFace(Executor ex) {
    final Executor ex2 = getSelectionExectorWrapper(ex);

    applyToEachFace(ex2);
  }

  private static Executor getSelectionExectorWrapper(Executor ex) {
    final Executor ex_copy = ex;
    final Executor ex2 = new Executor(ex.input) {
      public Object execute(Object parameter) {
        final BaseElement base = (BaseElement) parameter;
        if (base.isSelected()) {
          ex_copy.execute(base);
        }
        return null;
      }
    };
    return ex2;
  }

  public static void applyToEachObject(Executor ex) {
    applyToEachNode(ex);
    applyToEachLink(ex);
    applyToEachFace(ex);
  }

  private static void applyToEachFace(Executor ex) {
    final NodeManager node_manager = ContextMananger.getNodeManager();
    if (node_manager == null) {
      return;
    }
    final FaceManager face_manager = node_manager.getFaceManager();
    final int number = face_manager.element.size();
    for (int counter = number; --counter >= 0;) {
      final Face candidate = (Face) face_manager.element.elementAt(counter);
      ex.execute(candidate);
    }
  }

  private static void applyToEachLink(Executor ex) {
    final NodeManager node_manager = ContextMananger.getNodeManager();
    if (node_manager == null) {
      return;
    }
    final LinkManager link_manager = node_manager.getLinkManager();
    final int number = link_manager.element.size();
    for (int i = number; --i >= 0;) {
      final Link candidate = (Link) link_manager.element.elementAt(i);
      ex.execute(candidate);
    }
  }

  private static void applyToEachNode(Executor ex) {
    final NodeManager node_manager = ContextMananger.getNodeManager();
    if (node_manager == null) {
      return;
    }
    final int number_of_nodes = node_manager.element.size();
    for (int counter = number_of_nodes; --counter >= 0;) {
      final Node candidate = (Node) node_manager.element.elementAt(counter);
      ex.execute(candidate);
    }
  }

  public static String combineSelection(final StringMatcher matcher) {
    final Executor ex = new Executor() {
      public Object execute(Object parameter) {
        final String name = ((BaseElement) parameter).name;
        if (name != null) {
          final String current = (String) this.output;
          String prefix = matcher.combine(current, name);
          this.output = prefix;
        }
        return null;
      }
    };
    SelectionManager.applyToEachSelectedObject(ex);

    return (String) ex.output;
  }
}

