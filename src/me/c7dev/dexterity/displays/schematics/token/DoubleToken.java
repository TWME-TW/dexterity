package me.c7dev.dexterity.displays.schematics.token;

public class DoubleToken extends Token {
	
	private double val;
	
	public DoubleToken(TokenType type, double x) {
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
	
	public String toString() {
		return getType() + "=" + val;
	}

}
