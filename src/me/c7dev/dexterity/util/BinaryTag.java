package me.c7dev.dexterity.util;

import java.util.BitSet;

import me.c7dev.dexterity.displays.schematics.NBTEncoder;

public class BinaryTag { //fixed length bit set
	
	public BitSet bits;
	public int length;
	
	public BinaryTag(int length) {
		this.length = length;
		this.bits = new BitSet(length);
	}
	
	public BinaryTag(BitSet bits, int length) {
		this.bits = bits;
		this.length = length;
	}
	
	public String toString() {
		StringBuilder r = new StringBuilder();
		for (int i = 0; i < length; i++) {
			r.append(bits.get(i) ? "1" : "0");
		}
		return r.toString();
	}
	
	public String serialize() {
		NBTEncoder enc = new NBTEncoder();
		enc.append(this);
		String r = DexUtils.bytesToHex(enc.getData());
		if (length % 8 != 0) r += "," + length;
		return r;
	}

}
