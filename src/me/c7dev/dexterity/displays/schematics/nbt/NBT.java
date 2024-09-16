package me.c7dev.dexterity.displays.schematics.nbt;

import java.util.UUID;

import me.c7dev.dexterity.util.BinaryTag;

public class NBT {
	
	public enum NBTType {
		//for backwards compatability, the order is FINAL - add new tokens to bottom.
		DISPLAY_DELIMITER,
		SECTION_DELIMITER,
		BLOCK_DELIMITER,
		DATA_END,
		ASCII,
		SPECIFIER,
		BLOCKDATA,
		HASH,
		SIGNATURE,
		DX, //offset from center
		DY,
		DZ,
		YAW,
		PITCH,
		ROLL,
		SCALE_X,
		SCALE_Y,
		SCALE_Z,
		TRANS_X, //if not implied by scale
		TRANS_Y,
		TRANS_Z,
		QUAT_X,
		QUAT_Y,
		QUAT_Z,
		QUAT_W,
		ROFFSET_X,
		ROFFSET_Y,
		ROFFSET_Z,
		//add any new types here
	}
	
	private UUID uuid = UUID.randomUUID();
	private NBTType type;
	private BinaryTag tag;
	private int depth = 0;
	
	public NBT(NBTType p) {
		type = p;
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	public boolean equals(Object o) {
		if (o instanceof NBT) {
			NBT n = (NBT) o;
			return n.getUniqueId().equals(uuid);
		}
		return false;
	}
	
	public int hashCode() {
		return uuid.hashCode();
	}
	
	public Object getValue() {
		return null;
	}
	
	public BinaryTag getTag() {
		return tag;
	}
	
	public NBTType getType() {
		return type;
	}
	
	public void setTag(BinaryTag s) {
		tag = s;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int d) {
		depth = d;
	}
	
}
