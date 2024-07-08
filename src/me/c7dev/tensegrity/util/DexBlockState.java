package me.c7dev.tensegrity.util;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class DexBlockState {
	
	private Location loc;
	private UUID uuid;
	private DexTransformation trans;
	private BlockData block;
	private DexterityDisplay disp;
	
	public DexBlockState(DexBlock db) {
		loc = db.getLocation().clone();
		uuid = db.getEntity().getUniqueId();
		trans = db.getTransformation().clone();
		block = db.getEntity().getBlock();
		disp = db.getDexterityDisplay();
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	public DexTransformation getTransformation() {
		return trans;
	}
	
	public BlockData getBlock() {
		return block;
	}
	
	public DexterityDisplay getDisplay() {
		return disp;
	}

}
