package me.c7dev.dexterity.util;

import java.util.Base64;
import java.util.BitSet;

import org.bukkit.Bukkit;

import me.c7dev.dexterity.displays.schematics.TokenEncoder;

public class BinaryTag { //fixed length bit set
	
	public BitSet bits;
	public int length;
	
	public BinaryTag(int length) {
		this.length = length;
		this.bits = new BitSet(length);
	}
	
	public BinaryTag(String s) {
		String[] split = s.split(",");
		String hex_str = split[0];
		byte[] data = DexUtils.hexStringToBytes(hex_str);
		int len = -1;
		if (split.length > 1) len = Integer.parseInt(split[1]);
		else len = data.length*8;
		
		int shift = (8 - (len % 8));
		if (shift == 8) shift = 0;
		
		bits = new BitSet(len);
		length = len;
		
		byte mask = Byte.MIN_VALUE;
		int index = 0;
		for(int i = 0; i < length; i++) {
			byte b = i < data.length ? data[i] : 0;
			
			for (int j = 0; j < 8; j++) {
				if (index == length) return;
				int masked = b & mask;
				if (masked != 0) bits.set(index);
				b <<= 1;
				index++;
			}
		}
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
		TokenEncoder enc = new TokenEncoder();
		enc.append(this);
		String r = DexUtils.bytesToHex(enc.getData());
		if (length % 8 != 0) r += "," + length;
		return r;
	}

}
