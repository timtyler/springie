package com.springie.elements.base;

public class BaseType {
	public boolean hidden;
	public boolean selected;
	public boolean disabled;
	public int radius;
	public int log_mass;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (disabled ? 1231 : 1237);
		result = prime * result + (hidden ? 1231 : 1237);
		result = prime * result + log_mass;
		result = prime * result + radius;
		result = prime * result + (selected ? 1231 : 1237);
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
		BaseType other = (BaseType) obj;
		if (disabled != other.disabled)
			return false;
		if (hidden != other.hidden)
			return false;
		if (log_mass != other.log_mass)
			return false;
		if (radius != other.radius)
			return false;
		if (selected != other.selected)
			return false;
		return true;
	}
}
