package me.c7dev.tensegrity.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

public class BlockDisplayFace {
	
	private BlockDisplay b;
	private BlockFace bf_;
	private Vector offset_;
	private Location loc_;
	
	public BlockDisplayFace(BlockDisplay block, BlockFace blockFace, Vector offset, Location loc) {
		b = block;
		bf_ = blockFace;
		offset_ = offset;
		loc_ = loc;
	}
	
	public BlockDisplay getBlockDisplay() {
		return b;
	}
	
	public BlockFace getBlockFace() {
		return bf_;
	}
	
	public Vector getOffsetFromFaceCenter() {
		return offset_;
	}
	
	public Location getLocation() {
		return loc_;
	}

}
