package me.c7dev.dexterity.util;

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
	
	/**
	 * Retrieves the vector along the plane of the block face from the center of the face to the precise location of the click
	 * @return Unmodifiable vector
	 */
	public Vector getOffsetFromFaceCenter() {
		return offset_.clone();
	}
	
	/**
	 * Retrieves the precise location on the block face that the player clicked
	 * @return Unmodifiable location
	 */
	public Location getClickLocation() {
		return loc_.clone();
	}
	
	/**
	 * Retrieves the center of the block display entity
	 * @return Unmodifiable location
	 */
	public Location getDisplayCenterLocation() {
		return center_.clone();
	}
	
	/**
	 * Retrieves a unit vector that is perpendicular to the clicked block face
	 * @return Unmodifiable unit vector
	 */
	public Vector getNormal() {
		return normal_.clone();
	}
	
	/**
	 * Retrieves the entity's relative up direction basis vector
	 * @return Unmodifiable unit vector
	 */
	public Vector getUpDir() {
		return up_dir_.clone();
	}
	
	/**
	 * Retrieves the entity's relative east direction basis vector
	 * @return Unmodifiable unit vector
	 */
	public Vector getEastDir() {
		return east_dir_.clone();
	}
	
	/**
	 * Retrieves the entity's relative south direction basis vector
	 * @return Unmodifiable unit vector
	 */
	public Vector getSouthDir() {
		return south_dir_.clone();
	}
	
	/**
	 * Retrieves the distance from the player's eye location to the block face in units of blocks
	 * @return
	 */
	public double getDistance() {
		return dist_;
	}
	
	/**
	 * Retrieves a wrapper for the calculated roll offset, if roll is used in the block display's rotation
	 * @return
	 */
	public RollOffset getRollOffset() {
		return ro;
	}

}
