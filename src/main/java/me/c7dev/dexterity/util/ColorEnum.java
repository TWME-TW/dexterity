package me.c7dev.dexterity.util;

import org.bukkit.Color;

public enum ColorEnum {
	
	AQUA(Color.AQUA),
	BLACK(Color.BLACK),
	BLUE(Color.BLUE),
	FUCHSIA(Color.FUCHSIA),
	GRAY(Color.GRAY),
	GREEN(Color.GREEN),
	LIME(Color.LIME),
	MAROON(Color.MAROON),
	NAVY(Color.NAVY),
	OLIVE(Color.OLIVE),
	ORANGE(Color.ORANGE),
	PURPLE(Color.PURPLE),
	RED(Color.RED),
	SILVER(Color.SILVER),
	TEAL(Color.TEAL),
	WHITE(Color.WHITE),
	YELLOW(Color.YELLOW);
	
	private Color c;
	ColorEnum(Color c_) {
		c = c_;
	}
	
	public Color getColor() {
		return c;
	}

}
