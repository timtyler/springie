package com.springie.modification.redundancy;

import java.util.ArrayList;

import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.NodeManager;

public class TypeRedundancyRemoverLink {
  NodeManager node_manager;

  public TypeRedundancyRemoverLink(NodeManager node_manager) {
    this.node_manager = node_manager;
  }

  public void removeRedundancy() {
    removeRedundancyInLinkTypes();
    this.node_manager.getLinkManager().each_has_its_own_type = false;
  }

  public void removeRedundancyInLinkTypes() {
    final ArrayList v = this.node_manager.getLinkManager().link_type_factory.array;
    final int size = v.size();
    for (int i = size; --i >= 0;) {
      final LinkType nt = (LinkType) v.get(i);
      if (equalspreviousLinkType(v, i)) {
        final int first = getFirstLinkType(v, nt, i);
        replaceLinkTypeWithPrevious(v, i, first);
      }
    }

    this.node_manager.getLinkManager().each_has_its_own_type = false;
  }

  private void replaceLinkTypeWithPrevious(ArrayList v, int old, int nww) {
    final LinkType nt_old = (LinkType) v.get(old);
    final LinkType nt_nww = (LinkType) v.get(nww);
    final int n_o_l = this.node_manager.getLinkManager().element.size();
    for (int i = n_o_l; --i >= 0;) {
      final Link l = (Link) this.node_manager.getLinkManager().element.get(i);
      if (l.type.equals(nt_old)) {
        l.type = nt_nww;
      }
    }

    v.remove(old);
  }

  private int getFirstLinkType(ArrayList v, LinkType t, int max) {
    for (int i = max; --i >= 0;) {
      if (t.equals(v.get(i))) {
        return i;
      }
    }

    return -1;
  }

  private boolean equalspreviousLinkType(ArrayList v, int max) {
    final LinkType nt = (LinkType) v.get(max);

    return getFirstLinkType(v, nt, max) > 0;
  }
}
