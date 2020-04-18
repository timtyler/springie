package uk.org.fdl.object;

import java.util.Vector;

class FDLObjectCollection extends FDLObject {  
  public Vector children;
  public String separator = " ";

  public Vector getChildren() {
    return this.children;
  }

  public void setChildren(Vector children) {
    this.children = children;
  }

  public void add(Vector children) {
    ensureChildrenExist();
    
    final int children_size = this.children.size();
    for (int i = 0; i < children_size; i++) {
      final FDLObject element = (FDLObject) children.elementAt(i);
      add(element);
    }
  }

  public void add(FDLObject child) {
    ensureChildrenExist();

    this.children.addElement(child);
  }

  private void ensureChildrenExist() {
    if (this.children == null) {
      this.children = new Vector();
    }
  }

  public void addContentsOf(FDLObjectCollection child) {
    ensureChildrenExist();

    for (int i = 0; i < child.children.size(); i++) {
      final Object tag = child.children.elementAt(i);
      this.children.addElement(tag);
    }
  }
}