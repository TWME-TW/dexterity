package me.c7dev.dexterity.util;

import java.util.UUID;

import org.bukkit.Color;
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
		uuid = db.getUniqueId();
		trans = db.getTransformation().clone();
		block = db.getEntity().getBlock();
		disp = db.getDexterityDisplay();
		roll = db.getRoll();
	}
	
	public DexBlockState(Location loc, UUID uuid, DexTransformation trans, BlockData block, DexterityDisplay disp, float roll, Color glow) {
		this.loc = loc;
		this.uuid = uuid;
		this.trans = trans;
		this.block = block;
		this.disp = disp;
		this.roll = roll;
		//TODO glow
	}
	
	public Location getLocation() {
		return loc;
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	public void setUniqueId(UUID u) {
		uuid = u;
	}
	
	public DexTransformation getTransformation() {
		return trans;
	}
	
	public BlockData getBlock() {
		return block;
	}
	
	public void setBlock(BlockData bd) {
		block = bd;
	}
	
	public DexterityDisplay getDisplay() {
		return disp;
	}
	
	public void setDisplay(DexterityDisplay d) {
		disp = d;
	}
	
	public void setRoll(float f) {
		roll = f;
	}
	
	public float getRoll() {
		return roll;
	}

}
