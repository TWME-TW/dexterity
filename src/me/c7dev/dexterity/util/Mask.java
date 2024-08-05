package me.c7dev.dexterity.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

public class Mask {
	
	private List<Material> blocks = new ArrayList<>();
	private boolean negative = false;
	
	public Mask() {
		
	}
	
	public Mask(Material m) {
		blocks.add(m);
	}
	
	public void setNegative(boolean b) {
		negative = b;
	}

	public List<Material> getBlocks() {
		return blocks;
	}
	
	public boolean isAllowed(Material m) {
		if (blocks.size() == 0) return true;
		boolean included = blocks.contains(m);
		if (negative) included = !included;
		return included;
	}

}
