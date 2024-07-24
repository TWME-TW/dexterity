package me.c7dev.tensegrity.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

public class ClickedBlockDisplay {
	
	private BlockDisplay b;
	private BlockFace bf_;
	private Vector offset_, normal_, up_dir_, east_dir_, south_dir_;
	private Location loc_, center_;
	private double dist_;
	private RollOffset ro = null;
	
	private Vector getNormal(BlockFace f, Vector up, Vector east, Vector south) {
		switch(f) {
		case UP: return up.clone().normalize();
		case DOWN: return up.clone().multiply(-1).normalize();
		case SOUTH: return south.clone().normalize();
		case NORTH: return south.clone().multiply(-1).normalize();
		case EAST: return east.clone().normalize();
		case WEST: return east.clone().multiply(-1).normalize();
		default: return new Vector(0, 0, 0);
		}
	}
	
	public ClickedBlockDisplay(BlockDisplay block, BlockFace blockFace, Vector offset, Location loc, Location centerLoc, 
			Vector up_dir, Vector east_dir, Vector south_dir, double dist) {
		b = block;
		bf_ = blockFace;
		offset_ = offset;
		loc_ = loc;
		center_ = centerLoc;
		up_dir_ = up_dir.normalize();
		east_dir_ = east_dir.normalize();
		south_dir_ = south_dir.normalize();
		normal_ = getNormal(blockFace, up_dir, east_dir, south_dir);
		dist_ = dist;
	}
	
	public void setRollOffset(RollOffset ro) {
		this.ro = ro;
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
	
	public Vector getUpDir() {
		return up_dir_.clone();
	}
	
	public Vector getEastDir() {
		return east_dir_.clone();
	}
	
	public Vector getSouthDir() {
		return south_dir_.clone();
	}
	
	public double getDistance() {
		return dist_;
	}
	
	public RollOffset getRollOffset() {
		return ro;
	}

}
