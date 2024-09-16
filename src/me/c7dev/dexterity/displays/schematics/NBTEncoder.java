package me.c7dev.dexterity.displays.schematics;

import java.util.ArrayList;
import java.util.List;

import me.c7dev.dexterity.util.BinaryTag;

public class NBTEncoder {
	
	private List<Byte> data = new ArrayList<>();
	private byte count = 7, buffer = 0;
	
	public NBTEncoder() {
		
	}
	
	public void append(BinaryTag tag) {
		for (byte i = 0; i < tag.length; i++) {
			if (tag.bits.get(i)) buffer |= (1 << count);
			count--;
			if (count < 0) {
				data.add(buffer);
				buffer = 0;
				count = 7;
			}
		}
	}
	
	public byte[] getData() {
		if (buffer != 0) data.add(buffer);
		
		byte[] ret = new byte[data.size()];
		for (int i = 0; i < data.size(); i++) {
			ret[i] = data.get(i).byteValue();
		}
		return ret;
	}

}
