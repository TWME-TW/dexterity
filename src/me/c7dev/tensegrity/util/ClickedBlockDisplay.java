package me.c7dev.tensegrity.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

public class BlockDisplayFace {
	
	private BlockDisplay b;
	private BlockFace bf_;
	private Vector offset_;
	private Location loc_, center_;
	
	public BlockDisplayFace(BlockDisplay block, BlockFace blockFace, Vector offset, Location loc, Location centerLoc) {
		b = block;
		bf_ = blockFace;
		offset_ = offset;
		loc_ = loc;
		center_ = centerLoc;
	}
	
	public BlockDisplay getBlockDisplay() {
		return b;
	}
	
	public BlockFace getBlockFace() {
		return bf_;
	}
	
	public Vector getOffsetFromFaceCenter() {
		return offset_.clone();
	}
	
	public Location getClickLocation() {
		return loc_.clone();
	}
	
	public Location getDisplayCenterLocation() {
		return center_.clone();
	}

}
