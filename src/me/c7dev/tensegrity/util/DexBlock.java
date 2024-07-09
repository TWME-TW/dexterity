package me.c7dev.tensegrity.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;

public class DexBlock {

	private BlockDisplay entity;
	private DexTransformation trans;
	private DexterityDisplay disp;
	//private boolean armor_stand;
	
	//public static final Vector AS_OFFSET = new Vector(0.5, -0.5, 0.5);
	
	public DexBlock(Block display, DexterityDisplay d) {
		disp = d;
		trans = DexTransformation.newDefaultTransformation();
		this.entity = display.getLocation().getWorld().spawn(display.getLocation().clone().add(0.5, 0.5, 0.5), BlockDisplay.class, (spawned) -> {
			spawned.setBlock(display.getBlockData());
			spawned.setTransformation(trans.build());
			spawned.setTeleportDuration(1);
		});
		d.getPlugin().setMappedDisplay(this);
		display.setType(Material.AIR);
	}
	public DexBlock(BlockDisplay bd, DexterityDisplay d) {
		entity = bd;
		disp = d;
		bd.setTeleportDuration(1);
		trans = new DexTransformation(bd.getTransformation());
		d.getPlugin().setMappedDisplay(this);
	}
	public DexBlock(DexBlockState state) {
		disp = state.getDisplay();
		trans = state.getTransformation();
		entity = state.getLocation().getWorld().spawn(state.getLocation(), BlockDisplay.class, a -> {
			a.setBlock(state.getBlock());
			a.setTransformation(state.getTransformation().build());
			a.setTeleportDuration(1);
		});
		if (state.getDisplay() != null) {
			state.getDisplay().getPlugin().setMappedDisplay(this);
			state.getDisplay().getBlocks().add(this);
		} else Bukkit.broadcastMessage("disp null");
	}
	
	public BlockDisplay getEntity() {
		return this.entity;
	}
	
	@Deprecated
	public void setDexterityDisplay(DexterityDisplay d) {
		disp = d;
	}
	
	public DexterityDisplay getDexterityDisplay() {
		return disp;
	}
		
	public DexTransformation getTransformation() {
		return trans;
	}
	
	public DexBlockState getState() {
		return new DexBlockState(this);
	}
	
	public void loadState(DexBlockState state) {
		if (entity.isDead()) return;
		trans = state.getTransformation();
		entity.teleport(state.getLocation());
		entity.setTransformation(state.getTransformation().build());
		entity.setBlock(state.getBlock());
	}
		
	public void setTransformation(DexTransformation dt) {
		trans = dt;
		entity.setTransformation(dt.build());
	}
	
	public void updateTransformation() {
		entity.setTransformation(trans.build());
	}
	
	public void setRotation(double xrad, double yrad, double zrad) {		
		Quaternionf ql = new Quaternionf(Math.sin(xrad/4), Math.sin(yrad/4), Math.sin(zrad/4), Math.cos(zrad/4)*Math.cos(yrad/4)*Math.cos(xrad/4));
		trans.setLeftRotation(ql).setRightRotation(ql);
		updateTransformation();
	}
	
	public void teleport(Location loc) {
		entity.teleport(loc);
	}
	public void move(Vector v) {
		entity.teleport(entity.getLocation().add(v));
	}
	public void move(double x, double y, double z) {
		entity.teleport(entity.getLocation().add(x, y, z));
	}
	public void setBrightness(int blockLight, int skyLight) {
		//entity.setBrightness(new Brightness(blockLight, skyLight));
	}
	
	public void remove() {
		disp.getBlocks().remove(this);
		disp.getPlugin().clearMappedDisplay(entity.getUniqueId());
		entity.remove();
		if (disp.getBlocks().size() == 0 && disp.getSubdisplays().size() == 0) {
			disp.remove(false);
		}
	}
	
	public Location getLocation() {
		return entity.getLocation();
	}

}
