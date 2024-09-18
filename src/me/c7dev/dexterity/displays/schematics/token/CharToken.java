package me.c7dev.dexterity.displays.schematics.token;

public class CharToken extends Token {
	
	private char val;
	
	public CharToken(char x) {
		super(TokenType.ASCII);
		val = x;
	}
	
	@Override
	public Object getValue() {
		return val;
	}
	
	public char getCharValue() {
		return val;
	}
	
	public String toString() {
		return getType() + "=" + val;
	}

}
