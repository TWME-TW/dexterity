package me.c7dev.dexterity.displays.schematics.nbt;

public class CharNBT extends NBT {
	
	private char val;
	
	public CharNBT(char x) {
		super(NBTType.ASCII);
		val = x;
	}
	
	@Override
	public Object getValue() {
		return val;
	}
	
	public char getDoubleValue() {
		return val;
	}
	

}
