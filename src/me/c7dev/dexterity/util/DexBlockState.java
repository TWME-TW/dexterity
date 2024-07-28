package me.c7dev.dexterity.util;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import me.c7dev.dexterity.displays.DexterityDisplay;

public class DexBlockState {
	
	private Location loc;
	private UUID uuid;
	private DexTransformation trans;
	private BlockData block;
	private DexterityDisplay disp;
	private float roll;
	
	public DexBlockState(DexBlock db) {
		loc = db.getEntity().getLocation().clone();
		uuid = db.getEntity().getUniqueId();
		trans = db.getTransformation().clone();
		block = db.getEntity().getBlock();
		disp = db.getDexterityDisplay();
		roll = db.getRoll();
	}
	
	public void setDisplay(DexterityDisplay d) {
		disp = d;
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
	
	public float getRoll() {
		return roll;
	}

}
