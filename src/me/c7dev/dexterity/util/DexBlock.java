package me.c7dev.dexterity.util;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import me.c7dev.dexterity.displays.DexterityDisplay;

public class DexBlock {

	private BlockDisplay entity;
	private DexTransformation trans;
	private DexterityDisplay disp;
	private float roll = 0;
	//private boolean armor_stand;
	
	//public static final Vector AS_OFFSET = new Vector(0.5, -0.5, 0.5);
	
	public static final int TELEPORT_DURATION = 2;
	
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
		this(bd, d, 0f);
	}
	
	@Deprecated //must manually subtract roll offset, used in placing db for efficiency
	public DexBlock(BlockDisplay bd, DexterityDisplay d, float roll) {
		entity = bd;
		disp = d;
		this.roll = roll;
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
	
	public void loadRoll(HashMap<OrientationKey, RollOffset> cache) {
		Quaternionf r = trans.getLeftRotation();
		
		OrientationKey key = new OrientationKey(trans.getScale().getX(), trans.getScale().getY(), trans.getScale().getZ(), r);
		RollOffset cached = cache.get(key);
		if (cached != null) {
			trans.setRollOffset(cached.getOffset());
			trans.getDisplacement().subtract(DexUtils.hadimard(cached.getOffset(), trans.getScale()));
			roll = cached.getRoll();
		} else {
			if (r.w != 0) {
				if (r.x == 0 && r.y == 0 && r.z != 0) {
					RollOffset c = new RollOffset(r);
					trans.setRollOffset(c.getOffset());
					trans.getDisplacement().subtract(DexUtils.hadimard(c.getOffset(), trans.getScale()));
					roll = c.getRoll();
					cache.put(key, c);
				}
			}
		}
	}
	
	public void loadRoll() { //async
		Quaternionf r = trans.getLeftRotation();
		
		if (r.w != 0) {
			if (r.x == 0 && r.y == 0 && r.z != 0) {
				RollOffset c = new RollOffset(r);
				trans.setRollOffset(c.getOffset());
				trans.getDisplacement().subtract(c.getOffset());
				roll = c.getRoll();
			}
		}
	}
	
	public BlockDisplay getEntity() {
		return this.entity;
	}
	
	@Deprecated
	public void setDexterityDisplay(DexterityDisplay d) {
		if (d == null) return;
		disp = d;
	}
	
	public float getRoll() {
		return roll;
	}
	
	public void setRoll(float f) { //TODO potential optimization is to store same vec, quaternion ref for many db
		if (Math.abs(f - roll) < 0.0000001) return;
		RollOffset c = new RollOffset(f);
		trans.setLeftRotation(c.getQuaternion());
		trans.setRollOffset(c.getOffset());
		roll = f;
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
		return entity.getLocation().add(trans.getDisplacement()).add(trans.getScale().clone().multiply(0.5));
	}

}
