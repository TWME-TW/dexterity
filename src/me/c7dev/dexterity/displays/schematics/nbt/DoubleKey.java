package me.c7dev.dexterity.displays.schematics.nbt;

import me.c7dev.dexterity.displays.schematics.nbt.NBT.NBTType;

public class DoubleKey {
	
	private double val;
	private NBTType type;

	public DoubleKey(NBTType type, double val) {
		this.val = val;
		this.type = type;
	}

	public boolean equals(Object o) {
		if (!(o instanceof DoubleKey)) return false;
		DoubleKey k = (DoubleKey) o;
		return k.getValue() == val && k.getType() == type;
	}

	public int hashCode() {
		return (int) val ^ type.hashCode();
	}

	public double getValue() {
		return val;
	}

	public NBTType getType() {
		return type;
	}
	
}
