package me.c7dev.tensegrity.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

public class ClickedBlockDisplay {
	
	private BlockDisplay b;
	private BlockFace bf_;
	private Vector offset_, normal_;
	private Location loc_, center_;
	private double dist_;
	
	public ClickedBlockDisplay(BlockDisplay block, BlockFace blockFace, Vector offset, Location loc, Location centerLoc, Vector normal, double dist) {
		b = block;
		bf_ = blockFace;
		offset_ = offset;
		loc_ = loc;
		center_ = centerLoc;
		normal_ = normal;
		dist_ = dist;
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
	
	public Vector getNormal() {
		return normal_.clone();
	}
	
	public double getDistance() {
		return dist_;
	}

}
