package me.c7dev.tensegrity.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class SavedBlockState {
	
	private Material mat;
	private Location loc;
	private BlockData state;
	
	public SavedBlockState(Block b) {
		mat = b.getType();
		loc = b.getLocation();
		state = b.getBlockData();
	}
	
	public Material getMaterial() {
		return mat;
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public BlockData getData() {
		return state;
	}

}
