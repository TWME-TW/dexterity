package me.c7dev.dexterity.displays.schematics;

import java.util.ArrayList;

import me.c7dev.dexterity.util.DexBlockState;

/**
 * Holds the blocks list and needed metadata before spawning the display
 */
public class SimpleDisplayState {
	
	private String label;
	private ArrayList<DexBlockState> blocks = new ArrayList<>();
	
	public SimpleDisplayState(String label) {
		this.label = label;
	}
	
	public SimpleDisplayState(String label, ArrayList<DexBlockState> blocks) {
		this.label = label;
		this.blocks = blocks;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void addBlock(DexBlockState s) {
		if (s.getBlock() != null) blocks.add(s);
	}
	
	public void setLabel(String s) {
		label = s;
	}

	public ArrayList<DexBlockState> getBlocks() {
		return blocks;
	}
	
}
