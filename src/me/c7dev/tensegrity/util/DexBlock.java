package me.c7dev.tensegrity.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class DexBlock {

	private BlockDisplay entity;
	private DexTransformation trans;
	private DexterityDisplay disp;
	private float roll = 0;
	//private boolean armor_stand;
	
	//public static final Vector AS_OFFSET = new Vector(0.5, -0.5, 0.5);
	
	private static final double ROOT2INV = (Math.sqrt(2)/2), PI4 = Math.PI/4;
	private static final int TELEPORT_DURATION = 2;
	
	public DexBlock(Block display, DexterityDisplay d) {
		disp = d;
		trans = DexTransformation.newDefaultTransformation();
		this.entity = display.getLocation().getWorld().spawn(display.getLocation().clone().add(0.5, 0.5, 0.5), BlockDisplay.class, (spawned) -> {
			spawned.setBlock(display.getBlockData());
			spawned.setTransformation(trans.build());
			spawned.setTeleportDuration(TELEPORT_DURATION);
		});
		d.getPlugin().setMappedDisplay(this);
		display.setType(Material.AIR);
	}
	public DexBlock(BlockDisplay bd, DexterityDisplay d) {
		entity = bd;
		disp = d;
		bd.setTeleportDuration(TELEPORT_DURATION);
		trans = new DexTransformation(bd.getTransformation());
		d.getPlugin().setMappedDisplay(this);
	}
	public DexBlock(DexBlockState state) {
		disp = state.getDisplay();
		trans = state.getTransformation();
		entity = state.getLocation().getWorld().spawn(state.getLocation(), BlockDisplay.class, a -> {
			a.setBlock(state.getBlock());
			a.setTransformation(state.getTransformation().build());
			a.setTeleportDuration(TELEPORT_DURATION);
		});
		roll = state.getRoll();
		if (state.getDisplay() != null) {
			state.getDisplay().getPlugin().setMappedDisplay(this);
			state.getDisplay().getBlocks().add(this);
		}
	}
	
	public void loadRoll() { //async
		Quaternionf r = trans.getLeftRotation();
		if (r.w != 0) {
			if (r.x == 0 && r.y == 0 && r.z != 0) {
				double frad = Math.acos(-r.w) * 2;
				roll = (float) Math.toDegrees(frad);
				Vector offset = new Vector(0.5 - (ROOT2INV*Math.cos(frad + PI4)), 0.5 - (ROOT2INV*Math.sin(frad+PI4)), 0);
				trans.setRollOffset(offset);
				trans.getDisplacement().subtract(offset);
			}
		}
	}
	
	public BlockDisplay getEntity() {
		return this.entity;
	}
	
	@Deprecated
	public void setDexterityDisplay(DexterityDisplay d) {
		disp = d;
	}
	
	public float getRoll() {
		return roll;
	}
	
	public void setRoll(float f) { //TODO potential optimization is to store same vec, quaternion ref for many db
		if (Math.abs(f - roll) < 0.0000001) return;
		roll = f;
		float frad = (float) Math.toRadians(f);
		Quaternionf ql = new Quaternionf(new AxisAngle4f(frad, 0f, 0f, 1f));
		trans.setLeftRotation(ql);
		trans.setRollOffset(new Vector(0.5 - (ROOT2INV*Math.cos(frad + PI4)), 0.5 - (ROOT2INV*Math.sin(frad+PI4)), 0));
		updateTransformation();
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
		roll = state.getRoll();
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
	
	public void teleport(Location loc) {
		entity.teleport(loc);
	}
	public void move(Vector v) {
		entity.teleport(entity.getLocation().add(v));
	}
	public void move(double x, double y, double z) {
		entity.teleport(entity.getLocation().add(x, y, z));
	}
//	public void setBrightness(int blockLight, int skyLight) {
//		entity.setBrightness(new Brightness(blockLight, skyLight));
//	}
	
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
