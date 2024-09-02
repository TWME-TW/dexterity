package me.c7dev.dexterity.util;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import me.c7dev.dexterity.displays.DexterityDisplay;

public class DexBlock {

	private UUID uuid;
	private BlockDisplay entity;
	private DexTransformation trans;
	private DexterityDisplay disp;
	private float roll = 0;
	private Vector tempv;
	
	public static final int TELEPORT_DURATION = 2;
	
	/**
	 * Convert a block into block display
	 * @param block The block to convert
	 * @param d The selection to register the new block display under
	 */
	public DexBlock(Block block, DexterityDisplay d) {
		disp = d;
		trans = DexTransformation.newDefaultTransformation();
		entity = d.getPlugin().spawn(block.getLocation().clone().add(0.5, 0.5, 0.5), BlockDisplay.class, spawned -> {
			spawned.setBlock(block.getBlockData());
			spawned.setTransformation(trans.build());
		});
		uuid = entity.getUniqueId();
		d.getPlugin().setMappedDisplay(this);
		block.setType(Material.AIR);
	}
	
	/**
	 * Create a wrapper for a block display that is part of a selection
	 * @param bd The block display to register
	 * @param d The selection to register the new block display under
	 */
	public DexBlock(BlockDisplay bd, DexterityDisplay d) {
		this(bd, d, 0f);
	}
	
	/**
	 * Create a wrapper for a block display that is part of a selection.
	 * Must manually subtract the {@link RollOffset} afterwards
	 * 
	 * @param bd The block display to register
	 * @param d The selection to register the new block display under
	 * @param roll The roll, in degrees, that will forced into the wrapper
	 */
	@Deprecated //must manually subtract roll offset, used in placing db for efficiency
	public DexBlock(BlockDisplay bd, DexterityDisplay d, float roll) {
		entity = bd;
		disp = d;
		uuid = bd.getUniqueId();
		this.roll = roll;
		if (!d.getPlugin().isLegacy()) bd.setTeleportDuration(TELEPORT_DURATION);
		trans = new DexTransformation(bd.getTransformation());
		d.getPlugin().setMappedDisplay(this);
	}
	
	/**
	 * Spawn a block display wrapper based on a previously recorded state
	 * 
	 * @param state
	 */
	public DexBlock(DexBlockState state) {
		disp = state.getDisplay();
		trans = state.getTransformation();
		entity = state.getDisplay().getPlugin().spawn(state.getLocation(), BlockDisplay.class, a -> {
			a.setBlock(state.getBlock());
			a.setTransformation(state.getTransformation().build());
		});
		uuid = state.getUniqueId();
		roll = state.getRoll();
		if (state.getDisplay() != null) {
			state.getDisplay().getPlugin().setMappedDisplay(this);
			state.getDisplay().addBlock(this);
		}
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	/**
	 * Calculate the roll, recommended to do async (done automatically when plugin is enabled)
	 * @param cache Modifyable cache for similar rotation orientations
	 */
	public void loadRoll(HashMap<OrientationKey, RollOffset> cache) {
		Quaternionf r = trans.getLeftRotation();
		
		OrientationKey key = new OrientationKey(trans.getScale().getX(), trans.getScale().getY(), r);
		RollOffset cached = cache.get(key);
		if (cached != null) {
			trans.getDisplacement().add(trans.getRollOffset());
			trans.setRollOffset(cached.getOffset());
			trans.getDisplacement().subtract(cached.getOffset());
			roll = cached.getRoll();
		} else {
			if (r.w != 0) {
				if (r.x == 0 && r.y == 0 && r.z != 0) {
					RollOffset c = new RollOffset(r, trans.getScale());
					trans.getDisplacement().add(trans.getRollOffset());
					trans.setRollOffset(c.getOffset());
					trans.getDisplacement().subtract(c.getOffset());
					roll = c.getRoll();
					cache.put(key, c);
				}
			}
		}
	}
	
	/**
	 * Calculate the roll, recommended to do async (done automatically when plugin is enabled)
	 */
	public void loadRoll() { //async
		Quaternionf r = trans.getLeftRotation();
		
		if (r.w != 0) {
			if (r.x == 0 && r.y == 0 && r.z != 0) {
				RollOffset c = new RollOffset(r, trans.getScale());
				trans.getDisplacement().add(trans.getRollOffset());
				trans.setRollOffset(c.getOffset());
				trans.getDisplacement().subtract(c.getOffset());
				roll = c.getRoll();
			}
		}
	}
	
	public BlockDisplay getEntity() {
		return this.entity;
	}
	
	/**
	 * Get the temporary vector, used in processing
	 */
	public Vector getTempVector() {
		return tempv;
	}
	
	/**
	 * Set the temporary vector, used in processing
	 */
	public void setTempVector(Vector v) {
		tempv = v;
	}
	
	@Deprecated
	public void setDexterityDisplay(DexterityDisplay d) {
		if (d == null) return;
		disp = d;
	}
	
	/**
	 * Gets the roll in degrees. Yaw and pitch can be retrieved from the entity's location
	 * @return The roll in degrees
	 */
	public float getRoll() {
		return roll;
	}
	
	/**
	 * Set a new transformation for the given roll
	 * @param f The roll in degrees
	 */
	public void setRoll(float f) { //TODO potential optimization is to store same vec, quaternion ref for many db
		if (Math.abs(f - roll) < 0.0000001) return;
		RollOffset c = new RollOffset(f, trans.getScale());
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
	
	/**
	 * Loads in another state without spawning a new entity
	 * @param state
	 */
	public void loadState(DexBlockState state) {
		if (entity.isDead()) return;
		trans = state.getTransformation();
		roll = state.getRoll();
		entity.teleport(state.getLocation());
		entity.setTransformation(state.getTransformation().build());
		entity.setBlock(state.getBlock());
	}
		
	/**
	 * Sets the transformation wrapper and updates the entity
	 * @param dt
	 */
	public void setTransformation(DexTransformation dt) {
		trans = dt;
		entity.setTransformation(dt.build());
	}
	
	/**
	 * Updates the entity's transformation to the current values of its mutable {@link DexTransformation}
	 */
	public void updateTransformation() {
		
		entity.setTransformation(trans.build());
	}
	
	public void teleport(Location loc) {
		entity.teleport(loc);
	}
	
	/**
	 * Move the block display entity by an offset
	 * @param v
	 */
	public void move(Vector v) {
		entity.teleport(entity.getLocation().add(v));
	}
	
	/**
	 * Move the block display entity by an offset
	 * @param x Distance in blocks
	 * @param y Distance in blocks
	 * @param z Distance in blocks
	 */
	public void move(double x, double y, double z) {
		entity.teleport(entity.getLocation().add(x, y, z));
	}
	
//	public void setBrightness(int blockLight, int skyLight) {
//		entity.setBrightness(new Brightness(blockLight, skyLight));
//	}
	
	/**
	 * Kill entity and unregister from selection
	 */
	public void remove() {
		disp.removeBlock(this);
		disp.getPlugin().clearMappedDisplay(entity.getUniqueId());
		entity.remove();
		if (disp.getBlocks().length == 0 && disp.getSubdisplayCount() == 0) {
			disp.remove(false);
		}
	}
	
	/**
	 * Roll-offset adjusted location of the center of the block display entity.
	 * This is not necessarily the entity's location if other plugins are being used.
	 * 
	 * @return Unmodifiable location of the center of the block display
	 */
	public Location getLocation() {
		return entity.getLocation().add(trans.getDisplacement()).add(trans.getScale().clone().multiply(0.5));
	}

}
