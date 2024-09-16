package me.c7dev.dexterity.displays.schematics.nbt;

public class StringNBT extends NBT {
	
	private String val;
	
	public StringNBT(NBTType type, String x) {
		super(type);
		val = x;
	}
	
	@Override
	public Object getValue() {
		return val;
	}
	
	public String getStringValue() {
		return val;
	}

}
