package me.c7dev.dexterity.displays.schematics.nbt;

import me.c7dev.dexterity.displays.schematics.nbt.NBT.NBTType;

public class StringKey {
	
	private String val;
	private NBTType type;

	public StringKey(NBTType type, String val) {
		this.val = val;
		this.type = type;
	}

	public boolean equals(Object o) {
		if (!(o instanceof StringKey)) return false;
		StringKey k = (StringKey) o;
		return k.getValue().hashCode() == val.hashCode() && k.getType() == type;
	}

	public int hashCode() {
		return val.hashCode() ^ type.hashCode();
	}

	public String getValue() {
		return val;
	}

	public NBTType getType() {
		return type;
	}

}
