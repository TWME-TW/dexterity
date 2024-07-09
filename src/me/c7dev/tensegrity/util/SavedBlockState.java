package me.c7dev.tensegrity.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class SavedBlockState {
	
	private Location loc;
	private BlockData state;
	
	public SavedBlockState(Block b) {
		loc = b.getLocation();
		state = b.getBlockData();
	}
	
	public SavedBlockState(Location loc, BlockData data) {
		this.loc = loc;
		state = data;
	}
	
	public Material getMaterial() {
		return state.getMaterial();
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public BlockData getData() {
		return state;
	}

}
