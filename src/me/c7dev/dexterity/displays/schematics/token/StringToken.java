package me.c7dev.dexterity.displays.schematics.token;

public class StringToken extends Token {
	
	private String val;
	
	public StringToken(TokenType type, String x) {
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
	
	public String toString() {
		return getType() + "=\"" + val + "\"";
	}

}
