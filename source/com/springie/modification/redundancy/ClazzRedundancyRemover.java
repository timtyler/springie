package com.springie.modification.redundancy;

import java.util.List;

import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;

public class ClazzRedundancyRemover {
	NodeManager node_manager;

	public ClazzRedundancyRemover(NodeManager node_manager) {
		this.node_manager = node_manager;
	}

	public void removeRedundancy() {
		removeRedundancyInClazzes();
		this.node_manager.each_has_its_own_clazz = false;
		this.node_manager.getLinkManager().each_has_its_own_clazz = false;
		this.node_manager.getFaceManager().each_has_its_own_clazz = false;
	}

	public void removeRedundancyInClazzes() {
		final List v = this.node_manager.clazz_factory.array;
		final int size = v.size();
		// Log.log("Initial classes: " + size);
		for (int i = size; --i >= 0;) {
			final Clazz nt = (Clazz) v.get(i);
			if (equalspreviousClazz(v, i)) {
				final int first = getFirstClazz(v, nt, i);
				replaceClazzWithPrevious(v, i, first);
			}
		}

		// Log.log("Remaining classes: " + v.size());
	}

	private void replaceClazzWithPrevious(List v, int old, int nww) {
		final Clazz c_old = (Clazz) v.get(old);
		final Clazz c_new = (Clazz) v.get(nww);

		replaceClassInNodes(c_old, c_new);
		replaceClassInLinks(c_old, c_new);
		replaceClassInPolygons(c_old, c_new);

		v.remove(old);
	}

	private void replaceClassInNodes(final Clazz c_old, final Clazz c_new) {
		final int n = this.node_manager.element.size();
		for (int i = n; --i >= 0;) {
			final Node node = (Node) this.node_manager.element.get(i);
			if (node.clazz.equals(c_old)) {
				node.clazz = c_new;
			}
		}
	}

	private void replaceClassInLinks(final Clazz c_old, final Clazz c_new) {
		final int n_o_p = this.node_manager.getLinkManager().element.size();
		for (int i = n_o_p; --i >= 0;) {
			final Link l = (Link) this.node_manager.getLinkManager().element.get(i);
			if (l.clazz.equals(c_old)) {
				l.clazz = c_new;
			}
		}
	}

	private void replaceClassInPolygons(final Clazz c_old, final Clazz c_new) {
		final int n_o_p = this.node_manager.getFaceManager().element.size();
		for (int i = n_o_p; --i >= 0;) {
			final Face p = (Face) this.node_manager.getFaceManager().element.get(i);
			if (p.clazz.equals(c_old)) {
				p.clazz = c_new;
			}
		}
	}

	private int getFirstClazz(List v, Clazz t, int max) {
		for (int i = max; --i >= 0;) {
			if (t.equals(v.get(i))) {
				return i;
			}
		}

		return -1;
	}

	private boolean equalspreviousClazz(List v, int max) {
		final Clazz t = (Clazz) v.get(max);

		return getFirstClazz(v, t, max) > 0;
	}
}