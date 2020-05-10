package com.springie.elements.nodes;

import com.springie.elements.base.BaseType;
import com.springie.utilities.random.Hortensius32Fast;

public class NodeType extends BaseType {
	public boolean pinned;
	public int charge;
	public int counter;

	static Hortensius32Fast static_rnd = new Hortensius32Fast();

	protected NodeType() {
		setSize(18);
	}

	protected NodeType(NodeType current) {
		makeEqualTo(current);
	}

	public void setMass(int log_mass) {
		this.log_mass = log_mass;
	}

	public void setSize(int r) {
		this.radius = r;
	}

	public void makeEqualTo(NodeType t) {
		this.log_mass = t.log_mass;
		this.charge = t.charge;
		this.hidden = t.hidden;
		this.radius = t.radius;
		this.pinned = t.pinned;
		this.selected = t.selected;
		this.counter = t.counter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + charge;
		result = prime * result + counter;
		result = prime * result + (pinned ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeType other = (NodeType) obj;
		if (charge != other.charge)
			return false;
		if (counter != other.counter)
			return false;
		if (pinned != other.pinned)
			return false;
		return true;
	}
}
