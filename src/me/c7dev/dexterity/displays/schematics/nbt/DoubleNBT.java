package me.c7dev.dexterity.displays.schematics.nbt;

public class DoubleNBT extends NBT {
	
	private double val;
	
	public DoubleNBT(NBTType type, double x) {
		super(type);
		val = x;
	}
	
	@Override
	public Object getValue() {
		return val;
	}
	
	public double getDoubleValue() {
		return val;
	}

}
